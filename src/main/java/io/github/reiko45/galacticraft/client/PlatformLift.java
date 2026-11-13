/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityPlatform#updateClient and
 * entities.player.StatsClientCapability#startPlatformAscent / getPlatformVelocity). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.block.HydraulicPlatformBlock;
import io.github.reiko45.galacticraft.block.entity.HydraulicPlatformBlockEntity;
import io.github.reiko45.galacticraft.network.PlatformNetwork;
import io.github.reiko45.galacticraft.world.GCDimensions;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Hydraulic Platform lift, run by the client as in 1.12.2: standing on the middle of a platform, sneak to sink to
 * the next platform below or jump to rise to the next above. The player is moved straight up or down, speeding up to
 * 0.45 blocks a tick and easing to 0.08 near the end, through the platform in between (whose blocks stop colliding,
 * here and, told by a packet, on the server).
 */
public final class PlatformLift {
    @Nullable
    private static HydraulicPlatformBlockEntity through;
    @Nullable
    private static HydraulicPlatformBlockEntity lower;
    private static double targetY;
    private static double velocity;
    private static boolean packetSent;

    private PlatformLift() {
    }

    public static boolean controlled() {
        return through != null;
    }

    /** The platform the moving floor is drawn for while the lift runs (the lower one). */
    public static boolean drawsMovingFloor(HydraulicPlatformBlockEntity platform) {
        return platform == lower;
    }

    /** 1.12.2 updateClient on the north-west corner: light up under a player, and start the lift. */
    public static void check(HydraulicPlatformBlockEntity platform) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        final var pos = platform.getBlockPos();
        final AABB middle = new AABB(pos.getX() + 0.9D, pos.getY() + 0.75D, pos.getZ() + 0.9D, pos.getX() + 1.1D, pos.getY() + 1.85D, pos.getZ() + 1.1D);
        if (platform.getLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, middle).isEmpty()) {
            return;
        }
        platform.setLightOn();
        if (player == null || controlled() || player.isPassenger() || !player.getBoundingBox().intersects(middle)) {
            return;
        }
        if (player.input.shiftKeyDown) {
            final int down = platform.nextPlatform(-1, player.getBoundingBox());
            if (down == -1) {
                platform.refuse();
            } else if (down > 0 && platform.getLevel().getBlockEntity(pos.below(down)) instanceof HydraulicPlatformBlockEntity target) {
                start(platform, target, pos.getY() - down + (player.level().dimension().equals(GCDimensions.ORBIT) ? 0.97D : HydraulicPlatformBlock.HEIGHT));
            }
        } else if (player.input.jumping) {
            final int up = platform.nextPlatform(1, player.getBoundingBox());
            if (up == -1) {
                platform.refuse();
            } else if (up > 0 && platform.getLevel().getBlockEntity(pos.above(up)) instanceof HydraulicPlatformBlockEntity target) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                start(target, platform, pos.getY() + up + HydraulicPlatformBlock.HEIGHT + 0.01D);
            }
        }
    }

    /** 1.12.2 startPlatformAscent: {@code passThrough} is the upper platform, which stops colliding on the way. */
    private static void start(HydraulicPlatformBlockEntity passThrough, HydraulicPlatformBlockEntity bottom, double target) {
        through = passThrough;
        lower = bottom;
        passThrough.markNoCollide(true);
        passThrough.setMoving(true);
        bottom.setMoving(true);
        targetY = target;
        velocity = 0.0D;
        packetSent = false;
        PacketDistributor.sendToServer(new PlatformNetwork.PlatformLift(passThrough.getBlockPos(), true));
    }

    /** 1.12.2 PlayerClient: while the lift runs, the player only goes straight up or down. */
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !controlled()) {
            return;
        }
        player.setDeltaMovement(0.0D, velocity(player.getY()), 0.0D);
        player.fallDistance = 0.0F;
    }

    /** 1.12.2 getPlatformVelocity. */
    private static double velocity(double y) {
        final double delta = targetY - y + 0.03D;
        if (Math.abs(delta) < 0.04D || through == null || through.isRemoved()) {
            finish();
            return 0.0D;
        }
        final double wanted;
        if (delta > 0.0D) {
            wanted = delta < 1.0D + 8 * velocity * velocity ? 0.08D : 0.45D;
            if (delta < 0.6D) {
                sendEnd();
            }
        } else {
            wanted = delta > -1.0D - 8 * velocity * velocity ? -0.08D : -0.45D;
            if (delta > -1.0D) {
                sendEnd();
            }
        }
        if (velocity < wanted) {
            velocity = Math.min(wanted, velocity + 0.036D);
        } else if (velocity > wanted) {
            velocity = Math.max(wanted, velocity - 0.036D);
        }
        return velocity;
    }

    private static void sendEnd() {
        if (!packetSent && through != null) {
            PacketDistributor.sendToServer(new PlatformNetwork.PlatformLift(through.getBlockPos(), false));
            packetSent = true;
        }
    }

    private static void finish() {
        velocity = 0.0D;
        sendEnd();
        if (through != null) {
            through.markNoCollide(false);
            through.setMoving(false);
        }
        if (lower != null) {
            lower.setMoving(false);
        }
        through = null;
        lower = null;
    }
}
