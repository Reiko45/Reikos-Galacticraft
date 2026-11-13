/*
 * Copyright (c) 2011-2017 SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * Ported to Minecraft 1.21.1 by BuildCraft Reborn.
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license,
 * which is located as "LICENSE-API" in the BuildCraft Reborn source code distribution.
 */
package buildcraftreborn.api.mj;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Compile-time copy of the parts of BuildCraft Reborn's MjAPI that Galacticraft uses (the full class is in BuildCraft
 * Reborn's jar, which is what runs). Not packaged into Galacticraft's jar.
 */
public class MjAPI {
    /** A single minecraft joule, in micro joules (the power system base unit) */
    public static final long ONE_MINECRAFT_JOULE = getMjValue();
    /** The same as {@link #ONE_MINECRAFT_JOULE}, but a shorter field name */
    public static final long MJ = ONE_MINECRAFT_JOULE;

    public static final BlockCapability<IMjConnector, @Nullable Direction> CAP_CONNECTOR =
        BlockCapability.createSided(id("mj_connector"), IMjConnector.class);
    public static final BlockCapability<IMjReceiver, @Nullable Direction> CAP_RECEIVER =
        BlockCapability.createSided(id("mj_receiver"), IMjReceiver.class);
    public static final BlockCapability<IMjReadable, @Nullable Direction> CAP_READABLE =
        BlockCapability.createSided(id("mj_readable"), IMjReadable.class);
    public static final BlockCapability<IMjPassiveProvider, @Nullable Direction> CAP_PASSIVE_PROVIDER =
        BlockCapability.createSided(id("mj_passive_provider"), IMjPassiveProvider.class);

    private static long getMjValue() {
        return 1_000_000L;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("buildcraftreborn", path);
    }
}
