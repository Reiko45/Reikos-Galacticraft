package io.github.reiko45.galacticraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/** 1.12.2 ItemKey: dropped by a dungeon boss, opens the treasure chest of the same tier. */
public class DungeonKeyItem extends Item {
    private final int tier;

    public DungeonKeyItem(Properties properties, int tier) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.tier = tier;
    }

    public int tier() {
        return this.tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.galacticraftcore.tier_1_dungeon_key.desc").withStyle(ChatFormatting.GRAY));
    }
}
