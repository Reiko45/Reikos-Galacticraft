/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockEnclosed, types enclosed_aluminum_wire
 * and enclosed_heavy_aluminum_wire). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sealable Aluminum Wire and Sealable Heavy Aluminum Wire (1.12.2 BlockEnclosed): a wire run through a full tin
 * decoration block, so a wall can carry power and still keep the air in. It joins power networks like any wire.
 */
public class EnclosedWireBlock extends AluminumWireBlock implements io.github.reiko45.galacticraft.item.ShiftDescribed, PartialSealable {
    public EnclosedWireBlock(Properties properties, boolean heavy) {
        super(properties, heavy);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    /** 1.12.2 getShiftDescription: a text shared with others like it. */
    @Override
    public String shiftDescriptionKey(net.minecraft.world.item.ItemStack stack) {
        return "block.galacticraftcore.enclosed.description";
    }
}
