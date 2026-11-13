package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Standard Wrench (1.12.2 ItemUniversalWrench): rotates Galacticraft machines (1 durability); sneak-use moves an
 * electric machine's energy input/output faces instead, and use on a Fluid Pipe switches it between normal and pull mode
 * (both without durability, like 1.12.2).
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof FluidPipeBlock pipe && !context.isSecondaryUseActive()) {
            if (!context.getLevel().isClientSide) {
                pipe.toggleMode(context.getLevel(), context.getClickedPos(), state);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (state.getBlock() instanceof io.github.reiko45.galacticraft.block.SpinThrusterBlock thruster) {
            if (!context.getLevel().isClientSide) {
                thruster.reverse(context.getLevel(), context.getClickedPos(), state);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (state.getBlock() instanceof io.github.reiko45.galacticraft.block.Wrenchable wrenchable) {
            if (!context.getLevel().isClientSide && wrenchable.onWrench(context.getLevel(), context.getClickedPos(), state, context.getPlayer())
                    && context.getPlayer() != null) {
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand()));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (state.getBlock() instanceof MachineBlock machine) {
            if (context.isSecondaryUseActive()) {
                if (!context.getLevel().isClientSide && !machine.cycleSides(context.getLevel(), context.getClickedPos(), state)) {
                    return InteractionResult.PASS;
                }
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
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
