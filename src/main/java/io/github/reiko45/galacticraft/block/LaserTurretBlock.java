/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.blocks.BlockLaserTurret). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.List;
import javax.annotation.Nullable;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.12.2 BlockLaserTurret: a machine block (its energy input on the left, moved by sneaking with a wrench) with the
 * turret standing on an invisible block above it (1.12.2 BlockMulti LASER_TURRET). Whoever places it owns it. It seals
 * on every side.
 */
public class LaserTurretBlock extends ConfigurableMachineBlock implements PartialSealable {
    public LaserTurretBlock(Properties properties) {
        super(properties, LaserTurretBlockEntity::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final BlockPos above = context.getClickedPos().above();
        if (!context.getLevel().isInWorldBounds(above) || !context.getLevel().getBlockState(above).canBeReplaced()) {
            if (context.getPlayer() != null && context.getLevel().isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.warning.no_room"), true);
            }
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (placer instanceof Player player && level.getBlockEntity(pos) instanceof LaserTurretBlockEntity turret) {
                turret.setOwner(player);
            }
            placePart(level, pos);
        }
    }

    /** 1.12.2 BlockMulti#makeFakeBlock: the block the turret stands on. */
    public static void placePart(Level level, BlockPos pos) {
        final BlockPos above = pos.above();
        level.setBlock(above, GCBlocks.LASER_TURRET_PART.get().defaultBlockState()
                .setValue(SolarPartBlock.PART, SolarPartBlock.Part.DISH), Block.UPDATE_ALL);
        if (level.getBlockEntity(above) instanceof SolarPartBlockEntity part) {
            part.setMainPos(pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof LaserTurretBlockEntity turret) {
            turret.openMenu(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos.above()) instanceof SolarPartBlockEntity part
                && pos.equals(part.mainPos())) {
            level.removeBlock(pos.above(), false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof LaserTurretBlockEntity turret) {
                    LaserTurretBlockEntity.clientTick(lvl, pos, st, turret);
                }
            };
        }
        return super.getTicker(level, state, type);
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    /** 1.12.2 getShiftDescription. */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".description").withStyle(ChatFormatting.GRAY));
    }
}
