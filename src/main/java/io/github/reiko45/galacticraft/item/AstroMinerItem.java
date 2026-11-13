/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.items.ItemAstroMiner). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import io.github.reiko45.galacticraft.entity.AstroMinerEntity;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.world.station.SpaceStations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Astro Miner (1.12.2 ItemAstroMiner): used on an Astro Miner Base, it docks a new miner there for you. Not on a space
 * station, not on a base that has one already, and not more than {@code maximumAstroMiners} at once (creative players
 * excepted).
 */
public class AstroMinerItem extends Item implements HoldableItem {
    public AstroMinerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MinerBaseBlockEntity base) || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final ServerPlayer player = (ServerPlayer) context.getPlayer();
        if (context.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel && SpaceStations.isOrbit(serverLevel)) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.fail_7"));
            return InteractionResult.FAIL;
        }
        if (base.linkedMinerId() != null) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.fail_linked"));
            return InteractionResult.FAIL;
        }
        // 1.12.2: give any loaded miner a moment to link itself to a freshly loaded base
        final MinerBaseBlockEntity master = base.getMaster();
        if (master == null || master.ticks() < 15) {
            return InteractionResult.FAIL;
        }
        final GCPlayerData data = GCPlayerData.get(player);
        if (data.astroMinerCount >= GCConfig.ASTRO_MINER_MAX.get() && !player.getAbilities().instabuild) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.fail_2"));
            return InteractionResult.FAIL;
        }
        if (!base.spawnMiner(player)) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.fail_1").append(" ")
                    .append(AstroMinerEntity.sharedBlocker.getName()));
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            data.astroMinerCount++;
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
