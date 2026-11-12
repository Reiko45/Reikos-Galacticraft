/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockMinerBase and
 * tile.TileEntityMinerBaseSingle / TileEntityMinerBase#checkNewMinerBases). Copyright (c) 2023 Team Galacticraft.
 * Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Astro Miner Base (1.12.2 BlockMinerBase): eight of these placed as a 2 x 2 x 2 cube turn into a working base (the next
 * tick, as in 1.12.2).
 */
public class MinerBaseBlock extends Block {
    public static final MapCodec<MinerBaseBlock> CODEC = simpleCodec(MinerBaseBlock::new);

    public MinerBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /** Is there a whole cube of single base blocks with its lowest corner at {@code corner}? */
    private boolean isCube(Level level, BlockPos corner) {
        for (BlockPos part : MinerBaseBlockEntity.parts(corner)) {
            if (!level.getBlockState(part).is(this)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    final BlockPos corner = pos.offset(dx, dy, dz);
                    if (isCube(level, corner)) {
                        build(level, corner);
                        return;
                    }
                }
            }
        }
    }

    /** 1.12.2 checkNewMinerBases: the eight blocks become one base, mastered by the lowest corner. */
    public static void build(Level level, BlockPos corner) {
        for (BlockPos part : MinerBaseBlockEntity.parts(corner)) {
            level.setBlock(part, GCBlocks.MINER_BASE_FULL.get().defaultBlockState(), 2);
        }
        for (BlockPos part : MinerBaseBlockEntity.parts(corner)) {
            if (level.getBlockEntity(part) instanceof MinerBaseBlockEntity base) {
                base.setMaster(corner);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
