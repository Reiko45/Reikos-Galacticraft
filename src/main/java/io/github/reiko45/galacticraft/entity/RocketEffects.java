/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.EntityTier1Rocket#spawnParticles,
 * planets.mars.entities.EntityTier2Rocket and EntityCargoRocket#spawnParticles, planets.asteroids.entities
 * .EntityTier3Rocket#spawnParticles and #makeFlame, core.entities.EntityLander#getParticleMap, api.vector.Vector3
 * #getRotationMatrix and core.client.fx.ParticleLaunchFlame / ParticleLanderFlame). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.registry.GCParticles;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rocket and lander exhaust with 1.12.2's own particles (launch flames, which leave launch smoke, and the lander's flame),
 * placed as 1.12.2 placed them; and the burns those flames gave whatever living thing they touched.
 */
public final class RocketEffects {
    private RocketEffects() {
    }

    /**
     * 1.12.2 onUpdate: exhaust every tick once launched; while counting down, one tick in (countdown / 100), so it builds
     * up towards lift-off; never without fuel, nor with the client's disableSpaceshipParticles.
     */
    static boolean showsExhaust(RocketEntity rocket) {
        if (!rocket.hasFuel() || GCConfig.client(GCConfig.DISABLE_SPACESHIP_PARTICLES)) {
            return false;
        }
        if (rocket.launched()) {
            return true;
        }
        if (rocket.phase() != RocketEntity.Phase.IGNITED) {
            return false;
        }
        final int countdown = rocket.countdown();
        return rocket.getRandom().nextInt(countdown >= 100 ? countdown / 100 : 1) == 0;
    }

    /** The exhaust's way out, {@code scale} blocks a tick: straight down from an upright rocket (x, y, z). */
    private static double[] exhaust(Entity vehicle, double scale) {
        final double sinPitch = Math.sin(Math.toRadians(vehicle.getXRot()));
        final double yaw = Math.toRadians(vehicle.getYRot());
        return new double[] {scale * Math.cos(yaw) * sinPitch, scale * Math.cos(Math.toRadians(vehicle.getXRot() - 180.0F)),
                scale * Math.sin(yaw) * sinPitch};
    }

    private static void flame(Entity vehicle, boolean launched, double x, double y, double z, double dx, double dy, double dz) {
        vehicle.level().addAlwaysVisibleParticle(launched ? GCParticles.LAUNCH_FLAME.get() : GCParticles.LAUNCH_FLAME_IDLE.get(), true,
                x, y, z, dx, dy, dz);
    }

    /** The nine-flame jet: four jittered corners, the middle and the four sides, {@code spread} out from (x, z). */
    private static void jet(Entity vehicle, boolean launched, double spread, double x, double y, double z, double dx, double dy, double dz) {
        final RandomSource r = vehicle.getRandom();
        flame(vehicle, launched, x + spread - r.nextDouble() / 10.0D, y, z + spread - r.nextDouble() / 10.0D, dx, dy, dz);
        flame(vehicle, launched, x - spread + r.nextDouble() / 10.0D, y, z + spread - r.nextDouble() / 10.0D, dx, dy, dz);
        flame(vehicle, launched, x - spread + r.nextDouble() / 10.0D, y, z - spread + r.nextDouble() / 10.0D, dx, dy, dz);
        flame(vehicle, launched, x + spread - r.nextDouble() / 10.0D, y, z - spread + r.nextDouble() / 10.0D, dx, dy, dz);
        flame(vehicle, launched, x, y, z, dx, dy, dz);
        flame(vehicle, launched, x + spread, y, z, dx, dy, dz);
        flame(vehicle, launched, x - spread, y, z, dx, dy, dz);
        flame(vehicle, launched, x, y, z + spread, dx, dy, dz);
        flame(vehicle, launched, x, y, z - spread, dx, dy, dz);
    }

