package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.BrightAirBlock;
import io.github.reiko45.galacticraft.entity.EntryPodEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.entity.Tier3RocketEntity;
import io.github.reiko45.galacticraft.item.RocketItem;
import io.github.reiko45.galacticraft.menu.Schematics;
import io.github.reiko45.galacticraft.menu.Tier3RocketBenchMenu;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator;
import io.github.reiko45.galacticraft.world.Atmosphere;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7d: the asteroid belt, its blocks and titanium, the Tier 3 Rocket and the Entry Pod. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AsteroidsTests {
    private static final String AIR = "air_room";
    private static final long SEED = 1234L;

    private AsteroidsTests() {
    }

    private static AsteroidsChunkGenerator generator(MinecraftServer server) {
        final RegistryAccess registries = server.registryAccess();
        return new AsteroidsChunkGenerator(new FixedBiomeSource(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(GCDimensions.ASTEROIDS_BIOME)));
    }

    private static RandomState random(MinecraftServer server, long seed) {
        return RandomState.create(server.registryAccess().asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
    }

    private static ProtoChunk chunk(MinecraftServer server, AsteroidsChunkGenerator generator, RandomState random, int chunkX, int chunkZ) {
        final RegistryAccess registries = server.registryAccess();
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, LevelHeightAccessor.create(0, 256),
                registries.registryOrThrow(Registries.BIOME), null);
        generator.fillFromNoise(Blender.empty(), random, null, chunk).join();
        return chunk;
    }

    private static boolean isRock(BlockState state) {
        return state.is(GCBlocks.ASTEROID_ROCK_0.get()) || state.is(GCBlocks.ASTEROID_ROCK_1.get()) || state.is(GCBlocks.ASTEROID_ROCK_2.get());
    }

    /**
     * ChunkProviderAsteroids: asteroids of rock between y 13 and 243 (centres 48-208, up to 25 + 8 + 2 out) in empty
     * space, with Bright Air lighting the faces that look west, towards the sun.
     */
    @GameTest(template = "empty")
    public static void asteroidBeltIsMostlyEmptySpace(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final AsteroidsChunkGenerator generator = generator(server);
        final RandomState random = random(server, SEED);
        int rock = 0;
        int solid = 0;
        int bright = 0;
        int litFaces = 0;
        int total = 0;
        int minY = 256;
        int maxY = 0;
        for (int cx = -6; cx < 6; cx++) {
            for (int cz = -6; cz < 6; cz++) {
                final ProtoChunk chunk = chunk(server, generator, random, cx, cz);
                final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 256; y++) {
                            final BlockState state = chunk.getBlockState(pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z));
                            total++;
                            if (state.is(GCBlocks.BRIGHT_AIR.get())) {
                                bright++;
                                helper.assertTrue(state.getValue(BrightAirBlock.LEVEL) >= 4 && state.getValue(BrightAirBlock.LEVEL) <= 14,
                                        "bright air glows 4-14: " + state);
                                if (x < 15 && !chunk.getBlockState(pos.east()).isAir()) {
                                    litFaces++;
                                }
                            } else if (!state.isAir()) {
                                solid++;
                                minY = Math.min(minY, y);
                                maxY = Math.max(maxY, y);
                                if (isRock(state)) {
                                    rock++;
                                }
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(solid > 2_000, "some asteroids in 144 chunks: " + solid);
        helper.assertTrue(solid < total / 10, "mostly empty space: " + solid + " of " + total);
        helper.assertTrue(rock > solid / 2, "mostly asteroid rock: " + rock + " of " + solid);
        helper.assertTrue(minY >= 13 && maxY <= 243, "between y " + minY + " and " + maxY);
        helper.assertTrue(bright > 100, "bright air: " + bright);
        helper.assertTrue(litFaces == bright, "each bright air lights the face to its east: " + litFaces + " of " + bright);
        helper.succeed();
    }

    /**
     * The centres asteroidsNear works out (as 1.12.2 WorldProviderAsteroids listed them) are where the generator puts
     * asteroids: a small one is solid at its centre.
     */
    @GameTest(template = "empty")
    public static void asteroidCentresMatchTheTerrain(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final AsteroidsChunkGenerator generator = generator(server);
        final RandomState random = random(server, SEED);
        final List<AsteroidsChunkGenerator.Asteroid> asteroids = generator.asteroidsNear(random, 0, 0, 160);
        helper.assertTrue(asteroids.size() > 5, "asteroids within 160 blocks: " + asteroids.size());
        int checked = 0;
        for (AsteroidsChunkGenerator.Asteroid asteroid : asteroids) {
            helper.assertTrue(asteroid.y() >= 48 && asteroid.y() < 208 && asteroid.size() >= 5 && asteroid.size() < 25, "in range: " + asteroid);
            // below the hollow size, and big enough that the turbulence can't open the centre
            if (asteroid.size() >= 8 && asteroid.size() < 15 && checked < 4) {
                final ProtoChunk chunk = chunk(server, generator, random, asteroid.x() >> 4, asteroid.z() >> 4);
                final BlockState centre = chunk.getBlockState(new BlockPos(asteroid.x(), asteroid.y(), asteroid.z()));
                helper.assertFalse(centre.isAir(), "solid at the centre of " + asteroid + ": " + centre);
                checked++;
            }
        }
        helper.assertTrue(checked > 0, "checked some centres");
        helper.succeed();
    }

    /** WorldProviderAsteroids: tier 3, gravity 0.072 below vanilla, falls a tenth as bad, arrows 0.002, no air, -1.5 cold. */
    @GameTest(template = "empty")
    public static void asteroidPhysicsAndTravel(GameTestHelper helper) {
        final CelestialPhysics asteroids = CelestialPhysics.of(GCDimensions.ASTEROIDS);
        helper.assertTrue(asteroids != null && !asteroids.breathable(), "no air");
        helper.assertValueEqual(asteroids.gravityReduction(), 0.072D, "gravity");
        helper.assertValueEqual(asteroids.fallDamageModifier(), 0.1F, "fall damage");
        helper.assertValueEqual(asteroids.arrowGravity(), 0.002F, "arrows");
        helper.assertValueEqual(asteroids.fuelUsage(), 0.9D, "fuel use");
        helper.assertValueEqual(Atmosphere.of(GCDimensions.ASTEROIDS), List.of(), "no gases");
        helper.assertValueEqual(Atmosphere.thermalLevel(GCDimensions.ASTEROIDS), -1.5F, "cold");
        helper.assertValueEqual(Atmosphere.thermalLevel(GCDimensions.MARS), -1.0F, "Mars is cold too");
        helper.assertValueEqual(Atmosphere.thermalLevel(GCDimensions.MOON), 0.0F, "the Moon isn't");
        helper.assertValueEqual(SpaceTravel.destination(GCDimensions.ASTEROIDS).tier(), 3, "tier 3 destination");
        final ItemStack rocket = SpaceTravel.rocketItem(3, 2);
        helper.assertTrue(rocket.is(GCItems.TIER_3_ROCKET.get()) && RocketItem.storage(rocket) == 2, "a tier 3 traveller gets their rocket back");
        helper.succeed();
    }

    /** The Tier 3 page: heavy nose cone, ten Tier 3 plates, two boosters, four heavy fins and a heavy engine; Creeper Boss schematic. */
    @GameTest(template = "empty")
    public static void tier3WorkbenchMakesRockets(GameTestHelper helper) {
        final SimpleContainer matrix = new SimpleContainer(Tier3RocketBenchMenu.MATRIX_SIZE);
        matrix.setItem(1, new ItemStack(GCItems.HEAVY_NOSE_CONE.get()));
        for (int i = 2; i < 12; i++) {
            matrix.setItem(i, new ItemStack(GCItems.HEAVY_DUTY_PLATE_TIER_3.get()));
        }
        matrix.setItem(12, new ItemStack(GCItems.TIER_1_BOOSTER.get()));
        matrix.setItem(16, new ItemStack(GCItems.TIER_1_BOOSTER.get()));
        for (int i : new int[]{13, 14, 17, 18}) {
            matrix.setItem(i, new ItemStack(GCItems.HEAVY_ROCKET_FINS.get()));
        }
        matrix.setItem(15, new ItemStack(GCItems.HEAVY_ROCKET_ENGINE.get()));
        final ItemStack result = Tier3RocketBenchMenu.craft(matrix);
        helper.assertTrue(result.is(GCItems.TIER_3_ROCKET.get()), "tier 3 rocket: " + result);
        matrix.setItem(19, new ItemStack(Items.CHEST));
        matrix.setItem(21, new ItemStack(Items.CHEST));
        helper.assertValueEqual(RocketItem.storage(Tier3RocketBenchMenu.craft(matrix)), 2, "two chests");
        matrix.setItem(1, new ItemStack(GCItems.NOSE_CONE.get()));
        helper.assertTrue(Tier3RocketBenchMenu.craft(matrix).isEmpty(), "the light nose cone won't do");

        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertFalse(Schematics.unlocked(player).contains(Schematics.ROCKET_T3), "locked at first");
        helper.assertTrue(Schematics.unlock(player, new ItemStack(GCItems.TIER_3_ROCKET_SCHEMATIC.get())), "the Creeper Boss's schematic unlocks it");
        final List<Schematics.Page> pages = Schematics.unlocked(player);
        helper.assertValueEqual(pages.indexOf(Schematics.ROCKET_T3), pages.size() - 2, "last before Add New Schematic");
        helper.succeed();
    }

    /** EntityTier3Rocket: tier 3, 1,500 mB, 2.5 x the thrust, which tops out at 1.4 (2 without air). */
    @GameTest(template = AIR, skyAccess = true, timeoutTicks = 600)
    public static void tier3RocketClimbsFast(GameTestHelper helper) {
        final Tier3RocketEntity rocket = Tier3RocketEntity.create(helper.getLevel(), 0, 0, 0, 0, Tier3RocketEntity.FUEL_CAPACITY);
        helper.assertValueEqual(rocket.tier(), 3, "tier");
        helper.assertValueEqual(rocket.fuelCapacity(), 1500, "fuel");
        helper.assertTrue(rocket.item(1, 300).is(GCItems.TIER_3_ROCKET.get()), "drops itself");
        final BlockPos abs = helper.absolutePos(new BlockPos(6, 1, 6));
        rocket.setPos(abs.getX() + 0.5D, abs.getY(), abs.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(rocket);
        rocket.ignite();
        helper.succeedWhen(() -> {
            helper.assertTrue(rocket.phase() == RocketEntity.Phase.LAUNCHED, "launched");
            // vanilla gravity in the test world: 2.5 x min(t / 150, 1.4) reaches 3.5 blocks/tick
            helper.assertTrue(rocket.getDeltaMovement().y > 2.5D, "climbing fast: " + rocket.getDeltaMovement().y);
            rocket.discard();
        });
    }

    /** EntityEntryPod: holds for 40 ticks, drops at 0.5 blocks/tick and faster, settles without exploding and lights up. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void entryPodSettlesAndLights(GameTestHelper helper) {
        final BlockPos abs = helper.absolutePos(new BlockPos(6, 6, 6));
        final NonNullList<ItemStack> cargo = NonNullList.withSize(2, ItemStack.EMPTY);
        cargo.set(0, new ItemStack(Items.APPLE, 3));
        final EntryPodEntity pod = EntryPodEntity.create(helper.getLevel(), abs.getX() + 0.5D, abs.getY(), abs.getZ() + 0.5D, cargo, 0);
        helper.getLevel().addFreshEntity(pod);
        helper.runAfterDelay(30, () -> helper.assertTrue(Math.abs(pod.getY() - abs.getY()) < 0.01D, "waits for the ground to load"));
        helper.succeedWhen(() -> {
            helper.assertTrue(pod.onGround() && pod.isAlive(), "landed whole");
            // (absolute: 1.21.1 GameTestHelper#relativePos turns positions half way round)
            final BlockState light = helper.getLevel().getBlockState(pod.blockPosition().above(2));
            helper.assertTrue(light.is(GCBlocks.BRIGHT_AIR.get()), "bright air over the pod: " + light);
            helper.assertTrue(pod.cargo().getItem(0).is(Items.APPLE), "with the cargo");
            pod.discard();
        });
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, ItemStack tool) {
        final ServerLevel level = helper.getLevel();
        return state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.TOOL, tool));
    }

    /**
     * BlockBasicAsteroids: ilmenite needs a diamond-level pick and gives an iron and a titanium shard, the other ores
     * drop themselves; Dense Ice needs silk touch; a titanium pick mines it all.
     */
    @GameTest(template = "empty")
    public static void asteroidBlocksDropLike112(GameTestHelper helper) {
        final BlockState ilmenite = GCBlocks.ILMENITE_ORE.get().defaultBlockState();
        helper.assertFalse(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(ilmenite), "iron can't mine ilmenite");
        final ItemStack titaniumPick = new ItemStack(GCItems.TITANIUM_PICKAXE.get());
        helper.assertTrue(titaniumPick.isCorrectToolForDrops(ilmenite), "titanium can");
        helper.assertTrue(titaniumPick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "and obsidian");
        final List<ItemStack> shards = drops(helper, ilmenite, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(shards.size() == 2 && shards.stream().anyMatch(s -> s.is(GCItems.IRON_SHARD.get()))
                && shards.stream().anyMatch(s -> s.is(GCItems.TITANIUM_SHARD.get())), "an iron and a titanium shard: " + shards);
        final List<ItemStack> iron = drops(helper, GCBlocks.ASTEROID_IRON_ORE.get().defaultBlockState(), new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(iron.size() == 1 && iron.get(0).is(GCBlocks.ASTEROID_IRON_ORE.get().asItem()), "asteroid iron ore drops itself: " + iron);
        final List<ItemStack> rock = drops(helper, GCBlocks.ASTEROID_ROCK_1.get().defaultBlockState(), new ItemStack(Items.WOODEN_PICKAXE));
        helper.assertTrue(rock.size() == 1 && rock.get(0).is(GCBlocks.ASTEROID_ROCK_1.get().asItem()), "rock: " + rock);
        helper.assertTrue(drops(helper, GCBlocks.DENSE_ICE.get().defaultBlockState(), new ItemStack(Items.DIAMOND_PICKAXE)).isEmpty(), "ice needs silk touch");
        final ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        helper.assertValueEqual(drops(helper, GCBlocks.DENSE_ICE.get().defaultBlockState(), silk).size(), 1, "silk-touched ice");
        helper.assertValueEqual(new ItemStack(GCItems.TITANIUM_PICKAXE.get()).getMaxDamage(), 760, "760 uses");
        helper.assertValueEqual(new ItemStack(GCItems.TITANIUM_CHESTPLATE.get()).getMaxDamage(), 26 * 16, "chestplate durability");
        helper.succeed();
    }

    /** BlockIceAsteroids: broken outside a Galacticraft world it leaves water like ice; Bright Air is air that glows. */
    @GameTest(template = AIR)
    public static void denseIceAndBrightAir(GameTestHelper helper) {
        final BlockPos ice = new BlockPos(6, 1, 6);
        helper.setBlock(ice.below(), Blocks.STONE);
        // as a player breaking it: the block is gone, then playerDestroy decides about the water
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        GCBlocks.DENSE_ICE.get().playerDestroy(helper.getLevel(), player, helper.absolutePos(ice), GCBlocks.DENSE_ICE.get().defaultBlockState(), null,
                new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertBlockPresent(Blocks.WATER, ice);
        final BlockState bright = GCBlocks.BRIGHT_AIR.get().defaultBlockState().setValue(BrightAirBlock.LEVEL, 9);
        helper.assertTrue(bright.isAir() && bright.getLightEmission() == 9 && bright.canBeReplaced(), "bright air: air, light 9, replaceable");
        helper.succeed();
    }
}
