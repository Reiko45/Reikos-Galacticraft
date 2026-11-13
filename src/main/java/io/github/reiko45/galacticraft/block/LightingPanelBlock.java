/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockPanelLighting and
 * core.tile.TileEntityPanelLight). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Lighting Panels (1.12.2 BlockPanelLighting, five types): a block with a lit panel on one face, placed facing the
 * player. Square gives light 11, Spotlight 7, Linear 9 and the two Dark ones 2; a redstone signal switches it off. The
 * wrench turns the patterned ones (Linear and Dark a quarter turn, the diagonal Dark one through four) before swinging
 * the panel round to the next face: down, up, north, east, south, west.
 */
public class LightingPanelBlock extends Block implements io.github.reiko45.galacticraft.item.ShiftDescribed, Wrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final PanelType type;

    public LightingPanelBlock(Properties properties, PanelType type) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? type.light() : 0));
        this.type = type;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROTATION, 0).setValue(LIT, true));
    }

    public PanelType type() {
        return this.type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        final boolean lit = !level.hasNeighborSignal(pos);
        if (lit != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    /** 1.12.2 onUseWrench. */
    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        final int rotation = state.getValue(ROTATION);
        if (rotation + 1 < this.type.rotations()) {
            level.setBlock(pos, state.setValue(ROTATION, rotation + 1), Block.UPDATE_ALL);
            return true;
        }
        final Direction next = switch (state.getValue(FACING)) {
            case DOWN -> Direction.UP;
            case UP -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.DOWN;
        };
        level.setBlock(pos, state.setValue(FACING, next).setValue(ROTATION, 0), Block.UPDATE_ALL);
        return true;
    }

    /** 1.12.2 PanelType: its texture, light and how many ways the wrench turns it. */
    public enum PanelType {
        SQUARE("square", 11, 1), SPOTS("spots", 7, 1), LINEAR("linear", 9, 2), SF("sf", 2, 2), SFDIAG("sfdiag", 2, 4);

        private final String name;
        private final int light;
        private final int rotations;

        PanelType(String name, int light, int rotations) {
            this.name = name;
            this.light = light;
            this.rotations = rotations;
        }

        public String getName() {
            return this.name;
        }

        public int light() {
            return this.light;
        }

        public int rotations() {
            return this.rotations;
        }
    }

    /** 1.12.2 getShiftDescription: a text shared with others like it. */
    @Override
    public String shiftDescriptionKey(net.minecraft.world.item.ItemStack stack) {
        return "block.galacticraftcore.lighting_panel.description";
    }
}
