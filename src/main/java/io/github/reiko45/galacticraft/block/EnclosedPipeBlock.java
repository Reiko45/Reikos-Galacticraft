/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockEnclosed, type enclosed_fluid_pipe).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sealable Fluid Pipe (1.12.2 BlockEnclosed, enclosed_fluid_pipe, the "oxygen pipe"): a plain fluid pipe run through a
 * full tin decoration block. It is part of any pipe network beside it and keeps the air in; it can't be dyed or set to
 * pull, like the walkway pipe.
 */
public class EnclosedPipeBlock extends FluidPipeBlock implements PartialSealable {
    public static final MapCodec<EnclosedPipeBlock> CODEC = simpleCodec(EnclosedPipeBlock::new);

    public EnclosedPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void toggleMode(Level level, BlockPos pos, BlockState state) {
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.galacticraftcore.enclosed.desc").withStyle(ChatFormatting.GRAY));
    }
}
