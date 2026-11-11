package io.github.reiko45.galacticraft.machine;

/** What a machine face does with energy. */
public enum EnergyAccess {
    NONE(false, false), INPUT(true, false), OUTPUT(false, true);

    public final boolean receive;
    public final boolean extract;

    EnergyAccess(boolean receive, boolean extract) {
        this.receive = receive;
        this.extract = extract;
    }
}
