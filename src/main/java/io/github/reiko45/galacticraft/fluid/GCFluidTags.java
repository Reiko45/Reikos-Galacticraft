package io.github.reiko45.galacticraft.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Convention fluid tags. 1.12.2 accepted any fluid named like oil (oil, oilgc, crude_oil...) in the Refinery; the
 * {@code c:oil} tag is the modern way for other mods' oil to qualify.
 */
public final class GCFluidTags {
    public static final TagKey<Fluid> OIL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "oil"));
    public static final TagKey<Fluid> FUEL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "fuel"));

    private GCFluidTags() {
    }
}
