/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockDish and tile.TileEntityDish).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.CommsDishBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2 BlockDish (Communications Dish): the base of a radio dish 9 blocks high and 7 across, its upper parts filled
 * with invisible part blocks (1.12.2 BlockMulti DISH_LARGE); the dish turns and nods slowly. In 1.12.2 it did nothing
 * else (its battery store had no way in). Placing it needs all that room clear (1.12.2 replaced whatever was there);
 * breaking any part breaks it all.
 */
public class CommsDishBlock extends BaseEntityBlock implements PartialSealable {
    public static final MapCodec<CommsDishBlock> CODEC = simpleCodec(CommsDishBlock::new);

    public CommsDishBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 1.12.2 TileEntityDish#getPositions: a 3-high post, two 3 x 3 layers, two 7 x 7 and a 5 x 5 (corners cut off). */
    public static List<BlockPos> partPositions(BlockPos pos) {
        final List<BlockPos> positions = new ArrayList<>();
        for (int y = 1; y <= 3; y++) {
            positions.add(pos.above(y));
        }
        for (int y = 4; y <= 5; y++) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    positions.add(pos.offset(x, y, z));
                }
            }
        }
        for (int y = 6; y <= 7; y++) {
            for (int x = -3; x < 4; x++) {
                for (int z = -3; z < 4; z++) {
                    if (Math.abs(x) + Math.abs(z) != 6) {
                        positions.add(pos.offset(x, y, z));
                    }
                }
            }
        }
        for (int x = -2; x < 3; x++) {
            for (int z = -2; z < 3; z++) {
                if (Math.abs(x) + Math.abs(z) != 4) {
                    positions.add(pos.offset(x, 8, z));
                }
            }
        }
        return positions;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (BlockPos part : partPositions(context.getClickedPos())) {
            if (!context.getLevel().isInWorldBounds(part) || !context.getLevel().getBlockState(part).canBeReplaced()) {
                if (context.getPlayer() != null && context.getLevel().isClientSide) {
                    context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.warning.no_room"), true);
                }
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeParts(level, pos);
        }
    }

    /** 1.12.2 BlockMulti#makeFakeBlock for every part. */
    public static void placeParts(Level level, BlockPos pos) {
        for (BlockPos partPos : partPositions(pos)) {
            level.setBlock(partPos, GCBlocks.COMMS_DISH_PART.get().defaultBlockState().setValue(SolarPartBlock.PART, SolarPartBlock.Part.DISH), Block.UPDATE_ALL);
            if (level.getBlockEntity(partPos) instanceof SolarPartBlockEntity part) {
                part.setMainPos(pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            for (BlockPos partPos : partPositions(pos)) {
                if (level.getBlockEntity(partPos) instanceof SolarPartBlockEntity part && pos.equals(part.mainPos())) {
                    level.removeBlock(partPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CommsDishBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, io.github.reiko45.galacticraft.registry.GCBlockEntities.COMMS_DISH.get(), (l, p, s, be) -> be.clientTick()) : null;
    }
}
