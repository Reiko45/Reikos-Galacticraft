package io.github.reiko45.galacticraft.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Exhaust flames (1.12.2 launchFlameIdle / launchFlameLaunched / ParticleLanderFlame), drawn with vanilla particles. */
final class RocketEffects {
    private RocketEffects() {
    }

    static void spawnFlames(Tier1RocketEntity rocket) {
        final RandomSource random = rocket.getRandom();
        final double y = rocket.getY() - 0.4D;
        if (rocket.launched()) {
            for (int i = 0; i < 9; i++) {
                final double dx = (i % 3 - 1) * 0.4D + (random.nextDouble() - 0.5D) * 0.1D;
                final double dz = (i / 3 - 1) * 0.4D + (random.nextDouble() - 0.5D) * 0.1D;
                rocket.level().addParticle(ParticleTypes.FLAME, rocket.getX() + dx, y, rocket.getZ() + dz, dx * 0.1D, -0.6D, dz * 0.1D);
                rocket.level().addParticle(ParticleTypes.LARGE_SMOKE, rocket.getX() + dx, y - 1.0D, rocket.getZ() + dz, dx * 0.2D, -0.3D, dz * 0.2D);
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
