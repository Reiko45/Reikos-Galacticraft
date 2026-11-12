/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.EventHandlerMars#onPlayerWakeUp and
 * core.entities.player.GCPlayerHandler, the cryogenic chamber cooldown). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.event;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.CryogenicChamberBlock;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sleeping in a Cryogenic Chamber: allowed by day, standing up (a standing-size player whose eyes sit where a standing
 * player's would, from the bed height vanilla puts sleepers at), and a full sleep heals and starts the cooldown.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class CryogenicSleep {
    /** Vanilla puts sleepers 0.6875 above the block; the chamber floor is 0.125: eyes 1.62 above it, less the 0.3 camera lift. */
    public static final float RENDER_DROP = 0.5625F;
    public static final EntityDimensions IN_CHAMBER = EntityDimensions.scalable(0.6F, 1.8F).withEyeHeight(1.62F - RENDER_DROP - 0.3F);

    private CryogenicSleep() {
    }

    @SubscribeEvent
    public static void onContinueSleeping(CanContinueSleepingEvent event) {
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW && CryogenicChamberBlock.inChamber(event.getEntity())) {
            event.setContinueSleeping(true);
        }
    }

    /** 1.12.2 onPlayerWakeUp: woken by the night being skipped (not by leaving), heal 5 and wait 6000 ticks. */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        final Player player = event.getEntity();
        if (!player.level().isClientSide && !event.wakeImmediately() && !event.updateLevel() && CryogenicChamberBlock.inChamber(player)) {
            player.heal(CryogenicChamberBlock.HEAL);
            GCPlayerData.get(player).cryogenicChamberCooldown = CryogenicChamberBlock.COOLDOWN;
        }
    }

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (event.getPose() == Pose.SLEEPING && event.getEntity() instanceof Player player && CryogenicChamberBlock.inChamber(player)) {
            event.setNewSize(IN_CHAMBER);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        final Player player = event.getEntity();
        // The pose arrives before the sleeping position (on the client), so the size is fixed up once both are known.
        if (player.isSleeping() && player.getBbHeight() < 1.0F && CryogenicChamberBlock.inChamber(player)) {
            player.refreshDimensions();
        }
        if (!player.level().isClientSide) {
            final GCPlayerData data = GCPlayerData.get(player);
            if (data.cryogenicChamberCooldown > 0) {
                data.cryogenicChamberCooldown--;
            }
        }
    }
}
