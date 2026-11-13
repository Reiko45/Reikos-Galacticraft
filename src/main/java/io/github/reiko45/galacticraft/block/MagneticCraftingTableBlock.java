/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockCrafting). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.MagneticCraftingTableBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.12.2 BlockCrafting (Magnetic Crafting Table): a crafting table that keeps its grid, remembers the last recipe crafted
 * on it, and lets hoppers fill the grid to that recipe and take the results out. It faces any of the six ways (towards
 * the player, or up or down when placed from well above or below), and the wrench turns it on.
 */
public class MagneticCraftingTableBlock extends BaseEntityBlock implements Wrenchable {
    public static final MapCodec<MagneticCraftingTableBlock> CODEC = simpleCodec(MagneticCraftingTableBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MagneticCraftingTableBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return context.getPlayer() == null ? defaultBlockState() : defaultBlockState().setValue(FACING, facingFrom(context.getClickedPos(), context.getPlayer()));
    }

    /** 1.12.2 getFacingFromEntity (vanilla's old piston rule). */
    public static Direction facingFrom(BlockPos pos, LivingEntity placer) {
        if (Mth.abs((float) placer.getX() - pos.getX()) < 3.0F && Mth.abs((float) placer.getZ() - pos.getZ()) < 3.0F) {
            final double eyes = placer.getEyeY();
            if (eyes - pos.getY() > 2.0D) {
                return Direction.UP;
            }
            if (pos.getY() - eyes > 1.0D) {
                return Direction.DOWN;
            }
        }
        return placer.getDirection().getOpposite();
    }

    /** 1.12.2 rotate6Ways: down, up, north, east, south, west, and round again. */
    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        final Direction facing = state.getValue(FACING);
        final Direction next = switch (facing) {
            case DOWN -> Direction.UP;
            case UP -> Direction.NORTH;
            case WEST -> Direction.DOWN;
            default -> facing.getClockWise();
        };
        level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MagneticCraftingTableBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 1.12.2 dropEntireInventory: the grid, and any results already made and not yet taken. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MagneticCraftingTableBlockEntity table) {
            Containers.dropContents(level, pos, table.matrix());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), table.takeHiddenOutput());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagneticCraftingTableBlockEntity(pos, state);
    }
}
