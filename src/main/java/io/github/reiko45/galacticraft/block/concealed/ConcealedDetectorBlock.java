/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockConcealedDetector and
 * core.tile.TileEntityPlayerDetector). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.concealed;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Concealed Detector (1.12.2 BlockConcealedDetector, TileEntityPlayerDetector): a player sensor disguised as a tin
 * decoration block (VARIANT 0) or Moon rock (VARIANT 1), found in the abandoned bases where it opens their doors. Every
 * 25 ticks it looks for players in a 14 x 8 x 14 space in front of it (FACING: 0 south, 1 west, 2 north, 3 east, the way
 * its placer faced) and gives a redstone signal of 15 while there is none (so a door it powers opens when someone
 * comes). Once it has seen someone the space grows 3 blocks every way until they leave. It drops nothing.
 */
public class ConcealedDetectorBlock extends BaseEntityBlock {
    public static final MapCodec<ConcealedDetectorBlock> CODEC = simpleCodec(ConcealedDetectorBlock::new);
    public static final IntegerProperty VARIANT = IntegerProperty.create("var", 0, 1);
    public static final IntegerProperty FACING = IntegerProperty.create("facing", 0, 3);
    public static final BooleanProperty DETECTED = BooleanProperty.create("det");

    public ConcealedDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, 0).setValue(VARIANT, 0).setValue(DETECTED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT, DETECTED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().get2DDataValue());
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** 1.12.2 getWeakPower: 15 until it sees a player, nothing while it gets a direct signal itself. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level instanceof Level real && real.hasNeighborSignal(pos)) {
            return 0;
        }
        return state.getValue(DETECTED) ? 0 : 15;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Detector(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.CONCEALED_DETECTOR.get(), Detector::serverTick);
    }

    /** 1.12.2 TileEntityPlayerDetector. */
    public static class Detector extends BlockEntity {
        private int ticks = 24;
        private boolean result;

        public Detector(BlockPos pos, BlockState state) {
            super(GCBlockEntities.CONCEALED_DETECTOR.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Detector detector) {
            if (++detector.ticks < 25) {
                return;
            }
            detector.ticks = 0;
            final int facing = state.hasProperty(FACING) ? state.getValue(FACING) : 0;
            final int x = pos.getX();
            final double y = pos.getY();
            final int z = pos.getZ();
            final double range = 14.0D;
            final double h = detector.result ? 3.0D : 0.0D;
            final AABB search = switch (facing) {
                case 0 -> new AABB(x - range / 2 + 0.5D - h, y - 6 - h, z - range - h, x + range / 2 + 0.5D + h, y + 2 + h, z + h);
                case 1 -> new AABB(x + 1 - h, y - 6 - h, z - range / 2 + 0.5D - h, x + range + 1 + h, y + 2 + h, z + range / 2 + 0.5D + h);
                case 2 -> new AABB(x - range / 2 + 0.5D - h, y - 6 - h, z + 1 - h, x + range / 2 + 0.5D + h, y + 2 + h, z + range + 1.0D + h);
                default -> new AABB(x - range - h, y - 6 - h, z - range / 2 + 0.5D - h, x + h, y + 2 + h, z + range / 2 + 0.5D + h);
            };
            detector.result = !level.getEntitiesOfClass(Player.class, search).isEmpty();
            if (state.hasProperty(DETECTED) && state.getValue(DETECTED) != detector.result) {
                level.setBlock(pos, state.setValue(DETECTED, detector.result), Block.UPDATE_ALL);
            }
        }

        public boolean detectingPlayer() {
            return this.result;
        }
    }
}
