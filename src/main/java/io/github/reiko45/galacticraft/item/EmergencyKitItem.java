/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemEmergencyKit). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.item.gear.GearItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 1.12.2 ItemEmergencyKit (Space Emergency Kit): one use. Right-click puts on an oxygen mask, oxygen gear, two full light
 * tanks and a red parachute where there is room, and hands over a Heavy Duty Pickaxe, a dehydrated potato, a potion of
 * healing and a long potion of night vision (and any gear that didn't fit).
 */
public class EmergencyKitItem extends Item {
    public EmergencyKitItem(Properties properties) {
        super(properties);
    }

    /** 1.12.2 getContents, in its slot order (also the recipe's). */
    public static List<ItemStack> contents() {
        final ItemStack healing = new ItemStack(Items.POTION);
        healing.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
        final ItemStack nightVision = new ItemStack(Items.POTION);
        nightVision.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.LONG_NIGHT_VISION));
        return List.of(new ItemStack(GCItems.OXYGEN_MASK.get()), new ItemStack(GCItems.OXYGEN_GEAR.get()),
                ((OxygenTankItem) GCItems.LIGHT_OXYGEN_TANK.get()).full(), ((OxygenTankItem) GCItems.LIGHT_OXYGEN_TANK.get()).full(),
                new ItemStack(GCItems.HEAVY_DUTY_PICKAXE.get()), new ItemStack(GCItems.DEHYDRATED_POTATO.get()), healing, nightVision,
                new ItemStack(GCItems.PARACHUTES.get(13).get()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            for (ItemStack content : contents()) {
                final ItemStack left = GearItem.equip(serverPlayer, content);
                if (!left.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, left, 0);
                }
            }
            stack.setCount(0);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
