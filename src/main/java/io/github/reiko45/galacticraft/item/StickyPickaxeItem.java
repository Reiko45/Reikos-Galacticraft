package io.github.reiko45.galacticraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.12.2 ItemPickaxeStickyMars: a Desh Pickaxe coated in slime (made by using one on a Slimeling Egg). It is the only
 * tool that can collect Creeper Eggs from Mars dungeons; otherwise it digs like a Desh Pickaxe.
 */
public class StickyPickaxeItem extends PickaxeItem {
    public StickyPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
