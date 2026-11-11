package io.github.reiko45.galacticraft.world;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.entity.MeteorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * 1.12.2 GCPlayerHandler#throwMeteors: every tick, for each player on a world with meteors, a small meteor starts at
 * y 355 some 160-180 blocks east with chance 1 in (frequency x 750 / spawn modifier), a large one with a third of that.
 * They drift west by 0.5-2.5 blocks per tick while gravity pulls them down, so they come down around the player. When
 * several players are within 100 blocks only one of them (the oldest) gets meteors.
 */
public final class MeteorShowers {
    private static final double SPAWN_HEIGHT = 355.0D;

    private MeteorShowers() {
    }

    public static void tick(ServerPlayer player) {
        final CelestialPhysics physics = CelestialPhysics.of(player.level().dimension());
        final double modifier = GCConfig.METEOR_SPAWN_MOD.get();
        if (physics == null || physics.meteorFrequency() <= 0.0D || modifier <= 0.0D) {
            return;
        }
        final ServerLevel level = player.serverLevel();
        final int chance = Math.max(1, (int) (physics.meteorFrequency() * 750.0D / modifier));
        if (level.random.nextInt(chance) == 0 && isOldestNearby(player)) {
            throwMeteor(player, 1);
        }
        if (level.random.nextInt(chance * 3) == 0 && isOldestNearby(player)) {
            throwMeteor(player, 6);
        }
    }

    private static boolean isOldestNearby(ServerPlayer player) {
        final Player closest = player.level().getNearestPlayer(player.getX(), player.getY(), player.getZ(), 100.0D,
                other -> other != player && !other.isSpectator());
        return closest == null || closest.getId() <= player.getId();
    }

    /** Spawn one meteor for {@code player}; also used by the tests and the screenshot tour. */
    public static MeteorEntity throwMeteor(ServerPlayer player, int size) {
        final ServerLevel level = player.serverLevel();
        final RandomSource random = level.random;
        int x = random.nextInt(20) + 160;
        final int z = random.nextInt(20) - 10;
        final double motionX = random.nextDouble() * 2.0D - 2.5D;
        final double motionZ = random.nextDouble() * 5.0D - 2.5D;
        // 1.12.2 kept it inside the view distance; entities only move within the simulation distance now.
        final int range = Math.max(2, Math.min(level.getServer().getPlayerList().getViewDistance(),
                level.getServer().getPlayerList().getSimulationDistance()));
        final int px = Mth.floor(player.getX());
        if ((x + px >> 4) - (px >> 4) >= range) {
            x = ((px >> 4) + range << 4) - 1 - px;
        }
        final MeteorEntity meteor = MeteorEntity.create(level, player.getX() + x, SPAWN_HEIGHT, player.getZ() + z, motionX, 0.0D, motionZ, size);
        level.addFreshEntity(meteor);
        return meteor;
    }
}
