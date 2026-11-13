package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.ArcLampBlock;
import io.github.reiko45.galacticraft.block.LightingPanelBlock;
import io.github.reiko45.galacticraft.block.SpaceGlassBlock;
import io.github.reiko45.galacticraft.block.airlock.AirLockControllerBlockEntity;
import io.github.reiko45.galacticraft.block.airlock.AirLockSealBlock;
import io.github.reiko45.galacticraft.block.concealed.ConcealedDetectorBlock;
import io.github.reiko45.galacticraft.block.concealed.ConcealedRedstoneBlock;
import io.github.reiko45.galacticraft.block.entity.ArcLampBlockEntity;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7g part 1: the space base blocks. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceBaseTests {
    private static final String AIR = "air_room";

    private SpaceBaseTests() {
    }

    private static BlockState at(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z));
    }

    /**
     * BlockSpaceGlass: panes join their neighbours (a straight run east-west, a corner), get a frame where they meet
     * other blocks, and seal air sideways but not up or down.
     */
    @GameTest(template = AIR)
    public static void spaceGlassJoinsAndSealsSideways(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 2, 2), GCBlocks.SPACE_GLASS_CLEAR.get());
        }
        final BlockState middle = at(helper, 3, 2, 2);
        helper.assertTrue(middle.getValue(SpaceGlassBlock.ROTATION).y() % 180 != 0, "an east-west run is turned: " + middle);
        helper.assertTrue(middle.getValue(SpaceGlassBlock.MODEL) == SpaceGlassBlock.GlassModel.STANDARD_S2A,
                "framed above and below (nothing there): " + middle.getValue(SpaceGlassBlock.MODEL));
        // a corner
        helper.setBlock(new BlockPos(4, 2, 3), GCBlocks.SPACE_GLASS_CLEAR.get());
        final SpaceGlassBlock.GlassModel corner = at(helper, 4, 2, 2).getValue(SpaceGlassBlock.MODEL);
        helper.assertTrue(corner == SpaceGlassBlock.GlassModel.CORNER_S2, "a framed corner: " + corner);
        // a lone pane on the floor and under a ceiling
        helper.setBlock(new BlockPos(8, 1, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 2, 8), GCBlocks.SPACE_GLASS_STRONG_TIN.get());
        helper.assertTrue(at(helper, 8, 2, 8).getValue(SpaceGlassBlock.MODEL).frameModel() != null, "a lone pane is framed");
        final BlockState pane = at(helper, 3, 2, 2);
        final BlockPos abs = helper.absolutePos(new BlockPos(3, 2, 2));
        helper.assertFalse(OxygenUtil.canBlockPassAir(helper.getLevel(), pane, abs, Direction.NORTH), "seals sideways");
        helper.assertTrue(OxygenUtil.canBlockPassAir(helper.getLevel(), pane, abs, Direction.UP), "not up and down");
        // breaking it gives the same glass back
        helper.assertTrue(GCBlocks.SPACE_GLASS_STRONG_TIN.get().asItem() != net.minecraft.world.item.Items.AIR, "has an item");
        helper.succeed();
    }

    /**
     * TileEntityAirLockController: a 4 x 4 ring of frames with a controller seals its 2 x 2 opening while no player is
     * near (by default it opens for anyone within a block), and with Invert Selection it does the opposite.
     */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void airLockSealsItsDoorway(GameTestHelper helper) {
        final int z = 6;
        for (int x = 3; x <= 6; x++) {
            for (int y = 1; y <= 4; y++) {
                if (x == 3 || x == 6 || y == 1 || y == 4) {
                    helper.setBlock(new BlockPos(x, y, z), x == 4 && y == 1 ? GCBlocks.AIR_LOCK_CONTROLLER.get() : GCBlocks.AIR_LOCK_FRAME.get());
                }
            }
        }
        final AirLockControllerBlockEntity controller = (AirLockControllerBlockEntity) helper.getBlockEntity(new BlockPos(4, 1, z));
        helper.startSequence().thenWaitUntil(() -> {
            for (int x = 4; x <= 5; x++) {
                for (int y = 2; y <= 3; y++) {
                    helper.assertBlockPresent(GCBlocks.AIR_LOCK_SEAL.get(), new BlockPos(x, y, z));
                }
            }
        }).thenExecute(() -> {
            helper.assertTrue(at(helper, 4, 2, z).getValue(AirLockSealBlock.CONNECTION) == AirLockSealBlock.Connection.Z,
                    "turned along the doorway: " + at(helper, 4, 2, z));
            helper.assertTrue(controller.isActive(), "shut");
            final BlockPos seal = helper.absolutePos(new BlockPos(4, 2, z));
            helper.assertFalse(OxygenUtil.canBlockPassAir(helper.getLevel(), at(helper, 4, 2, z), seal, Direction.UP), "a seal seals every way");
            controller.configure(false, true, 0, true, false);
        }).thenWaitUntil(() -> helper.assertTrue(at(helper, 4, 2, z).isAir() && at(helper, 5, 3, z).isAir(), "inverted: open with nobody near"))
                .thenSucceed();
    }

    /**
     * TileEntityArclamp: on the floor, aiming along it, it lights the air next to the surfaces in front of it with
     * Bright Air, and a redstone signal puts it all out again.
     */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void arcLampLightsTheRoom(GameTestHelper helper) {
        // a closed stone room (7 x 5 x 7 inside) keeps the light in the test
        for (BlockPos pos : BlockPos.betweenClosed(2, 0, 2, 10, 6, 10)) {
            if (pos.getX() == 2 || pos.getX() == 10 || pos.getY() == 0 || pos.getY() == 6 || pos.getZ() == 2 || pos.getZ() == 10) {
                helper.setBlock(pos, Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(6, 1, 6), GCBlocks.ARC_LAMP.get().defaultBlockState().setValue(ArcLampBlock.FACING, Direction.DOWN));
        final ArcLampBlockEntity lamp = (ArcLampBlockEntity) helper.getBlockEntity(new BlockPos(6, 1, 6));
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(lamp.isLit(), "lit");
            helper.assertTrue(lamp.litAir().size() > 10, "bright air over the floor: " + lamp.litAir().size());
            for (BlockPos pos : lamp.litAir()) {
                helper.assertTrue(helper.getLevel().getBlockState(pos).is(GCBlocks.BRIGHT_AIR.get()), "bright air at " + pos);
            }
            helper.setBlock(new BlockPos(6, 1, 7), Blocks.REDSTONE_BLOCK);
        });
        helper.runAtTickTime(10, () -> {
            helper.assertFalse(lamp.isLit(), "switched off");
            helper.assertTrue(lamp.litAir().isEmpty(), "all dark again");
            helper.succeed();
        });
    }

    /** BlockConcealedRedstone: carries a signal block to block, one weaker each step, and lights a lamp at its end. */
    @GameTest(template = AIR, timeoutTicks = 40)
    public static void concealedRedstoneCarriesASignal(GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 1, 1), Blocks.REDSTONE_LAMP);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.REDSTONE_BLOCK);
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), GCBlocks.CONCEALED_REDSTONE.get());
        }
        helper.succeedWhen(() -> {
            helper.assertValueEqual(at(helper, 2, 1, 1).getValue(ConcealedRedstoneBlock.POWER), 15, "full by the source");
            helper.assertValueEqual(at(helper, 5, 1, 1).getValue(ConcealedRedstoneBlock.POWER), 12, "weaker further on");
            helper.assertTrue(at(helper, 6, 1, 1).getValue(RedstoneLampBlock.LIT), "the lamp lights");
        });
    }

    /** BlockConcealedRepeater: a full-block repeater. */
    @GameTest(template = AIR, timeoutTicks = 40)
    public static void concealedRepeaterRepeats(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(2, 1, 1), GCBlocks.CONCEALED_REPEATER.get().defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.REDSTONE_LAMP);
        helper.succeedWhen(() -> {
            helper.assertTrue(at(helper, 2, 1, 1).getValue(RepeaterBlock.POWERED), "powered");
            helper.assertTrue(at(helper, 3, 1, 1).getValue(RedstoneLampBlock.LIT), "the lamp lights");
            helper.assertTrue(at(helper, 2, 1, 1).isCollisionShapeFullBlock(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1))), "a full block");
        });
    }

    /** TileEntityPlayerDetector: a signal of 15 while nobody is in front of it, none once a player comes. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void concealedDetectorSeesPlayers(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 1, 1);
        helper.setBlock(pos, GCBlocks.CONCEALED_DETECTOR.get().defaultBlockState().setValue(ConcealedDetectorBlock.FACING, 2));
        helper.runAtTickTime(30, () -> {
            helper.assertFalse(at(helper, 6, 1, 1).getValue(ConcealedDetectorBlock.DETECTED), "nobody yet");
            helper.assertValueEqual(helper.getLevel().getSignal(helper.absolutePos(pos), Direction.DOWN), 15, "a signal while empty");
        });
        final net.minecraft.server.level.ServerPlayer[] player = new net.minecraft.server.level.ServerPlayer[1];
        helper.runAtTickTime(31, () -> {
            player[0] = helper.makeMockServerPlayerInLevel();
            final net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5D, 1.0D, 6.5D));
            player[0].teleportTo(at.x, at.y, at.z);
        });
        helper.runAtTickTime(60, () -> {
            helper.getLevel().getServer().getPlayerList().remove(player[0]);
            helper.assertTrue(at(helper, 6, 1, 1).getValue(ConcealedDetectorBlock.DETECTED), "sees the player");
            helper.assertValueEqual(helper.getLevel().getSignal(helper.absolutePos(pos), Direction.DOWN), 0, "no signal");
            helper.succeed();
        });
    }

    /** BlockEnclosed: the sealable wires and pipe seal every side; a Grating seals none and holds water. */
    @GameTest(template = AIR)
    public static void sealableBlocksAndGrating(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.SEALABLE_HEAVY_ALUMINUM_WIRE.get());
        helper.setBlock(new BlockPos(2, 1, 1), GCBlocks.SEALABLE_ALUMINUM_WIRE.get());
        helper.setBlock(new BlockPos(3, 1, 1), GCBlocks.SEALABLE_FLUID_PIPE.get());
        helper.assertTrue(at(helper, 1, 1, 1).getValue(io.github.reiko45.galacticraft.block.AluminumWireBlock.CONNECTIONS.get(Direction.EAST)),
                "the sealable wires join up");
        for (int x = 1; x <= 3; x++) {
            for (Direction dir : Direction.values()) {
                helper.assertFalse(OxygenUtil.canBlockPassAir(helper.getLevel(), at(helper, x, 1, 1), helper.absolutePos(new BlockPos(x, 1, 1)), dir),
                        "sealed " + dir);
            }
        }
        helper.setBlock(new BlockPos(5, 1, 1), GCBlocks.GRATING.get());
        helper.assertTrue(OxygenUtil.canBlockPassAir(helper.getLevel(), at(helper, 5, 1, 1), helper.absolutePos(new BlockPos(5, 1, 1)), Direction.DOWN),
                "air through a grating");
        helper.setBlock(new BlockPos(5, 1, 1), GCBlocks.GRATING.get().defaultBlockState().setValue(io.github.reiko45.galacticraft.block.GratingBlock.WATERLOGGED, true));
        helper.assertTrue(at(helper, 5, 1, 1).getFluidState().is(Fluids.WATER), "water under a grating");
        helper.succeed();
    }

    /** BlockPanelLighting: light by type, off with a redstone signal; the wrench turns the patterned panels, then the face. */
    @GameTest(template = AIR, timeoutTicks = 20)
    public static void lightingPanelsLightAndTurn(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), GCBlocks.LIGHTING_PANEL_SQUARE.get());
        helper.assertValueEqual(at(helper, 2, 2, 2).getLightEmission(), 11, "square: 11");
        helper.setBlock(new BlockPos(3, 2, 2), GCBlocks.LIGHTING_PANEL_SF.get());
        helper.assertValueEqual(at(helper, 3, 2, 2).getLightEmission(), 2, "dark: 2");
        final LightingPanelBlock linear = GCBlocks.LIGHTING_PANEL_LINEAR.get();
        final BlockPos pos = new BlockPos(5, 2, 2);
        helper.setBlock(pos, linear.defaultBlockState().setValue(LightingPanelBlock.FACING, Direction.NORTH));
        linear.onWrench(helper.getLevel(), helper.absolutePos(pos), at(helper, 5, 2, 2), null);
        helper.assertValueEqual(at(helper, 5, 2, 2).getValue(LightingPanelBlock.ROTATION), 1, "a quarter turn first");
        linear.onWrench(helper.getLevel(), helper.absolutePos(pos), at(helper, 5, 2, 2), null);
        helper.assertTrue(at(helper, 5, 2, 2).getValue(LightingPanelBlock.FACING) == Direction.EAST
                && at(helper, 5, 2, 2).getValue(LightingPanelBlock.ROTATION) == 0, "then the next face");
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertValueEqual(at(helper, 2, 2, 2).getLightEmission(), 0, "off with a signal"));
    }
}
