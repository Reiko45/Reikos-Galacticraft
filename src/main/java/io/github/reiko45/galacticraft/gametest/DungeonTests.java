package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import io.github.reiko45.galacticraft.entity.SkeletonBossEntity;
import io.github.reiko45.galacticraft.item.SchematicItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.dungeon.Dungeon;
import io.github.reiko45.galacticraft.world.dungeon.DungeonConfig;
import io.github.reiko45.galacticraft.world.dungeon.DungeonPiece;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 5b: evolved mobs, the Moon dungeon layout, the Skeleton Boss and the Tier 1 Treasure Chest. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DungeonTests {
    private static final String AIR = "air_room";

    private DungeonTests() {
    }

    /** 1.12.2 attributes: zombie 30, skeleton 25, spider 22, creeper 25 health; enderman as vanilla. */
    @GameTest(template = AIR)
    public static void evolvedMobsUseLegacyStats(GameTestHelper helper) {
        check(helper, GCEntities.EVOLVED_ZOMBIE.get(), 30.0F);
        check(helper, GCEntities.EVOLVED_SKELETON.get(), 25.0F);
        check(helper, GCEntities.EVOLVED_SPIDER.get(), 22.0F);
        check(helper, GCEntities.EVOLVED_CREEPER.get(), 25.0F);
        check(helper, GCEntities.EVOLVED_ENDERMAN.get(), 40.0F);
        for (EntityType<?> type : List.of(GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.EVOLVED_SKELETON.get(), GCEntities.EVOLVED_SPIDER.get(),
                GCEntities.EVOLVED_CREEPER.get(), GCEntities.EVOLVED_ENDERMAN.get(), GCEntities.SKELETON_BOSS.get())) {
            helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable()) != LootTable.EMPTY,
                    type.getDescriptionId() + " has a loot table");
        }
        helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(DungeonPiece.MOON_CHEST) != LootTable.EMPTY,
                "dungeon_tier_1 loot table exists");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, EntityType<? extends Mob> type, float health) {
        final LivingEntity mob = helper.spawn(type, 6, 2, 6);
        helper.assertTrue(mob.getMaxHealth() == health, type.getDescriptionId() + " max health " + mob.getMaxHealth() + ", expected " + health);
        mob.discard();
    }

    /**
     * 1.12.2 DungeonStart: within 10 attempts every layout ends boss room, corridor, treasure room; rooms never overlap.
     * Both the Moon's and Mars's (with its fixed 24x11x24 boss room) layouts.
     */
    @GameTest(template = "empty")
    public static void dungeonLayoutsEndInBossAndTreasure(GameTestHelper helper) {
        for (DungeonConfig config : DungeonConfig.values()) {
            layouts(helper, config);
        }
        helper.succeed();
    }

    private static void layouts(GameTestHelper helper, DungeonConfig config) {
        int complete = 0;
        for (long seed = 0; seed < 200; seed++) {
            final List<Dungeon.Node> layout = Dungeon.plan(config, RandomSource.create(seed), 1000, 1000);
            final int n = layout.size();
            if (n >= 5 && layout.get(n - 1).kind() == DungeonPiece.Kind.TREASURE && layout.get(n - 2).kind() == DungeonPiece.Kind.CORRIDOR
                    && layout.get(n - 3).kind() == DungeonPiece.Kind.BOSS) {
                complete++;
            }
            helper.assertTrue(layout.get(0).kind() == DungeonPiece.Kind.CRATER && layout.get(1).kind() == DungeonPiece.Kind.ENTRANCE,
                    "starts with crater and entrance");
            for (int i = 1; i < n; i++) {
                final Dungeon.Node node = layout.get(i);
                // (RoomBossVenus is sunk 2 blocks below the dungeon floor)
                final int floor = config == DungeonConfig.VENUS && node.kind() == DungeonPiece.Kind.BOSS ? config.y() - 2 : config.y();
                helper.assertTrue(node.box().minY() >= floor && node.box().maxY() <= config.y() + 12, config + " piece at dungeon depth: " + node);
                if (config == DungeonConfig.MARS && node.kind() == DungeonPiece.Kind.BOSS) {
                    helper.assertTrue(node.sizeX() == 24 && node.sizeY() == 11 && node.sizeZ() == 24, "RoomBossMars is 24x11x24: " + node);
                }
                if (node.kind() == DungeonPiece.Kind.CORRIDOR) {
                    continue;
                }
                for (int j = i + 1; j < n; j++) {
                    final Dungeon.Node other = layout.get(j);
                    if (other.kind() != DungeonPiece.Kind.CORRIDOR) {
                        helper.assertFalse(node.box().intersects(other.box()), "rooms overlap (seed " + seed + "): " + node + " / " + other);
                    }
                }
            }
        }
        helper.assertTrue(complete >= 196, config + " complete layouts: " + complete + "/200");
    }

    /** The structure set places a Moon dungeon in each 44x44 chunk region, and the structure builds a full layout there. */
    @GameTest(template = "empty")
    public static void moonDungeonIsPlaced(GameTestHelper helper) {
        final net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        final net.minecraft.core.RegistryAccess registries = server.registryAccess();
        final var biome = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(io.github.reiko45.galacticraft.world.GCDimensions.MOON_BIOME);
        final io.github.reiko45.galacticraft.world.MoonChunkGenerator generator = new io.github.reiko45.galacticraft.world.MoonChunkGenerator(
                new net.minecraft.world.level.biome.FixedBiomeSource(biome), GCBlocks.MOON_TURF.get().defaultBlockState(),
                GCBlocks.MOON_DIRT.get().defaultBlockState(), GCBlocks.MOON_ROCK.get().defaultBlockState());
        final long seed = 1234L;
        final net.minecraft.world.level.levelgen.RandomState random = net.minecraft.world.level.levelgen.RandomState.create(registries.asGetterLookup(),
                net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD, seed);
        final var state = generator.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), random, seed);
        final var structure = registries.registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(io.github.reiko45.galacticraft.data.GCWorldgenProvider.MOON_DUNGEON);
        final var placements = state.getPlacementsForStructure(structure);
        helper.assertValueEqual(placements.size(), 1, "placements for the Moon dungeon");
        final var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) placements.get(0);
        final net.minecraft.world.level.ChunkPos chunk = placement.getPotentialStructureChunk(seed, 0, 0);
        helper.assertTrue(chunk.x >= 0 && chunk.x < 44 && chunk.z >= 0 && chunk.z < 44, "one chunk in the first region: " + chunk);
        final var context = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(registries, generator, generator.getBiomeSource(),
                random, server.getStructureManager(), seed, chunk, net.minecraft.world.level.LevelHeightAccessor.create(0, 256), structure.value().biomes()::contains);
        helper.assertTrue(structure.value().findValidGenerationPoint(context).isPresent(), "a generation point in " + chunk);
        final var start = structure.value().generate(registries, generator, generator.getBiomeSource(), random, server.getStructureManager(), seed, chunk, 0,
                net.minecraft.world.level.LevelHeightAccessor.create(0, 256), structure.value().biomes()::contains);
        helper.assertTrue(start.isValid() && start.getPieces().size() >= 5, "a valid start with its pieces: " + start.getPieces().size());
        helper.succeed();
    }

    /** RoomBoss / RoomTreasure pieces: the boss spawner knows its room and the chest; the treasure chest starts locked. */
    @GameTest(template = AIR)
    public static void bossAndTreasureRoomsAreBuilt(GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        final BoundingBox bossBox = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 11, origin.getY() + 4, origin.getZ() + 5);
        final BlockPos chestAt = origin.offset(6, 1, 9);
        final DungeonPiece boss = new DungeonPiece(DungeonConfig.MOON, new Dungeon.Node(DungeonPiece.Kind.BOSS, bossBox,
                net.minecraft.core.Direction.SOUTH, 11, 4, 5), 0, chestAt);
        final BoundingBox treasureBox = new BoundingBox(origin.getX() + 2, origin.getY(), origin.getZ() + 7, origin.getX() + 10, origin.getY() + 4, origin.getZ() + 11);
        final DungeonPiece treasure = new DungeonPiece(DungeonConfig.MOON, new Dungeon.Node(DungeonPiece.Kind.TREASURE, treasureBox,
                net.minecraft.core.Direction.SOUTH, 8, 4, 4), 0, null);
        final BoundingBox all = new BoundingBox(origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1, origin.getX() + 12, origin.getY() + 6, origin.getZ() + 12);
        boss.postProcess(helper.getLevel(), helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(1L), all, helper.getLevel().getChunkAt(origin).getPos(), origin);
        treasure.postProcess(helper.getLevel(), helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(2L), all, helper.getLevel().getChunkAt(origin).getPos(), origin);

        final BlockPos spawnerAt = origin.offset(5, 1, 2);
        helper.assertTrue(helper.getLevel().getBlockState(spawnerAt).is(GCBlocks.BOSS_SPAWNER.get()), "boss spawner in the middle of the boss room");
        helper.assertTrue(helper.getLevel().getBlockEntity(spawnerAt) instanceof DungeonSpawnerBlockEntity spawner
                && chestAt.equals(spawner.chestPos()) && spawner.roomBounds() != null, "spawner knows its room and the treasure chest");
        final BlockPos treasureAt = origin.offset(6, 1, 9);
        helper.assertTrue(helper.getLevel().getBlockEntity(treasureAt) instanceof TreasureChestBlockEntity chest && chest.isLocked(),
                "locked treasure chest in the middle of the treasure room");
        helper.assertTrue(helper.getLevel().getBlockState(bossBox.getCenter().atY(origin.getY())).is(GCBlocks.MOON_DUNGEON_BRICK.get()),
                "boss room floor is dungeon brick");
        helper.succeed();
    }

    /** TileEntityTreasureChest: locked until a Tier 1 Dungeon Key is used on it (the key is used up outside creative). */
    @GameTest(template = AIR)
    public static void treasureChestOpensWithKey(GameTestHelper helper) {
        final BlockPos chestPos = new BlockPos(6, 1, 6);
        helper.setBlock(chestPos, GCBlocks.TREASURE_CHEST_TIER_1.get());
        final TreasureChestBlockEntity chest = helper.getBlockEntity(chestPos);
        helper.assertTrue(chest.isLocked(), "starts locked");
        chest.refill(new ItemStack(GCItems.BUGGY_SCHEMATIC.get()), 42L);
        helper.assertTrue(chest.isLocked(), "refilling relocks");
        int filled = 0;
        boolean schematic = false;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            final ItemStack stack = chest.getItem(slot);
            filled += stack.isEmpty() ? 0 : 1;
            schematic |= stack.is(GCItems.BUGGY_SCHEMATIC.get());
        }
        helper.assertTrue(schematic && filled >= 5, "refill rolls dungeon loot (" + filled + " stacks) and adds the schematic");

        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5D, 1.0D, 4.5D)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
        helper.useBlock(chestPos, player);
        helper.assertTrue(chest.isLocked(), "a stick does not unlock it");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.TIER_1_DUNGEON_KEY.get()));
        helper.useBlock(chestPos, player);
        helper.assertFalse(chest.isLocked(), "the key unlocks it");
        helper.assertTrue(player.getMainHandItem().getCount() == (player.getAbilities().instabuild ? 1 : 0), "key used up outside creative");
        helper.succeed();
    }

    /** TileEntityDungeonSpawner: a player entering the boss room wakes the Skeleton Boss (150 health, holding a bow). */
    @GameTest(template = AIR, batch = "boss_spawn")
    public static void bossSpawnsWhenPlayerEnters(GameTestHelper helper) {
        final BlockPos spawnerPos = new BlockPos(6, 1, 6);
        helper.setBlock(spawnerPos, GCBlocks.BOSS_SPAWNER.get());
        final DungeonSpawnerBlockEntity spawner = helper.getBlockEntity(spawnerPos);
        final BlockPos min = helper.absolutePos(new BlockPos(1, 1, 1));
        final BlockPos max = helper.absolutePos(new BlockPos(11, 7, 11));
        spawner.setRoom(BoundingBox.fromCorners(min, max), null);
        helper.spawn(GCEntities.EVOLVED_ZOMBIE.get(), 3, 2, 3);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5D, 1.0D, 9.5D)));
        helper.succeedWhen(() -> {
            final List<SkeletonBossEntity> bosses = helper.getEntities(GCEntities.SKELETON_BOSS.get());
            helper.assertTrue(bosses.size() == 1, "one boss spawned, found " + bosses.size());
            final SkeletonBossEntity boss = bosses.get(0);
            helper.assertTrue(boss.getMaxHealth() == 150.0F, "150 health");
            helper.assertTrue(boss.getMainHandItem().is(net.minecraft.world.item.Items.BOW), "holds a bow");
            helper.assertEntityNotPresent(GCEntities.EVOLVED_ZOMBIE.get());
        });
    }

    /** EntitySkeletonBoss onCollideWithPlayer: it grabs its target, holds on (no dismounting) and throws them off. */
    @GameTest(template = AIR, batch = "boss_grab", timeoutTicks = 200)
    public static void bossGrabsAndThrows(GameTestHelper helper) {
        final SkeletonBossEntity boss = helper.spawn(GCEntities.SKELETON_BOSS.get(), 6, 1, 6);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(boss.position());
        helper.runAfterDelay(25, () -> {
            boss.setTarget(player);
            boss.playerTouch(player);
            helper.assertTrue(player.getVehicle() == boss, "grabbed");
            player.stopRiding();
            helper.assertTrue(player.getVehicle() == boss, "can't get away while held");
            helper.runAfterDelay(50, () -> {
                helper.assertTrue(player.getVehicle() == null, "thrown off after 40 ticks");
                helper.succeed();
            });
        });
    }

    /** EntityBossBase#onDeathUpdate: after 200 ticks of dying it fills the treasure chest and drops the dungeon key. */
    @GameTest(template = AIR, batch = "boss_death", timeoutTicks = 300)
    public static void bossDeathFillsTreasureChest(GameTestHelper helper) {
        final BlockPos chestPos = new BlockPos(10, 1, 10);
        helper.setBlock(chestPos, GCBlocks.TREASURE_CHEST_TIER_1.get());
        final BlockPos spawnerPos = new BlockPos(2, 1, 2);
        helper.setBlock(spawnerPos, GCBlocks.BOSS_SPAWNER.get());
        final DungeonSpawnerBlockEntity spawner = helper.getBlockEntity(spawnerPos);
        spawner.setRoom(BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(11, 7, 11))),
                helper.absolutePos(chestPos));
        final SkeletonBossEntity boss = helper.spawn(GCEntities.SKELETON_BOSS.get(), 6, 1, 6);
        boss.setSpawner(helper.absolutePos(spawnerPos));
        boss.kill();
        helper.runAfterDelay(SkeletonBossEntity.DEATH_TICKS + 10, () -> {
            helper.assertEntityNotPresent(GCEntities.SKELETON_BOSS.get());
            final TreasureChestBlockEntity chest = helper.getBlockEntity(chestPos);
            boolean schematic = false;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                schematic |= chest.getItem(slot).getItem() instanceof SchematicItem;
            }
            helper.assertTrue(schematic, "treasure chest holds a schematic");
            helper.assertTrue(chest.isLocked(), "and is locked");
            final List<ItemEntity> keys = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16.0D),
                    item -> item.getItem().is(GCItems.TIER_1_DUNGEON_KEY.get()));
            helper.assertTrue(keys.size() == 1, "dropped the Tier 1 Dungeon Key");
            helper.succeed();
        });
    }
}
