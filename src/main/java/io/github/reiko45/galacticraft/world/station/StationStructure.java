/*
 * Starter station layout from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.WorldGenSpaceStation and
 * ChunkProviderOrbit). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.station;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The station every new space station starts as (1.12.2 WorldGenSpaceStation): a 14 x 7 x 7 tin hull, a 5 x 5 bay
 * sunk into one end and a room with glass windows at the other, and the Space Station Arrival Block in the middle of
 * the room (1.12.2 put it at 0,64,0 and the hull at -10,62,-3).
 */
public final class StationStructure {
    /** Hull corner relative to the arrival block. */
    public static final BlockPos HULL_OFFSET = new BlockPos(-10, -2, -3);
    /** Where arriving players stand, relative to the arrival block: beside it, on the room's floor. */
    public static final BlockPos ARRIVAL_OFFSET = new BlockPos(-2, 0, 0);

    /** y layer by y layer, z row by z row, x across: '#' tin decoration block, 'G' glass pane. */
    private static final String[] LAYOUT = {
            "######........",
            "######........",
            "######........",
            "######........",
            "######........",
            "######........",
            "######........",
            "##############",
            "#.....########",
            "#.....########",
            "#.....########",
            "#.....########",
            "#.....########",
            "##############",
            "......###...##",
            ".......#.....#",
            "..............",
            "..............",
            "..............",
            ".......#.....#",
            "......###...##",
            "......##G...G#",
            ".......#.....G",
            "..............",
            "..............",
            "..............",
            ".......#.....G",
            "......##G...G#",
            "......###...##",
            ".......#.....#",
            "..............",
            "..............",
            "..............",
            ".......#.....#",
            "......###...##",
            "......########",
            "......##.....#",
            "......##.....#",
            "......##.....#",
            "......##.....#",
            "......##.....#",
            "......########",
            ".......#######",
            ".......#######",
            ".......#######",
            ".......#######",
            ".......#######",
            ".......#######",
            ".......#######"
    };

    private StationStructure() {
    }

    public static void build(ServerLevel level, BlockPos base) {
        final BlockPos corner = base.offset(HULL_OFFSET);
        final BlockState hull = GCBlocks.TIN_DECORATION_BLOCK_2.get().defaultBlockState();
        for (int y = 0; y < 7; y++) {
            for (int z = 0; z < 7; z++) {
                final String row = LAYOUT[y * 7 + z];
                for (int x = 0; x < row.length(); x++) {
                    final char c = row.charAt(x);
                    if (c == '#') {
                        level.setBlock(corner.offset(x, y, z), hull, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // Panes after the hull so they join up with it
        for (int y = 0; y < 7; y++) {
            for (int z = 0; z < 7; z++) {
                final String row = LAYOUT[y * 7 + z];
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) == 'G') {
                        final BlockPos pos = corner.offset(x, y, z);
                        level.setBlock(pos, Block.updateFromNeighbourShapes(Blocks.GLASS_PANE.defaultBlockState(), level, pos), Block.UPDATE_ALL);
                    }
                }
            }
        }
        level.setBlock(base, GCBlocks.SPACE_STATION_BASE.get().defaultBlockState(), Block.UPDATE_ALL);
    }
}
