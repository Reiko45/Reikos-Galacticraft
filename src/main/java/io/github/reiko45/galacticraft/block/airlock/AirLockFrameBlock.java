/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockAirLockFrame). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.airlock;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Air Lock Frame (1.12.2 BlockAirLockFrame, air_lock_frame): build a rectangle of frames around a doorway (or a flat one
 * round a hatch) with an Air Lock Controller in it, and the controller seals the opening.
 */
public class AirLockFrameBlock extends Block {
    public AirLockFrameBlock(Properties properties) {
        super(properties);
    }

    /** A frame or a controller (1.12.2 TileEntityAirLock). */
    public static boolean isAirLock(BlockState state) {
        return state.is(GCBlocks.AIR_LOCK_FRAME.get()) || state.is(GCBlocks.AIR_LOCK_CONTROLLER.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.galacticraftcore.air_lock_frame.desc").withStyle(ChatFormatting.GRAY));
    }
}
