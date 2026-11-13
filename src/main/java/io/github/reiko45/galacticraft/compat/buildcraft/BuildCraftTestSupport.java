package io.github.reiko45.galacticraft.compat.buildcraft;

import buildcraftreborn.api.mj.IMjReadable;
import buildcraftreborn.api.mj.IMjReceiver;
import buildcraftreborn.api.mj.MjAPI;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * BuildCraft Reborn's side of the GameTests (gametest.BuildCraftTests), only called with it installed. Its blocks and
 * items are looked up by name, and the few calls outside its API are made by reflection.
 */
public final class BuildCraftTestSupport {
    private BuildCraftTestSupport() {
    }

    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(BuildCraftCompat.MOD_ID, path));
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(BuildCraftCompat.MOD_ID, path));
    }

    public static Fluid fluid(String path) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(BuildCraftCompat.MOD_ID, path));
    }

    /** Turns a BuildCraft engine to the receiver beside it (TileEngineBase_BC8#rotateIfInvalid) and says where it faces. */
    @Nullable
    public static Direction aimEngine(BlockEntity engine) {
        try {
            engine.getClass().getMethod("rotateIfInvalid").invoke(engine);
            return (Direction) engine.getClass().getMethod("getCurrentFacing").invoke(engine);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BuildCraft Reborn's engine has changed", e);
        }
    }

    /** The MJ stored in a block that shows it (a laser, a Galacticraft machine), in MJ. */
    public static double storedMj(Level level, BlockPos pos, @Nullable Direction side) {
        final IMjReadable readable = level.getCapability(MjAPI.CAP_READABLE, pos, side);
        return readable == null ? -1.0D : readable.getStored() / (double) MjAPI.MJ;
    }

    /** What a Galacticraft block on that face asks of a BuildCraft engine, in MJ (-1 if it isn't an MJ receiver there). */
    public static double requestedMj(Level level, BlockPos pos, Direction side) {
        final IMjReceiver receiver = level.getCapability(MjAPI.CAP_RECEIVER, pos, side);
        return receiver == null ? -1.0D : receiver.getPowerRequested() / (double) MjAPI.MJ;
    }

    /** BuildCraft's own wrench test (EntityUtil#isWrench). */
    public static boolean isWrenchToBuildCraft(ItemStack stack) {
        try {
            final Method isWrench = Class.forName("buildcraftreborn.lib.misc.EntityUtil").getMethod("isWrench", ItemStack.class);
            return (boolean) isWrench.invoke(null, stack);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BuildCraft Reborn's wrench test has changed", e);
        }
    }

    /** A fluid's combustion engine values (BuildcraftFuelRegistry.fuel): {MJ a tick, ticks a bucket}, or null if it doesn't burn. */
    @Nullable
    public static double[] fuel(Fluid fluid) {
        try {
            final Object manager = Class.forName("buildcraftreborn.api.fuels.BuildcraftFuelRegistry").getField("fuel").get(null);
            final Object fuel = manager.getClass().getMethod("getFuel", FluidStack.class).invoke(manager, new FluidStack(fluid, 1000));
            if (fuel == null) {
                return null;
            }
            final Class<?> type = Class.forName("buildcraftreborn.api.fuels.IFuel");
            final long power = (long) type.getMethod("getPowerPerCycle").invoke(fuel);
            final int time = (int) type.getMethod("getTotalBurningTime").invoke(fuel);
            return new double[] {power / (double) MjAPI.MJ, time};
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BuildCraft Reborn's fuel registry has changed", e);
        }
    }
}
