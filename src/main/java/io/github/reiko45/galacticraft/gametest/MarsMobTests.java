package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.SlimelingEggBlock;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SlimelingEggBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import io.github.reiko45.galacticraft.entity.CreeperBossEntity;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.entity.SlimelingInventory;
import io.github.reiko45.galacticraft.entity.SludgelingEntity;
import io.github.reiko45.galacticraft.entity.TntProjectileEntity;
import io.github.reiko45.galacticraft.entity.monster.EvolvedCreeper;
import io.github.reiko45.galacticraft.entity.monster.EvolvedZombie;
import io.github.reiko45.galacticraft.item.SchematicItem;
import io.github.reiko45.galacticraft.menu.SlimelingMenu;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCFeatures;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCModTags;
import io.github.reiko45.galacticraft.util.ColorUtil;
import io.github.reiko45.galacticraft.world.dungeon.Dungeon;
import io.github.reiko45.galacticraft.world.dungeon.DungeonConfig;
import io.github.reiko45.galacticraft.world.dungeon.DungeonPiece;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7b: the Mars dungeon and Creeper Boss, creeper eggs, bacterial sludge and sludgelings, and slimelings. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarsMobTests {
    private static final String AIR = "air_room";

    private MarsMobTests() {
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, double x, double z) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(x, 1.0D, z)));
        return player;
    }

    private static List<ItemEntity> items(GameTestHelper helper, net.minecraft.world.item.Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20.0D),
                entity -> entity.getItem().is(item));
    }

    // --- Creeper Boss, TNT and the Mars dungeon ---

    /** 1.12.2 EntityCreeperBoss: 200 health, hurt only by fireballs (and /kill); loses heads at 2/3 and 1/3 health. */
    @GameTest(template = AIR)
    public static void creeperBossOnlyFearsFireballs(GameTestHelper helper) {
        final CreeperBossEntity boss = helper.spawn(GCEntities.CREEPER_BOSS.get(), 6, 1, 6);
        helper.assertTrue(boss.getMaxHealth() == 200.0F, "200 health, was " + boss.getMaxHealth());
        helper.assertTrue(boss.getType().fireImmune() && boss.getType().is(GCModTags.BREATHES_IN_SPACE), "fire immune, breathes in space");
        final ServerPlayer player = player(helper, 3.5D, 3.5D);
        helper.assertFalse(boss.hurt(helper.getLevel().damageSources().playerAttack(player), 50.0F), "a punch does nothing");
        helper.assertFalse(boss.hurt(helper.getLevel().damageSources().explosion(null, null), 50.0F), "nor does an explosion");
        helper.assertTrue(boss.getHealth() == 200.0F, "still at full health");
        helper.assertTrue(boss.hurt(helper.getLevel().damageSources().source(DamageTypes.FIREBALL, player, player), 70.0F), "its own TNT hurts");
        helper.assertTrue(boss.getHealth() == 130.0F, "lost 70 health, has " + boss.getHealth());
        helper.runAfterDelay(2, () -> {
            helper.assertValueEqual(boss.headsRemaining(), 2, "heads below two thirds");
            boss.setHealth(60.0F);
            helper.runAfterDelay(2, () -> {
                helper.assertValueEqual(boss.headsRemaining(), 1, "heads below one third");
                helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(boss.getType().getDefaultLootTable()) != LootTable.EMPTY,
                        "has a loot table");
                helper.succeed();
            });
        });
    }

    /** 1.12.2 getGuaranteedLoot: Cargo Rocket or Tier 3 Rocket; the Astro Miner only once the Tier 3 Rocket is known. */
    @GameTest(template = "empty")
    public static void creeperBossSchematicDependsOnWhatYouKnow(GameTestHelper helper) {
        check(helper, false, false, Set.of(GCItems.CARGO_ROCKET_SCHEMATIC.get(), GCItems.TIER_3_ROCKET_SCHEMATIC.get()));
        check(helper, true, false, Set.of(GCItems.CARGO_ROCKET_SCHEMATIC.get(), GCItems.ASTRO_MINER_SCHEMATIC.get()));
        check(helper, false, true, Set.of(GCItems.CARGO_ROCKET_SCHEMATIC.get(), GCItems.TIER_3_ROCKET_SCHEMATIC.get()));
        final Set<net.minecraft.world.item.Item> both = new HashSet<>();
        final RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 200; i++) {
            both.add(CreeperBossEntity.guaranteedLoot(random, true, true).getItem());
        }
        helper.assertTrue(both.size() == 3, "knowing both, still a choice of one of them or the Cargo Rocket: " + both);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean t3, boolean miner, Set<net.minecraft.world.item.Item> expected) {
        final Set<net.minecraft.world.item.Item> found = new HashSet<>();
        final RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 100; i++) {
            found.add(CreeperBossEntity.guaranteedLoot(random, t3, miner).getItem());
        }
        helper.assertValueEqual(found, expected, "schematics (T3 known " + t3 + ", Astro Miner known " + miner + ")");
    }

    /** EntityProjectileTNT: fireball damage 6 + 3 per difficulty step on what it hits, then a small explosion; deflectable. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void tntHitsAndExplodes(GameTestHelper helper) {
        final EvolvedZombie zombie = helper.spawn(GCEntities.EVOLVED_ZOMBIE.get(), 6, 1, 10);
        zombie.setNoAi(true);
        final Pig owner = helper.spawn(EntityType.PIG, 1, 1, 1);
        owner.setNoAi(true);
        final Vec3 from = helper.absoluteVec(new Vec3(6.5D, 1.5D, 3.5D));
        final TntProjectileEntity tnt = new TntProjectileEntity(helper.getLevel(), owner, from.x, from.y, from.z, new Vec3(0.0D, 0.0D, 1.0D));
        tnt.setDeltaMovement(0.0D, 0.0D, 0.5D);
        helper.getLevel().addFreshEntity(tnt);
        helper.assertTrue(tnt.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && tnt.isPickable(), "can be punched back");
        final float difficulty = switch (helper.getLevel().getDifficulty()) {
            case HARD -> 2.0F;
            case NORMAL -> 1.0F;
            default -> 0.0F;
        };
        helper.succeedWhen(() -> {
            helper.assertTrue(tnt.isRemoved(), "the TNT is gone");
            helper.assertTrue(zombie.getHealth() <= 30.0F - (6.0F + 3.0F * difficulty), "hit hard: " + zombie.getHealth());
        });
    }

    /** RoomBossMars: brick walls with a doorway, a brick and glowstone ceiling, creeper eggs by the walls, the spawner. */
    @GameTest(template = AIR)
    public static void marsBossAndTreasureRoomsAreBuilt(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        final BoundingBox box = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 10, origin.getY() + 6, origin.getZ() + 10);
        final DungeonPiece boss = new DungeonPiece(DungeonConfig.MARS, new Dungeon.Node(DungeonPiece.Kind.BOSS, box, Direction.SOUTH, 10, 6, 10), 0, null);
        boss.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(7L), box.inflatedBy(1),
                level.getChunkAt(origin).getPos(), origin);
        helper.assertTrue(level.getBlockState(origin.offset(5, 1, 5)).is(GCBlocks.MARS_BOSS_SPAWNER.get()), "Mars boss spawner in the middle");
        helper.assertTrue(level.getBlockEntity(origin.offset(5, 1, 5)) instanceof DungeonSpawnerBlockEntity spawner && spawner.roomBounds() != null,
                "the spawner knows its room");
        helper.assertTrue(level.getBlockState(origin.offset(0, 3, 5)).is(GCBlocks.MARS_DUNGEON_BRICK.get()), "brick walls");
        helper.assertTrue(level.getBlockState(origin.offset(5, 0, 5)).is(GCBlocks.MARS_DUNGEON_BRICK.get()), "brick floor");
        helper.assertTrue(level.getBlockState(origin.offset(5, 6, 5)).is(GCBlocks.MARS_DUNGEON_BRICK.get()), "brick ceiling away from the walls");
        helper.assertTrue(level.getBlockState(origin.offset(5, 1, 0)).isAir() && level.getBlockState(origin.offset(6, 2, 0)).isAir()
                && level.getBlockState(origin.offset(5, 3, 0)).is(GCBlocks.MARS_DUNGEON_BRICK.get()), "a two-high doorway in the south wall");
        int eggs = 0;
        for (int i = 1; i < 10; i++) {
            for (int k = 1; k < 10; k++) {
                final boolean nearWall = i <= 2 || k <= 2 || i >= 8 || k >= 8;
                final BlockState floor = level.getBlockState(origin.offset(i, 1, k));
                if (floor.is(GCBlocks.CREEPER_EGG.get())) {
                    eggs++;
                    helper.assertTrue(nearWall, "creeper eggs only by the walls");
                }
                if (level.getBlockState(origin.offset(i, 6, k)).is(Blocks.GLOWSTONE)) {
                    helper.assertTrue(nearWall, "glowstone only near the walls");
                }
            }
        }
        helper.assertTrue(eggs > 0, "some creeper eggs");

        final BoundingBox treasureBox = new BoundingBox(origin.getX() + 2, origin.getY() + 1, origin.getZ() + 2, origin.getX() + 8, origin.getY() + 5, origin.getZ() + 8);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(1, 1, 1), origin.offset(9, 5, 9))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        final DungeonPiece treasure = new DungeonPiece(DungeonConfig.MARS, new Dungeon.Node(DungeonPiece.Kind.TREASURE, treasureBox, Direction.SOUTH, 6, 4, 6), 0, null);
        treasure.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(8L), treasureBox.inflatedBy(1),
                level.getChunkAt(origin).getPos(), origin);
        helper.assertTrue(level.getBlockEntity(origin.offset(5, 2, 5)) instanceof TreasureChestBlockEntity chest && chest.isLocked() && chest.tier() == 2,
                "a locked Tier 2 Treasure Chest in the treasure room");
        helper.succeed();
    }

    /** The structure set places a Mars dungeon in each 44x44 chunk region; its layout has the 24x11x24 boss room. */
    @GameTest(template = "empty")
    public static void marsDungeonIsPlaced(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var registries = server.registryAccess();
        final var biome = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(io.github.reiko45.galacticraft.world.GCDimensions.MARS_BIOME);
        final io.github.reiko45.galacticraft.world.MarsChunkGenerator generator = new io.github.reiko45.galacticraft.world.MarsChunkGenerator(
                new net.minecraft.world.level.biome.FixedBiomeSource(biome), GCBlocks.MARS_FINE_REGOLITH.get().defaultBlockState(),
                GCBlocks.MARS_REGOLITH.get().defaultBlockState(), GCBlocks.MARS_STONE.get().defaultBlockState());
        final long seed = 4321L;
        final var random = net.minecraft.world.level.levelgen.RandomState.create(registries.asGetterLookup(),
                net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD, seed);
        final var state = generator.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), random, seed);
        final var structure = registries.registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(io.github.reiko45.galacticraft.data.GCWorldgenProvider.MARS_DUNGEON);
        final var placements = state.getPlacementsForStructure(structure);
        helper.assertValueEqual(placements.size(), 1, "placements for the Mars dungeon");
        final var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) placements.get(0);
        final net.minecraft.world.level.ChunkPos chunk = placement.getPotentialStructureChunk(seed, 0, 0);
        helper.assertTrue(chunk.x >= 0 && chunk.x < 44 && chunk.z >= 0 && chunk.z < 44, "one chunk in the first region: " + chunk);
        final var moonPlacement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) registries
                .registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(io.github.reiko45.galacticraft.data.GCWorldgenProvider.MOON_DUNGEONS).placement();
        helper.assertFalse(moonPlacement.getPotentialStructureChunk(seed, 0, 0).equals(chunk), "not where the Moon's would be");
        final var start = structure.value().generate(registries, generator, generator.getBiomeSource(), random, server.getStructureManager(), seed, chunk, 0,
                net.minecraft.world.level.LevelHeightAccessor.create(0, 256), structure.value().biomes()::contains);
        helper.assertTrue(start.isValid() && start.getPieces().size() >= 5, "a valid start with its pieces: " + start.getPieces().size());
        boolean bossRoom = false;
        for (var piece : start.getPieces()) {
            if (piece instanceof DungeonPiece room && room.kind() == DungeonPiece.Kind.BOSS) {
                bossRoom = room.getBoundingBox().getXSpan() == 25 && room.getBoundingBox().getYSpan() == 12 && room.getBoundingBox().getZSpan() == 25
                        && room.getBoundingBox().minY() == DungeonConfig.MARS.y();
            }
        }
        helper.assertTrue(bossRoom, "a 24x11x24 boss room at y 30");
        helper.succeed();
    }

    /** The Mars boss spawner wakes the Creeper Boss and clears evolved skeletons, zombies and spiders (not creepers). */
    @GameTest(template = AIR, batch = "creeper_boss_spawn")
    public static void marsSpawnerWakesCreeperBoss(GameTestHelper helper) {
        final BlockPos spawnerPos = new BlockPos(6, 1, 6);
        helper.setBlock(spawnerPos, GCBlocks.MARS_BOSS_SPAWNER.get());
        final DungeonSpawnerBlockEntity spawner = helper.getBlockEntity(spawnerPos);
        spawner.setRoom(BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(11, 8, 11))), null);
        helper.spawn(GCEntities.EVOLVED_SKELETON.get(), 3, 1, 3).setNoAi(true);
        final EvolvedCreeper creeper = helper.spawn(GCEntities.EVOLVED_CREEPER.get(), 9, 1, 3);
        creeper.setNoAi(true);
        player(helper, 3.5D, 9.5D);
        helper.succeedWhen(() -> {
            final List<CreeperBossEntity> bosses = helper.getEntities(GCEntities.CREEPER_BOSS.get());
            helper.assertTrue(bosses.size() == 1, "one Creeper Boss, found " + bosses.size());
            helper.assertEntityNotPresent(GCEntities.EVOLVED_SKELETON.get());
            helper.assertTrue(creeper.isAlive(), "creepers are left alone");
        });
    }

    /** Beaten, the Creeper Boss fills the Tier 2 Treasure Chest (with a rocket schematic) and drops the Tier 2 key. */
    @GameTest(template = AIR, batch = "creeper_boss_death", timeoutTicks = 300)
    public static void creeperBossDeathFillsTierTwoChest(GameTestHelper helper) {
        final BlockPos chestPos = new BlockPos(11, 1, 11);
        helper.setBlock(chestPos, GCBlocks.TREASURE_CHEST_TIER_2.get());
        final BlockPos spawnerPos = new BlockPos(1, 1, 1);
        helper.setBlock(spawnerPos, GCBlocks.MARS_BOSS_SPAWNER.get());
        final DungeonSpawnerBlockEntity spawner = helper.getBlockEntity(spawnerPos);
        spawner.setRoom(BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(11, 8, 11))),
                helper.absolutePos(chestPos));
        final CreeperBossEntity boss = helper.spawn(GCEntities.CREEPER_BOSS.get(), 6, 1, 6);
        boss.setSpawner(helper.absolutePos(spawnerPos));
        boss.kill();
        helper.runAfterDelay(CreeperBossEntity.DEATH_TICKS + 10, () -> {
            helper.assertEntityNotPresent(GCEntities.CREEPER_BOSS.get());
            final TreasureChestBlockEntity chest = helper.getBlockEntity(chestPos);
            boolean rocket = false;
            int filled = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                final ItemStack stack = chest.getItem(slot);
                filled += stack.isEmpty() ? 0 : 1;
                rocket |= stack.getItem() instanceof SchematicItem && !stack.is(GCItems.ASTRO_MINER_SCHEMATIC.get());
            }
            helper.assertTrue(rocket && filled >= 6, "Tier 2 loot (" + filled + " stacks) with a rocket schematic");
            helper.assertTrue(items(helper, GCItems.TIER_2_DUNGEON_KEY.get()).size() == 1, "dropped the Tier 2 Dungeon Key");
            helper.succeed();
        });
    }

    /** BlockTier2TreasureChest: only a Tier 2 Dungeon Key opens it. */
    @GameTest(template = AIR)
    public static void tierTwoChestNeedsTierTwoKey(GameTestHelper helper) {
        final BlockPos chestPos = new BlockPos(6, 1, 6);
        helper.setBlock(chestPos, GCBlocks.TREASURE_CHEST_TIER_2.get());
        final TreasureChestBlockEntity chest = helper.getBlockEntity(chestPos);
        final ServerPlayer player = player(helper, 6.5D, 4.5D);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.TIER_1_DUNGEON_KEY.get()));
        helper.useBlock(chestPos, player);
        helper.assertTrue(chest.isLocked(), "the Tier 1 key doesn't fit");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.TIER_2_DUNGEON_KEY.get()));
        helper.useBlock(chestPos, player);
        helper.assertFalse(chest.isLocked(), "the Tier 2 key does");
        helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(DungeonConfig.MARS.loot()) != LootTable.EMPTY,
                "dungeon_tier_2 loot table exists");
        helper.succeed();
    }

    /** BlockCreeperEgg: only the Sticky Desh Pickaxe breaks it; an explosion hatches a baby Evolved Creeper. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void creeperEggHatchesWhenBlownUp(GameTestHelper helper) {
        final BlockPos eggPos = new BlockPos(6, 1, 6);
        helper.setBlock(eggPos, GCBlocks.CREEPER_EGG.get());
        final BlockState egg = helper.getBlockState(eggPos);
        final ServerPlayer player = player(helper, 6.5D, 4.5D);
        final BlockPos at = helper.absolutePos(eggPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.DESH_PICKAXE.get()));
        helper.assertTrue(egg.getDestroyProgress(player, helper.getLevel(), at) == 0.0F && !egg.canHarvestBlock(helper.getLevel(), at, player),
                "a Desh Pickaxe can't break it");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.STICKY_DESH_PICKAXE.get()));
        helper.assertTrue(egg.getDestroyProgress(player, helper.getLevel(), at) == 0.2F && egg.canHarvestBlock(helper.getLevel(), at, player),
                "the Sticky Desh Pickaxe breaks and collects it");
        helper.useBlock(eggPos, player);
        helper.assertBlockPresent(GCBlocks.CREEPER_EGG.get(), eggPos);
        final Vec3 blast = helper.absoluteVec(new Vec3(6.5D, 1.5D, 7.5D));
        helper.getLevel().explode(null, blast.x, blast.y, blast.z, 2.0F, Level.ExplosionInteraction.BLOCK);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(GCBlocks.CREEPER_EGG.get(), eggPos);
            final List<EvolvedCreeper> creepers = helper.getEntities(GCEntities.EVOLVED_CREEPER.get());
            helper.assertTrue(creepers.size() == 1 && creepers.get(0).isBaby(), "a baby Evolved Creeper hatched");
            helper.assertTrue(creepers.get(0).getBbHeight() < 1.2F, "half size: " + creepers.get(0).getBbHeight());
            helper.assertTrue(items(helper, GCBlocks.CREEPER_EGG.get().asItem()).isEmpty(), "the egg doesn't drop");
        });
    }

    // --- Bacterial sludge and sludgelings ---

    /** BlockSludge#onEntityCollision: wading in stirs up sludgelings, three at most within five blocks. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void sludgeSpawnsSludgelings(GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 1, 6), GCFluids.BACTERIAL_SLUDGE_BLOCK.get());
        helper.spawn(EntityType.PIG, new Vec3(6.5D, 1.0D, 6.5D));
        // a player nearby, or the (not persistent) sludgelings despawn at once
        player(helper, 1.5D, 1.5D);
        helper.runAfterDelay(40, () -> {
            // they are fast (speed 1.0) and may already have left the test's box
            final List<SludgelingEntity> sludgelings = helper.getLevel().getEntitiesOfClass(SludgelingEntity.class,
                    new AABB(helper.absolutePos(new BlockPos(6, 1, 6))).inflate(32.0D));
            helper.assertValueEqual(sludgelings.size(), 3, "sludgelings");
            final SludgelingEntity sludgeling = sludgelings.get(0);
            helper.assertTrue(sludgeling.getMaxHealth() == 7.0F && sludgeling.getType().is(EntityTypeTags.ARTHROPOD)
                    && sludgeling.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER) && sludgeling.getType().is(GCModTags.BREATHES_IN_SPACE),
                    "7 health, arthropod, breathes underwater and in space");
            helper.succeed();
        });
    }

    // --- Slimelings ---

    /** BlockSlimelingEgg / TileEntitySlimelingEgg: using the egg cracks it, and a tame slimeling of its colour hatches. */
    @GameTest(template = AIR, timeoutTicks = 120)
    public static void slimelingEggHatchesTamePet(GameTestHelper helper) {
        final BlockPos eggPos = new BlockPos(6, 1, 6);
        helper.setBlock(eggPos, GCBlocks.RED_SLIMELING_EGG.get());
        final ServerPlayer player = player(helper, 6.5D, 4.5D);
        helper.useBlock(eggPos, player);
        helper.assertTrue(helper.getBlockState(eggPos).getValue(SlimelingEggBlock.BROKEN), "cracked");
        final SlimelingEggBlockEntity egg = helper.getBlockEntity(eggPos);
        helper.assertTrue(egg.timeToHatch() >= 19 && egg.timeToHatch() < 70, "hatches within 20-70 ticks: " + egg.timeToHatch());
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(GCBlocks.RED_SLIMELING_EGG.get(), eggPos);
            final List<SlimelingEntity> slimelings = helper.getEntities(GCEntities.SLIMELING.get());
            helper.assertTrue(slimelings.size() == 1, "one slimeling hatched");
            final SlimelingEntity slimeling = slimelings.get(0);
            helper.assertTrue(slimeling.isTame() && slimeling.isOwnedBy(player) && slimeling.ownerName().equals(player.getGameProfile().getName()),
                    "tame, and the player's");
            helper.assertTrue(slimeling.red() == 1.0F && slimeling.green() == 0.0F && slimeling.blue() == 0.0F, "red");
            helper.assertTrue(slimeling.getHealth() == 20.0F && Math.abs(slimeling.getMaxHealth() - 20.001F) < 0.01F, "20 health");
        });
    }

    /** BlockSlimelingEgg#harvestBlock: a pickaxe collects the egg; a Silk Touch Desh Pickaxe becomes a Sticky one. */
    @GameTest(template = AIR)
    public static void silkTouchDeshPickaxeTurnsSticky(GameTestHelper helper) {
        final BlockPos eggPos = new BlockPos(6, 1, 6);
        helper.setBlock(eggPos, GCBlocks.BLUE_SLIMELING_EGG.get());
        final ServerPlayer player = player(helper, 6.5D, 4.5D);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        final BlockPos at = helper.absolutePos(eggPos);
        final BlockState state = helper.getBlockState(eggPos);
        helper.assertFalse(state.onDestroyedByPlayer(helper.getLevel(), at, player, false, helper.getLevel().getFluidState(at)),
                "anything but a pickaxe can't take it");
        helper.assertTrue(helper.getBlockState(eggPos).getValue(SlimelingEggBlock.BROKEN), "it cracks instead");
        final ItemStack pickaxe = new ItemStack(GCItems.DESH_PICKAXE.get());
        final var silkTouch = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        pickaxe.enchant(silkTouch, 1);
        pickaxe.setDamageValue(10);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.assertTrue(player.hasCorrectToolForDrops(state), "a Desh Pickaxe can collect it");
        state.getBlock().playerDestroy(helper.getLevel(), player, at, state, helper.getLevel().getBlockEntity(at), pickaxe.copy());
        final ItemStack held = player.getMainHandItem();
        helper.assertTrue(held.is(GCItems.STICKY_DESH_PICKAXE.get()) && held.getDamageValue() == 10
                && EnchantmentHelper.getItemEnchantmentLevel(silkTouch, held) == 1, "now a Sticky Desh Pickaxe, same wear and enchantments");
        helper.assertTrue(items(helper, GCBlocks.BLUE_SLIMELING_EGG.get().asItem()).size() == 1, "and the egg drops");
        helper.succeed();
    }

    /** EntitySlimeling#processInteract: slime balls tame a wild one (1 in 3), which then sits with 20 health. */
    @GameTest(template = AIR)
    public static void slimelingTamesWithSlimeBalls(GameTestHelper helper) {
        final SlimelingEntity slimeling = helper.spawn(GCEntities.SLIMELING.get(), 6, 1, 6);
        helper.assertTrue(slimeling.getMaxHealth() == 8.0F && !slimeling.isTame(), "a wild one has 8 health");
        final ServerPlayer player = player(helper, 6.5D, 4.5D);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SLIME_BALL, 64));
        int tries = 0;
        while (!slimeling.isTame() && tries < 60) {
            slimeling.mobInteract(player, InteractionHand.MAIN_HAND);
            tries++;
        }
        helper.assertTrue(slimeling.isTame() && slimeling.isOwnedBy(player), "tamed after " + tries + " slime balls");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 64 - tries, "slime balls eaten");
        helper.assertTrue(slimeling.isOrderedToSit() && slimeling.getHealth() == 20.0F, "sits, with 20 health");
        helper.succeed();
    }

    /** Feeding: a meal of its favourite food buys one Grow / Breed / Strengthen / Heal, for the owner only. */
    @GameTest(template = AIR)
    public static void slimelingFeedingNeedsAMeal(GameTestHelper helper) {
        final SlimelingEntity slimeling = helper.spawn(GCEntities.SLIMELING.get(), 6, 1, 6);
        final ServerPlayer owner = player(helper, 6.5D, 4.5D);
        slimeling.tame(owner);
        final int age = slimeling.slimelingAge();
        slimeling.handleAction(owner, SlimelingEntity.ACTION_GROW, "");
        helper.assertTrue(slimeling.slimelingAge() < age + 5000, "no growing without a meal");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(slimeling.favouriteFood(), 2));
        slimeling.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(owner.getMainHandItem().getCount(), 1, "ate one");
        final ServerPlayer stranger = player(helper, 4.5D, 4.5D);
        slimeling.handleAction(stranger, SlimelingEntity.ACTION_GROW, "");
        helper.assertTrue(slimeling.slimelingAge() < age + 5000, "a stranger can't use the meal");
        slimeling.handleAction(owner, SlimelingEntity.ACTION_GROW, "");
        helper.assertTrue(slimeling.slimelingAge() >= age + 5000, "the owner grows it by 5000");
        final float damage = slimeling.attackDamage();
        slimeling.handleAction(owner, SlimelingEntity.ACTION_STRENGTHEN, "");
        helper.assertTrue(slimeling.attackDamage() == damage, "one meal, one action");
        slimeling.handleAction(owner, SlimelingEntity.ACTION_RENAME, "Blob");
        helper.assertTrue(slimeling.hasCustomName() && slimeling.getCustomName().getString().equals("Blob"), "renamed");
        slimeling.handleAction(owner, SlimelingEntity.ACTION_SIT, "");
        helper.assertTrue(slimeling.isOrderedToSit(), "sits when told");
        helper.succeed();
    }

    /** InventorySlimeling: the bag's slots work only with the bag in, and taking the bag out spills them. */
    @GameTest(template = AIR)
    public static void slimelingBagSpillsWhenRemoved(GameTestHelper helper) {
        final SlimelingEntity slimeling = helper.spawn(GCEntities.SLIMELING.get(), 6, 1, 6);
        slimeling.setNoAi(true);
        final ServerPlayer owner = player(helper, 6.5D, 4.5D);
        final SlimelingMenu menu = new SlimelingMenu(1, owner.getInventory(), slimeling.inventory(), slimeling);
        helper.assertFalse(menu.getSlot(SlimelingMenu.CARGO_START).isActive(), "no bag, no bag slots");
        final SlimelingInventory inventory = slimeling.inventory();
        inventory.setItem(SlimelingInventory.BAG_SLOT, new ItemStack(GCItems.SLIMELING_CARGO.get()));
        helper.assertTrue(menu.getSlot(SlimelingMenu.CARGO_START).isActive(), "the bag opens its slots");
        inventory.setItem(SlimelingInventory.CARGO_START, new ItemStack(Items.DIRT, 5));
        inventory.setItem(SlimelingInventory.CARGO_START + SlimelingInventory.CARGO_SLOTS - 1, new ItemStack(Items.COBBLESTONE, 3));
        inventory.removeItem(SlimelingInventory.BAG_SLOT, 1);
        helper.assertTrue(inventory.getItem(SlimelingInventory.CARGO_START).isEmpty(), "bag slots emptied");
        helper.assertTrue(items(helper, Items.DIRT).size() == 1 && items(helper, Items.COBBLESTONE).size() == 1, "their contents dropped");
        helper.succeed();
    }

    /** Slimelings grow from half to full size; bred ones are tame and their colour is the parents' mixed by hue. */
    @GameTest(template = AIR)
    public static void slimelingsGrowAndBreed(GameTestHelper helper) {
        final SlimelingEntity red = helper.spawn(GCEntities.SLIMELING.get(), 4, 1, 6);
        final SlimelingEntity yellow = helper.spawn(GCEntities.SLIMELING.get(), 8, 1, 6);
        red.setNoAi(true);
        yellow.setNoAi(true);
        red.setColor(1.0F, 0.0F, 0.0F);
        yellow.setColor(1.0F, 1.0F, 0.0F);
        final ServerPlayer owner = player(helper, 6.5D, 4.5D);
        red.tame(owner);
        yellow.tame(owner);
        helper.assertTrue(red.isBaby() && Math.abs(red.getBbWidth() - 0.45F) < 0.01F, "hatchlings are small: " + red.getBbWidth());
        red.setSlimelingAge(SlimelingEntity.MAX_AGE);
        helper.assertTrue(!red.isBaby() && Math.abs(red.getBbWidth() - 0.9F) < 0.01F, "grown ones twice as big: " + red.getBbWidth());
        final SlimelingEntity baby = (SlimelingEntity) red.getBreedOffspring(helper.getLevel(), yellow);
        helper.assertTrue(baby != null && baby.isTame() && baby.isOwnedBy(owner), "the baby is tame and the owner's");
        helper.assertTrue(baby.red() > 0.9F && baby.green() > 0.4F && baby.green() < 0.8F && baby.blue() < 0.1F,
                "red and yellow make orange: " + baby.red() + " " + baby.green() + " " + baby.blue());
        final Vec3 purple = ColorUtil.addColorsRealistically(new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D));
        helper.assertTrue(purple.x > 0.3D && purple.z > 0.3D && purple.y < 0.2D, "red and blue make purple: " + purple);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(red.getMaxHealth() - 50.001F) < 0.01F, "a grown tame slimeling has 50 health: " + red.getMaxHealth());
            helper.assertTrue(red.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.29D, "speed 0.3");
            helper.succeed();
        });
    }

    /** WorldGenEggs: eggs only in air on fine regolith. */
    @GameTest(template = AIR)
    public static void slimelingEggsLieOnFineRegolith(GameTestHelper helper) {
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                helper.setBlock(new BlockPos(x, 1, z), GCBlocks.MARS_FINE_REGOLITH.get());
            }
        }
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(new BlockPos(6, 2, 6));
        int placed = 0;
        for (int seed = 0; seed < 60; seed++) {
            placed += GCFeatures.SLIMELING_EGGS.get().place(NoneFeatureConfiguration.INSTANCE, level, level.getChunkSource().getGenerator(),
                    RandomSource.create(seed), origin) ? 1 : 0;
        }
        int eggs = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(12, 6, 12)))) {
            if (level.getBlockState(pos).getBlock() instanceof SlimelingEggBlock) {
                eggs++;
                helper.assertTrue(pos.getY() == origin.getY() && level.getBlockState(pos.below()).is(GCBlocks.MARS_FINE_REGOLITH.get()),
                        "egg on fine regolith at " + pos);
            }
        }
        helper.assertTrue(placed > 0 && eggs > 0 && eggs <= placed, "eggs placed: " + placed + ", found " + eggs);
        helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(GCEntities.SLIMELING.get().getDefaultLootTable()) != LootTable.EMPTY,
                "slimelings have a loot table");
        helper.succeed();
    }
}
