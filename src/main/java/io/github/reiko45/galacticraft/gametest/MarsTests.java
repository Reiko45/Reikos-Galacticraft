package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.CavernousVinesBlock;
import io.github.reiko45.galacticraft.entity.LandingBalloonsEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.entity.Tier2RocketEntity;
import io.github.reiko45.galacticraft.item.DeshArmorItem;
import io.github.reiko45.galacticraft.item.RocketItem;
import io.github.reiko45.galacticraft.menu.Schematics;
import io.github.reiko45.galacticraft.menu.Tier2RocketBenchMenu;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.MarsCaverns;
import io.github.reiko45.galacticraft.world.MarsChunkGenerator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7a: Mars terrain, caverns and blocks, desh gear, the Tier 2 Rocket and the Landing Balloons. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarsTests {
    private static final String AIR = "air_room";
    private static final long SEED = 1234L;

    private MarsTests() {
    }

    private static MarsChunkGenerator generator(MinecraftServer server) {
        final RegistryAccess registries = server.registryAccess();
        return new MarsChunkGenerator(new FixedBiomeSource(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(GCDimensions.MARS_BIOME)),
                GCBlocks.MARS_FINE_REGOLITH.get().defaultBlockState(), GCBlocks.MARS_REGOLITH.get().defaultBlockState(),
                GCBlocks.MARS_STONE.get().defaultBlockState());
    }

    private static ProtoChunk chunk(MinecraftServer server, long seed, int chunkX, int chunkZ, boolean carve) {
        final RegistryAccess registries = server.registryAccess();
        final MarsChunkGenerator generator = generator(server);
        final RandomState random = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, LevelHeightAccessor.create(0, 256),
                registries.registryOrThrow(Registries.BIOME), null);
        generator.fillFromNoise(Blender.empty(), random, null, chunk).join();
        if (carve) {
            generator.applyCarvers(null, seed, random, null, null, chunk, GenerationStep.Carving.AIR);
        }
        return chunk;
    }

    private static boolean isMars(BlockState state) {
        return state.is(GCBlocks.MARS_FINE_REGOLITH.get()) || state.is(GCBlocks.MARS_REGOLITH.get()) || state.is(GCBlocks.MARS_STONE.get());
    }

    /** ChunkProviderSpace: fine regolith on top of a few blocks of regolith over Mars stone, bedrock at y 0, hills around y 93. */
    @GameTest(template = "empty")
    public static void marsTerrainIsLayered(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        int min = 256;
        int max = 0;
        for (int cx = -8; cx <= 8; cx += 4) {
            for (int cz = -8; cz <= 8; cz += 4) {
                final ProtoChunk chunk = chunk(server, SEED, cx, cz, false);
                for (int x = 0; x < 16; x += 5) {
                    for (int z = 0; z < 16; z += 5) {
                        final BlockPos column = new BlockPos(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z);
                        helper.assertTrue(chunk.getBlockState(column).is(Blocks.BEDROCK), "bedrock at y 0");
                        final int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        min = Math.min(min, top);
                        max = Math.max(max, top);
                        helper.assertTrue(chunk.getBlockState(column.atY(top)).is(GCBlocks.MARS_FINE_REGOLITH.get()),
                                "fine regolith on top at " + column.atY(top) + ": " + chunk.getBlockState(column.atY(top)));
                        helper.assertTrue(chunk.getBlockState(column.atY(top - 1)).is(GCBlocks.MARS_REGOLITH.get()), "regolith below the top");
                        helper.assertTrue(chunk.getBlockState(column.atY(top - 8)).is(GCBlocks.MARS_STONE.get()), "stone further down");
                        helper.assertTrue(chunk.getBlockState(column.atY(top + 1)).isAir(), "air above");
                    }
                }
            }
        }
        helper.assertTrue(min > 30 && max < 255, "terrain between y " + min + " and " + max);
        helper.assertTrue(max - min > 8, "hills and valleys: " + min + " to " + max);
        helper.succeed();
    }

    /** MapGenCaveMars: tunnels through regolith and stone below the surface, deterministic per seed. */
    @GameTest(template = "empty")
    public static void marsCavesAreCarved(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        int caveAir = 0;
        for (int cx = 0; cx < 6; cx++) {
            for (int cz = 0; cz < 6; cz++) {
                final ProtoChunk chunk = chunk(server, SEED, cx, cz, true);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final BlockPos column = new BlockPos(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z);
                        final int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        for (int y = 1; y < Math.min(surface - 3, 120); y++) {
                            final BlockState state = chunk.getBlockState(column.atY(y));
                            if (state.isAir()) {
                                caveAir++;
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(caveAir > 200, "caves carved: " + caveAir + " air blocks underground");
        final ProtoChunk a = chunk(server, 77L, 3, -2, true);
        final ProtoChunk b = chunk(server, 77L, 3, -2, true);
        for (int y = 1; y < 128; y++) {
            for (int x = 0; x < 16; x += 3) {
                final BlockPos pos = new BlockPos(a.getPos().getMinBlockX() + x, y, a.getPos().getMinBlockZ() + 7);
                helper.assertTrue(a.getBlockState(pos) == b.getBlockState(pos), "caves are deterministic");
            }
        }
        helper.succeed();
    }

    /** MapGenCavernMars: a huge cavern at y 17-33 with a flat floor, hanging cavernous vines and pools of sludge. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void marsCavernsHoldVinesAndSludge(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final List<BlockPos> centres = MarsCaverns.centres(SEED, 0, 0, 40);
        helper.assertTrue(!centres.isEmpty(), "about one chunk in a hundred holds a cavern");
        final BlockPos centre = centres.get(0);
        final ChunkPos middle = new ChunkPos(centre);
        int vines = 0;
        int sludge = 0;
        int air = 0;
        for (int cx = middle.x - 2; cx <= middle.x + 2; cx++) {
            for (int cz = middle.z - 2; cz <= middle.z + 2; cz++) {
                final ProtoChunk chunk = chunk(server, SEED, cx, cz, true);
                for (BlockPos pos : BlockPos.betweenClosed(chunk.getPos().getMinBlockX(), 17, chunk.getPos().getMinBlockZ(),
                        chunk.getPos().getMaxBlockX(), 33, chunk.getPos().getMaxBlockZ())) {
                    final BlockState state = chunk.getBlockState(pos);
                    if (state.is(GCBlocks.CAVERNOUS_VINES.get())) {
                        vines++;
                        int below = 0;
                        while (below < 5 && chunk.getBlockState(pos.below(below)).is(GCBlocks.CAVERNOUS_VINES.get())) {
                            below++;
                        }
                        helper.assertValueEqual(state.getValue(CavernousVinesBlock.HANGING), below, "vines hanging from " + pos);
                    } else if (state.is(GCFluids.BACTERIAL_SLUDGE_BLOCK.get())) {
                        // Pools lie on the floor, though a worm cave carved later may open a hole under one (as in 1.12.2).
                        if (isMars(chunk.getBlockState(pos.below()))) {
                            sludge++;
                        }
                    } else if (state.isAir()) {
                        air++;
                    }
                }
                if (cx == middle.x && cz == middle.z) {
                    helper.assertTrue(chunk.getBlockState(centre).isAir() || chunk.getBlockState(centre).is(GCBlocks.CAVERNOUS_VINES.get()),
                            "hollow at the centre " + centre);
                    helper.assertTrue(isMars(chunk.getBlockState(centre.atY(18))), "flat floor at y 18");
                    helper.assertTrue(chunk.getBlockState(centre.atY(19)).isAir() || !isMars(chunk.getBlockState(centre.atY(19))),
                            "open just above the floor");
                }
            }
        }
        helper.assertTrue(air > 20_000, "a big hollow: " + air);
        helper.assertTrue(vines > 0 && sludge > 0, "vines " + vines + " and sludge " + sludge);
        helper.succeed();
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, ItemStack tool) {
        final ServerLevel level = helper.getLevel();
        return state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.TOOL, tool));
    }

    /** BlockBasicMars: desh ore needs diamond and gives Raw Desh, Mars stone breaks to cobblestone, the regolith wants a shovel. */
    @GameTest(template = "empty")
    public static void marsBlocksDropLike112(GameTestHelper helper) {
        final BlockState desh = GCBlocks.DESH_ORE.get().defaultBlockState();
        final List<ItemStack> fromDiamond = drops(helper, desh, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(fromDiamond.size() == 1 && fromDiamond.get(0).is(GCItems.RAW_DESH.get()), "raw desh: " + fromDiamond);
        helper.assertFalse(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(desh), "iron can't mine desh");
        helper.assertTrue(new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(desh), "diamond can");
        final List<ItemStack> stone = drops(helper, GCBlocks.MARS_STONE.get().defaultBlockState(), new ItemStack(Items.STONE_PICKAXE));
        helper.assertTrue(stone.size() == 1 && stone.get(0).is(GCBlocks.MARS_COBBLESTONE.get().asItem()), "cobblestone: " + stone);
        helper.assertTrue(new ItemStack(Items.WOODEN_SHOVEL).isCorrectToolForDrops(GCBlocks.MARS_FINE_REGOLITH.get().defaultBlockState()),
                "a shovel digs regolith");
        helper.assertValueEqual(GCBlocks.MARS_DUNGEON_BRICK.get().getExplosionResistance(), 40.0F, "dungeon brick resistance");
        helper.assertValueEqual(GCBlocks.DESH_BLOCK.get().getExplosionResistance(), 60.0F, "desh block resistance");
        final List<ItemStack> vines = drops(helper, GCBlocks.CAVERNOUS_VINES.get().defaultBlockState(), ItemStack.EMPTY);
        helper.assertTrue(vines.isEmpty(), "vines need shears: " + vines);
        helper.assertValueEqual(drops(helper, GCBlocks.CAVERNOUS_VINES.get().defaultBlockState(), new ItemStack(Items.SHEARS)).size(), 1, "sheared vines");
        helper.succeed();
    }

    /** MarsItems TOOLDESH / ARMORDESH: netherite-level digging, 1024 uses, armour 4/7/9/4 with toughness 3, three texture sheets. */
    @GameTest(template = "empty")
    public static void deshGearMatches112(GameTestHelper helper) {
        final ItemStack pick = new ItemStack(GCItems.DESH_PICKAXE.get());
        helper.assertTrue(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "desh mines obsidian");
        helper.assertValueEqual(io.github.reiko45.galacticraft.registry.GCTiers.DESH.getIncorrectBlocksForDrops(),
                net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL, "harvest level 4: netherite's place");
        helper.assertValueEqual(pick.getMaxDamage(), 1024, "1024 uses");
        helper.assertTrue(new ItemStack(GCItems.STICKY_DESH_PICKAXE.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "sticky pick digs too");
        final int[] defence = {4, 9, 7, 4};
        final DeshArmorItem[] pieces = {GCItems.DESH_HELMET.get(), GCItems.DESH_CHESTPLATE.get(), GCItems.DESH_LEGGINGS.get(), GCItems.DESH_BOOTS.get()};
        for (int i = 0; i < pieces.length; i++) {
            helper.assertValueEqual(pieces[i].getDefense(), defence[i], "defence of " + pieces[i]);
            helper.assertValueEqual(pieces[i].getToughness(), 3.0F, "toughness");
        }
        helper.assertValueEqual(new ItemStack(GCItems.DESH_HELMET.get()).getMaxDamage(), ArmorItem.Type.HELMET.getDurability(42), "durability factor 42");
        final ItemStack chest = new ItemStack(GCItems.DESH_CHESTPLATE.get());
        helper.assertTrue(GCItems.DESH_CHESTPLATE.get().getArmorTexture(chest, null, EquipmentSlot.CHEST, null, false).getPath().endsWith("desh_2.png"),
                "chestplate uses desh_2");
        helper.assertTrue(GCItems.DESH_LEGGINGS.get().getArmorTexture(chest, null, EquipmentSlot.LEGS, null, true).getPath().endsWith("desh_3.png"),
                "leggings use desh_3");
        helper.assertTrue(GCItems.DESH_HELMET.get().isValidRepairItem(new ItemStack(GCItems.DESH_HELMET.get()), new ItemStack(GCItems.DESH_INGOT.get())),
                "repaired with desh ingots");
        helper.succeed();
    }

    /** BlockCavernousVine: hangs from a solid ceiling, grows down into air, glows brightest at the tip, holds and poisons mobs. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void cavernousVinesGrowGlowAndGrab(GameTestHelper helper) {
        final BlockPos ceiling = new BlockPos(6, 7, 6);
        helper.setBlock(ceiling, GCBlocks.MARS_STONE.get());
        helper.setBlock(ceiling.below(), GCBlocks.CAVERNOUS_VINES.get());
        final BlockPos top = helper.absolutePos(ceiling.below());
        final ServerLevel level = helper.getLevel();
        level.getBlockState(top).randomTick(level, top, level.getRandom());
        helper.assertBlockPresent(GCBlocks.CAVERNOUS_VINES.get(), ceiling.below(2));
        helper.assertBlockProperty(ceiling.below(2), CavernousVinesBlock.TYPE, 1);
        final Block vines = GCBlocks.CAVERNOUS_VINES.get();
        helper.assertValueEqual(level.getBlockState(top.below()).getLightEmission(level, top.below()), 15, "the tip glows 15");
        helper.assertValueEqual(level.getBlockState(top).getLightEmission(level, top), 11, "4 less above it");
        // Without support the strand falls apart
        helper.setBlock(ceiling, Blocks.AIR);
        helper.assertBlockNotPresent(vines, ceiling.below());
        helper.assertBlockNotPresent(vines, ceiling.below(2));
        // A mob in a vine is pulled up and poisoned
        helper.setBlock(ceiling, GCBlocks.MARS_STONE.get());
        helper.setBlock(ceiling.below(), vines);
        helper.setBlock(ceiling.below(2), vines);
        helper.setBlock(ceiling.below(3), vines);
        final Pig pig = helper.spawn(EntityType.PIG, ceiling.below(3));
        final double startY = pig.getY();
        // Each tick the vine resets the mob's motion to +0.06 before gravity, so it hangs, sinking ~0.02 blocks a tick
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.POISON), "poisoned");
            helper.assertTrue(pig.getY() > startY - 0.5D, "held by the vines (free fall would drop 3 blocks): " + (pig.getY() - startY));
            helper.succeed();
        });
    }

    /** CelestialPhysics for Mars (WorldProviderMars) and the travel rules: Mars needs a Tier 2 rocket. */
    @GameTest(template = "empty")
    public static void marsPhysicsAndTravelTier(GameTestHelper helper) {
        final CelestialPhysics mars = CelestialPhysics.of(GCDimensions.MARS);
        helper.assertTrue(mars != null && !mars.breathable(), "Mars is airless");
        helper.assertValueEqual(mars.gravityReduction(), 0.058D, "gravity");
        helper.assertValueEqual(mars.fallDamageModifier(), 0.38F, "fall damage");
        helper.assertValueEqual(mars.arrowGravity(), 0.015F, "arrows");
        helper.assertValueEqual(SpaceTravel.destination(GCDimensions.MARS).tier(), 2, "tier 2 destination");
        helper.assertValueEqual(SpaceTravel.destination(GCDimensions.MOON).tier(), 1, "the Moon stays tier 1");
        final ItemStack rocket = SpaceTravel.rocketItem(2, 3);
        helper.assertTrue(rocket.is(GCItems.TIER_2_ROCKET.get()), "a tier 2 traveller gets a tier 2 rocket back");
        helper.assertValueEqual(RocketItem.storage(rocket), 3, "with its storage");
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        GCPlayerData.get(player).rocketTier = 2;
        helper.assertValueEqual(SpaceTravel.selectionPayload(player).tier(), 2, "the destination screen knows the tier");
        helper.succeed();
    }

    /** The Tier 2 page: nose cone, ten heavy-duty plates, two boosters, four fins and an engine; unlocked by its schematic. */
    @GameTest(template = "empty")
    public static void tier2WorkbenchMakesRockets(GameTestHelper helper) {
        final SimpleContainer matrix = new SimpleContainer(Tier2RocketBenchMenu.MATRIX_SIZE);
        matrix.setItem(1, new ItemStack(GCItems.NOSE_CONE.get()));
        for (int i = 2; i < 12; i++) {
            matrix.setItem(i, new ItemStack(GCItems.HEAVY_DUTY_PLATE.get()));
        }
        matrix.setItem(12, new ItemStack(GCItems.TIER_1_BOOSTER.get()));
        matrix.setItem(16, new ItemStack(GCItems.TIER_1_BOOSTER.get()));
        for (int i : new int[]{13, 14, 17, 18}) {
            matrix.setItem(i, new ItemStack(GCItems.ROCKET_FINS.get()));
        }
        matrix.setItem(15, new ItemStack(GCItems.TIER_1_ROCKET_ENGINE.get()));
        final ItemStack result = Tier2RocketBenchMenu.craft(matrix);
        helper.assertTrue(result.is(GCItems.TIER_2_ROCKET.get()), "tier 2 rocket: " + result);
        helper.assertValueEqual(RocketItem.storage(result), 0, "no chests");
        matrix.setItem(20, new ItemStack(Items.CHEST));
        helper.assertValueEqual(RocketItem.storage(Tier2RocketBenchMenu.craft(matrix)), 1, "one chest");
        matrix.setItem(5, new ItemStack(GCItems.HEAVY_PLATING.get()));
        helper.assertTrue(Tier2RocketBenchMenu.craft(matrix).isEmpty(), "tier 1 plating won't do");

        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertFalse(Schematics.unlocked(player).contains(Schematics.ROCKET_T2), "locked at first");
        final ItemStack schematic = new ItemStack(GCItems.TIER_2_ROCKET_SCHEMATIC.get());
        helper.assertTrue(Schematics.unlock(player, schematic), "the Moon dungeon schematic unlocks it");
        helper.assertTrue(schematic.isEmpty(), "schematic used up");
        helper.assertTrue(Schematics.unlocked(player).contains(Schematics.ROCKET_T2), "page unlocked");
        helper.succeed();
    }

    private static RocketEntity ignited(GameTestHelper helper, RocketEntity rocket, BlockPos at) {
        final BlockPos abs = helper.absolutePos(at);
        rocket.setPos(abs.getX() + 0.5D, abs.getY(), abs.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(rocket);
        rocket.ignite();
        // Skip the 400 tick countdown
        final CompoundTag tag = new CompoundTag();
        rocket.saveWithoutId(tag);
        tag.putInt("Countdown", 1);
        rocket.load(tag);
        return rocket;
    }

    /** EntityTier2Rocket: twice the Tier 1's climb rate, 1 mB every 2 ticks instead of 3, a 1,500 mB tank. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void tier2RocketClimbsFasterAndBurnsMore(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketEntity tier1 = ignited(helper, Tier1RocketEntity.create(level, 0, 0, 0, 0, Tier1RocketEntity.FUEL_CAPACITY), new BlockPos(3, 1, 6));
        final RocketEntity tier2 = ignited(helper, Tier2RocketEntity.create(level, 0, 0, 0, 0, Tier2RocketEntity.FUEL_CAPACITY), new BlockPos(9, 1, 6));
        helper.assertValueEqual(tier2.fuelCapacity(), 1500, "tank");
        helper.assertValueEqual(tier2.tier(), 2, "tier");
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(tier1.launched() && tier2.launched(), "both launched");
            final double ratio = tier2.getDeltaMovement().y / tier1.getDeltaMovement().y;
            helper.assertTrue(Math.abs(ratio - 2.0D) < 1.0E-6, "twice as fast: " + tier2.getDeltaMovement().y + " vs " + tier1.getDeltaMovement().y);
            helper.assertTrue(Tier2RocketEntity.FUEL_CAPACITY - tier2.fuel() > Tier1RocketEntity.FUEL_CAPACITY - tier1.fuel(),
                    "burns faster: " + tier2.fuel() + " / " + tier1.fuel());
            helper.assertTrue(tier2.toItem().is(GCItems.TIER_2_ROCKET.get()), "drops as a tier 2 rocket");
            tier1.discard();
            tier2.discard();
            helper.succeed();
        });
    }

    /** EntityLandingBalloons: falls, then bounces (4 blocks/tick the first time) while ridden; no getting out mid-bounce. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void landingBalloonsBounce(GameTestHelper helper) {
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        final BlockPos start = helper.absolutePos(new BlockPos(6, 3, 6));
        final LandingBalloonsEntity balloons = LandingBalloonsEntity.create(helper.getLevel(), start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D,
                NonNullList.withSize(3, ItemStack.EMPTY), 100);
        helper.getLevel().addFreshEntity(balloons);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.startRiding(balloons, true);
        helper.assertTrue(balloons.keepsRider(), "held in while falling");
        helper.assertFalse(balloons.hurt(helper.getLevel().damageSources().playerAttack(player), 5.0F), "can't be broken before it lands");
        final double[] highest = {0.0D};
        helper.succeedWhen(() -> {
            highest[0] = Math.max(highest[0], balloons.getDeltaMovement().y);
            helper.assertTrue(balloons.groundHits() >= 1, "bounced");
            helper.assertValueEqual(highest[0], 4.0D, "first bounce at 4 blocks/tick");
            player.stopRiding();
            helper.assertTrue(player.getVehicle() == balloons, "no getting out mid-bounce");
            balloons.discard();
        });
    }
}
