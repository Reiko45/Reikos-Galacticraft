package io.github.reiko45.galacticraft.machine;

import net.minecraft.util.StringRepresentable;

/**
 * Input and output faces of an Energy Storage Module/Cluster (1.12.2 IMachineSides, TWOFACES_HORIZ). The order
 * is the 1.12.2 sneak + wrench cycle: (R,L) -> (R,B) -> (L,B) -> (L,R) -> (B,R) -> (B,L).
 */
public enum StorageSides implements StringRepresentable {
    RIGHT_LEFT(RelativeSide.RIGHT, RelativeSide.LEFT),
    RIGHT_BACK(RelativeSide.RIGHT, RelativeSide.BACK),
    LEFT_BACK(RelativeSide.LEFT, RelativeSide.BACK),
    LEFT_RIGHT(RelativeSide.LEFT, RelativeSide.RIGHT),
    BACK_RIGHT(RelativeSide.BACK, RelativeSide.RIGHT),
    BACK_LEFT(RelativeSide.BACK, RelativeSide.LEFT);

    private final RelativeSide input;
    private final RelativeSide output;

    StorageSides(RelativeSide input, RelativeSide output) {
        this.input = input;
        this.output = output;
    }

    public RelativeSide input() {
        return this.input;
    }

    public RelativeSide output() {
        return this.output;
    }

    public StorageSides next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return this.input.name().toLowerCase() + "_" + this.output.name().toLowerCase();
    }
}
