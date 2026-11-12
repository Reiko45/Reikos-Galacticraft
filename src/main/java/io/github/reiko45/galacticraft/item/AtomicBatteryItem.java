/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.items.ItemBatteryAtomic). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Atomic Battery (1.12.2 ItemBatteryAtomic): built round a Radioisotope Core, it never runs down but gives only 7 gJ a
 * tick, counted as tier 2 power; it can't be charged.
 */
public class AtomicBatteryItem extends ElectricItem {
    public static final float TRANSFER = 7.0F;

    public AtomicBatteryItem(Properties properties) {
        super(properties.stacksTo(1), Float.POSITIVE_INFINITY, TRANSFER);
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
        return Math.max(0.0F, Math.min(amount, TRANSFER));
    }

    @Override
    public int tier() {
        return 2;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.galacticraftcore.infinite_item").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("gui.galacticraftcore.low_energy_output").withStyle(ChatFormatting.GOLD));
    }
}
