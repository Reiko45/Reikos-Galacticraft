package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.block.MachineBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/** Standard Wrench (1.12.2 ItemUniversalWrench): rotates Galacticraft machines. */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof MachineBlock machine) {
            if (!context.getLevel().isClientSide) {
                machine.rotateWithWrench(context.getLevel(), context.getClickedPos(), state);
                if (context.getPlayer() != null) {
                    context.getItemInHand().hurtAndBreak(1, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand()));
                }
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
