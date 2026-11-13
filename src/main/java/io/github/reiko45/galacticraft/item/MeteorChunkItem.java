/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemMeteorChunk). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.entity.MeteorChunkEntity;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.12.2 ItemMeteorChunk (Throwable Meteor Chunk), stacks of 16: right-click throws it. Smelted it comes out hot (a burn
 * time on the stack, 1.12.2 metadata 1): a hot chunk sets what it hits on fire, and cools in the inventory after 45
 * shown seconds (counted at half speed, as 1.12.2 did). Dispensers throw them too.
 */
public class MeteorChunkItem extends Item implements ProjectileItem {
    /** 1.12.2 METEOR_BURN_TIME. */
    public static final float BURN_TIME = 45 * 20;

    public MeteorChunkItem(Properties properties) {
        super(properties);
    }

    public static boolean isHot(ItemStack stack) {
        return stack.has(GCDataComponents.METEOR_BURN_TIME.get());
    }

    public static ItemStack hot(ItemStack stack) {
        stack.set(GCDataComponents.METEOR_BURN_TIME.get(), BURN_TIME);
        return stack;
    }

    /** 1.12.2 onUpdate: cool down, half a tick each tick, then become a cold chunk. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        final Float burnTime = stack.get(GCDataComponents.METEOR_BURN_TIME.get());
        if (burnTime != null && !level.isClientSide) {
            if (burnTime >= 0.5F) {
                stack.set(GCDataComponents.METEOR_BURN_TIME.get(), burnTime - 0.5F);
            } else {
                stack.remove(GCDataComponents.METEOR_BURN_TIME.get());
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isHot(stack) ? "item.galacticraftcore.meteor_chunk_hot" : getDescriptionId(stack));
    }

    /** 1.12.2 addInformation: "Hot 22.5s". */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        final Float burnTime = stack.get(GCDataComponents.METEOR_BURN_TIME.get());
        if (burnTime != null) {
            tooltip.add(Component.translatable("item.galacticraftcore.hot_description").append(" " + Math.round(burnTime / 10.0F) / 2.0F)
                    .append(Component.translatable("gui.galacticraftcore.seconds")).withStyle(ChatFormatting.GRAY));
        }
    }

    /** 1.12.2 onItemRightClick: throw it, a bit faster than an arrow from a weak pull. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final boolean hot = isHot(stack);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F,
                0.0001F / (level.random.nextFloat() * 0.1F));
        if (!level.isClientSide) {
            final MeteorChunkEntity chunk = new MeteorChunkEntity(level, player, hot);
            chunk.pickup = player.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
            level.addFreshEntity(chunk);
        }
        stack.consume(1, player);
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 1.12.2 BehaviorProjectileDispense for meteor chunks. */
    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        final MeteorChunkEntity chunk = new MeteorChunkEntity(level, position.x(), position.y(), position.z(), isHot(stack));
        chunk.pickup = AbstractArrow.Pickup.ALLOWED;
        return chunk;
    }
}
