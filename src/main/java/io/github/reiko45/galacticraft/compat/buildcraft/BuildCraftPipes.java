package io.github.reiko45.galacticraft.compat.buildcraft;

import buildcraftreborn.api.transport.pipe.PipeApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** BuildCraft Reborn's pipe API, for the sealable BuildCraft pipes. Only loaded with BuildCraft Reborn installed. */
final class BuildCraftPipes {
    private BuildCraftPipes() {
    }

    static boolean hasPipe(Level level, BlockPos pos) {
        return level.getCapability(PipeApi.CAP_PIPE, pos, null) != null;
    }
}
