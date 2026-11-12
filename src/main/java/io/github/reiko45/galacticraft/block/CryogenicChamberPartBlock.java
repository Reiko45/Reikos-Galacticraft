/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockMulti, type cryo_chamber). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The middle and top of a Cryogenic Chamber (1.12.2 BlockMulti cryo_chamber, models cryo_1 and cryo_2). Using it uses
 * the chamber; breaking it breaks the chamber.
 */
public class CryogenicChamberPartBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CryogenicChamberPartBlock> CODEC = simpleCodec(CryogenicChamberPartBlock::new);
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public CryogenicChamberPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TOP, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TOP);
    }

    private static BlockPos mainPos(BlockState state, BlockPos pos) {
        return pos.below(state.getValue(TOP) ? 2 : 1);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        final BlockPos main = mainPos(state, pos);
        final BlockState mainState = level.getBlockState(main);
        if (mainState.getBlock() instanceof CryogenicChamberBlock) {
            return mainState.useWithoutItem(level, player, hit.withPosition(main));
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            final BlockPos main = mainPos(state, pos);
            if (level.getBlockState(main).getBlock() instanceof CryogenicChamberBlock) {
                level.destroyBlock(main, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(GCBlocks.CRYOGENIC_CHAMBER.get());
    }

    /** See-through for the camera (third person from outside, and the view out of it while asleep). */
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getVisualShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                       net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
}
