/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.items.ItemVolcanicPickaxe). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Volcanic Pickaxe (1.12.2 ItemVolcanicPickaxe, from the Venus dungeon): diamond level, 1561 uses, very fast. Each block
 * mined takes the eight around it in the plane the player faces (a floor or ceiling when looking steeply down or up),
 * each as if the player had mined it, for a use each.
 */
public class VolcanicPickaxeItem extends PickaxeItem {
    private static boolean breaking;

    public VolcanicPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        final boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (breaking || level.isClientSide || !(miner instanceof ServerPlayer player)) {
            return result;
        }
        Direction facing = miner.getDirection();
        if (miner.getXRot() < -45.0F) {
            facing = Direction.UP;
        } else if (miner.getXRot() > 45.0F) {
            facing = Direction.DOWN;
        }
        breaking = true;
        try {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1 && !stack.isEmpty(); j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    final BlockPos other = switch (facing.getAxis()) {
                        case Y -> pos.offset(i, 0, j);
                        case X -> pos.offset(0, i, j);
                        default -> pos.offset(i, j, 0);
                    };
                    final BlockState otherState = level.getBlockState(other);
                    if (otherState.getDestroySpeed(level, other) >= 0.0F && !otherState.isAir()) {
                        // 1.12.2 replicated PlayerInteractionManager#tryHarvestBlock; this is the same thing
                        player.gameMode.destroyBlock(other);
                    }
                }
            }
        } finally {
            breaking = false;
        }
        return result;
    }
}
