/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.oxygen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that decides for itself which way air gets through it (1.12.2 IPartialSealableBlock): Space Glass seals
 * sideways only, Air Lock seals and sealable wires on every side, Grating not at all.
 */
public interface PartialSealable {
    /** Does air moving in direction {@code direction} stop at this block (or, leaving it, at this side)? */
    boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction);
}