    /** 1.12.2 EntityTier1Rocket#spawnParticles: the jet once launched; four little flames every other tick on the pad. */
    static void tier1(RocketEntity rocket) {
        final double[] v = exhaust(rocket, 2.0D);
        final Vec3 m = rocket.getDeltaMovement();
        double y = rocket.getY() + v[1] - m.y + 1.2D;
        final double x2 = rocket.getX() + v[0] - m.x;
        final double z2 = rocket.getZ() + v[2] - m.z;
        if (rocket.launched()) {
            jet(rocket, true, 0.4D, x2, y, z2, v[0], v[1], v[2]);
        } else if (rocket.tickCount % 2 == 0) {
            final RandomSource r = rocket.getRandom();
            y += 0.6D;
            final double[][] corners = {{0.4D, 0.4D}, {-0.4D, 0.4D}, {-0.4D, -0.4D}, {0.4D, -0.4D}};
            for (double[] corner : corners) {
                flame(rocket, false, x2 + corner[0] - Math.signum(corner[0]) * r.nextDouble() / 10.0D, y,
                        z2 + corner[1] - Math.signum(corner[1]) * r.nextDouble() / 10.0D, r.nextDouble() / 2.0D - 0.25D, 0.0D, r.nextDouble() / 2.0D - 0.25D);
            }
        }
    }

    /**
     * 1.12.2 EntityTier2Rocket#spawnParticles: the jet, and behind it a larger fireball of sixteen flames (on the pad
     * too, every other tick, higher up). 1.12.2 also asked for four "blueflame" jets, which its client never drew.
     */
    static void tier2(RocketEntity rocket) {
        final boolean launched = rocket.launched();
        if (rocket.tickCount % 2 == 0 && !launched) {
            return;
        }
        final double[] v = exhaust(rocket, 2.9D);
        final Vec3 m = rocket.getDeltaMovement();
        final double y = rocket.getY() + v[1] - m.y + (launched ? 1.0D : 2.5D);
        final double x2 = rocket.getX() + v[0] - m.x;
        final double z2 = rocket.getZ() + v[2] - m.z;
        final double x3 = x2 + v[0] / 2.0D;
        final double y3 = y + v[1] / 2.0D;
        final double z3 = z2 + v[2] / 2.0D;
        jet(rocket, launched, 0.4D, x2, y, z2, v[0], v[1], v[2]);
        final RandomSource r = rocket.getRandom();
        final double a = 4.0D;
        final double bx = v[0] + 0.5D / a;
        final double bz = v[2] + 0.5D / a;
        final double[][] ball = {
                {0.2D, 0.4D, 0.2D, 6}, {-0.2D, 0.4D, 0.2D, 6}, {-0.2D, 0.4D, -0.2D, 6}, {0.2D, 0.4D, -0.2D, 6},
                {0.2D, -0.4D, 0.2D, 6}, {-0.2D, -0.4D, 0.2D, 6}, {-0.2D, -0.4D, -0.2D, 6}, {0.2D, -0.4D, -0.2D, 6},
                {0.7D, 0.0D, 0.7D, 8}, {-0.7D, 0.0D, 0.7D, 8}, {-0.7D, 0.0D, -0.7D, 8}, {0.7D, 0.0D, -0.7D, 8}};
        for (double[] b : ball) {
            flame(rocket, launched, x3 + b[0] - Math.signum(b[0]) * r.nextDouble() / b[3], y3 + b[1], z3 + b[2] - Math.signum(b[2]) * r.nextDouble() / b[3],
                    bx - r.nextDouble() / a, v[1], bz - r.nextDouble() / a);
        }
        flame(rocket, launched, x3 + 0.7D - r.nextDouble() / 8.0D, y3, z3 - r.nextDouble() / 8.0D, bx - r.nextDouble() / a, v[1], bz - r.nextDouble() / a);
        flame(rocket, launched, x3 - 0.7D + r.nextDouble() / 8.0D, y3, z3 - r.nextDouble() / 8.0D, bx - r.nextDouble() / a, v[1], bz - r.nextDouble() / a);
        flame(rocket, launched, x3 + r.nextDouble() / 8.0D, y3, z3 + 0.7D + r.nextDouble() / 8.0D, bx - r.nextDouble() / a, v[1], bz - r.nextDouble() / a);
        flame(rocket, launched, x3 - r.nextDouble() / 8.0D, y3, z3 - 0.7D + r.nextDouble() / 8.0D, bx - r.nextDouble() / a, v[1], bz - r.nextDouble() / a);
    }

