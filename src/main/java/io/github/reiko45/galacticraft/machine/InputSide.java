package io.github.reiko45.galacticraft.machine;

import net.minecraft.util.StringRepresentable;

/**
 * Which face of an electric machine takes power (1.12.2 IMachineSides for single-input machines). Sneak + wrench
 * cycles LEFT -> BACK -> RIGHT, as in 1.12.2.
 */
public enum InputSide implements StringRepresentable {
    LEFT("left", RelativeSide.LEFT),
    BACK("back", RelativeSide.BACK),
    RIGHT("right", RelativeSide.RIGHT);

    private final String name;
    private final RelativeSide side;

    InputSide(String name, RelativeSide side) {
        this.name = name;
        this.side = side;
    }

    public RelativeSide side() {
        return this.side;
    }

    public InputSide next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
