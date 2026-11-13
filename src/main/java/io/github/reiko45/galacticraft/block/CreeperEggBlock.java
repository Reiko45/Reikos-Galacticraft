/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.blocks.BlockCreeperEgg). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.entity.monster.EvolvedCreeper;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Creeper Egg (1.12.2 BlockCreeperEgg), found along the walls of the Creeper Boss's room: it falls like a dragon egg but
 * never teleports. Only a Sticky Desh Pickaxe can break it (in 5 ticks) and collect it. An explosion destroys it and
 * hatches a baby Evolved Creeper three blocks above.
 */
public class CreeperEggBlock extends DragonEggBlock {
    @SuppressWarnings("unchecked")
    public static final MapCodec<DragonEggBlock> CODEC = (MapCodec<DragonEggBlock>) (MapCodec<?>) simpleCodec(CreeperEggBlock::new);

    public CreeperEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<DragonEggBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
    }

    private static boolean sticky(Player player) {
        return player.getMainHandItem().is(GCItems.STICKY_DESH_PICKAXE.get());
    }

    /** 1.12.2 getPlayerRelativeBlockHardness: 0.2 a tick with the Sticky Desh Pickaxe, otherwise unbreakable. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return sticky(player) ? 0.2F : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return sticky(player);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    /** 1.12.2 onBlockExploded: a baby Evolved Creeper hatches. */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            final EvolvedCreeper creeper = GCEntities.EVOLVED_CREEPER.get().create(level);
            if (creeper != null) {
                creeper.setBaby(true);
                creeper.moveTo(pos.getX() + 0.5D, pos.getY() + 3.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(creeper);
            }
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    /** 1.12.2 getPickBlock returned nothing. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
