/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockTelemetry). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.TelemetryBlockEntity;
import io.github.reiko45.galacticraft.item.FrequencyModuleItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.12.2 BlockTelemetry (Telemetry Unit): gathers data from the rocket, player or mob its frequency module was linked
 * to, for Display Screens within 32 blocks. Using a frequency module that tracks something on it links that; worn, a
 * module used on it links its wearer. (1.12.2's facing code had no property to set, so the unit has none.)
 */
public class TelemetryBlock extends BaseEntityBlock {
    public static final MapCodec<TelemetryBlock> CODEC = simpleCodec(TelemetryBlock::new);

    public TelemetryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 1.12.2 onMachineActivated with a frequency module in hand. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof FrequencyModuleItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TelemetryBlockEntity telemetry) {
            final UUID linked = FrequencyModuleItem.linkedEntity(stack);
            if (linked != null) {
                telemetry.addTrackedEntity(linked);
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.telemetry_succeed.message"));
            } else {
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.telemetry_fail.message"));
            }
            FrequencyModuleItem.setTelemetry(stack, level, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            final ItemStack wearing = GCPlayerData.get(player).gear(GearSlot.FREQUENCY_MODULE);
            if (wearing.isEmpty()) {
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.telemetry_fail_no_frequency_module.message"));
            } else if (!FrequencyModuleItem.hasTelemetry(wearing)) {
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.telemetry_fail_wearing_it.message"));
            } else {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TelemetryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.TELEMETRY.get(), (l, p, s, be) -> be.serverTick());
    }
}
