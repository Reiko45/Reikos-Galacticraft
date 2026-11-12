package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import io.github.reiko45.galacticraft.entity.JuicerEntity;
import io.github.reiko45.galacticraft.entity.SpiderQueenEntity;
import io.github.reiko45.galacticraft.entity.WebShotEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.dungeon.Dungeon;
import io.github.reiko45.galacticraft.world.dungeon.DungeonConfig;
import io.github.reiko45.galacticraft.world.dungeon.DungeonPiece;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7f: the Venus dungeon, the Spider Queen and her Juicers. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VenusDungeonTests {
    private VenusDungeonTests() {
    }

    /**
     * MapGenDungeonVenus layouts: complete (boss, corridor, treasure), corridors 5 wide, never two spawner rooms in a
     * row, the 20-25 x 10-11 x 20-25 boss room sunk 2 blocks with its way out remembered.
     */
    @GameTest(template = "empty")
    public static void venusDungeonLayouts(GameTestHelper helper) {
        int complete = 0;
        int spawners = 0;
        for (long seed = 0; seed < 200; seed++) {
            final List<Dungeon.Node> layout = Dungeon.plan(DungeonConfig.VENUS, RandomSource.create(seed), 1000, 1000);
            final int n = layout.size();
            final boolean whole = n >= 5 && layout.get(n - 1).kind() == DungeonPiece.Kind.TREASURE
                    && layout.get(n - 2).kind() == DungeonPiece.Kind.CORRIDOR && layout.get(n - 3).kind() == DungeonPiece.Kind.BOSS;
            if (whole) {
                complete++;
                final Dungeon.Node boss = layout.get(n - 3);
                helper.assertTrue(boss.sizeX() >= 20 && boss.sizeX() <= 25 && boss.sizeY() >= 10 && boss.sizeY() <= 11, "RoomBossVenus size: " + boss);
                helper.assertValueEqual(boss.box().minY(), DungeonConfig.VENUS.y() - 2, "the boss room sunk 2 blocks");
                helper.assertValueEqual(boss.exit(), layout.get(n - 2).direction().getOpposite(), "the boss room knows its way out");
            }
            DungeonPiece.Kind lastRoom = null;
            for (int i = 1; i < n; i++) {
                final Dungeon.Node node = layout.get(i);
                if (node.kind() == DungeonPiece.Kind.CORRIDOR) {
                    final int across = node.direction().getAxis() == Direction.Axis.X ? node.box().getZSpan() : node.box().getXSpan();
                    helper.assertValueEqual(across, 6, "corridors 5 wide (6 blocks with the far wall)");
                    continue;
                }
                if (node.kind() == DungeonPiece.Kind.SPAWNER) {
                    spawners++;
                    helper.assertFalse(lastRoom == DungeonPiece.Kind.SPAWNER, "no two spawner rooms in a row (seed " + seed + ")");
                }
                lastRoom = node.kind();
            }
        }
        helper.assertTrue(complete >= 190, "complete layouts: " + complete + "/200");
        helper.assertTrue(spawners > 200, "plenty of spawner rooms: " + spawners);
        helper.succeed();
    }

    /**
     * CorridorVenus and RoomBossVenus built: double walls with lava behind iron bars every 4 blocks, brick II floors;
     * the boss room's dome with web strings ending in web torches, and its spawner in the middle knowing the hollow.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void venusCorridorAndBossRoomAreBuilt(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(-4000, 0, 2000).atY(80);
        final BoundingBox hall = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 5, origin.getY() + 7, origin.getZ() + 12);
        new DungeonPiece(DungeonConfig.VENUS, new Dungeon.Node(DungeonPiece.Kind.CORRIDOR, hall, Direction.SOUTH, 5, 7, 12), 0, null)
                .postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1L), hall.inflatedBy(1),
                        level.getChunkAt(origin).getPos(), origin);
        helper.assertTrue(level.getBlockState(origin.offset(0, 2, 3)).is(Blocks.LAVA) && level.getBlockState(origin.offset(1, 2, 3)).is(Blocks.IRON_BARS)
                && level.getBlockState(origin.offset(5, 2, 3)).is(Blocks.LAVA) && level.getBlockState(origin.offset(4, 2, 3)).is(Blocks.IRON_BARS),
                "lava behind bars on both sides, every 4 blocks");
        helper.assertTrue(level.getBlockState(origin.offset(2, 2, 3)).isAir() && level.getBlockState(origin.offset(3, 3, 6)).isAir(), "a clear way down the middle");
        helper.assertTrue(level.getBlockState(origin.offset(1, 3, 5)).is(GCBlocks.VENUS_DUNGEON_BRICK_1.get()), "brick I walls");
        helper.assertTrue(level.getBlockState(origin.offset(2, 0, 5)).is(GCBlocks.VENUS_DUNGEON_BRICK_2.get())
                && level.getBlockState(origin.offset(2, 7, 5)).is(GCBlocks.VENUS_DUNGEON_BRICK_2.get()), "brick II floor and ceiling");

        final BlockPos roomOrigin = origin.offset(0, 0, 30);
        final BoundingBox room = new BoundingBox(roomOrigin.getX(), roomOrigin.getY(), roomOrigin.getZ(), roomOrigin.getX() + 22, roomOrigin.getY() + 12,
                roomOrigin.getZ() + 22);
        new DungeonPiece(DungeonConfig.VENUS, new Dungeon.Node(DungeonPiece.Kind.BOSS, room, Direction.SOUTH, 22, 10, 22, Direction.WEST), 0,
                roomOrigin.offset(40, 2, 11))
                .postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(5L), room.inflatedBy(1),
                        level.getChunkAt(roomOrigin).getPos(), roomOrigin);
        final BlockPos spawnerPos = roomOrigin.offset(11, 1, 11);
        helper.assertTrue(level.getBlockState(spawnerPos).is(GCBlocks.VENUS_BOSS_SPAWNER.get()), "the Venus boss spawner in the middle");
        helper.assertTrue(level.getBlockEntity(spawnerPos) instanceof DungeonSpawnerBlockEntity spawner && spawner.roomBounds() != null
                && spawner.chestPos() != null, "it knows the hollow and the treasure");
        helper.assertTrue(level.getBlockState(roomOrigin.offset(11, 0, 11)).is(GCBlocks.VENUS_DUNGEON_BRICK_2.get()), "brick II floor");
        helper.assertTrue(level.getBlockState(roomOrigin.offset(11, 6, 11)).isAir(), "a high dome in the middle");
        helper.assertTrue(level.getBlockState(roomOrigin.offset(1, 3, 1)).is(GCBlocks.VENUS_DUNGEON_BRICK_1.get()), "solid brick in the corners");
        helper.assertTrue(level.getBlockState(roomOrigin.offset(11, 3, 1)).isAir() && level.getBlockState(roomOrigin.offset(21, 3, 11)).isAir(),
                "doorways for the way in (north) and the way out (east)");
        int webs = 0;
        int torches = 0;
        for (BlockPos pos : BlockPos.betweenClosed(roomOrigin, roomOrigin.offset(22, 11, 22))) {
            if (level.getBlockState(pos).is(GCBlocks.WEB_STRING.get())) {
                webs++;
            } else if (level.getBlockState(pos).is(GCBlocks.WEB_TORCH.get())) {
                torches++;
                helper.assertTrue(level.getBlockState(pos.above()).is(GCBlocks.WEB_STRING.get()) || level.getBlockState(pos.above()).is(GCBlocks.VENUS_DUNGEON_BRICK_1.get()),
                        "a web torch hangs on its string");
            }
        }
        helper.assertTrue(torches > 0 && webs > 0, "web strings (" + webs + ") ending in web torches (" + torches + ")");
        helper.succeed();
    }

    /** RoomTreasureVenus: a locked Tier 3 Treasure Chest that only the Tier 3 key opens. */
    @GameTest(template = "air_room")
    public static void venusTreasureRoomHoldsATier3Chest(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        final BoundingBox box = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 6, origin.getY() + 6, origin.getZ() + 6);
        new DungeonPiece(DungeonConfig.VENUS, new Dungeon.Node(DungeonPiece.Kind.TREASURE, box, Direction.SOUTH, 6, 6, 6), 0, null)
                .postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(9L), box.inflatedBy(1),
                        level.getChunkAt(origin).getPos(), origin);
        helper.assertTrue(level.getBlockEntity(origin.offset(3, 1, 3)) instanceof TreasureChestBlockEntity chest && chest.isLocked() && chest.tier() == 3,
                "a locked Tier 3 Treasure Chest");
        helper.assertTrue(level.getBlockState(origin.offset(1, 2, 1)).is(Blocks.GLOWSTONE), "glowstone corner pillars");
        helper.assertTrue(level.getBlockState(origin.offset(3, 0, 3)).is(GCBlocks.VENUS_DUNGEON_BRICK_2.get()), "brick II floor");
        helper.assertValueEqual(((io.github.reiko45.galacticraft.item.DungeonKeyItem) GCItems.TIER_3_DUNGEON_KEY.get()).tier(), 3, "the Tier 3 key");
        helper.succeed();
    }

    /**
     * EntitySpiderQueen: 150 health; hurt past two thirds she goes to hide (and can't be hurt while she does); her web
     * shots slow; her reward is a Volcanic Pickaxe or a Shield Controller and the Tier 3 key.
     */
    @GameTest(template = "air_room", timeoutTicks = 200)
    public static void spiderQueenHidesAndShootsWebs(GameTestHelper helper) {
        final SpiderQueenEntity queen = helper.spawn(GCEntities.SPIDER_QUEEN.get(), new BlockPos(3, 1, 3));
        helper.assertValueEqual(queen.getMaxHealth(), 150.0F, "150 health");
        queen.hurt(queen.damageSources().generic(), 40.0F);
        helper.assertFalse(queen.shouldEvade, "a scratch");
        queen.invulnerableTime = 0;
        queen.hurt(queen.damageSources().generic(), 20.0F);
        helper.assertTrue(queen.shouldEvade, "past two thirds she hides");
        queen.setBurrowedCount((byte) 0);
        helper.assertTrue(queen.isInvulnerableTo(queen.damageSources().generic()), "untouchable in her cocoon");
        queen.setBurrowedCount((byte) -1);
        helper.assertTrue(queen.chestTier() == 3, "she guards the Tier 3 chest");

        // (aimed high like an arrow but flying straight, a web shot passes over short targets far away, as in 1.12.2)
        final net.minecraft.world.entity.npc.Villager pig = helper.spawn(EntityType.VILLAGER, new BlockPos(7, 1, 3));
        final WebShotEntity shot = new WebShotEntity(helper.getLevel(), queen, pig, 0.8F, 0.0F);
        helper.getLevel().addFreshEntity(shot);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the web shot slows what it hits");
            helper.assertValueEqual(pig.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier(), 2, "Slowness III");
            queen.discard();
            pig.discard();
        });
    }

    /** EntityJuicer: off a dungeon brick floor it glides up to the brick ceiling and hangs there, upside down. */
    @GameTest(template = "air_room", timeoutTicks = 200)
    public static void juicerLeapsToTheCeiling(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), GCBlocks.VENUS_DUNGEON_BRICK_2.get());
                helper.setBlock(new BlockPos(x, 6, z), GCBlocks.VENUS_DUNGEON_BRICK_1.get());
            }
        }
        final JuicerEntity juicer = helper.spawn(GCEntities.JUICER.get(), new BlockPos(3, 1, 3));
        juicer.readyToJump();
        helper.succeedWhen(() -> {
            helper.assertTrue(juicer.isHanging() && juicer.jumpTarget() == null, "hanging under the ceiling");
            helper.assertTrue(juicer.getY() > helper.absolutePos(new BlockPos(0, 5, 0)).getY() - 0.1D, "up by the ceiling: " + juicer.getY());
            juicer.discard();
        });
    }
}
