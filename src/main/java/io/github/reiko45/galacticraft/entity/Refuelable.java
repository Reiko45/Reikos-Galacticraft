package io.github.reiko45.galacticraft.entity;

/** A vehicle a Fuel Loader can fill through its pad (1.12.2 IDockable#addFuel). */
public interface Refuelable {
    /** @return the mB taken (or that would be taken) */
    int addFuel(int amount, boolean execute);
}
