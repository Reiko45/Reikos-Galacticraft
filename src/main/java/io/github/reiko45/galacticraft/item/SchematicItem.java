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

    /** 1.12.2 ItemSchematic#onItemUse: hang it on the side of a block, like a painting. */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        final net.minecraft.core.Direction face = context.getClickedFace();
        final net.minecraft.core.BlockPos pos = context.getClickedPos().relative(face);
        final net.minecraft.world.entity.player.Player player = context.getPlayer();
        final int index = io.github.reiko45.galacticraft.entity.HangingSchematicEntity.indexOf(context.getItemInHand());
        if (face.getAxis().isVertical() || index < 0 || player != null && !player.mayUseItemAt(pos, face, context.getItemInHand())) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        final net.minecraft.world.level.Level level = context.getLevel();
        final io.github.reiko45.galacticraft.entity.HangingSchematicEntity hanging =
                new io.github.reiko45.galacticraft.entity.HangingSchematicEntity(level, pos, face, index);
        if (hanging.survives()) {
            if (!level.isClientSide) {
                hanging.playPlacementSound();
                level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, hanging.position());
                level.addFreshEntity(hanging);
            }
            context.getItemInHand().shrink(1);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("schematic.galacticraftcore." + this.schematic).withStyle(ChatFormatting.GRAY));
    }
}
