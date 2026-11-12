/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.items.ItemGrappleHook). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.entity.GrappleEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * Grapple (1.12.2 ItemGrappleHook, a bow underneath): draw and let go to fire the hook, whatever the draw. Each shot
 * takes a string from your inventory (given back when you collect the hook) and wears the grapple by one of 384 uses;
 * creative players need no string.
 */
public class GrappleItem extends Item {
    public GrappleItem(Properties properties) {
        super(properties.durability(384));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        final boolean creative = player.getAbilities().instabuild;
        ItemStack string = ItemStack.EMPTY;
        if (!creative) {
            for (ItemStack candidate : player.getInventory().items) {
                if (candidate.is(Tags.Items.STRINGS)) {
                    string = candidate;
                    break;
                }
            }
        }
        if (!creative && string.isEmpty()) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("gui.galacticraftcore.grapple.fail"), false);
            }
            return;
        }
        final ItemStack pickup = string.isEmpty() ? ItemStack.EMPTY : string.copyWithCount(1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
        if (!level.isClientSide()) {
            final GrappleEntity grapple = GrappleEntity.shoot(level, player, 2.0F, pickup);
            grapple.setPickup(creative ? 2 : 1);
            level.addFreshEntity(grapple);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
        if (!creative) {
            string.shrink(1);
        }
    }
}
