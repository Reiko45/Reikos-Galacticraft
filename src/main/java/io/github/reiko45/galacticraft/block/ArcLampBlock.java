/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockBrightLamp). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.ArcLampBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Arc Lamp (1.12.2 BlockBrightLamp, arclamp): a floodlight on a mounting plate. FACING is the side it is mounted on;
 * the wrench swings the lamp round to point another way along its plate. It glows itself (light 13) and lights up a
 * broad cone of the space in front of it ({@link ArcLampBlockEntity}); a redstone signal turns it off.
 */
public class ArcLampBlock extends BaseEntityBlock implements Wrenchable {
    public static final MapCodec<ArcLampBlock> CODEC = simpleCodec(ArcLampBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape DOWN = Block.box(3.2D, 0.0D, 3.2D, 12.8D, 9.6D, 12.8D);
    private static final VoxelShape UP = Block.box(3.2D, 6.4D, 3.2D, 12.8D, 16.0D, 12.8D);
    private static final VoxelShape NORTH = Block.box(3.2D, 3.2D, 0.0D, 12.8D, 12.8D, 9.6D);
    private static final VoxelShape SOUTH = Block.box(3.2D, 3.2D, 6.4D, 12.8D, 12.8D, 16.0D);
    private static final VoxelShape WEST = Block.box(0.0D, 3.2D, 3.2D, 9.6D, 12.8D, 12.8D);
    private static final VoxelShape EAST = Block.box(6.4D, 3.2D, 3.2D, 16.0D, 12.8D, 12.8D);

    public ArcLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 1.12.2 getStateForPlacement: mounted on the block clicked if that face is solid, else facing the click. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Direction facing = context.getClickedFace();
        final Direction opposite = facing.getOpposite();
        final BlockPos behind = context.getClickedPos().relative(opposite);
        if (context.getLevel().getBlockState(behind).isFaceSturdy(context.getLevel(), behind, facing)) {
            return defaultBlockState().setValue(FACING, opposite);
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
            case NORTH -> NORTH;
            case DOWN -> DOWN;
            case UP -> UP;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcLampBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.ARC_LAMP.get(), ArcLampBlockEntity::serverTick);
    }

    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (level.getBlockEntity(pos) instanceof ArcLampBlockEntity lamp) {
            lamp.facingChanged();
            return true;
        }
        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcLampBlockEntity lamp) {
            lamp.revertAir();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.galacticraftcore.arc_lamp.desc").withStyle(ChatFormatting.GRAY));
    }
}
