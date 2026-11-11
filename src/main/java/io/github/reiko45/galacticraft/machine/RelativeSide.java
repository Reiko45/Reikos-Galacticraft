package io.github.reiko45.galacticraft.machine;

import net.minecraft.core.Direction;

/** A block face relative to a machine's horizontal facing (front = the face looking at the player who placed it). */
public enum RelativeSide {
    FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

    public static RelativeSide of(Direction facing, Direction side) {
        if (side == Direction.UP) {
            return TOP;
        }
        if (side == Direction.DOWN) {
            return BOTTOM;
        }
        if (side == facing) {
            return FRONT;
        }
        if (side == facing.getOpposite()) {
            return BACK;
        }
        return side == facing.getClockWise() ? LEFT : RIGHT;
    }
}
