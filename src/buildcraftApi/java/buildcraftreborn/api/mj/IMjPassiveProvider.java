/*
 * Copyright (c) 2011-2017 SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * Ported to Minecraft 1.21.1 by BuildCraft Reborn.
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license,
 * which is located as "LICENSE-API" in the BuildCraft Reborn source code distribution.
 */
package buildcraftreborn.api.mj;

/** Designates a machine that provides power passively- it does not attempt to manually output its power (like an
 * engine). Power can be extracted from this by powered wooden kinesis pipes, for example. */
public interface IMjPassiveProvider extends IMjConnector {
    /** Attempts to extract power from this provider
     *
     * @return Either 0, min, max, or a value between min and max. */
    long extractPower(long min, long max, boolean simulate);
}
