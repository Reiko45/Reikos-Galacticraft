/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemPreLaunchChecklist). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 1.12.2 ItemPreLaunchChecklist: a book of things to do before flying to each world, ticked off by hand; the ticks are
 * kept on the stack ("checklistData"). It can't be enchanted.
 */
public class PrelaunchChecklistItem extends Item {
    public PrelaunchChecklistItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            io.github.reiko45.galacticraft.client.ClientPayloads.openChecklist(hand, checklist(stack));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /** The ticked boxes, by key. */
    public static CompoundTag checklist(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("checklistData");
    }

    public static void setChecklist(ItemStack stack, CompoundTag data) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("checklistData", data));
    }
}
