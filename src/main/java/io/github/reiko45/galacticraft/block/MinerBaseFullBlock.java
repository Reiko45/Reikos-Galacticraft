/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockMinerBaseFull and
 * tile.TileEntityMinerBase#onBlockRemoval / onDestroy). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import io.github.reiko45.galacticraft.item.WrenchItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A built Astro Miner Base (1.12.2 BlockMinerBaseFull): drawn whole by its master's renderer. Using any part opens the
 * dock; the wrench turns it (while no miner is linked). Breaking any part breaks the whole base back into eight Astro
 * Miner Base blocks, and drops its store.
 */
public class MinerBaseFullBlock extends BaseEntityBlock {
    public static final MapCodec<MinerBaseFullBlock> CODEC = simpleCodec(MinerBaseFullBlock::new);
    /** Set while one part's removal takes the rest of the base with it. */
    private static boolean breaking;

    public MinerBaseFullBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MinerBaseBlockEntity base) {
                base.rotate();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MinerBaseBlockEntity base && base.getMaster() != null) {
            if (!level.isClientSide()) {
                final MinerBaseBlockEntity master = base.getMaster();
                player.openMenu(master, master.getBlockPos());
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !breaking && level.getBlockEntity(pos) instanceof MinerBaseBlockEntity base) {
            final BlockPos master = base.masterPos();
            if (master != null) {
                breaking = true;
                try {
                    final MinerBaseBlockEntity masterBase = base.getMaster();
                    if (masterBase != null) {
                        ((MachineBlockEntity) masterBase).dropContents(level, master);
                        if (masterBase.linkedMiner() != null) {
                            masterBase.unlinkMiner();
                        }
                    }
                    for (BlockPos part : MinerBaseBlockEntity.parts(master)) {
                        if (!part.equals(pos) && level.getBlockState(part).is(this)) {
                            level.removeBlock(part, false);
                        }
                    }
                } finally {
                    breaking = false;
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(GCBlocks.MINER_BASE.get());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerBaseBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof MachineBlockEntity machine) {
                MachineBlockEntity.serverTick(lvl, pos, st, machine);
            }
        };
    }
}
