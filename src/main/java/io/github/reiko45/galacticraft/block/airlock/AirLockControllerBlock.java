/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockAirLockFrame, air_lock_controller).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.airlock;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Air Lock Controller (1.12.2 BlockAirLockFrame, air_lock_controller): part of an air lock's frame, with a settings
 * screen for its owner. ACTIVE (sealed) is a block state so open screens can show it.
 */
public class AirLockControllerBlock extends BaseEntityBlock {
    public static final MapCodec<AirLockControllerBlock> CODEC = simpleCodec(AirLockControllerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AirLockControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirLockControllerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.AIR_LOCK_CONTROLLER.get(), AirLockControllerBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof AirLockControllerBlockEntity controller) {
            controller.setOwner(player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AirLockControllerBlockEntity controller) {
            controller.openScreen(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AirLockControllerBlockEntity controller) {
            controller.unsealAirLock();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 1.12.2 canConnectRedstone: redstone dust points into it. */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.galacticraftcore.air_lock_frame.desc").withStyle(ChatFormatting.GRAY));
    }
}
