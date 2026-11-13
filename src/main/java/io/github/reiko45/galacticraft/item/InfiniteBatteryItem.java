/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemBatteryInfinite). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Infinite Battery (1.12.2 ItemBatteryInfinite, creative only): in a machine's battery slot it gives whatever the
 * machine can take, counted as tier 3 power, and never runs down; it can't be charged.
 */
public class InfiniteBatteryItem extends ElectricItem {
    public InfiniteBatteryItem(Properties properties) {
        super(properties.stacksTo(1), Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    public float stored(ItemStack stack) {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public void setStored(ItemStack stack, float gJ) {
    }

    @Override
    public float charge(ItemStack stack, float amount, boolean simulate) {
        return 0.0F;
    }

    @Override
    public float discharge(ItemStack stack, float amount, boolean simulate) {
        return Math.max(0.0F, amount);
    }

    @Override
    public int tier() {
        return 3;
    }

    @Override
    public ItemStack charged() {
        return new ItemStack(this);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.galacticraftcore.infinite_item").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("gui.galacticraftcore.creative_only.desc").withStyle(ChatFormatting.RED));
    }
}
