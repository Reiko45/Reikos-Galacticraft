package io.github.reiko45.galacticraft.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Exhaust flames (1.12.2 launchFlameIdle / launchFlameLaunched / ParticleLanderFlame), drawn with vanilla particles. */
final class RocketEffects {
    private RocketEffects() {
    }

    /**
     * A 3x3 jet under the rocket; the Tier 2 adds a wider ring of flames and four blue jets (1.12.2
     * EntityTier2Rocket#spawnParticles) and the Tier 3 four jets, drawn lower the faster it climbs so the exhaust trails
     * behind.
     */
    static void spawnFlames(RocketEntity rocket, int tier) {
        final RandomSource random = rocket.getRandom();
        final double y = rocket.launched() && tier >= 2 ? rocket.getY() - 1.9D - rocket.getDeltaMovement().y : rocket.getY() - 0.4D;
        if (rocket.launched() && tier >= 3) {
            // 1.12.2 EntityTier3Rocket#spawnParticles: four jets, each a cross of flames with four jittered corners
            final double[][] jets = {{-0.32D, 0.0D}, {0.0D, -0.32D}, {0.32D, 0.0D}, {0.0D, 0.32D}};
            final double[][] cross = {{0.4D, 0.4D}, {-0.4D, 0.4D}, {-0.4D, -0.4D}, {0.4D, -0.4D}, {0.0D, 0.0D}, {0.4D, 0.0D}, {-0.4D, 0.0D},
                    {0.0D, 0.4D}, {0.0D, -0.4D}};
            for (double[] jet : jets) {
                for (int i = 0; i < cross.length; i++) {
                    final double jitter = i < 4 ? 0.1D : 0.0D;
                    final double dx = jet[0] + cross[i][0] - Math.signum(cross[i][0]) * random.nextDouble() * jitter;
                    final double dz = jet[1] + cross[i][1] - Math.signum(cross[i][1]) * random.nextDouble() * jitter;
                    rocket.level().addParticle(ParticleTypes.FLAME, rocket.getX() + dx, y, rocket.getZ() + dz, dx * 0.1D, -0.6D, dz * 0.1D);
                    if (i == 4) {
                        rocket.level().addParticle(ParticleTypes.LARGE_SMOKE, rocket.getX() + dx, y - 1.0D, rocket.getZ() + dz, dx * 0.2D, -0.3D, dz * 0.2D);
                    }
                }
            }
        } else if (rocket.launched()) {
            for (int i = 0; i < 9; i++) {
                final double dx = (i % 3 - 1) * 0.4D + (random.nextDouble() - 0.5D) * 0.1D;
                final double dz = (i / 3 - 1) * 0.4D + (random.nextDouble() - 0.5D) * 0.1D;
                rocket.level().addParticle(ParticleTypes.FLAME, rocket.getX() + dx, y, rocket.getZ() + dz, dx * 0.1D, -0.6D, dz * 0.1D);
                rocket.level().addParticle(ParticleTypes.LARGE_SMOKE, rocket.getX() + dx, y - 1.0D, rocket.getZ() + dz, dx * 0.2D, -0.3D, dz * 0.2D);
            }
            if (tier >= 2) {
                for (int i = 0; i < 8; i++) {
                    final double angle = i * Math.PI / 4.0D;
                    final double dx = Math.cos(angle) * 0.7D + (random.nextDouble() - 0.5D) * 0.15D;
                    final double dz = Math.sin(angle) * 0.7D + (random.nextDouble() - 0.5D) * 0.15D;
                    rocket.level().addParticle(ParticleTypes.FLAME, rocket.getX() + dx, y - 0.5D, rocket.getZ() + dz, dx * 0.15D, -0.9D, dz * 0.15D);
                }
                for (int i = 0; i < 4; i++) {
                    final double dx = i < 2 ? (i == 0 ? -0.8D : 0.8D) : 0.0D;
                    final double dz = i >= 2 ? (i == 2 ? -0.8D : 0.8D) : 0.0D;
                    rocket.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, rocket.getX() + dx, y, rocket.getZ() + dz, 0.0D, -0.6D, 0.0D);
                }
            }
        } else if (rocket.tickCount % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                rocket.level().addParticle(ParticleTypes.SMOKE, rocket.getX() + (random.nextDouble() - 0.5D) * 0.8D, y + 0.6D,
                        rocket.getZ() + (random.nextDouble() - 0.5D) * 0.8D, random.nextDouble() / 2.0D - 0.25D, 0.0D, random.nextDouble() / 2.0D - 0.25D);
            }
        }
    }

    /**
     * 1.12.2 EntityLander#spawnParticles: five jets from the base, pushed out faster than the lander falls (the 1.12.2
     * particle started at y + 1 + motionY / 2 with the lander's own speed added).
     */
    static void spawnLanderFlames(Entity lander) {
        final RandomSource random = lander.getRandom();
        final double motionY = lander.getDeltaMovement().y;
        final double y = lander.getY() + 1.0D + motionY / 2.0D;
        final double[][] offsets = {{0.0D, 0.0D}, {0.2D, 0.2D}, {-0.2D, 0.2D}, {0.2D, -0.2D}, {-0.2D, -0.2D}};
        for (double[] offset : offsets) {
            lander.level().addParticle(ParticleTypes.FLAME, lander.getX() + offset[0] + (random.nextDouble() - 0.5D) * 0.1D, y,
                    lander.getZ() + offset[1] + (random.nextDouble() - 0.5D) * 0.1D, 0.0D, motionY - 1.0D, 0.0D);
        }
    }
}
