package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.FallenMeteorBlockEntity;
import io.github.reiko45.galacticraft.entity.MeteorEntity;
import io.github.reiko45.galacticraft.footprint.Footprint;
import io.github.reiko45.galacticraft.footprint.Footprints;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.MeteorShowers;
import io.github.reiko45.galacticraft.world.MoonChunkGenerator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 5a: Moon caves, meteors and the Fallen Meteor, footprints, low gravity for items and arrows. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoonTests {
    private static final String AIR = "air_room";

    private MoonTests() {
    }

    private static ProtoChunk carvedChunk(MinecraftServer server, long seed, int chunkX, int chunkZ) {
        final RegistryAccess registries = server.registryAccess();
        final MoonChunkGenerator generator = new MoonChunkGenerator(
                new FixedBiomeSource(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(GCDimensions.MOON_BIOME)),
                GCBlocks.MOON_TURF.get().defaultBlockState(), GCBlocks.MOON_DIRT.get().defaultBlockState(), GCBlocks.MOON_ROCK.get().defaultBlockState());
        final RandomState random = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, LevelHeightAccessor.create(0, 256),
                registries.registryOrThrow(Registries.BIOME), null);
        generator.fillFromNoise(Blender.empty(), random, null, chunk).join();
        generator.applyCarvers(null, seed, random, null, null, chunk, GenerationStep.Carving.AIR);
        return chunk;
    }

    /** MapGenCavesMoon: tunnels below the surface, carved only through moon blocks, never through bedrock. */
    @GameTest(template = "empty")
    public static void moonCavesAreCarved(GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        int caveAir = 0;
        for (int cx = 0; cx < 6; cx++) {
            for (int cz = 0; cz < 6; cz++) {
                final ProtoChunk chunk = carvedChunk(server, 1234L, cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final BlockPos column = new BlockPos(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z);
                        helper.assertTrue(chunk.getBlockState(column).is(Blocks.BEDROCK), "bedrock at y 0 stays");
                        final int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        for (int y = 1; y < surface - 3; y++) {
                            final BlockState state = chunk.getBlockState(column.atY(y));
                            if (state.isAir()) {
                                caveAir++;
                            } else {
                                helper.assertTrue(state.is(GCBlocks.MOON_ROCK.get()) || state.is(GCBlocks.MOON_DIRT.get())
                                        || state.is(GCBlocks.MOON_TURF.get()) || state.is(Blocks.BEDROCK), "only moon blocks underground: " + state);
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(caveAir > 200, "caves carved: " + caveAir + " air blocks underground");
        final ProtoChunk a = carvedChunk(server, 77L, 3, -2);
        final ProtoChunk b = carvedChunk(server, 77L, 3, -2);
        for (int y = 1; y < 128; y++) {
            for (int x = 0; x < 16; x += 3) {
                final BlockPos pos = new BlockPos(a.getPos().getMinBlockX() + x, y, a.getPos().getMinBlockZ() + 7);
                helper.assertTrue(a.getBlockState(pos) == b.getBlockState(pos), "caves are deterministic");
            }
        }
        helper.succeed();
    }

    /** A meteor that hits the ground explodes and leaves a hot Fallen Meteor in its crater. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void meteorLeavesHotFallenMeteor(GameTestHelper helper) {
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        final BlockPos start = helper.absolutePos(new BlockPos(6, 7, 6));
        helper.getLevel().addFreshEntity(MeteorEntity.create(helper.getLevel(), start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D,
                0.0D, -0.5D, 0.0D, 1));
        helper.succeedWhen(() -> {
            final List<BlockPos> meteors = BlockPos.betweenClosedStream(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(12, 8, 12)))
                    .filter(pos -> helper.getLevel().getBlockState(pos).is(GCBlocks.FALLEN_METEOR.get())).map(BlockPos::immutable).toList();
            helper.assertValueEqual(meteors.size(), 1, "one fallen meteor");
            helper.assertTrue(helper.getLevel().getBlockEntity(meteors.get(0)) instanceof FallenMeteorBlockEntity meteor
                    && meteor.heatLevel() > FallenMeteorBlockEntity.MAX_HEAT_LEVEL - 100, "it is hot");
            helper.assertTrue(!helper.getLevel().getBlockState(meteors.get(0).below()).isAir(), "resting on the ground");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(MeteorEntity.class, new AABB(start).inflate(20)).isEmpty(), "meteor gone");
        });
    }

    /** A hot Fallen Meteor falls like sand, keeps its heat, cools by one per tick and burns what touches it. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void fallenMeteorFallsCoolsAndBurns(GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 0, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 5, 6), GCBlocks.FALLEN_METEOR.get());
        final Pig pig = helper.spawn(EntityType.PIG, new BlockPos(6, 1, 6));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(GCBlocks.FALLEN_METEOR.get(), new BlockPos(6, 1, 6));
            helper.assertBlockNotPresent(GCBlocks.FALLEN_METEOR.get(), new BlockPos(6, 5, 6));
            final FallenMeteorBlockEntity meteor = helper.getBlockEntity(new BlockPos(6, 1, 6));
            helper.assertTrue(meteor.heatLevel() < FallenMeteorBlockEntity.MAX_HEAT_LEVEL && meteor.heatLevel() > FallenMeteorBlockEntity.MAX_HEAT_LEVEL - 100,
                    "cooling: " + meteor.heatLevel());
            helper.assertTrue(pig.isOnFire() || pig.getRemainingFireTicks() > 0 || pig.isDeadOrDying(), "the pig got burnt");
        });
    }

    /** Placed by hand, a Fallen Meteor is already cold. */
    @GameTest(template = "empty")
    public static void placedFallenMeteorIsCold(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, GCBlocks.FALLEN_METEOR.get());
        final BlockPos abs = helper.absolutePos(BlockPos.ZERO);
        GCBlocks.FALLEN_METEOR.get().setPlacedBy(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null, new ItemStack(GCBlocks.FALLEN_METEOR.get()));
        helper.assertValueEqual(((FallenMeteorBlockEntity) helper.getBlockEntity(BlockPos.ZERO)).heatLevel(), 0, "cold");
        helper.succeed();
    }

    /** Showers: meteors start at y 355, 160-180 blocks east (or at the edge of the simulated area), drifting west. */
    @GameTest(template = "empty")
    public static void meteorShowerSpawnsMeteors(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final MeteorEntity meteor = MeteorShowers.throwMeteor(player, 6);
        helper.assertValueEqual(meteor.size(), 6, "large meteor");
        helper.assertTrue(Math.abs(meteor.getY() - 355.0D) < 0.01D, "starts at y 355");
        helper.assertTrue(meteor.getX() > player.getX() && meteor.getX() <= player.getX() + 180.0D, "east of the player");
        helper.assertTrue(meteor.getDeltaMovement().x < 0.0D, "drifting west");
        meteor.discard();
        final CelestialPhysics moon = CelestialPhysics.of(GCDimensions.MOON);
        helper.assertTrue(moon != null && moon.meteorFrequency() == 5.0D, "1 in 3,750 per tick on the Moon");
        helper.assertTrue(CelestialPhysics.of(net.minecraft.world.level.Level.OVERWORLD) == null, "no meteors on Earth");
        helper.succeed();
    }

    /** Walking on moon turf leaves alternating left/right prints every 0.35 (squared) blocks, on top of the turf. */
    @GameTest(template = AIR)
    public static void footprintsOnMoonTurf(GameTestHelper helper) {
        for (int x = 0; x < 13; x++) {
            helper.setBlock(new BlockPos(x, 0, 6), GCBlocks.MOON_TURF.get());
        }
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos start = helper.absolutePos(new BlockPos(1, 1, 6));
        player.setYRot(-90.0F); // walking east (+x)
        player.setPos(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D);
        final GCPlayerData data = GCPlayerData.get(player);
        data.lastFootX = player.getX();
        data.lastFootZ = player.getZ();
        for (int i = 0; i < 30; i++) {
            player.setPos(player.getX() + 0.3D, player.getY(), player.getZ());
            Footprints.step(player, data);
        }
        final List<Footprint> prints = printsAlong(helper, start, 13);
        helper.assertTrue(prints.size() >= 4, "several prints: " + prints.size());
        final Footprint first = prints.get(0);
        final Footprint second = prints.get(1);
        helper.assertTrue(Math.abs(first.y() - (start.getY() + 0.005D)) < 0.006D, "on top of the turf: " + first.y());
        helper.assertTrue(Math.signum(first.z() - (start.getZ() + 0.5D)) != Math.signum(second.z() - (start.getZ() + 0.5D)), "left, then right");
        helper.assertValueEqual(first.rotation(), -270.0F, "pointing the way the player walks");
        // No prints off the turf
        final int onTurf = prints.size();
        player.setPos(player.getX(), player.getY(), player.getZ() + 3.0D);
        data.lastFootZ = player.getZ();
        for (int i = 0; i < 10; i++) {
            player.setPos(player.getX() + 0.3D, player.getY(), player.getZ());
            Footprints.step(player, data);
        }
        helper.assertValueEqual(printsAlong(helper, start, 16).size(), onTurf, "nothing on air");
        helper.succeed();
    }

    /** Prints in the chunks along {@code length} blocks east of {@code start}, in walking order. */
    private static List<Footprint> printsAlong(GameTestHelper helper, BlockPos start, int length) {
        final java.util.Set<ChunkPos> chunks = new java.util.LinkedHashSet<>();
        for (int dx = 0; dx <= length; dx++) {
            chunks.add(new ChunkPos(start.offset(dx, 0, 0)));
            chunks.add(new ChunkPos(start.offset(dx, 0, 4)));
        }
        final List<Footprint> prints = new java.util.ArrayList<>();
        for (ChunkPos chunk : chunks) {
            prints.addAll(Footprints.in(helper.getLevel(), chunk));
        }
        prints.removeIf(print -> Math.abs(print.y() - start.getY()) > 0.5D || print.x() < start.getX() || print.x() > start.getX() + length + 1);
        prints.sort(java.util.Comparator.comparingDouble(Footprint::x));
        return prints;
    }

    /** 1.12.2 TransformerHooks: items fall at max(0.002, 0.04 - 0.062 / 1.75), arrows at 0.005 on the Moon. */
    @GameTest(template = "empty")
    public static void lowGravityForItemsAndArrows(GameTestHelper helper) {
        final CelestialPhysics moon = CelestialPhysics.of(GCDimensions.MOON);
        helper.assertTrue(moon != null && Math.abs(moon.itemGravity() - 0.004571428D) < 1.0E-6D, "item gravity " + (moon == null ? 0 : moon.itemGravity()));
        helper.assertValueEqual(moon.arrowGravity(), 0.005F, "arrow gravity");
        // On Earth the mixins leave vanilla values alone
        final var item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(net.minecraft.world.item.Items.STONE));
        helper.assertTrue(Math.abs(item.getGravity() - 0.04D) < 1.0E-9D, "vanilla item gravity on Earth: " + item.getGravity());
        final var arrow = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, helper.getLevel());
        helper.assertTrue(Math.abs(arrow.getGravity() - 0.05D) < 1.0E-9D, "vanilla arrow gravity on Earth: " + arrow.getGravity());
        helper.succeed();
    }
}
