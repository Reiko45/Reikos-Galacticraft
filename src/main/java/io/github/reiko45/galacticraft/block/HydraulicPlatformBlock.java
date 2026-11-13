/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockPlatform). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.HydraulicPlatformBlockEntity;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.world.GCDimensions;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.12.2 BlockPlatform (Hydraulic Platform): four in a 2 x 2 square join into one platform (each block then a corner,
 * TYPE); standing on the middle of one, sneak to go down to the next platform below, or jump to go up to the next one
 * above, up to 16 blocks. A slab from 6 to 14 pixels high (to the top of the block in zero gravity). It can't be placed
 * as a third in a row, nor on top of another; it seals only on top.
 */
public class HydraulicPlatformBlock extends BaseEntityBlock implements PartialSealable {
    public static final MapCodec<HydraulicPlatformBlock> CODEC = simpleCodec(HydraulicPlatformBlock::new);
    public static final EnumProperty<Corner> TYPE = EnumProperty.create("type", Corner.class);
    /** 1.12.2 HEIGHT: the top of the platform. */
    public static final float HEIGHT = 0.875F;
    private static final VoxelShape SHAPE = Block.box(0, 6, 0, 16, 14, 16);
    private static final VoxelShape SHAPE_ZERO_G = Block.box(0, 6, 0, 16, 16, 16);
    private static final VoxelShape SELECT_SE = Block.box(9, 0, 9, 16, 14, 16);
    private static final VoxelShape SELECT_OTHER = Block.box(0, 0, 0, 7, 14, 7);

    /** 1.12.2 EnumCorner: the corner's metadata, and its (swapped) blockstate name kept from 1.12.2. */
    public enum Corner implements StringRepresentable {
        NONE("none"), NW("sw"), SW("nw"), NE("se"), SE("ne");

        private final String name;

        Corner(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Corner byIndex(int index) {
            return values()[Math.floorMod(index, 5)];
        }
    }

    public HydraulicPlatformBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(TYPE, Corner.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static boolean zeroG(BlockGetter level) {
        return level instanceof Level world && world.dimension().equals(GCDimensions.ORBIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 1.12.2 getSelectedBoundingBox: while a lift passes through, only the post at the corner can be picked
        if (level.getBlockEntity(pos) instanceof HydraulicPlatformBlockEntity platform && platform.noCollide()) {
            return state.getValue(TYPE) == Corner.SE ? SELECT_SE : SELECT_OTHER;
        }
        return zeroG(level) ? SHAPE_ZERO_G : SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof HydraulicPlatformBlockEntity platform && platform.noCollide()) {
            return Shapes.empty();
        }
        return zeroG(level) ? SHAPE_ZERO_G : SHAPE;
    }

    /** 1.12.2 checkAxis: two unjoined platforms in a row that way already. */
    private static boolean twoInARow(Level level, BlockPos pos, Direction direction) {
        int same = 0;
        for (int i = 1; i <= 2; i++) {
            final BlockState state = level.getBlockState(pos.relative(direction, i));
            if (state.getBlock() instanceof HydraulicPlatformBlock && state.getValue(TYPE) == Corner.NONE) {
                same++;
            }
        }
        return same > 1;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (twoInARow(level, pos, direction)) {
                return null;
            }
        }
        if (level.getBlockState(pos.below()).getBlock() instanceof HydraulicPlatformBlock && context.getClickedFace() == Direction.UP) {
            return null;
        }
        return defaultBlockState();
    }

    /** 1.12.2 breakBlock: breaking a corner breaks the platform up into single blocks. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof HydraulicPlatformBlockEntity platform) {
            platform.onBroken();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return direction == Direction.UP;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydraulicPlatformBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, GCBlockEntities.HYDRAULIC_PLATFORM.get(), (l, p, s, platform) -> platform.tick());
    }
}
