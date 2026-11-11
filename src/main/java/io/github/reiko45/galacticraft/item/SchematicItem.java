package io.github.reiko45.galacticraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.12.2 ItemSchematic: a NASA Workbench schematic found in dungeons. Placed in the workbench's "Add New Schematic"
 * page it unlocks the page with this {@link #schematic() id} for the player, for good.
 */
public class SchematicItem extends Item {
    private final String schematic;

    public SchematicItem(Properties properties, String schematic) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.schematic = schematic;
    }

    /** The workbench page this unlocks (see {@code menu.Schematics}). */
    public String schematic() {
        return this.schematic;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("schematic.galacticraftcore." + this.schematic).withStyle(ChatFormatting.GRAY));
    }
}
