package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.data.GCWorldgenProvider;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.OilPocketFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 2b oil processing: refinery, tanks, canisters, pipes and oil generation. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidTests {
    private static final String ROOM = "machine_room";
    private static final IFluidHandler.FluidAction EXECUTE = IFluidHandler.FluidAction.EXECUTE;
    private static final IFluidHandler.FluidAction SIMULATE = IFluidHandler.FluidAction.SIMULATE;

    private FluidTests() {
    }

    private static IFluidHandler fluids(GameTestHelper helper, BlockPos rel, Direction side) {
        final IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(rel), side);
        if (handler == null) {
            throw new GameTestAssertPosException("no fluid handler on " + side, helper.absolutePos(rel), rel, helper.getTick());
        }
        return handler;
    }

    private static FluidStack oil(int amount) {
        return new FluidStack(GCFluids.OIL.get(), amount);
    }

    /** Oil in through the left (east when facing north), fuel out through the right (west), nothing elsewhere. */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void refineryTurnsOilIntoFuel(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(3, 1, 1);
        helper.setBlock(pos, GCBlocks.REFINERY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final RefineryBlockEntity refinery = helper.getBlockEntity(pos);
        refinery.energy().setStored(16_000.0F);

        helper.assertValueEqual(fluids(helper, pos, Direction.EAST).fill(oil(100), EXECUTE), 100, "oil accepted on the left");
        helper.assertValueEqual(fluids(helper, pos, Direction.WEST).fill(oil(100), SIMULATE), 0, "oil refused on the right");
        helper.assertValueEqual(fluids(helper, pos, Direction.EAST).drain(100, SIMULATE).getAmount(), 0, "oil cannot be drained");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.SOUTH) == null,
                "no fluid access on the back");
        helper.succeedWhen(() -> {
            final FluidStack fuel = fluids(helper, pos, Direction.WEST).drain(1000, SIMULATE);
            helper.assertTrue(fuel.is(GCFluids.FUEL.get()) && fuel.getAmount() >= 20, "refined fuel: " + fuel.getAmount());
            helper.assertValueEqual(refinery.oilTank().getFluidAmount() + refinery.fuelTank().getFluidAmount(), 100, "1 mB oil -> 1 mB fuel");
            helper.assertValueEqual(refinery.status(), MachineStatus.REFINING, "status while oil is left");
        });
    }

    /** Two stacked tanks hold 32,000 mB as one; the bottom fills first and is drained, then refilled from above. */
    @GameTest(template = ROOM, timeoutTicks = 100)
    public static void stackedTanksActAsOne(GameTestHelper helper) {
        final BlockPos bottom = new BlockPos(1, 1, 1);
        final BlockPos top = bottom.above();
        helper.setBlock(bottom, GCBlocks.FLUID_TANK.get());
        helper.setBlock(top, GCBlocks.FLUID_TANK.get());
        helper.assertBlockProperty(bottom, io.github.reiko45.galacticraft.block.FluidTankBlock.UP, true);

        helper.assertValueEqual(fluids(helper, top, Direction.UP).fill(oil(40_000), EXECUTE), 32_000, "filled through the top tank");
        final FluidTankBlockEntity lower = helper.getBlockEntity(bottom);
        final FluidTankBlockEntity upper = helper.getBlockEntity(top);
        helper.assertValueEqual(lower.tank().getFluidAmount(), 16_000, "bottom full");
        helper.assertValueEqual(upper.tank().getFluidAmount(), 16_000, "top full");
        helper.assertValueEqual(fluids(helper, top, Direction.UP).fill(new FluidStack(GCFluids.FUEL.get(), 10), SIMULATE), 0,
                "a different liquid is refused");

        // 1.12.2: draining takes from the bottom tank; the tank above then pours down into it.
        helper.assertValueEqual(fluids(helper, top, Direction.UP).drain(20_000, EXECUTE).getAmount(), 16_000, "drained the bottom tank");
        helper.succeedWhen(() -> {
            helper.assertValueEqual(lower.tank().getFluidAmount(), 16_000, "bottom after settling");
            helper.assertValueEqual(upper.tank().getFluidAmount(), 0, "top after settling");
        });
    }

    /** An empty canister filled with fuel becomes a fuel canister, and an emptied fuel canister an empty (oil) canister. */
    @GameTest(template = "empty")
    public static void canisterSwapsBetweenOilAndFuel(GameTestHelper helper) {
        final IFluidHandlerItem empty = new ItemStack(GCItems.OIL_CANISTER.get()).getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(empty != null, "canister has a fluid handler");
        helper.assertValueEqual(empty.fill(new FluidStack(GCFluids.FUEL.get(), 1500), EXECUTE), 1000, "fills 1,000 mB");
        final ItemStack fuel = empty.getContainer();
        helper.assertTrue(fuel.is(GCItems.FUEL_CANISTER.get()), "became a fuel canister");

        final IFluidHandlerItem full = fuel.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertValueEqual(full.fill(oil(100), SIMULATE), 0, "no oil into a fuel canister");
        helper.assertValueEqual(full.drain(1000, EXECUTE).getAmount(), 1000, "drains fully");
        helper.assertTrue(full.getContainer().is(GCItems.OIL_CANISTER.get()), "back to an empty canister");
        helper.succeed();
    }

    /** Tank -> pull pipe -> pipe -> pipe -> tank. */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void pullPipeMovesOilBetweenTanks(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 1), GCBlocks.FLUID_TANK.get());
        helper.setBlock(new BlockPos(4, 1, 1), GCBlocks.FLUID_TANK.get());
        final FluidTankBlockEntity source = helper.getBlockEntity(new BlockPos(0, 1, 1));
        final FluidTankBlockEntity target = helper.getBlockEntity(new BlockPos(4, 1, 1));
        source.tank().fill(oil(2_000), EXECUTE);
        final BlockState pipe = GCBlocks.FLUID_PIPE.get().defaultBlockState();
        helper.setBlock(new BlockPos(1, 1, 1), pipe.setValue(FluidPipeBlock.PULL, true));
        helper.setBlock(new BlockPos(2, 1, 1), pipe);
        helper.setBlock(new BlockPos(3, 1, 1), pipe);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(target.tank().getFluidAmount(), 2_000, "all oil arrived");
            helper.assertValueEqual(source.tank().getFluidAmount(), 0, "source emptied");
        });
    }

    /** Red and blue pipes do not join (a white pipe joins any colour), so nothing reaches the far tank. */
    @GameTest(template = ROOM, timeoutTicks = 100)
    public static void differentlyDyedPipesStaySeparate(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 1), GCBlocks.FLUID_TANK.get());
        helper.setBlock(new BlockPos(4, 1, 1), GCBlocks.FLUID_TANK.get());
        final FluidTankBlockEntity source = helper.getBlockEntity(new BlockPos(0, 1, 1));
        final FluidTankBlockEntity target = helper.getBlockEntity(new BlockPos(4, 1, 1));
        source.tank().fill(oil(2_000), EXECUTE);
        final BlockState pipe = GCBlocks.FLUID_PIPE.get().defaultBlockState();
        helper.setBlock(new BlockPos(1, 1, 1), pipe.setValue(FluidPipeBlock.PULL, true));
        helper.setBlock(new BlockPos(2, 1, 1), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.RED));
        helper.setBlock(new BlockPos(3, 1, 1), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.BLUE));
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(target.tank().getFluidAmount(), 0, "blue pipe is not part of the red network");
            helper.assertTrue(source.tank().getFluidAmount() < 2_000, "white+red network pulled oil");
            helper.succeed();
        });
    }

    /** The 1.12.2 pocket shape in solid stone, and the feature reaching Overworld biomes through the biome modifier. */
    @GameTest(template = "stone_room")
    public static void oilPocketsGenerate(GameTestHelper helper) {
        helper.assertTrue(OilPocketFeature.placePocket(helper.getLevel(), helper.absolutePos(new BlockPos(5, 3, 5)), 3), "pocket placed");
        int oil = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 10, 6, 10)) {
            if (helper.getBlockState(pos).is(GCFluids.OIL_BLOCK.get())) {
                oil++;
                final int dx = pos.getX() - 5;
                final int dy = pos.getY() - 3;
                final int dz = pos.getZ() - 5;
                helper.assertTrue(dx * dx + 3 * dy * dy + dz * dz <= 9 && Math.abs(dy) <= 1, "oil outside the ellipsoid at " + pos);
            }
        }
        // r = 3: 71 positions with |y| <= 1 satisfy x^2 + 3y^2 + z^2 <= 9 (none are next to air in solid stone).
        helper.assertValueEqual(oil, 71, "oil blocks");

        final Holder<PlacedFeature> feature = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .getHolderOrThrow(GCWorldgenProvider.OIL_POCKET_PLACED);
        final Biome plains = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        helper.assertTrue(plains.getGenerationSettings().features().get(GenerationStep.Decoration.LAKES.ordinal()).contains(feature),
                "plains generate oil pockets");
        helper.succeed();
    }
}
