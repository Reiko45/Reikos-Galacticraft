package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.VenusEntryPodEntity;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.player.GCPlayerHandler;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.Atmosphere;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.VenusBiomeSource;
import io.github.reiko45.galacticraft.world.VenusChunkGenerator;
import io.github.reiko45.galacticraft.world.VenusWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7f: Venus, its terrain, weather, air and arrival. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VenusTests {
    private static final long SEED = 4321L;

    private VenusTests() {
    }

    private static VenusChunkGenerator generator(MinecraftServer server, long seed) {
        final RegistryAccess registries = server.registryAccess();
        final var biomes = registries.registryOrThrow(Registries.BIOME);
        final VenusBiomeSource source = new VenusBiomeSource(biomes.getHolderOrThrow(GCDimensions.VENUS_FLAT),
                biomes.getHolderOrThrow(GCDimensions.VENUS_MOUNTAIN), biomes.getHolderOrThrow(GCDimensions.VENUS_VALLEY));
        source.setSeed(seed);
        return new VenusChunkGenerator(source);
    }

    /** A Venus chunk as far as its caves and surface (no features). */
    private static ProtoChunk chunk(MinecraftServer server, VenusChunkGenerator generator, long seed, int chunkX, int chunkZ, boolean surface) {
        final RegistryAccess registries = server.registryAccess();
        final RandomState random = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, LevelHeightAccessor.create(0, 256),
                registries.registryOrThrow(Registries.BIOME), null);
        generator.fillFromNoise(Blender.empty(), random, null, chunk).join();
        if (surface) {
            generator.carveAndSurface(seed, generator.biomeManager(seed), random, chunk);
        }
        return chunk;
    }

    /**
     * BiomeProviderVenus: all three biomes, in regions dozens of blocks across (neighbouring 4-block cells mostly agree),
     * the same for the same seed and different for another.
     */
    @GameTest(template = "empty")
    public static void venusBiomesFormRegions(GameTestHelper helper) {
        final VenusBiomeSource source = generator(helper.getLevel().getServer(), SEED).venusBiomes();
        final int[] counts = new int[3];
        int same = 0;
        int pairs = 0;
        for (int x = -200; x < 200; x += 2) {
            for (int z = -200; z < 200; z += 2) {
                final int biome = source.biomeAt(x, z);
                counts[biome]++;
                pairs++;
                if (source.biomeAt(x + 1, z) == biome) {
                    same++;
                }
            }
        }
        helper.assertTrue(counts[0] > 1000 && counts[1] > 1000 && counts[2] > 1000,
                "flat " + counts[0] + ", mountain " + counts[1] + ", valley " + counts[2]);
        helper.assertTrue(same > pairs * 0.8, "regions: " + same + " of " + pairs + " neighbours agree");
        final VenusBiomeSource again = generator(helper.getLevel().getServer(), SEED).venusBiomes();
        final VenusBiomeSource other = generator(helper.getLevel().getServer(), SEED + 1).venusBiomes();
        int differ = 0;
        for (int i = 0; i < 400; i++) {
            helper.assertTrue(again.biomeAt(i * 3, -i * 5) == source.biomeAt(i * 3, -i * 5), "same seed, same biomes");
            if (other.biomeAt(i * 3, -i * 5) != source.biomeAt(i * 3, -i * 5)) {
                differ++;
            }
        }
        helper.assertTrue(differ > 100, "another seed, other biomes: " + differ);
        helper.succeed();
    }

    /**
     * ChunkProviderVenus terrain: hard rock from bedrock up; mountains stand higher than flats, which stand higher than
     * valleys; flats and mountains are crusted with hard rock over soft rock, valleys topped with soft rock.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void venusTerrainFollowsBiomes(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final VenusChunkGenerator generator = generator(server, SEED);
        final BiomeManager biomes = generator.biomeManager(SEED);
        final long[] heightSum = new long[3];
        final int[] columns = new int[3];
        int softUnderHard = 0;
        int softTopped = 0;
        for (int cx = -24; cx <= 24; cx += 6) {
            for (int cz = -24; cz <= 24; cz += 6) {
                final ProtoChunk chunk = chunk(server, generator, SEED, cx, cz, true);
                for (int x = 1; x < 16; x += 7) {
                    for (int z = 1; z < 16; z += 7) {
                        final BlockPos column = new BlockPos(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z);
                        helper.assertTrue(chunk.getBlockState(column).is(Blocks.BEDROCK), "bedrock at y 0");
                        int top = 255;
                        while (top > 0 && chunk.getBlockState(column.atY(top)).isAir()) {
                            top--;
                        }
                        final BlockState surface = chunk.getBlockState(column.atY(top));
                        final var biome = biomes.getBiome(column.atY(63));
                        final int id = biome.is(GCDimensions.VENUS_MOUNTAIN) ? 1 : biome.is(GCDimensions.VENUS_VALLEY) ? 2 : 0;
                        heightSum[id] += top;
                        columns[id]++;
                        if (id != 2 && top >= 62 && surface.is(GCBlocks.VENUS_HARD_ROCK.get())
                                && chunk.getBlockState(column.atY(top - 1)).is(GCBlocks.VENUS_SOFT_ROCK.get())) {
                            softUnderHard++;
                        }
                        if (id == 2 && surface.is(GCBlocks.VENUS_SOFT_ROCK.get())) {
                            softTopped++;
                        }
                        // deep down: hard rock, or a cave, lava tube or (in valleys) a pumice cave floor
                        final BlockState deep = chunk.getBlockState(column.atY(5));
                        helper.assertTrue(deep.is(GCBlocks.VENUS_HARD_ROCK.get()) || deep.isAir() || deep.is(Blocks.LAVA)
                                || deep.is(GCBlocks.VENUS_VOLCANIC_ROCK.get()) || deep.is(GCBlocks.PUMICE.get()), "hard rock deep down: " + deep);
                    }
                }
            }
        }
        helper.assertTrue(columns[0] > 0 && columns[1] > 0 && columns[2] > 0, "every biome sampled: " + java.util.Arrays.toString(columns));
        final double flat = (double) heightSum[0] / columns[0];
        final double mountain = (double) heightSum[1] / columns[1];
        final double valley = (double) heightSum[2] / columns[2];
        helper.assertTrue(mountain > flat && flat > valley, "mountains " + mountain + " > flats " + flat + " > valleys " + valley);
        helper.assertTrue(valley > 30 && mountain < 200, "heights in range: " + valley + " to " + mountain);
        helper.assertTrue(softUnderHard > 10, "hard rock crust over soft rock: " + softUnderHard);
        helper.assertTrue(softTopped > 3, "soft rock valley floors: " + softTopped);
        helper.succeed();
    }

    /**
     * MapGenCaveVenus and MapGenLavaVenus: caves under the surface; lava tubes whose lava never touches air beside or
     * below it (Venus Volcanic Rock seals it in); the same for the same seed.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void venusCavesAndLavaTubes(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final VenusChunkGenerator generator = generator(server, SEED);
        int caveAir = 0;
        int lava = 0;
        int volcanic = 0;
        for (int cx = 0; cx < 8; cx++) {
            for (int cz = 0; cz < 8; cz++) {
                final ProtoChunk chunk = chunk(server, generator, SEED, cx, cz, true);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final BlockPos column = new BlockPos(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z);
                        for (int y = 1; y < 50; y++) {
                            final BlockPos pos = column.atY(y);
                            final BlockState state = chunk.getBlockState(pos);
                            if (state.isAir()) {
                                caveAir++;
                            } else if (state.is(GCBlocks.VENUS_VOLCANIC_ROCK.get())) {
                                volcanic++;
                            } else if (state.is(Blocks.LAVA)) {
                                lava++;
                                for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                                    final BlockPos next = pos.relative(side);
                                    if ((next.getX() >> 4) == cx && (next.getZ() >> 4) == cz) {
                                        helper.assertFalse(chunk.getBlockState(next).is(Blocks.AIR), "lava at " + pos + " open to the " + side);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(caveAir > 500, "caves carved: " + caveAir);
        helper.assertTrue(lava > 0 && volcanic > 0, "lava tubes: " + lava + " lava, " + volcanic + " volcanic rock");
        final ProtoChunk a = chunk(server, generator(server, 99L), 99L, 2, 5, true);
        final ProtoChunk b = chunk(server, generator(server, 99L), 99L, 2, 5, true);
        for (int y = 1; y < 200; y += 2) {
            for (int x = 0; x < 16; x += 3) {
                final BlockPos pos = new BlockPos(a.getPos().getMinBlockX() + x, y, a.getPos().getMinBlockZ() + 9);
                helper.assertTrue(a.getBlockState(pos) == b.getBlockState(pos), "deterministic at " + pos);
            }
        }
        helper.succeed();
    }

    /**
     * A floor of Venus hard rock across the 20 x 20 test area, this deep, with open sky above (the test server roofs each
     * test with barriers, which these features would take for a mountain or the ground).
     */
    private static void venusGround(GameTestHelper helper, int depth) {
        for (int x = -1; x <= 20; x++) {
            for (int z = -1; z <= 20; z++) {
                for (int y = 28; y <= 30; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.BARRIER)) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
            }
        }
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                for (int y = 1; y <= depth; y++) {
                    helper.setBlock(new BlockPos(x, y, z), GCBlocks.VENUS_HARD_ROCK.get());
                }
            }
        }
    }

    /**
     * WorldGenVaporPool: a sealed ball of soft rock holding acid below its middle and air above, a Vapor Spout in the
     * ground straight over it and a clear shaft between.
     */
    @GameTest(template = "venus_ground", timeoutTicks = 100)
    public static void venusVaporPool(GameTestHelper helper) {
        venusGround(helper, 24);
        final BlockPos centre = helper.absolutePos(new BlockPos(10, 10, 10));
        helper.assertTrue(io.github.reiko45.galacticraft.world.VenusFeature.vaporPool(helper.getLevel(),
                net.minecraft.util.RandomSource.create(5L), centre), "placed");
        helper.assertTrue(helper.getLevel().getBlockState(centre.below()).is(io.github.reiko45.galacticraft.registry.GCFluids.SULPHURIC_ACID_BLOCK.get()),
                "acid at the bottom");
        helper.assertTrue(helper.getLevel().getBlockState(centre.above()).isAir(), "air at the top");
        BlockPos floor = centre;
        while (helper.getLevel().getBlockState(floor).is(io.github.reiko45.galacticraft.registry.GCFluids.SULPHURIC_ACID_BLOCK.get())) {
            floor = floor.below();
        }
        helper.assertTrue(helper.getLevel().getBlockState(floor).is(GCBlocks.VENUS_SOFT_ROCK.get())
                && helper.getLevel().getBlockState(floor.below()).is(GCBlocks.VENUS_SOFT_ROCK.get()), "a soft rock shell under the acid");
        helper.assertTrue(helper.getLevel().getBlockState(centre.atY(helper.absolutePos(BlockPos.ZERO).getY() + 24))
                .is(GCBlocks.VAPOR_SPOUT.get()), "the spout on the ground above");
        for (int y = centre.getY() + 9; y < helper.absolutePos(BlockPos.ZERO).getY() + 24; y++) {
            helper.assertTrue(helper.getLevel().getBlockState(centre.atY(y)).isAir(), "an open shaft at " + y);
        }
        helper.succeed();
    }

    /**
     * WorldGenCrashedProbe: a crater open to the sky with scorched walls and a Crashed Probe at the bottom whose loot
     * (and Radioisotope Core) waits for the first look inside.
     */
    @GameTest(template = "venus_ground", timeoutTicks = 100)
    public static void venusCrashedProbe(GameTestHelper helper) {
        venusGround(helper, 12);
        final BlockPos start = helper.absolutePos(new BlockPos(10, 14, 10));
        helper.assertTrue(io.github.reiko45.galacticraft.world.VenusFeature.crashedProbe(helper.getLevel(),
                net.minecraft.util.RandomSource.create(3L), start), "placed");
        BlockPos probe = null;
        int scorched = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(19, 12, 19)))) {
            final BlockState state = helper.getLevel().getBlockState(pos);
            if (state.is(GCBlocks.CRASHED_PROBE.get())) {
                probe = pos.immutable();
            } else if (state.is(GCBlocks.SCORCHED_VENUS_ROCK.get())) {
                scorched++;
            }
        }
        helper.assertTrue(probe != null, "a probe in the crater");
        helper.assertTrue(scorched > 5, "scorched walls: " + scorched);
        helper.assertTrue(helper.getLevel().getBlockState(probe.above(3)).isAir(), "open above");
        final var entity = (io.github.reiko45.galacticraft.block.entity.CrashedProbeBlockEntity) helper.getLevel().getBlockEntity(probe);
        helper.assertTrue(entity.getLootTable() != null && entity.hasCoreToDrop(), "salvage and core waiting");
        helper.succeed();
    }

    /** BiomeDecoratorVenus: seven ores that replace only Venus Hard Rock, 18-26 veins a chunk below y 60 (solar 50). */
    @GameTest(template = "empty")
    public static void venusOreFeatures(GameTestHelper helper) {
        final var configured = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (String ore : new String[]{"aluminum", "copper", "galena", "quartz", "silicon", "tin", "solar"}) {
            final var feature = configured.get(GalacticraftCore.id("venus_ore_" + ore));
            helper.assertTrue(feature != null && feature.config() instanceof net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration config
                    && config.targetStates.get(0).target.test(GCBlocks.VENUS_HARD_ROCK.get().defaultBlockState(), net.minecraft.util.RandomSource.create())
                    && !config.targetStates.get(0).target.test(GCBlocks.VENUS_SOFT_ROCK.get().defaultBlockState(), net.minecraft.util.RandomSource.create()),
                    ore + " ore replaces hard rock only");
        }
        final var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (var key : java.util.List.of(GCDimensions.VENUS_FLAT, GCDimensions.VENUS_MOUNTAIN, GCDimensions.VENUS_VALLEY)) {
            final var features = biomes.getOrThrow(key).getGenerationSettings().features();
            helper.assertTrue(features.size() > net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES.ordinal()
                    && features.get(net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES.ordinal()).size() == 7, key + " has the ores");
        }
        helper.succeed();
    }

    /**
     * WorldProviderVenus: gravity 0.0375 below vanilla, 0.38 fall damage, arrows 0.0275, solar (1/0.75)^3, no oxygen,
     * hot (thermal +5), corrosive CO2 and nitrogen; a Tier 3 destination.
     */
    @GameTest(template = "empty")
    public static void venusPhysicsAndAir(GameTestHelper helper) {
        final CelestialPhysics venus = CelestialPhysics.of(GCDimensions.VENUS);
        helper.assertTrue(venus == CelestialPhysics.VENUS, "Venus physics");
        helper.assertValueEqual(venus.gravityReduction(), 0.0375D, "gravity");
        helper.assertValueEqual(venus.fallDamageModifier(), 0.38F, "fall damage");
        helper.assertValueEqual(venus.arrowGravity(), 0.0275F, "arrows");
        helper.assertTrue(Math.abs(venus.solarMultiplier() - 2.370F) < 0.001F, "solar " + venus.solarMultiplier());
        helper.assertFalse(venus.breathable(), "no oxygen");
        helper.assertValueEqual(Atmosphere.thermalLevel(GCDimensions.VENUS), 5.0F, "hot");
        helper.assertTrue(Atmosphere.isCorrosive(GCDimensions.VENUS) && !Atmosphere.isCorrosive(GCDimensions.MARS), "only Venus corrodes");
        helper.assertValueEqual(Atmosphere.of(GCDimensions.VENUS), java.util.List.of(Atmosphere.Gas.CO2, Atmosphere.Gas.NITROGEN), "gases");
        final SpaceTravel.Destination destination = SpaceTravel.destination(GCDimensions.VENUS);
        helper.assertTrue(destination != null && destination.tier() == 3, "tier 3 destination");
        helper.succeed();
    }

    /**
     * GCPlayerHandler#checkShield: on Venus worn armour loses a point a second; a Shield Controller stops that (and
     * all other armour wear, TransformerHooks#armorDamageHook).
     */
    @GameTest(template = "empty")
    public static void venusCorrodesArmour(GameTestHelper helper) {
        @SuppressWarnings("removal")
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        final GCPlayerData data = GCPlayerData.get(player);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        for (int t = 1; t <= 100; t++) {
            player.tickCount = t;
            GCPlayerHandler.checkShield(player, data, true);
        }
        helper.assertValueEqual(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(), 5, "five seconds of acid air");
        for (int t = 101; t <= 200; t++) {
            player.tickCount = t;
            GCPlayerHandler.checkShield(player, data, false);
        }
        helper.assertValueEqual(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(), 5, "none elsewhere");
        data.setGear(GearSlot.SHIELD_CONTROLLER, new ItemStack(GCItems.SHIELD_CONTROLLER.get()));
        for (int t = 201; t <= 300; t++) {
            player.tickCount = t;
            GCPlayerHandler.checkShield(player, data, true);
        }
        helper.assertValueEqual(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(), 5, "the shield holds");
        final var event = new net.neoforged.neoforge.event.entity.living.ArmorHurtEvent(new java.util.EnumMap<>(EquipmentSlot.class), player,
                player.damageSources().generic());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "and stops other armour wear");
        helper.succeed();
    }

    /** WorldProviderVenus#updateWeatherOverride: rain builds 0.004 a tick towards its target, never past 0.6. */
    @GameTest(template = "empty")
    public static void venusWeatherCycles(GameTestHelper helper) {
        // The test server has no Venus; the cycle runs on the test level and puts its rain back afterwards.
        final ServerLevel venus = helper.getLevel();
        final float rain = venus.getRainLevel(1.0F);
        venus.setRainLevel(0.0F);
        VenusWeather.force(venus, true, 0.5F);
        for (int i = 0; i < 50; i++) {
            VenusWeather.run(venus);
        }
        helper.assertTrue(Math.abs(venus.getRainLevel(1.0F) - 0.2F) < 0.001F, "0.004 a tick: " + venus.getRainLevel(1.0F));
        for (int i = 0; i < 200; i++) {
            VenusWeather.run(venus);
        }
        helper.assertTrue(Math.abs(venus.getRainLevel(1.0F) - 0.5F) < 0.005F, "holds at its target: " + venus.getRainLevel(1.0F));
        VenusWeather.force(venus, true, 0.9F);
        for (int i = 0; i < 200; i++) {
            VenusWeather.run(venus);
        }
        helper.assertValueEqual(venus.getRainLevel(1.0F), VenusWeather.MAX_STRENGTH, "never heavier than 0.6");
        VenusWeather.force(venus, false, 0.5F);
        for (int i = 0; i < 200; i++) {
            VenusWeather.run(venus);
        }
        helper.assertValueEqual(venus.getRainLevel(1.0F), 0.0F, "and clears");
        venus.setRainLevel(rain);
        helper.assertFalse(VenusWeather.tick(helper.getLevel()), "other worlds keep vanilla weather");
        helper.succeed();
    }

    /**
     * EntityEntryPodVenus: waits 40 ticks, plunges at 2.5 blocks a tick, slowed by the air to about 0.7, and under its
     * parachute below y 242 to about 0.4.
     */
    @GameTest(template = "empty")
    public static void venusEntryPodDescends(GameTestHelper helper) {
        final BlockPos above = helper.absolutePos(new BlockPos(1, 0, 1));
        final VenusEntryPodEntity pod = VenusEntryPodEntity.create(helper.getLevel(), above.getX() + 0.5D, 900.0D, above.getZ() + 0.5D,
                NonNullList.withSize(3, ItemStack.EMPTY), 0);
        helper.assertTrue(pod.getType() == GCEntities.VENUS_ENTRY_POD.get(), "the Venus pod");
        for (int i = 0; i < 39; i++) {
            pod.tick();
        }
        helper.assertValueEqual(pod.getY(), 900.0D, "waits for the ground");
        pod.tick();
        helper.assertTrue(Math.abs(pod.getDeltaMovement().y - (-2.502D * 0.994D)) < 1.0E-6D, "plunges: " + pod.getDeltaMovement().y);
        for (int i = 0; i < 400; i++) {
            pod.tick();
        }
        helper.assertTrue(pod.getDeltaMovement().y < -0.69D && pod.getDeltaMovement().y > -0.72D, "air brakes it: " + pod.getDeltaMovement().y);
        helper.assertFalse(pod.parachuteOpen(), "no parachute up high");
        pod.setPos(pod.getX(), 240.0D, pod.getZ());
        for (int i = 0; i < 400; i++) {
            pod.tick();
        }
        helper.assertTrue(pod.parachuteOpen(), "parachute open at " + pod.getY());
        helper.assertTrue(pod.getDeltaMovement().y > -0.45D && pod.getDeltaMovement().y < -0.35D, "parachute speed: " + pod.getDeltaMovement().y);
        pod.discard();
        helper.succeed();
    }
}
