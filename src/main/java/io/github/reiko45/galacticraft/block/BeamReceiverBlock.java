/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockBeamReceiver). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.BeamOutputBlockEntity;
import io.github.reiko45.galacticraft.block.entity.BeamReceiverBlockEntity;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Energy Beam Receiver (1.12.2 BlockBeamReceiver): goes on a face of a machine that takes or gives energy (the face you
 * click, or failing that any usable one beside it); it falls off when no such face is left. Use it to aim it at the
 * next node in range.
 */
public class BeamReceiverBlock extends BaseEntityBlock {
    public static final MapCodec<BeamReceiverBlock> CODEC = simpleCodec(BeamReceiverBlock::new);
    /** The side the machine is on. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, box(4.8D, 4.8D, 4.8D, 11.2D, 16.0D, 11.2D),
            Direction.DOWN, box(3.2D, 0.0D, 3.2D, 12.8D, 6.72D, 12.8D),
            Direction.EAST, box(9.28D, 3.2D, 3.2D, 16.0D, 12.8D, 12.8D),
            Direction.WEST, box(0.0D, 3.2D, 3.2D, 6.72D, 12.8D, 12.8D),
            Direction.NORTH, box(3.2D, 3.2D, 0.0D, 12.8D, 12.8D, 6.72D),
            Direction.SOUTH, box(3.2D, 3.2D, 9.28D, 12.8D, 12.8D, 16.0D));

    public BeamReceiverBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * 1.12.2 getMetadataFromAngle: the preferred side if it has a usable energy face, else the first side that does; but
     * a Galacticraft machine on the preferred side without energy on that face refuses the receiver outright.
     */
    @Nullable
    public static Direction attachSide(Level level, BlockPos pos, Direction preferred) {
        if (BeamReceiverBlockEntity.modeFor(level, pos, preferred) != null) {
            return preferred;
        }
        if (level.getCapability(io.github.reiko45.galacticraft.energy.GCEnergy.BLOCK, pos.relative(preferred), null) != null) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            if (direction != preferred && BeamReceiverBlockEntity.modeFor(level, pos, direction) != null) {
                return direction;
            }
        }
        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Direction side = attachSide(context.getLevel(), context.getClickedPos(), context.getClickedFace().getOpposite());
        if (side == null) {
            if (context.getLevel().isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.receiver.cannot_attach").withStyle(ChatFormatting.RED),
                        false);
            }
            return null;
        }
        return defaultBlockState().setValue(FACING, side);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
        if (level.isClientSide()) {
            return;
        }
        final Direction side = attachSide(level, pos, state.getValue(FACING));
        if (side == null) {
            level.destroyBlock(pos, true);
        } else if (side != state.getValue(FACING)) {
            level.setBlock(pos, state.setValue(FACING, side), 3);
            if (level.getBlockEntity(pos) instanceof BeamReceiverBlockEntity receiver) {
                receiver.reattach(side);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BeamOutputBlockEntity output) {
            if (!level.isClientSide()) {
                return output.cycleTarget() ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeamReceiverBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof BeamOutputBlockEntity output) {
                output.tick();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
