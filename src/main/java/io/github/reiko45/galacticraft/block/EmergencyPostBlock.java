/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockEmergencyBox). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.EmergencyPostBlockEntity;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/**
 * 1.12.2 BlockEmergencyBox (Emergency Post): a glowing post (light 15, 4 to break, 42 against blasts) with a door on
 * each side. Using a side opens that door (and shuts the others); using it again takes the Emergency Kit inside (KIT),
 * or opening it with a kit in hand puts the kit in. It lights the ground around it and keeps breathing mobs away. It
 * seals at top and bottom only.
 */
public class EmergencyPostBlock extends BaseEntityBlock implements PartialSealable {
    public static final MapCodec<EmergencyPostBlock> CODEC = simpleCodec(EmergencyPostBlock::new);
    public static final BooleanProperty KIT = BooleanProperty.create("kit");

    public EmergencyPostBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(KIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EmergencyPostBlockEntity post) {
            post.click(player, hit.getDirection(), state.getValue(KIT));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EmergencyPostBlockEntity post) {
            post.revertAir();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return direction.getAxis() == Direction.Axis.Y;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmergencyPostBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, GCBlockEntities.EMERGENCY_POST.get(), (l, p, s, post) -> post.tick());
    }
}
