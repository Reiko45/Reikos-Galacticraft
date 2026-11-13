/*
 * Zero-g movement derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.player.FreefallHandler
 * and SpinManager#updatePlayerForSpin/#applyCentrifugalForce). Copyright (c) 2023 Team Galacticraft. Licensed under the
 * MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.station;

import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Zero gravity in orbit (1.12.2 FreefallHandler). A player who isn't touching anything — no block in or just around
 * them, reaching 0.2 further the way they face — is in freefall: they keep drifting the way they were going, Forward
 * pushes them gently on (1/200 per tick), Jump and Sneak drift them up and down, and nothing goes faster than 0.7
 * blocks per tick. Grab a wall or stand on a floor and normal (very light) gravity takes over. While the station spins
 * (see {@link StationSpin}) a floating player is carried round with it and turns with the sky; brushing against the
 * station then pushes them outwards, like the 1.12.2 centrifugal force. The driving player's own game runs this.
 */
public final class Freefall {
    private static final double THRUST = 1.0D / 200.0D;
    private static final double LIFT = 0.0032D;
    private static final double SPEED_LIMIT = 0.7D;
    /** 1.12.2 hard mode: a third of the thrust, less lift, but a higher top speed. */
    private static final double HARD_THRUST = 1.0D / 600.0D;
    private static final double HARD_LIFT = 0.002D;
    private static final double HARD_SPEED_LIMIT = 0.9D;

    private Freefall() {
    }

    /**
     * 1.12.2 EventHandlerGC#blockBreakSpeed: in orbit, a player off the floor digs five times as fast (making up for
     * the vanilla off-the-ground slowdown), except in hard mode.
     */
    public static void onBreakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        final Player player = event.getEntity();
        if (!player.onGround() && player.level().dimension().equals(GCDimensions.ORBIT) && !io.github.reiko45.galacticraft.GCConfig.hardMode()
                && event.getOriginalSpeed() < 5.0F) {
            event.setNewSpeed(event.getOriginalSpeed() * 5.0F);
        }
    }

    /** 1.12.2 FreefallHandler#testFreefall(EntityPlayerSP, boolean). */
    public static boolean inFreefall(Player player) {
        if (!player.level().dimension().equals(GCDimensions.ORBIT) || player.onGround() || player.isPassenger()
                || player.getAbilities().flying || player.isSpectator()) {
            return false;
        }
        // Reach 0.2 the way the player faces, split into overlapping quarters as in 1.12.2 (whose east/west reach
        // pointed backwards; here it points the way the player looks)
        final float rY = Mth.positiveModulo(player.getYRot(), 360.0F);
        double zReach = 0.0D;
        double xReach = 0.0D;
        if (rY < 80.0F || rY > 280.0F) {
            zReach = 0.2D;
        }
        if (rY < 170.0F && rY > 10.0F) {
            xReach = -0.2D;
        }
        if (rY < 260.0F && rY > 100.0F) {
            zReach = -0.2D;
        }
        if (rY < 350.0F && rY > 190.0F) {
            xReach = 0.2D;
        }
        final AABB reach = player.getBoundingBox().expandTowards(xReach, 0.0D, zReach);
        final GCNetwork.StationSpinSync spin = player.level().isClientSide ? StationSpin.Client.at(player.blockPosition()) : null;
        if (spin != null) {
            final int[] b = spin.bounds();
            if (reach.maxX < b[0] || reach.minX > b[1] || reach.maxY < b[2] || reach.minY > b[3] || reach.maxZ < b[4] || reach.minZ > b[5]) {
                return true; // nowhere near the station
            }
        }
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(reach.minX); x <= Mth.floor(reach.maxX); x++) {
            for (int y = Mth.floor(reach.minY); y <= Mth.floor(reach.maxY); y++) {
                for (int z = Mth.floor(reach.minZ); z <= Mth.floor(reach.maxZ); z++) {
                    if (!player.level().getBlockState(pos.set(x, y, z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Movement for one tick in freefall, instead of the vanilla walk/fall (1.12.2 FreefallHandler#freefallMotion):
     * momentum kept, gentle thrust, speed limit.
     */
    public static void travel(Player player, boolean jumping) {
        final boolean hard = io.github.reiko45.galacticraft.GCConfig.hardMode();
        final double thrust = hard ? HARD_THRUST : THRUST;
        final double lift = hard ? HARD_LIFT : LIFT;
        final double limit = hard ? HARD_SPEED_LIMIT : SPEED_LIMIT;
        final Vec3 motion = player.getDeltaMovement();
        double x = motion.x;
        double y = motion.y;
        double z = motion.z;
        if (player.zza != 0.0F) {
            x -= player.zza * Mth.sin(player.getYRot() * Mth.DEG_TO_RAD) * thrust;
            z += player.zza * Mth.cos(player.getYRot() * Mth.DEG_TO_RAD) * thrust;
        }
        if (player.isShiftKeyDown()) {
            y -= lift;
        }
        if (jumping) {
            y += lift;
        }
        player.setDeltaMovement(Mth.clamp(x, -limit, limit), Mth.clamp(y, -limit, limit), Mth.clamp(z, -limit, limit));
        player.move(MoverType.SELF, player.getDeltaMovement());
        player.resetFallDistance();
    }

    /**
     * 1.12.2 SpinManager#updatePlayerForSpin (per tick): carry a floating player round the spinning station and turn
     * them with it; if that bumps them into the station, push them outwards instead (applyCentrifugalForce).
     */
    public static void spin(Player player) {
        final GCNetwork.StationSpinSync spin = StationSpin.Client.at(player.blockPosition());
        if (spin == null || spin.angularVelocity() == 0.0F || !inFreefall(player)) {
            return;
        }
        final double xx = player.getX() - spin.centreX();
        final double zz = player.getZ() - spin.centreZ();
        final float angle = (float) Math.atan2(zz, xx) + spin.angularVelocity() / 3.0F;
        final double arc = Math.sqrt(xx * xx + zz * zz) * spin.angularVelocity();
        double offsetX = -arc * Mth.sin(angle);
        double offsetZ = arc * Mth.cos(angle);
        boolean centrifugal = false;
        if (player.level().noCollision(player, player.getBoundingBox())) {
            while (!player.level().noCollision(player, player.getBoundingBox().move(offsetX, 0.0D, offsetZ))) {
                if (!centrifugal) {
                    player.setDeltaMovement(player.getDeltaMovement().add(-offsetX, 0.0D, -offsetZ));
                }
                offsetX = Math.abs(offsetX / 2.0D) < 0.01D ? 0.0D : offsetX / 2.0D;
                offsetZ = Math.abs(offsetZ / 2.0D) < 0.01D ? 0.0D : offsetZ / 2.0D;
                centrifugal = true;
                if (offsetX == 0.0D && offsetZ == 0.0D) {
                    break;
                }
            }
            player.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
        }
        final float degrees = spin.angularVelocity() * Mth.RAD_TO_DEG;
        player.setYRot(player.getYRot() + degrees);
        player.yBodyRot += degrees;
        if (centrifugal) {
            // 1.12.2 applyCentrifugalForce: outwards along the nearest axis
            final double accel = Math.sqrt(xx * xx + zz * zz) * spin.angularVelocity() * spin.angularVelocity() * 4.0D;
            if (Math.abs(xx) > Math.abs(zz)) {
                player.setDeltaMovement(player.getDeltaMovement().add(Math.signum(xx) * accel, 0.0D, 0.0D));
            } else {
                player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.0D, Math.signum(zz) * accel));
            }
        }
    }
}
