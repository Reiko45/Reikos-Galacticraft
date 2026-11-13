/*
 * Copyright (c) 2011-2017 SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * Ported to Minecraft 1.21.1 by BuildCraft Reborn.
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license,
 * which is located as "LICENSE-API" in the BuildCraft Reborn source code distribution.
 */
package buildcraftreborn.api.transport.pipe;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Compile-time copy of the part of BuildCraft Reborn's PipeApi that Galacticraft uses (the full class is in BuildCraft
 * Reborn's jar, which is what runs). Not packaged into Galacticraft's jar.
 */
public final class PipeApi {
    public static final BlockCapability<IPipe, @Nullable Direction> CAP_PIPE =
        BlockCapability.createSided(id("pipe"), IPipe.class);

    // Internals

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("buildcraftreborn", path);
    }
}
