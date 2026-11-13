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
        return wrench(context);
    }

    /**
     * What a wrench does to a Galacticraft block. Any wrench in the common {@code c:tools/wrench} tag does it too (1.12.2
     * BlockAdvanced took BuildCraft's and IndustrialCraft's wrenches), so BuildCraft Reborn's wrench turns Galacticraft
     * machines; the Standard Wrench is in that tag, so other mods take it as a wrench.
     */
    public static InteractionResult wrench(UseOnContext context) {
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

    /** A Galacticraft block a wrench does something to. */
    public static boolean wrenchable(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock || state.getBlock() instanceof io.github.reiko45.galacticraft.block.SpinThrusterBlock
                || state.getBlock() instanceof io.github.reiko45.galacticraft.block.Wrenchable || state.getBlock() instanceof MachineBlock;
    }

    /** Another mod's wrench (in c:tools/wrench) used on a Galacticraft block works as the Standard Wrench does. */
    public static void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        final net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof WrenchItem || !stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)
                || !wrenchable(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        final InteractionResult result = wrench(new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec()));
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
