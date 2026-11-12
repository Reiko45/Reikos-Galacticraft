package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import net.minecraft.core.component.DataComponents;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Port of 1.12.2 {@code ItemElectricBase}: an item storing gJ, shown as a durability-style bar.
 * Charge lives in the {@link GCDataComponents#ENERGY} component instead of NBT. Like 1.12.2, empty items stack
 * (up to the item's max stack size) while charged ones do not; machines only charge or drain a single item.
 */
public class ElectricItem extends Item {
    private final float capacity;
    private final float transferRate;

    public ElectricItem(Properties properties, float capacity, float transferRate) {
        super(properties);
        this.capacity = capacity;
        this.transferRate = transferRate;
    }

    public float capacity() {
        return this.capacity;
    }

    public float stored(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.ENERGY.get(), 0.0F);
    }

    public void setStored(ItemStack stack, float gJ) {
        final float value = Mth.clamp(gJ, 0.0F, this.capacity);
        if (value > 0.0F) {
            stack.set(GCDataComponents.ENERGY.get(), value);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            stack.remove(GCDataComponents.ENERGY.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
    }

    /** @return gJ accepted */
    public float charge(ItemStack stack, float amount, boolean simulate) {
        if (stack.getCount() != 1) {
            return 0.0F;
        }
        final float accepted = Math.max(0.0F, Math.min(Math.min(amount, this.transferRate), this.capacity - stored(stack)));
        if (!simulate && accepted > 0.0F) {
            setStored(stack, stored(stack) + accepted);
        }
        return accepted;
    }

    /** @return gJ removed */
    public float discharge(ItemStack stack, float amount, boolean simulate) {
        if (stack.getCount() != 1) {
            return 0.0F;
        }
        final float removed = Math.max(0.0F, Math.min(Math.min(amount, this.transferRate), stored(stack)));
        if (!simulate && removed > 0.0F) {
            setStored(stack, stored(stack) - removed);
        }
        return removed;
    }

    /** The power tier a machine running off this item counts as (1.12.2 IItemElectricBase#getTierGC). */
    public int tier() {
        return 1;
    }

    /** A copy of this item fully charged (for the creative tab). */
    public ItemStack charged() {
        final ItemStack stack = new ItemStack(this);
        setStored(stack, this.capacity);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * stored(stack) / this.capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Red when empty, green when full, like the 1.12.2 durability colour.
        return Mth.hsvToRgb(Math.max(0.0F, stored(stack) / this.capacity) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(EnergyDisplay.format(stored(stack)) + " / " + EnergyDisplay.format(this.capacity)));
    }
}
