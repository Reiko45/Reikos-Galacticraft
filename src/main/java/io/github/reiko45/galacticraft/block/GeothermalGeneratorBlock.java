/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.blocks.BlockGeothermalGenerator).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.GeothermalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** 1.12.2 BlockGeothermalGenerator: a machine with an "active" look (vents lit) while it sits on a working spout. */
public class GeothermalGeneratorBlock extends MachineBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public GeothermalGeneratorBlock(Properties properties) {
        super(properties, GeothermalGeneratorBlockEntity::new);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> GeothermalGeneratorBlockEntity.clientTick(lvl, pos, st);
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof MachineBlockEntity machine) {
                MachineBlockEntity.serverTick(lvl, pos, st, machine);
            }
        };
    }
}
