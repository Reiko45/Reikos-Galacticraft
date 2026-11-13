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

    /** A sealable BuildCraft pipe put down as a player would: the block, then its setPlacedBy (which gives it the pipe). */
    private static void placeSealable(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block) {
        helper.setBlock(pos, block);
        final BlockPos abs = helper.absolutePos(pos);
        block.setPlacedBy(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null, new ItemStack(block));
    }

    /**
     * 1.12.2 BlockEnclosed's BuildCraft transport pipes: the stone pipe inside works with BuildCraft's own pipes beside
     * it, whether placed by a player or by a command (then it gets its pipe on the next tick): a hopper above the first
     * fills a chest at the far end.
     */
    @GameTest(template = AIR, timeoutTicks = 300)
    public static void sealedTransportPipesCarryItems(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        final BlockPos first = new BlockPos(1, 1, 1);
        placeSealable(helper, first, GCBlocks.SEALABLE_STONE_TRANSPORT_PIPE.get());
        helper.assertTrue(helper.getBlockEntity(first) != null
                && helper.getBlockEntity(first).getType() == BuildCraftCompat.pipeHolderType(), "BuildCraft's pipe holder inside");
        helper.assertTrue(BuildCraftCompat.hasPipe(helper.getLevel(), helper.absolutePos(first)), "placed with its pipe");
        BuildCraftTestSupport.placePipe(helper, new BlockPos(2, 1, 1), "pipe_stone_item");
        final BlockPos second = new BlockPos(3, 1, 1);
        helper.setBlock(second, GCBlocks.SEALABLE_STONE_TRANSPORT_PIPE.get());
        helper.assertFalse(BuildCraftCompat.hasPipe(helper.getLevel(), helper.absolutePos(second)), "no pipe yet when set by command");
        final BlockPos chest = new BlockPos(4, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        final BlockPos hopperPos = new BlockPos(1, 2, 1);
        helper.setBlock(hopperPos, Blocks.HOPPER); // pointing down into the first sealed pipe
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(BuildCraftCompat.hasPipe(helper.getLevel(), helper.absolutePos(second)), "the commanded one got its pipe");
            final net.minecraft.world.level.block.entity.HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
            hopper.setItem(0, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8));
            helper.succeedWhen(() -> {
                final net.minecraft.world.level.block.entity.ChestBlockEntity box = helper.getBlockEntity(chest);
                helper.assertValueEqual(box.countItem(net.minecraft.world.item.Items.COBBLESTONE), 8, "cobblestone in the chest");
            });
        });
    }

    /**
     * 1.12.2 BlockEnclosed's BuildCraft fluid pipes: oil pumped into a sealed stone fluid pipe (from the side a tank is on,
     * so the pipe is connected there) comes out into a Galacticraft Fluid Tank on the other side.
     */
    @GameTest(template = AIR, timeoutTicks = 300)
    public static void sealedFluidPipesCarryFluids(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        helper.setBlock(new BlockPos(0, 1, 1), GCBlocks.FLUID_TANK.get());
        final BlockPos pipe = new BlockPos(1, 1, 1);
        placeSealable(helper, pipe, GCBlocks.SEALABLE_STONE_FLUID_PIPE.get());
        final BlockPos tankPos = new BlockPos(2, 1, 1);
        helper.setBlock(tankPos, GCBlocks.FLUID_TANK.get());
        final int[] given = {0};
        helper.onEachTick(() -> {
            final IFluidHandler in = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pipe), Direction.WEST);
            if (in != null && given[0] < 1000) {
                given[0] += in.fill(new FluidStack(GCFluids.OIL.get(), Math.min(100, 1000 - given[0])), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.succeedWhen(() -> {
            final io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity tank = helper.getBlockEntity(tankPos);
            helper.assertTrue(given[0] > 0, "the sealed pipe took no oil");
            helper.assertTrue(tank.tank().getFluidAmount() >= 500 && tank.tank().getFluid().getFluid() == GCFluids.OIL.get(),
                    "the tank has " + tank.tank().getFluidAmount() + " mB");
        });
    }

    /**
     * 1.12.2 BlockEnclosed's BuildCraft kinesis pipes: MJ from a wooden kinesis pipe through a sealed stone and a sealed
     * golden kinesis pipe into an Energy Storage Module's input.
     */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void sealedKinesisPipesCarryMj(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        final BlockPos wood = new BlockPos(1, 1, 1);
        BuildCraftTestSupport.placePipe(helper, wood, "pipe_wood_power");
        placeSealable(helper, new BlockPos(2, 1, 1), GCBlocks.SEALABLE_STONE_KINESIS_PIPE.get());
        placeSealable(helper, new BlockPos(3, 1, 1), GCBlocks.SEALABLE_GOLDEN_KINESIS_PIPE.get());
        final EnergyStorageBlockEntity module = module(helper, new BlockPos(4, 1, 1));
        final long[] given = {0L};
        helper.onEachTick(() -> given[0] += BuildCraftTestSupport.giveMj(helper.getLevel(), helper.absolutePos(wood), Direction.WEST,
                buildcraftreborn.api.mj.MjAPI.MJ));
        helper.succeedWhen(() -> {
            helper.assertTrue(given[0] > 0L, "the wooden kinesis pipe took no power");
            helper.assertTrue(module.energy().getStoredGJ() > 0.0F, "the module stored " + module.energy().getStoredGJ());
        });
    }

    /**
     * 1.12.2 BlockEnclosed: the sealable pipes keep the air in, and break into themselves (not the BuildCraft pipe
     * inside); without BuildCraft Reborn they are plain tin blocks, kept out of the creative tab and recipe viewers.
     */
    @GameTest(template = AIR)
    public static void sealedBuildCraftPipesSealAndDropThemselves(GameTestHelper helper) {
        final net.minecraft.world.level.block.Block block = GCBlocks.SEALABLE_GOLDEN_KINESIS_PIPE.get();
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, block);
        helper.assertTrue(((io.github.reiko45.galacticraft.oxygen.PartialSealable) block).isSealed(helper.getLevel(), helper.absolutePos(pos),
                helper.getBlockState(pos), Direction.UP), "sealed");
        helper.assertValueEqual(io.github.reiko45.galacticraft.registry.GCCreativeTabs.hiddenWithoutItsMod(block.asItem()),
                !BuildCraftCompat.loaded(), "hidden only without BuildCraft Reborn");
        if (!BuildCraftCompat.loaded()) {
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) == null, "no block entity without BuildCraft Reborn");
            helper.succeed();
            return;
        }
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(BuildCraftCompat.hasPipe(helper.getLevel(), helper.absolutePos(pos)), "a pipe inside");
            helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
            helper.assertItemEntityPresent(block.asItem(), pos, 2.0D);
            helper.assertItemEntityNotPresent(BuildCraftTestSupport.item("pipe_gold_power"), pos, 2.0D);
            helper.succeed();
        });
    }
}
