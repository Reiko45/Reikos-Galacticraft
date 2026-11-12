package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.GeothermalGeneratorBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.GeothermalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarArrayControllerBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7f: the Venus machines, the Geothermal Generator and the Solar Array. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VenusMachineTests {
    private static final String GROUND = "venus_ground";

    private VenusMachineTests() {
    }

    /** Open the template's barrier ceiling to the sky. */
    private static void openSky(GameTestHelper helper) {
        for (int x = -1; x <= 20; x++) {
            for (int z = -1; z <= 20; z++) {
                for (int y = 27; y <= 30; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.BARRIER)) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
            }
        }
    }

    private static boolean canExtract(GameTestHelper helper, BlockPos rel, Direction side) {
        final IEnergyStorage storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(rel), side);
        return storage != null && storage.canExtract();
    }

    /**
     * TileEntityGeothermalGenerator: it needs a Vapor Spout right under it and Sulphuric Acid below that, through nothing
     * but air, less than 20 blocks under the generator.
     */
    @GameTest(template = GROUND)
    public static void geothermalNeedsSpoutOverAcid(GameTestHelper helper) {
        final BlockPos generator = new BlockPos(5, 21, 5);
        helper.setBlock(generator.below(), GCBlocks.VAPOR_SPOUT.get());
        final BlockPos acid = generator.below(19);
        helper.setBlock(acid, GCFluids.SULPHURIC_ACID_BLOCK.get());
        helper.assertTrue(GeothermalGeneratorBlockEntity.hasSpout(helper.getLevel(), helper.absolutePos(generator)), "acid 19 blocks down");
        helper.setBlock(acid, Blocks.AIR);
        helper.setBlock(generator.below(20), GCFluids.SULPHURIC_ACID_BLOCK.get());
        helper.assertFalse(GeothermalGeneratorBlockEntity.hasSpout(helper.getLevel(), helper.absolutePos(generator)), "acid 20 blocks down");
        helper.setBlock(generator.below(10), GCFluids.SULPHURIC_ACID_BLOCK.get());
        helper.setBlock(generator.below(5), Blocks.STONE);
        helper.assertFalse(GeothermalGeneratorBlockEntity.hasSpout(helper.getLevel(), helper.absolutePos(generator)), "rock in the shaft");
        helper.setBlock(generator.below(5), Blocks.AIR);
        helper.assertTrue(GeothermalGeneratorBlockEntity.hasSpout(helper.getLevel(), helper.absolutePos(generator)), "shaft open again");
        helper.setBlock(generator.below(), GCBlocks.VENUS_SOFT_ROCK.get());
        helper.assertFalse(GeothermalGeneratorBlockEntity.hasSpout(helper.getLevel(), helper.absolutePos(generator)), "no spout");
        // the wave: 115 at the start, between 30 and 200, near the top a quarter period in
        helper.assertValueEqual(GeothermalGeneratorBlockEntity.output(0L), 115, "mid-wave at t = 0");
        for (long t = 0; t < 400; t++) {
            final int watts = GeothermalGeneratorBlockEntity.output(t);
            helper.assertTrue(watts >= 30 && watts <= 200, "output " + watts + " at " + t);
        }
        helper.assertTrue(GeothermalGeneratorBlockEntity.output(79L) >= 199, "crest");
        helper.assertTrue(GeothermalGeneratorBlockEntity.output(236L) <= 31, "trough");
        helper.succeed();
    }

    /**
     * On a working spout the generator lights up, makes the wave's output, stores it and offers it on its left side
     * only; switched off it goes dark and stops.
     */
    @GameTest(template = GROUND, timeoutTicks = 200)
    public static void geothermalGeneratesOnSpout(GameTestHelper helper) {
        final BlockPos rel = new BlockPos(5, 6, 5);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(4, 0, 4), new BlockPos(6, 2, 6))) {
            helper.setBlock(pos, GCBlocks.VENUS_SOFT_ROCK.get());
        }
        helper.setBlock(new BlockPos(5, 1, 5), GCFluids.SULPHURIC_ACID_BLOCK.get());
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.AIR);
        helper.setBlock(rel.below(), GCBlocks.VAPOR_SPOUT.get());
        helper.setBlock(rel, GCBlocks.GEOTHERMAL_GENERATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final GeothermalGeneratorBlockEntity generator = (GeothermalGeneratorBlockEntity) helper.getBlockEntity(rel);
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(generator.hasValidSpout(), "found the spout");
            helper.assertTrue(helper.getBlockState(rel).getValue(GeothermalGeneratorBlock.ACTIVE), "looks active");
            final int expected = GeothermalGeneratorBlockEntity.output(helper.getLevel().getGameTime());
            helper.assertValueEqual(generator.generating(), expected, "output follows the wave");
            helper.assertTrue(generator.energy().getStoredGJ() > 0.0F, "stores energy");
            helper.assertTrue(canExtract(helper, rel, Direction.EAST), "power out on its left (east, facing north)");
            helper.assertFalse(canExtract(helper, rel, Direction.WEST) || canExtract(helper, rel, Direction.NORTH)
                    || canExtract(helper, rel, Direction.UP), "nowhere else");
            generator.toggleDisabled();
        });
        helper.runAtTickTime(28, () -> {
            helper.assertValueEqual(generator.generating(), 0, "switched off");
            helper.assertFalse(helper.getBlockState(rel).getValue(GeothermalGeneratorBlock.ACTIVE), "dark when off");
            generator.toggleDisabled();
            helper.assertTrue(generator.isDisabled(), "the button waits a second before it works again");
        });
        helper.runAtTickTime(50, () -> {
            generator.toggleDisabled();
            helper.assertFalse(generator.isDisabled(), "back on");
        });
        helper.runAtTickTime(53, () -> {
            helper.assertTrue(generator.generating() >= 30, "generating again");
            helper.succeed();
        });
    }

    /**
     * TileEntitySolarArrayController: it gathers the modules joined side by side to those on its left, right and back
     * (not in front, not stacked), counts the ones within 16 blocks across, and makes 6.3 gJ/t for each that sees the
     * sky. A roof over a module takes that one out; a roof high enough over the controller (1.12.2's odd column check)
     * stops the lot.
     */
    @GameTest(template = GROUND, timeoutTicks = 200, batch = "solar_array")
    public static void solarArrayCountsJoinedModules(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        helper.getLevel().setRainLevel(0.0F);
        helper.getLevel().setThunderLevel(0.0F);
        openSky(helper);
        final BlockPos controllerPos = new BlockPos(1, 1, 10);
        final BlockState module = GCBlocks.SOLAR_ARRAY_MODULE.get().defaultBlockState();
        helper.setBlock(new BlockPos(0, 1, 10), module); // left (west)
        helper.setBlock(new BlockPos(1, 1, 11), module); // back (south)
        for (int x = 2; x < 20; x++) {
            helper.setBlock(new BlockPos(x, 1, 10), module); // right (east), 18 long: the last two are out of range
        }
        helper.setBlock(new BlockPos(1, 1, 9), module); // in front: not joined
        helper.setBlock(new BlockPos(5, 2, 10), module); // stacked: not joined
        helper.setBlock(controllerPos, GCBlocks.SOLAR_ARRAY_CONTROLLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final SolarArrayControllerBlockEntity controller = (SolarArrayControllerBlockEntity) helper.getBlockEntity(controllerPos);
        helper.runAtTickTime(25, () -> {
            helper.assertValueEqual(controller.findModules().size(), 20, "joined modules");
            helper.assertValueEqual(controller.possibleSize(), 18, "in range");
            helper.assertValueEqual(controller.solarStrength(), 18, "in the sun");
            helper.assertValueEqual(controller.generating(), 113, "floor(18 * 6.3)");
            helper.assertTrue(canExtract(helper, controllerPos, Direction.NORTH), "power out of its front");
            helper.assertFalse(canExtract(helper, controllerPos, Direction.EAST) || canExtract(helper, controllerPos, Direction.SOUTH),
                    "nowhere else");
            helper.setBlock(new BlockPos(3, 3, 10), Blocks.STONE);
        });
        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(controller.possibleSize(), 18, "still in range");
            helper.assertValueEqual(controller.solarStrength(), 17, "one in the shade");
            helper.assertValueEqual(controller.generating(), 107, "floor(17 * 6.3)");
            helper.setBlock(controllerPos.above(2), Blocks.STONE);
        });
        // 1.12.2 looked at the column over the controller only from (its height above the world's floor) above it
        final BlockPos abs = helper.absolutePos(controllerPos);
        final int start = abs.getY() + (abs.getY() - helper.getLevel().getMinBuildHeight()) + 1;
        helper.runAtTickTime(75, () -> {
            helper.assertValueEqual(controller.solarStrength(), 17, "a low roof over the controller doesn't count");
            helper.assertTrue(start - helper.absolutePos(BlockPos.ZERO).getY() < 27, "roof height inside the template");
            helper.getLevel().setBlockAndUpdate(abs.atY(start), Blocks.STONE.defaultBlockState());
        });
        helper.runAtTickTime(100, () -> {
            helper.assertValueEqual(controller.solarStrength(), 0, "a high one does");
            helper.assertValueEqual(controller.generating(), 0, "nothing");
            helper.assertTrue(controller.energy().getStoredGJ() > 0.0F, "kept what it made");
            helper.succeed();
        });
    }
}
