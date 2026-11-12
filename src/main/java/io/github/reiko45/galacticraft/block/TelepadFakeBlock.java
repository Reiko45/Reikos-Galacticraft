/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockTelepadFake). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.TelepadFakeBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The invisible parts of a Short Range Telepad (1.12.2 BlockTelepadFake): thin plates around the pad and a canopy
 * above. They open the telepad's GUI, break the whole telepad when broken, drop nothing and shrug off explosions.
 */
public class TelepadFakeBlock extends BaseEntityBlock {
    public static final MapCodec<TelepadFakeBlock> CODEC = simpleCodec(TelepadFakeBlock::new);
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    private static final VoxelShape TOP_SHAPE = box(0.0D, 8.8D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape BOTTOM_SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 3.2D, 16.0D);

    public TelepadFakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(TOP, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? TOP_SHAPE : BOTTOM_SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TelepadFakeBlockEntity fake && fake.telepad() != null) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                fake.telepad().openMenu(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /** 1.12.2 onBlockRemoval: the whole telepad goes (the main block drops itself). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TelepadFakeBlockEntity fake && fake.mainPos() != null) {
            final BlockPos main = fake.mainPos();
            super.onRemove(state, level, pos, newState, moving);
            if (level.getBlockState(main).is(GCBlocks.SHORT_RANGE_TELEPAD.get())) {
                level.destroyBlock(main, true);
            }
            return;
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** 1.12.2 getBlockHardness: as hard as the telepad. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TelepadFakeBlockEntity fake && fake.mainPos() != null) {
            final BlockState main = level.getBlockState(fake.mainPos());
            if (main.getBlock() instanceof ShortRangeTelepadBlock) {
                return main.getDestroyProgress(player, level, fake.mainPos());
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(GCBlocks.SHORT_RANGE_TELEPAD.get());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TelepadFakeBlockEntity(pos, state);
    }
}