    /**
     * 1.12.2 EntityTier3Rocket#spawnParticles: four jets round the exhaust, turned with the rocket (each tipped a tenth
     * of the way out) and spreading as they go; on the pad, eight flames from each, every other tick.
     */
    static void tier3(RocketEntity rocket) {
        final double[] v = exhaust(rocket, 3.2D);
        final Vec3 m = rocket.getDeltaMovement();
        final double y2 = rocket.getY() + v[1] - 0.75D * m.y - 0.3D + 1.2D;
        final double x2 = rocket.getX() + v[0] + m.x;
        final double z2 = rocket.getZ() + v[2] + m.z;
        final double[] motion = {v[0] + m.x, v[1] + m.y, v[2] + m.z};
        final float angle = 315.0F - rocket.getYRot();
        final double[][] offsets = {{v[1] * 0.1D, -v[0] * 0.1D, v[2] * 0.1D}, {v[0] * 0.1D, -v[2] * 0.1D, v[1] * 0.1D},
                {-v[1] * 0.1D, v[0] * 0.1D, v[2] * 0.1D}, {v[0] * 0.1D, v[2] * 0.1D, -v[1] * 0.1D}};
        for (double[] offset : offsets) {
            final double[] d = rotate(offset, angle, motion);
            tier3Jet(rocket, x2 + d[0], y2 + d[1], z2 + d[2], motion[0] + d[0], motion[1] + d[1], motion[2] + d[2]);
        }
    }

    /** 1.12.2 EntityTier3Rocket#makeFlame. */
    private static void tier3Jet(RocketEntity rocket, double x, double y, double z, double mx, double my, double mz) {
        if (rocket.launched()) {
            jet(rocket, true, 0.4D, x, y, z, mx, my, mz);
            return;
        }
        if (rocket.tickCount % 2 == 0) {
            return;
        }
        final RandomSource r = rocket.getRandom();
        y += 1.6D;
        final double[][] corners = {{0.4D, 0.4D}, {-0.4D, 0.4D}, {-0.4D, -0.4D}, {0.4D, -0.4D}};
        for (double[] c : corners) {
            final double sx = Math.signum(c[0]);
            final double sz = Math.signum(c[1]);
            flame(rocket, false, x + c[0] - sx * r.nextDouble() / 10.0D, y, z + c[1] - sz * r.nextDouble() / 10.0D,
                    mx + sx * (0.1D + r.nextDouble() / 10.0D), my - 0.3D, mz + sz * (0.1D + r.nextDouble() / 10.0D));
        }
        flame(rocket, false, x + 0.4D, y, z, mx + 0.3D, my - 0.3D, mz);
        flame(rocket, false, x - 0.4D, y, z, mx - 0.3D, my - 0.3D, mz);
        flame(rocket, false, x, y, z + 0.4D, mx, my - 0.3D, mz + 0.3D);
        flame(rocket, false, x, y, z - 0.4D, mx, my - 0.3D, mz - 0.3D);
    }

    /**
     * 1.12.2 EntityCargoRocket#spawnParticles: a tighter jet from just under the rocket; coming in to land, the exhaust
     * is pushed out harder the higher it is (1.12.2: over the target pad; here over the ground under it).
     */
    static void cargo(RocketEntity rocket) {
        final double[] v = exhaust(rocket, 2.0D);
        if (rocket.phase() == RocketEntity.Phase.LANDING) {
            final int ground = rocket.level().getHeight(Heightmap.Types.MOTION_BLOCKING, rocket.getBlockX(), rocket.getBlockZ());
            final double modifier = Math.max(rocket.getY() - ground, 1.0D) / 60.0D;
            for (int i = 0; i < 3; i++) {
                v[i] *= modifier;
            }
        }
        jet(rocket, rocket.launched(), 0.2D, rocket.getX() + v[0], rocket.getY() - 0.4D, rocket.getZ() + v[2], v[0], v[1], v[2]);
    }

