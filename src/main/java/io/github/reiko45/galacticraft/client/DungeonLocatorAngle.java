/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.item.TextureDungeonFinder).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.item.DungeonLocatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Dungeon Locator's needle (1.12.2 TextureDungeonFinder#updateCompass): on a world with dungeons, carried, it swings
 * towards the bearing the server sent, turned by the way the player faces; anywhere else it spins at random. Like 1.12.2's
 * one animated texture, every locator shows the same needle, moved once a tick. The model picks one of 32 frames.
 */
public final class DungeonLocatorAngle {
    public static final int FRAMES = 32;
    private static float direction;
    private static double currentAngle;
    private static double angleDelta;
    private static long lastUpdate = Long.MIN_VALUE;

    private DungeonLocatorAngle() {
    }

    public static void setDirection(float degrees) {
        direction = degrees;
    }

    /** The {@code galacticraftcore:angle} item property: the frame, as a fraction of a turn (frame / 32, centred). */
    public static float get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getGameTime() != lastUpdate) {
            lastUpdate = minecraft.level.getGameTime();
            update(minecraft.level, minecraft.player);
        }
        final int frame = Math.floorMod((int) Math.floor((currentAngle / (Math.PI * 2.0D) + 1.0D) * FRAMES), FRAMES);
        return (frame + 0.5F) / FRAMES;
    }

    private static void update(ClientLevel level, @Nullable LocalPlayer player) {
        double target = 0.0D;
        if (player != null) {
            if (DungeonLocatorItem.hasDungeons(level.dimension()) && carries(player)) {
                final double angle = (player.getYRot() - direction) % 360.0D;
                target = -Math.toRadians(angle);
            } else {
                target = Math.random() * Math.PI * 2.0D;
            }
        }
        double delta = target - currentAngle;
        while (delta < -Math.PI) {
            delta += Math.PI * 2.0D;
        }
        while (delta >= Math.PI) {
            delta -= Math.PI * 2.0D;
        }
        delta = Math.max(-1.0D, Math.min(1.0D, delta));
        angleDelta += delta * 0.1D;
        angleDelta *= 0.8D;
        currentAngle += angleDelta;
    }

    private static boolean carries(LocalPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof DungeonLocatorItem) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() instanceof DungeonLocatorItem;
    }
}
