package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.event.GravityEvents;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.MoonChunkGenerator;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game checks for the ported content. Run with {@code ./gradlew runGameTestServer}; the server exits
 * non-zero if any test fails.
 *
 * <p>The vanilla GameTest server only creates the vanilla dimensions (it ignores datapack dimensions), so the
 * Moon is tested by running its chunk generator on in-memory chunks rather than through the real level.
 * See the README for the manual in-world check.
 */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortSmokeTests {
    private static final String EMPTY = "empty";

    private PortSmokeTests() {
    }

    private static ProtoChunk generateMoonChunk(MinecraftServer server, long seed, int chunkX, int chunkZ) {
        final RegistryAccess registries = server.registryAccess();
        final MoonChunkGenerator generator = new MoonChunkGenerator(
                new FixedBiomeSource(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(GCDimensions.MOON_BIOME)),
                GCBlocks.MOON_TURF.get().defaultBlockState(),
                GCBlocks.MOON_DIRT.get().defaultBlockState(),
                GCBlocks.MOON_ROCK.get().defaultBlockState());
        final RandomState random = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, LevelHeightAccessor.create(0, 256),
                registries.registryOrThrow(Registries.BIOME), null);
        return (ProtoChunk) generator.fillFromNoise(Blender.empty(), random, null, chunk).join();
    }

    /**
     * The GameTest server ignores datapack dimensions, so decode {@code data/galacticraftcore/dimension/moon.json}
     * exactly as a normal server would (LevelStem codec + registry lookups) and generate a chunk from it.
     */
    @GameTest(template = EMPTY)
    public static void moonDimensionDefinitionLoads(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final RegistryAccess registries = server.registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.DIMENSION_TYPE).containsKey(GCDimensions.MOON_TYPE),
                "Moon dimension type is not registered");
        final Resource resource = server.getResourceManager().getResource(GalacticraftCore.id("dimension/moon.json")).orElse(null);
        helper.assertTrue(resource != null, "dimension/moon.json is missing");
        final LevelStem stem;
        try (Reader reader = resource.openAsReader()) {
            stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), JsonParser.parseReader(reader))
                    .getOrThrow(error -> new IllegalStateException("moon.json does not decode: " + error));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        helper.assertTrue(stem.type().is(GCDimensions.MOON_TYPE), "Moon uses the wrong dimension type");
        helper.assertTrue(stem.generator() instanceof MoonChunkGenerator, "Moon uses the wrong generator: " + stem.generator());
        final RandomState random = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, 99L);
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, LevelHeightAccessor.create(0, 256),
                registries.registryOrThrow(Registries.BIOME), null);
        stem.generator().fillFromNoise(Blender.empty(), random, null, chunk).join();
        final int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 8, 8);
        helper.assertTrue(chunk.getBlockState(new BlockPos(8, top, 8)).is(GCBlocks.MOON_TURF.get()), "Decoded generator did not place moon turf");
        helper.succeed();
    }

    /** Every column: turf on top, moon dirt below it, bedrock at y=0, surface within the 1.12.2 terrain band. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void moonTerrainLayers(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                final ProtoChunk chunk = generateMoonChunk(server, 1234L, cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        final BlockPos pos = new BlockPos(x, top, z);
                        final String where = " at chunk " + chunk.getPos() + " " + pos;
                        helper.assertTrue(top >= 20 && top < 128, "Moon surface out of range" + where);
                        helper.assertTrue(chunk.getBlockState(pos).is(GCBlocks.MOON_TURF.get()), "Expected moon turf" + where);
                        helper.assertTrue(chunk.getBlockState(pos.below()).is(GCBlocks.MOON_DIRT.get()), "Expected moon dirt" + where);
                        helper.assertTrue(chunk.getBlockState(pos.above()).isAir(), "Expected air above the surface" + where);
                        helper.assertTrue(chunk.getBlockState(pos.atY(0)).is(Blocks.BEDROCK), "Expected bedrock" + where);
                        helper.assertTrue(chunk.getBlockState(pos.atY(10)).is(GCBlocks.MOON_ROCK.get()), "Expected moon rock" + where);
                    }
                }
            }
        }
        helper.succeed();
    }

    /** Same seed -> same terrain (chunks may generate in any order/thread); different seed -> different terrain. */
    @GameTest(template = EMPTY)
    public static void moonTerrainIsDeterministic(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ProtoChunk a = generateMoonChunk(server, 42L, 5, -7);
        final ProtoChunk b = generateMoonChunk(server, 42L, 5, -7);
        final ProtoChunk other = generateMoonChunk(server, 43L, 5, -7);
        boolean differs = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int ha = a.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                helper.assertTrue(ha == b.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), "Terrain is not deterministic at " + x + "," + z);
                differs |= ha != other.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            }
        }
        helper.assertTrue(differs, "Terrain ignores the world seed");
        helper.succeed();
    }

    /** 1.12.2 Moon gravity: 0.08 - 0.062 = 0.018, removed again when the entity leaves. */
    @GameTest(template = EMPTY)
    public static void moonGravityModifier(GameTestHelper helper) {
        final Zombie zombie = helper.spawn(EntityType.ZOMBIE, 0, 1, 0);
        helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.GRAVITY) - 0.08D) < 1.0E-6, "Overworld gravity is not vanilla");
        GravityEvents.applyGravity(zombie, CelestialPhysics.of(GCDimensions.MOON));
        final double moonGravity = zombie.getAttributeValue(Attributes.GRAVITY);
        GravityEvents.applyGravity(zombie, CelestialPhysics.of(helper.getLevel().dimension()));
        final double backHome = zombie.getAttributeValue(Attributes.GRAVITY);
        zombie.discard();
        helper.assertTrue(Math.abs(moonGravity - 0.018D) < 1.0E-6, "Moon gravity should be 0.018 but was " + moonGravity);
        helper.assertTrue(Math.abs(backHome - 0.08D) < 1.0E-6, "Gravity not restored after leaving the Moon: " + backHome);
        helper.succeed();
    }

    /** Canned food gives the empty Tin Canister back, like 1.12.2 ItemFood#onItemUseFinish. */
    @GameTest(template = EMPTY)
    public static void cannedFoodReturnsCanister(GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        final ItemStack remainder = new ItemStack(GCItems.CANNED_BEEF.get()).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(remainder.is(GCItems.TIN_CANISTER.get()), "Expected a tin canister back, got " + remainder);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 8, "Canned beef should restore 8 hunger");
        helper.succeed();
    }

    /** Recipes with NeoForge load conditions (e.g. "only if another mod adds steel ingots") may legitimately be absent. */
    private static boolean isConditional(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject().has("neoforge:conditions");
        } catch (IOException e) {
            return false;
        }
    }

    /** Recipes that fail to parse are only logged by vanilla; make sure every JSON in our namespace loaded. */
    @GameTest(template = EMPTY)
    public static void allRecipesLoad(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final Map<ResourceLocation, Resource> files = server.getResourceManager().listResources("recipe",
                file -> file.getNamespace().equals(GalacticraftCore.MOD_ID) && file.getPath().endsWith(".json"));
        final List<String> broken = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            final String path = file.getKey().getPath().substring("recipe/".length(), file.getKey().getPath().length() - ".json".length());
            if (server.getRecipeManager().byKey(GalacticraftCore.id(path)).isEmpty() && !isConditional(file.getValue())) {
                broken.add(path);
            }
        }
        helper.assertTrue(files.size() >= 90, "Expected at least 90 recipe files, found " + files.size());
        helper.assertTrue(broken.isEmpty(), "Recipes failed to load: " + broken);
        helper.succeed();
    }
}
