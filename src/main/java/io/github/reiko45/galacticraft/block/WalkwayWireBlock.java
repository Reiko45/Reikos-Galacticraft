/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockWalkway, type
 * walkway_wire). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Aluminum Wire Walkway (1.12.2 walkway_wire): a walkway with a heavy aluminum wire run underneath (1.12.2 gave it a
 * tier 2 TileEntityAluminumWire). It joins power networks like any wire; it fills the block when wired downwards.
 */
public class WalkwayWireBlock extends AluminumWireBlock {
    public static final MapCodec<WalkwayWireBlock> CODEC = simpleCodec(WalkwayWireBlock::new);

    public WalkwayWireBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CONNECTIONS.get(Direction.DOWN)) ? WalkwayBlock.SHAPE_DOWN : WalkwayBlock.SHAPE;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable(getDescriptionId() + ".desc").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
