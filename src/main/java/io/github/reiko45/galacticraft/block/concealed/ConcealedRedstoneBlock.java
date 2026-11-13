/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockConcealedRedstone). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.concealed;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Concealed Redstone (1.12.2 BlockConcealedRedstone): redstone dust inside a full tin decoration block. It carries a
 * signal like dust does, from block to block (dimming one step per block, stepping up or down where dust would) and
 * powers what touches it, but it is a solid block to build with.
 */
public class ConcealedRedstoneBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private boolean canProvidePower = true;
    private final Set<BlockPos> blocksNeedingUpdate = Sets.newHashSet();

    public ConcealedRedstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    private BlockState updateSurroundingRedstone(Level level, BlockPos pos, BlockState state) {
        state = calculateCurrentChanges(level, pos, pos, state);
        final List<BlockPos> list = Lists.newArrayList(this.blocksNeedingUpdate);
        this.blocksNeedingUpdate.clear();
        for (BlockPos blockpos : list) {
            level.updateNeighborsAt(blockpos, this);
        }
        return state;
    }

    /** 1.12.2 calculateCurrentChanges (BlockRedstoneWire's, for full blocks). */
    private BlockState calculateCurrentChanges(Level level, BlockPos pos1, BlockPos pos2, BlockState state) {
        final BlockState original = state;
        final int currentPower = state.getValue(POWER);
        int maxPower = getMaxCurrentStrength(level, pos2, 0);
        this.canProvidePower = false;
        final int k = level.getBestNeighborSignal(pos1);
        this.canProvidePower = true;
        if (k > 0 && k > maxPower - 1) {
            maxPower = k;
        }
        int l = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            final BlockPos blockpos = pos1.relative(direction);
            final boolean flag = blockpos.getX() != pos2.getX() || blockpos.getZ() != pos2.getZ();
            if (flag) {
                l = getMaxCurrentStrength(level, blockpos, l);
            }
            final BlockState bs = level.getBlockState(blockpos);
            final BlockState bsUp = level.getBlockState(pos1.above());
            if (bs.isRedstoneConductor(level, blockpos) && !bsUp.isRedstoneConductor(level, pos1.above())) {
                if (flag && pos1.getY() >= pos2.getY()) {
                    l = getMaxCurrentStrength(level, blockpos.above(), l);
                }
            } else if (!bs.isRedstoneConductor(level, blockpos) && flag && pos1.getY() <= pos2.getY()) {
                l = getMaxCurrentStrength(level, blockpos.below(), l);
            }
        }
        if (l > maxPower) {
            maxPower = l - 1;
        } else if (maxPower > 0) {
            maxPower--;
        } else {
            maxPower = 0;
        }
        if (k > maxPower - 1) {
            maxPower = k;
        }
        if (currentPower != maxPower) {
            state = state.setValue(POWER, maxPower);
            if (level.getBlockState(pos1) == original) {
                level.setBlock(pos1, state, Block.UPDATE_CLIENTS);
            }
            this.blocksNeedingUpdate.add(pos1);
            for (Direction direction : Direction.values()) {
                this.blocksNeedingUpdate.add(pos1.relative(direction));
            }
        }
        return state;
    }

    private void notifyWireNeighborsOfStateChange(Level level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(this) || state.is(Blocks.REDSTONE_WIRE)) {
            level.updateNeighborsAt(pos, this);
            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
        }
    }

    private void notifyNeighbors(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            final BlockPos blockpos = pos.relative(direction);
            if (level.getBlockState(blockpos).isRedstoneConductor(level, blockpos)) {
                notifyWireNeighborsOfStateChange(level, blockpos.above());
            } else {
                notifyWireNeighborsOfStateChange(level, blockpos.below());
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            updateSurroundingRedstone(level, pos, state);
            for (Direction direction : Direction.Plane.VERTICAL) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                notifyWireNeighborsOfStateChange(level, pos.relative(direction));
            }
            notifyNeighbors(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                notifyWireNeighborsOfStateChange(level, pos.relative(direction));
            }
            notifyNeighbors(level, pos);
        }
    }

    private int getMaxCurrentStrength(Level level, BlockPos pos, int strength) {
        final BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return strength;
        }
        return Math.max(state.getValue(POWER), strength);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updateSurroundingRedstone(level, pos, state);
        }
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return !this.canProvidePower ? 0 : getSignal(state, level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.canProvidePower ? state.getValue(POWER) : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return this.canProvidePower;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }
}
