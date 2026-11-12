/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.EventHandlerMars#onPlayerRotate and
 * #orientCamera). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import io.github.reiko45.galacticraft.block.CryogenicChamberBlock;
import io.github.reiko45.galacticraft.event.CryogenicSleep;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * A player asleep in a Cryogenic Chamber is drawn standing on its floor, facing out (1.12.2 cancelled the lying-down
 * rotation the same way): standing pose for the draw, dropped from vanilla's bed height to the floor.
 */
public final class CryogenicChamberRender {
    /** Players drawn upright this frame, to put back to the sleeping pose afterwards. */
    private static final java.util.Set<Player> UPRIGHT = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private CryogenicChamberRender() {
    }

    private static boolean upright(Player player) {
        return player.isSleeping() && player.getPose() == Pose.SLEEPING && CryogenicChamberBlock.inChamber(player);
    }

    public static void pre(RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        if (!upright(player)) {
            return;
        }
        // Vanilla draws a sleeping player even from their own first-person camera; standing, that would be inside the head
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (player == minecraft.getCameraEntity() && !minecraft.gameRenderer.getMainCamera().isDetached()) {
            event.setCanceled(true);
            return;
        }
        final BlockPos pos = player.getSleepingPos().orElseThrow();
        final BlockState state = player.level().getBlockState(pos);
        final float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        player.yBodyRot = player.yBodyRotO = yaw;
        player.yHeadRot = player.yHeadRotO = yaw;
        player.setPose(Pose.STANDING);
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0F, -CryogenicSleep.RENDER_DROP, 0.0F);
        UPRIGHT.add(player);
    }

    public static void post(RenderPlayerEvent.Post event) {
        final Player player = event.getEntity();
        if (UPRIGHT.remove(player)) {
            event.getPoseStack().popPose();
            player.setPose(Pose.SLEEPING);
        }
    }
}
