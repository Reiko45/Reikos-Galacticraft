package io.github.reiko45.galacticraft.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public final class GCTiers {
    /**
     * 1.12.2 TOOL_STEEL: harvest level 3 (diamond), 768 uses, efficiency 5, +2 damage, enchantability 8.
     * Repaired with Compressed Steel.
     */
    public static final Tier HEAVY_DUTY = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 768, 5.0F, 2.0F, 8,
            () -> Ingredient.of(GCItems.COMPRESSED_STEEL.get()));

    private GCTiers() {
    }
}
