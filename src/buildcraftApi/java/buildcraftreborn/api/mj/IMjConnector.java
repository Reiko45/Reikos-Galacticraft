/*
 * Copyright (c) 2011-2017 SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * Ported to Minecraft 1.21.1 by BuildCraft Reborn.
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license,
 * which is located as "LICENSE-API" in the BuildCraft Reborn source code distribution.
 */
package buildcraftreborn.api.mj;

/** Signifies that this should visibly connect to other Mj handling entities/tiles. This should NEVER be the tile
 * entity, but an encapsulated class that refers back to it. Use {@link MjAPI#CAP_CONNECTOR} to access this. */
public interface IMjConnector {
    /** Checks to see if this connector can connect to the other connector. By default this should check that the other
     * connector is the same power system. */
    boolean canConnect(IMjConnector other);
}
