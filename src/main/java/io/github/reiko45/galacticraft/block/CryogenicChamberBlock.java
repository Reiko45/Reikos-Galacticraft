/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityCryogenicChamber,
 * blocks.BlockMachineMars, items.ItemBlockMachine and EventHandlerMars#onPlayerWakeUp). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Cryogenic Chamber (1.12.2 BlockMachineMars type cryogenic_chamber): a 3-high glass tube, its opening facing the
 * player who placed it. Stepping in puts the player to sleep standing up, by day as well as by night; a full sleep
 * (everyone asleep, as with beds) heals 5 health and leaves a 5 minute cooldown. It does not set the spawn point.
 */
public class CryogenicChamberBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CryogenicChamberBlock> CODEC = simpleCodec(CryogenicChamberBlock::new);
    /** 1.12.2 EventHandlerMars#onPlayerWakeUp: 6000 ticks between uses. */
    public static final int COOLDOWN = 6000;
    public static final float HEAL = 5.0F;

    public CryogenicChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 1.12.2 ItemBlockMachine#placeBlockAt: the two blocks above must be free. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final BlockPos pos = context.getClickedPos();
        for (int y = 1; y < 3; y++) {
            final BlockPos part = pos.above(y);
            if (!context.getLevel().isInWorldBounds(part) || !context.getLevel().getBlockState(part).canBeReplaced()) {
                if (context.getPlayer() != null && context.getLevel().isClientSide) {
                    context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.warning.no_room"), true);
                }
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            final BlockState part = GCBlocks.CRYOGENIC_CHAMBER_PART.get().defaultBlockState().setValue(FACING, state.getValue(FACING));
            level.setBlock(pos.above(), part.setValue(CryogenicChamberPartBlock.TOP, false), Block.UPDATE_ALL);
            level.setBlock(pos.above(2), part.setValue(CryogenicChamberPartBlock.TOP, true), Block.UPDATE_ALL);
        }
    }

    /** Broken: the parts above go too (1.12.2 TileEntityCryogenicChamber#onDestroy). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            for (int y = 1; y < 3; y++) {
                if (level.getBlockState(pos.above(y)).is(GCBlocks.CRYOGENIC_CHAMBER_PART.get())) {
                    level.removeBlock(pos.above(y), false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            enter(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 1.12.2 TileEntityCryogenicChamber#sleepInBedAt: no night or monster checks, only the cooldown. */
    public static boolean enter(ServerPlayer player, BlockPos pos) {
        if (player.isSleeping() || !player.isAlive()) {
            return false;
        }
        final GCPlayerData data = GCPlayerData.get(player);
        if (data.cryogenicChamberCooldown > 0) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.cryogenic.cant_use", data.cryogenicChamberCooldown / 20));
            return false;
        }
        player.startSleeping(pos);
        player.refreshDimensions();
        ((ServerLevel) player.level()).updateSleepingPlayerList();
        return true;
    }

    /** Whether this sleeping entity is in a Cryogenic Chamber. */
    public static boolean inChamber(LivingEntity entity) {
        return entity.getSleepingPos().map(pos -> entity.level().getBlockState(pos).getBlock() instanceof CryogenicChamberBlock).orElse(false);
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable LivingEntity sleeper) {
        return true;
    }

    /** No occupied flag: any number of players can't share it anyway, and 1.12.2 never checked it. */
    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
    }

    /** Vanilla turns a sleeper's camera to look away from the bed's head: here, out of the opening. */
    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACING).getOpposite();
    }
}
