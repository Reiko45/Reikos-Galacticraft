/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockAirLockWall). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.airlock;

import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Air Lock Seal (1.12.2 BlockAirLockWall, air_lock_seal): the half-block-thick shutter a controller fills its doorway
 * with, sealed on every side. It turns to face along the doorway (x or z), or lies flat in a hatch. It can't be mined
 * (hardness 1000) and drops nothing.
 *
 * <p>Kept from 1.12.2: it looks to the west twice where it meant west and east, so a shutter only turns when the block
 * to its west is part of the doorway.
 */
public class AirLockSealBlock extends Block implements PartialSealable {
    public static final EnumProperty<Connection> CONNECTION = EnumProperty.create("connection", Connection.class);
    private static final VoxelShape X = Block.box(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    private static final VoxelShape Z = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);
    private static final VoxelShape FLAT = Block.box(0.0D, 4.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public AirLockSealBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(CONNECTION, Connection.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTION);
    }

    private static boolean isDoorway(BlockState state) {
        return state.is(GCBlocks.AIR_LOCK_FRAME.get()) || state.is(GCBlocks.AIR_LOCK_CONTROLLER.get()) || state.is(GCBlocks.AIR_LOCK_SEAL.get());
    }

    /** 1.12.2 getConnection. */
    public static Connection connection(BlockGetter level, BlockPos pos) {
        final BlockState xMin = level.getBlockState(pos.west());
        final BlockState xMax = level.getBlockState(pos.west());
        if (!isDoorway(xMin) && !isDoorway(xMax)) {
            return Connection.X;
        }
        int adjacent = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isDoorway(level.getBlockState(pos.relative(dir)))) {
                adjacent++;
            }
        }
        return adjacent == 4 ? Connection.FLAT : Connection.Z;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(CONNECTION, connection(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return state.setValue(CONNECTION, connection(level, pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(CONNECTION)) {
            case X -> X;
            case Z -> Z;
            case FLAT -> FLAT;
        };
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    public enum Connection implements StringRepresentable {
        X("x"), Z("z"), FLAT("flat");

        private final String name;

        Connection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
