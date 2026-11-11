package io.github.reiko45.galacticraft.footprint;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Footprints on the Moon (1.12.2 GCPlayerHandler#updateFeet, TickHandlerServer footprint map, WorldUtil
 * #getFootprintPosition). Walking on moon turf leaves a print every 0.35 blocks (squared distance, as in 1.12.2),
 * alternating left and right foot. The server keeps them per chunk in memory (they are not saved, as in 1.12.2), ages
 * them in loaded chunks every 100 ticks and drops them after 3,200 ticks or when the turf under them is gone. Players
 * get the prints of each chunk they start to see, and every new print near them.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class Footprints {
    private static final double STEP = 0.35D;
    private static final int AGE_INTERVAL = 100;
    private static final Map<ResourceKey<Level>, Map<Long, List<Footprint>>> PRINTS = new HashMap<>();

    private Footprints() {
    }

    /** Called every server tick for every player: footprints are left on the Moon. */
    public static void updateFeet(ServerPlayer player, GCPlayerData data) {
        if (player.level().dimension().equals(GCDimensions.MOON)) {
            step(player, data);
        } else {
            data.lastFootX = player.getX();
            data.lastFootZ = player.getZ();
        }
    }

    /** One tick of walking: a new print on moon turf every 0.35 (squared) blocks. */
    public static void step(ServerPlayer player, GCPlayerData data) {
        final double dx = player.getX() - data.lastFootX;
        final double dz = player.getZ() - data.lastFootZ;
        data.lastFootX = player.getX();
        data.lastFootZ = player.getZ();
        final double motionSqrd = dx * dx + dz * dz;
        if (motionSqrd <= 0.001D || motionSqrd > 4.0D || player.getAbilities().flying || player.isPassenger() || player.isSpectator()) {
            return;
        }
        final BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.05D, player.getZ());
        if (!player.level().getBlockState(below).is(GCBlocks.MOON_TURF.get())) {
            return;
        }
        if (data.distanceSinceLastStep <= STEP) {
            data.distanceSinceLastStep += motionSqrd;
            return;
        }
        // On top of the turf, with a little random height against z-fighting; to the left or right of the player.
        double x = player.getX();
        final double y = below.getY() + 1 + player.getRandom().nextFloat() / 100.0F;
        double z = player.getZ();
        final float side = data.lastStep == 0 ? 90.0F : -90.0F;
        final float a = (-player.getYRot() + side) * Mth.DEG_TO_RAD;
        x += Mth.sin(a) * 0.25D;
        z += Mth.cos(a) * 0.25D;
        final float rotation = player.getYRot() - 180.0F;
        final double[] xz = fitOnBlock(player.level(), rotation, x, y, z, player.getBlockX(), player.getBlockZ());
        add((ServerLevel) player.level(), new Footprint(xz[0], y, xz[1], rotation, (short) 0));
        data.lastStep = (data.lastStep + 1) % 2;
        data.distanceSinceLastStep = 0.0D;
    }

    /** 1.12.2 WorldUtil#getFootprintPosition: keep the 0.375 wide print on one block, and over ground. */
    private static double[] fitOnBlock(Level level, float rotation, double x, double y, double z, int playerX, int playerZ) {
        BlockPos main = BlockPos.containing(x, y - 0.5D, z);
        if (level.getBlockState(main).isAir()) {
            x += playerX - main.getX();
            z += playerZ - main.getZ();
            if (level.getBlockState(BlockPos.containing(x, y - 0.5D, z)).isAir()) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (!level.getBlockState(main.relative(direction)).isAir()) {
                        x += direction.getStepX();
                        z += direction.getStepZ();
                        break;
                    }
                }
            }
        }
        final int blockX = Mth.floor(x);
        final int blockZ = Mth.floor(z);
        final double scale = 0.375D;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int corner = 45; corner < 360; corner += 90) {
            final double cx = Math.sin(Math.toRadians(corner - rotation)) * scale + x;
            final double cz = Math.cos(Math.toRadians(corner - rotation)) * scale + z;
            minX = Math.min(minX, cx);
            maxX = Math.max(maxX, cx);
            minZ = Math.min(minZ, cz);
            maxZ = Math.max(maxZ, cz);
        }
        if (minX < blockX) {
            x += blockX - minX;
        }
        if (maxX > blockX + 1) {
            x -= maxX - (blockX + 1);
        }
        if (minZ < blockZ) {
            z += blockZ - minZ;
        }
        if (maxZ > blockZ + 1) {
            z -= maxZ - (blockZ + 1);
        }
        return new double[]{x, z};
    }

    public static void add(ServerLevel level, Footprint print) {
        final ChunkPos chunk = new ChunkPos(BlockPos.containing(print.x(), print.y(), print.z()));
        PRINTS.computeIfAbsent(level.dimension(), key -> new HashMap<>()).computeIfAbsent(chunk.toLong(), key -> new ArrayList<>()).add(print);
        final GCNetwork.FootprintAdded payload = new GCNetwork.FootprintAdded(chunk.toLong(), print);
        for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayers(chunk, false)) {
            GCNetwork.sendToPlayer(watcher, payload);
        }
    }

    /** The prints in a chunk (for tests). */
    public static List<Footprint> in(ServerLevel level, ChunkPos chunk) {
        final Map<Long, List<Footprint>> byChunk = PRINTS.get(level.dimension());
        final List<Footprint> prints = byChunk == null ? null : byChunk.get(chunk.toLong());
        return prints == null ? List.of() : List.copyOf(prints);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % AGE_INTERVAL != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            final Map<Long, List<Footprint>> byChunk = PRINTS.get(level.dimension());
            if (byChunk == null) {
                continue;
            }
            final Iterator<Map.Entry<Long, List<Footprint>>> chunks = byChunk.entrySet().iterator();
            while (chunks.hasNext()) {
                final Map.Entry<Long, List<Footprint>> entry = chunks.next();
                final ChunkPos pos = new ChunkPos(entry.getKey());
                if (!level.hasChunk(pos.x, pos.z)) {
                    continue;
                }
                entry.getValue().removeIf(print -> print.grow(AGE_INTERVAL)
                        || !level.getBlockState(BlockPos.containing(print.x(), print.y() - 0.5D, print.z())).is(GCBlocks.MOON_TURF.get()));
                if (entry.getValue().isEmpty()) {
                    chunks.remove();
                }
            }
        }
    }

    /** A player starts seeing a chunk: send its prints (1.12.2 sent the lists every 100 ticks). */
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        final Map<Long, List<Footprint>> byChunk = PRINTS.get(event.getLevel().dimension());
        final List<Footprint> prints = byChunk == null ? null : byChunk.get(event.getPos().toLong());
        if (prints != null && !prints.isEmpty()) {
            GCNetwork.sendToPlayer(event.getPlayer(), new GCNetwork.FootprintChunk(event.getPos().toLong(), List.copyOf(prints)));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PRINTS.clear();
    }
}