    /**
     * 1.12.2 EntityLander#getParticleMap: one big flame a tick, from a block above the lander's base, shot out at 4
     * blocks a tick (down, or tilted with the lander) plus half its own fall.
     */
    static void spawnLanderFlames(Entity lander) {
        final double sinPitch = Math.sin(Math.toRadians(lander.getXRot()));
        final double yaw = Math.toRadians(lander.getYRot());
        final double motionY = lander.getDeltaMovement().y;
        lander.level().addAlwaysVisibleParticle(GCParticles.LANDER_FLAME.get(), true, lander.getX(), lander.getY() + 1.0D + motionY / 2.0D,
                lander.getZ(), 4.0D * Math.cos(yaw) * sinPitch, -4.0D * Math.abs(Math.cos(Math.toRadians(lander.getXRot()))) + motionY / 2.0D,
                4.0D * Math.sin(yaw) * sinPitch);
    }

    /** 1.12.2 Vector3#rotate(angle, axis): {@code v} turned {@code degrees} about {@code axis}, as 1.12.2's matrix did it. */
    public static double[] rotate(double[] v, float degrees, double[] axis) {
        final double length = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        if (length < 1.0E-9D) {
            return v;
        }
        final double x = axis[0] / length;
        final double y = axis[1] / length;
        final double z = axis[2] / length;
        final float radians = degrees * Mth.DEG_TO_RAD;
        final double cos = Mth.cos(radians);
        final double sin = Mth.sin(radians);
        final double ocos = 1.0D - cos;
        return new double[] {
                v[0] * (x * x * ocos + cos) + v[1] * (y * x * ocos + z * sin) + v[2] * (x * z * ocos - y * sin),
                v[0] * (x * y * ocos - z * sin) + v[1] * (y * y * ocos + cos) + v[2] * (y * z * ocos + x * sin),
                v[0] * (x * z * ocos + y * sin) + v[1] * (y * z * ocos - x * sin) + v[2] * (z * z * ocos + cos)};
    }

    // ------------------------------------------------------------------ burns (server)

    /**
     * 1.12.2 ParticleLaunchFlame: a flame set a living thing it touched alight for 3 seconds, the rider aside. 1.12.2's
     * client told the server (S_SET_ENTITY_FIRE); here the server burns what is where the flames go: the column under a
     * launched rocket, and round its foot while it counts down.
     */
    static void burn(RocketEntity rocket) {
        final AABB zone = rocket.launched()
                ? new AABB(rocket.getX() - 1.6D, rocket.getY() - 30.0D, rocket.getZ() - 1.6D, rocket.getX() + 1.6D, rocket.getY() + 1.0D, rocket.getZ() + 1.6D)
                : new AABB(rocket.getX() - 3.0D, rocket.getY() - 1.5D, rocket.getZ() - 3.0D, rocket.getX() + 3.0D, rocket.getY() + 2.0D, rocket.getZ() + 3.0D);
        ignite(rocket, zone);
    }

    /** 1.12.2 ParticleLanderFlame: likewise, the column of flame under a lander still flying. */
    static void burnUnderLander(Entity lander) {
        ignite(lander, new AABB(lander.getX() - 1.2D, lander.getY() - 20.0D, lander.getZ() - 1.2D, lander.getX() + 1.2D, lander.getY() + 1.0D,
                lander.getZ() + 1.2D));
    }

    private static void ignite(Entity vehicle, AABB zone) {
        for (LivingEntity living : vehicle.level().getEntitiesOfClass(LivingEntity.class, zone,
                entity -> entity.isAlive() && !entity.isOnFire() && !vehicle.hasPassenger(entity))) {
            living.igniteForSeconds(3.0F);
        }
    }
}
