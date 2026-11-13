/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemFlag). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.entity.FlagEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Flag (1.12.2 ItemFlag): hold right-click to raise it, and let go once it is up (about 1.3 seconds) while looking at a
 * block to plant it there, flying your Space Race team's flag. Only one flag fits on a block.
 */
public class FlagItem extends Item implements HoldableItem {
    public FlagItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** 1.12.2 onPlayerStoppedUsing's power curve: (t^2 + 2t) / 3 for t seconds held, full at 1. */
    public static float charge(int useTicks) {
        final float seconds = useTicks / 20.0F;
        return Math.min(1.0F, (seconds * seconds + seconds * 2.0F) / 3.0F);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || charge(getUseDuration(stack, entity) - timeLeft) < 1.0F) {
            return;
        }
        final HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || level.isClientSide()) {
            return;
        }
        plant(level, player, stack, ((BlockHitResult) hit).getBlockPos());
    }

    /** Stands a flag on top of {@code pos}, unless one is there already; true if it was planted. */
    public static boolean plant(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (!level.getEntitiesOfClass(FlagEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1)).isEmpty()) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.flag.already_placed"));
            return false;
        }
        level.addFreshEntity(FlagEntity.create(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, (int) (player.getYRot() - 90.0F), player));
        level.playSound(null, pos, SoundType.METAL.getBreakSound(), SoundSource.BLOCKS, SoundType.METAL.getVolume(), SoundType.METAL.getPitch() + 2.0F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    /** 1.12.2 ItemFlag#shouldCrouch: the flag is carried standing. */
    @Override
    public boolean crouches(Player player) {
        return false;
    }
}
