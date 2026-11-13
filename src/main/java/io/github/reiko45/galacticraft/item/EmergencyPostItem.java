/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemBlockEmergencyBox). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.block.EmergencyPostBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/**
 * 1.12.2 ItemBlockEmergencyBox: the Emergency Post, and the one with a kit in it (1.12.2 metadata 1; here the block
 * state it places, which the block drops again).
 */
public class EmergencyPostItem extends BlockItem {
    public EmergencyPostItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean hasKit(ItemStack stack) {
        return Boolean.TRUE.equals(stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(EmergencyPostBlock.KIT));
    }

    public static ItemStack withKit(ItemStack stack) {
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(EmergencyPostBlock.KIT, true));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(hasKit(stack) ? getDescriptionId() + ".filled" : getDescriptionId());
    }
}
