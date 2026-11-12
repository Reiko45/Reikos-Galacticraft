/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.blocks.BlockCrashedProbe). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.CrashedProbeBlockEntity;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Crashed Probe (1.12.2 BlockCrashedProbe): an old probe sunk in a scorched crater in the Venus valleys, smoking, its
 * hold of salvage opened by using it. Broken, it gives itself, what it holds, and (if it crashed there) its
 * Radioisotope Core.
 */
public class CrashedProbeBlock extends BaseEntityBlock {
    public static final MapCodec<CrashedProbeBlock> CODEC = simpleCodec(CrashedProbeBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CrashedProbeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 1.12.2 onBlockPlacedBy: facing the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrashedProbeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrashedProbeBlockEntity probe) {
            player.openMenu(probe);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrashedProbeBlockEntity probe) {
            if (probe.hasCoreToDrop()) {
                // 1.12.2 spawnItem: a Radioisotope Core thrown up out of the wreck
                final RandomSource random = RandomSource.create(pos.asLong());
                final ItemEntity core = new ItemEntity(level, pos.getX() + random.nextFloat() * 0.7F + 0.15F,
                        pos.getY() + random.nextFloat() * 0.7F + 0.75F, pos.getZ() + random.nextFloat() * 0.7F + 0.15F,
                        new ItemStack(GCItems.RADIOISOTOPE_CORE.get()));
                core.setDefaultPickUpDelay();
                level.addFreshEntity(core);
            }
            Containers.dropContents(level, pos, probe);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.65D, pos.getY() + 1.0D, pos.getZ() + 0.9D, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.2D, pos.getY() + 1.0D, pos.getZ() + 0.2D, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 1.0D, pos.getY() + 0.25D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
    }
}
