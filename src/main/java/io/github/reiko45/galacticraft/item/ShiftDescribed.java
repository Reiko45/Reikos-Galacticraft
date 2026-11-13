package io.github.reiko45.galacticraft.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A block or item whose "Press Shift for more info" text (1.12.2 IShiftDescription) is not under its own
 * {@code <description id>.description} key: several blocks sharing one text, or a text only some variants have.
 */
public interface ShiftDescribed {
    /** The translation key of the text shown while the sneak key is held, or null for none. */
    @Nullable
    String shiftDescriptionKey(ItemStack stack);
}
