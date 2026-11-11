package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * NASA Workbench (1.12.2 BlockNasaWorkbench + TileEntityNasaWorkbench): a 3x3, 4 high multiblock (a plus shape on the
 * two layers above the base, and one block on top) drawn with the 1.12.2 OBJ model. Opens the schematic pages.
 */
public class NasaWorkbenchBlock extends Block {
    public static final MapCodec<NasaWorkbenchBlock> CODEC = simpleCodec(NasaWorkbenchBlock::new);

    public NasaWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** 1.12.2 TileEntityNasaWorkbench#getPositions. */
    public static List<BlockPos> partPositions(BlockPos pos) {
        final List<BlockPos> positions = new ArrayList<>();
        for (int y = 1; y < 3; y++) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    if (Math.abs(x) != 1 || Math.abs(z) != 1) {
                        positions.add(pos.offset(x, y, z));
                    }
                }
            }
        }
        positions.add(pos.above(3));
        return positions;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (BlockPos part : partPositions(context.getClickedPos())) {
            if (!context.getLevel().getBlockState(part).canBeReplaced() || !context.getLevel().isInWorldBounds(part)) {
                if (context.getPlayer() != null && context.getLevel().isClientSide) {
                    context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.warning.no_room"), true);
                }
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        for (BlockPos partPos : partPositions(pos)) {
            level.setBlock(partPos, GCBlocks.NASA_WORKBENCH_PART.get().defaultBlockState()
                    .setValue(SolarPartBlock.PART, SolarPartBlock.Part.WORKBENCH), Block.UPDATE_ALL);
            if (level.getBlockEntity(partPos) instanceof SolarPartBlockEntity part) {
                part.setMainPos(pos);
            }
        }
    }

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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            // 1.12.2 opened the first unlocked page: the Tier 1 Rocket
            io.github.reiko45.galacticraft.menu.Schematics.open((net.minecraft.server.level.ServerPlayer) player,
                    io.github.reiko45.galacticraft.menu.Schematics.unlocked(player).get(0), level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
