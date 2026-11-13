package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.compat.buildcraft.BuildCraftCompat;
import io.github.reiko45.galacticraft.compat.buildcraft.BuildCraftTestSupport;
import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * BuildCraft Reborn support (1.12.2's BuildCraft compatibility). Run with BuildCraft Reborn by passing
 * -PbuildcraftJar=&lt;its jar&gt; to runGameTestServer; without it the tests that need it pass at once.
 */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuildCraftTests {
    private static final String AIR = "air_room";

    private BuildCraftTests() {
    }

    private static boolean skip(GameTestHelper helper) {
        if (!BuildCraftCompat.loaded()) {
            helper.succeed();
            return true;
        }
        return false;
    }

    /** An Energy Storage Module facing north: power in on its right (west), out on its left (east). */
    private static EnergyStorageBlockEntity module(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, GCBlocks.ENERGY_STORAGE_MODULE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    /** 1.12.2 TileBaseUniversalElectrical as an IMjReceiver: a creative engine against the module's input fills it. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void engineChargesMachine(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        final EnergyStorageBlockEntity module = module(helper, new BlockPos(1, 1, 1));
        final BlockPos enginePos = new BlockPos(0, 1, 1);
        helper.setBlock(enginePos, BuildCraftTestSupport.block("engine_creative"));
        helper.assertValueEqual(BuildCraftTestSupport.aimEngine(helper.getBlockEntity(enginePos)), Direction.EAST, "engine facing the module");
        helper.assertTrue(BuildCraftTestSupport.requestedMj(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.WEST) > 0.0D,
                "the module's input asks for MJ");
        helper.assertTrue(BuildCraftTestSupport.requestedMj(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.EAST) < 0.0D,
                "its output takes none");
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(module.energy().getStoredGJ() > 0.0F, "stored " + module.energy().getStoredGJ());
            helper.assertTrue(BuildCraftTestSupport.storedMj(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.UP) > 0.0D,
                    "BuildCraft reads it in MJ");
        });
    }

    /** 1.12.2 TileEntityAluminumWire as an IMjReceiver: an engine on a wire powers the network. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void engineChargesWireNetwork(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        final EnergyStorageBlockEntity module = module(helper, new BlockPos(4, 1, 1));
        helper.setBlock(new BlockPos(3, 1, 1), GCBlocks.ALUMINUM_WIRE.get());
        helper.setBlock(new BlockPos(2, 1, 1), GCBlocks.ALUMINUM_WIRE.get());
        final BlockPos enginePos = new BlockPos(1, 1, 1);
        helper.setBlock(enginePos, BuildCraftTestSupport.block("engine_creative"));
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            helper.assertValueEqual(BuildCraftTestSupport.aimEngine(helper.getBlockEntity(enginePos)), Direction.EAST, "engine facing the wire");
            helper.succeedWhen(() -> helper.assertTrue(module.energy().getStoredGJ() > 0.0F, "stored " + module.energy().getStoredGJ()));
        });
    }

    /** 1.12.2 EnergyUtil: a module's output gives MJ to a BuildCraft laser beside it, at 16 gJ to the MJ. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void machinePowersLaser(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        final EnergyStorageBlockEntity module = module(helper, new BlockPos(1, 1, 1));
        module.energy().setStored(10_000.0F);
        final BlockPos laser = new BlockPos(2, 1, 1);
        helper.setBlock(laser, BuildCraftTestSupport.block("laser"));
        helper.succeedWhen(() -> {
            final double mj = BuildCraftTestSupport.storedMj(helper.getLevel(), helper.absolutePos(laser), null);
            helper.assertTrue(mj > 0.0D, "laser holds " + mj + " MJ");
            final float spent = 10_000.0F - module.energy().getStoredGJ();
            helper.assertTrue(Math.abs(spent - mj * 16.0D) < 1.0D, spent + " gJ for " + mj + " MJ");
        });
    }

    /**
     * Wrenches both ways (1.12.2 ItemUniversalWrench was BuildCraft's IToolWrench, and BlockAdvanced took BuildCraft's
     * wrench): the Standard Wrench is in c:tools/wrench, and BuildCraft's wrench turns a Galacticraft machine.
     */
    @GameTest(template = AIR)
    public static void wrenchesWorkBothWays(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(GCItems.STANDARD_WRENCH.get()).is(Tags.Items.TOOLS_WRENCH), "the Standard Wrench is a c:tools/wrench");
        if (skip(helper)) {
            return;
        }
        helper.assertTrue(BuildCraftTestSupport.isWrenchToBuildCraft(new ItemStack(GCItems.STANDARD_WRENCH.get())), "BuildCraft takes it as a wrench");
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GCBlocks.ELECTRIC_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        // A fake player: one joining the level would trip BuildCraft Reborn 2.1.0's unguarded volume_boxes payload
        final ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        final ItemStack wrench = new ItemStack(BuildCraftTestSupport.item("wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        final BlockPos abs = helper.absolutePos(pos);
        final InteractionResult result = player.gameMode.useItemOn(player, helper.getLevel(), wrench, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0.0D, 0.5D, 0.0D), Direction.UP, abs, false));
        helper.assertTrue(result.consumesAction(), "the click was taken: " + result);
        helper.assertTrue(helper.getBlockState(pos).getValue(MachineBlock.FACING) != Direction.NORTH,
                "turned to " + helper.getBlockState(pos).getValue(MachineBlock.FACING));
        helper.succeed();
    }

    /**
     * 1.12.2 GCFluids: Galacticraft fuel burns in BuildCraft's combustion engine at 5 MJ a tick for 9,000 ticks a bucket
     * (BuildcraftFuelRegistry.fuel.addFuel), over its plain c:fuel value; oil and fuel go both ways through the c: tags.
     */
    @GameTest(template = AIR)
    public static void fuelAndOilGoBothWays(GameTestHelper helper) {
        helper.assertTrue(GCFluids.FUEL.get().builtInRegistryHolder().is(GCFluidTags.FUEL), "Galacticraft fuel is c:fuel");
        helper.assertTrue(GCFluids.OIL.get().builtInRegistryHolder().is(GCFluidTags.OIL), "Galacticraft oil is c:oil");
        if (skip(helper)) {
            return;
        }
        final double[] fuel = BuildCraftTestSupport.fuel(GCFluids.FUEL.get());
        helper.assertTrue(fuel != null && fuel[0] == 5.0D && fuel[1] == 9_000.0D,
                "Galacticraft fuel burns at " + (fuel == null ? "nothing" : fuel[0] + " MJ/t for " + fuel[1] + " ticks"));
        // BuildCraft's oil into the Refinery, and its light fuel into the Fuel Loader
        final BlockPos refineryPos = new BlockPos(1, 1, 1);
        helper.setBlock(refineryPos, GCBlocks.REFINERY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final RefineryBlockEntity refinery = helper.getBlockEntity(refineryPos);
        final IFluidHandler refineryIn = refinery.fluidHandler(Direction.EAST); // oil in on its left
        helper.assertTrue(refineryIn != null && refineryIn.fill(new FluidStack(BuildCraftTestSupport.fluid("oil"), 1000), IFluidHandler.FluidAction.SIMULATE) > 0,
                "the Refinery takes BuildCraft's oil");
        final BlockPos loaderPos = new BlockPos(4, 1, 1);
        helper.setBlock(loaderPos, GCBlocks.FUEL_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final IFluidHandler loaderIn = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(loaderPos), Direction.WEST);
        helper.assertTrue(loaderIn != null && loaderIn.fill(new FluidStack(BuildCraftTestSupport.fluid("fuel_light"), 1000), IFluidHandler.FluidAction.SIMULATE) > 0,
                "the Fuel Loader takes BuildCraft's light fuel");
        final FuelLoaderBlockEntity loader = helper.getBlockEntity(loaderPos);
        helper.assertTrue(loader != null, "a loader");
        helper.succeed();
    }
}
