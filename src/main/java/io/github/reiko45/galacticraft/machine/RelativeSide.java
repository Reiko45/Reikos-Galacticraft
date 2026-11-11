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

    /** The world direction of this side for a block facing {@code facing}. */
    public Direction toDirection(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }
}
