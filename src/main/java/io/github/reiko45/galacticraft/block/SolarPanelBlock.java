package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Basic/Advanced Solar Panel base block (1.12.2 BlockSolar). Placing it adds a pole above and a 3x3 panel on top,
 * made of {@link SolarPartBlock}s; it needs that space free and no other solar panel within 2 blocks at the same height.
 */
public class SolarPanelBlock extends MachineBlock {
    public SolarPanelBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties, factory);
    }

    /** The pole (y+1) followed by the 3x3 panel (y+2), as in 1.12.2 getPositions. */
    public static List<BlockPos> partPositions(BlockPos pos) {
        final List<BlockPos> positions = new ArrayList<>(10);
        positions.add(pos.above());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                positions.add(pos.offset(x, 2, z));
            }
        }
        return positions;
    }

    public static boolean hasRoom(Level level, BlockPos pos) {
        for (BlockPos part : partPositions(pos)) {
            if (!level.getBlockState(part).canBeReplaced()) {
                return false;
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if ((x != 0 || z != 0) && level.getBlockState(pos.offset(x, 0, z)).getBlock() instanceof SolarPanelBlock) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!hasRoom(context.getLevel(), context.getClickedPos())) {
            if (context.getPlayer() != null && context.getLevel().isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.warning.no_room"), true);
            }
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        final List<BlockPos> parts = partPositions(pos);
        for (int i = 0; i < parts.size(); i++) {
            final BlockPos partPos = parts.get(i);
            level.setBlock(partPos, GCBlocks.SOLAR_PART.get().defaultBlockState()
                    .setValue(SolarPartBlock.PART, i == 0 ? SolarPartBlock.Part.POLE : SolarPartBlock.Part.PANEL), 3);
            if (level.getBlockEntity(partPos) instanceof SolarPartBlockEntity part) {
                part.setMainPos(pos);
            }
        }
    }

    /** Removing the base removes the parts (1.12.2 TileEntitySolar#onDestroy). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            for (BlockPos partPos : partPositions(pos)) {
                if (level.getBlockEntity(partPos) instanceof SolarPartBlockEntity part && pos.equals(part.mainPos())) {
                    level.removeBlock(partPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Ticks on the client too, to animate the panel following the sun. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return super.getTicker(level, state, type);
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof SolarPanelBlockEntity panel) {
                panel.clientTick();
            }
        };
    }
}
