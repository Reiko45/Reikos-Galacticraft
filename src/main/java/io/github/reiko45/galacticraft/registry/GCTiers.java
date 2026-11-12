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

    /**
     * 1.12.2 TOOLDESH: 1024 uses, efficiency 5, +2.5 damage, enchantability 10. MarsItems#registerHarvestLevels raised
     * the pickaxe, axe and shovel to harvest level 4, one above diamond, which is netherite's place since 1.16.
     * 1.12.2 never set a repair item for the tools (only the armour took Desh Ingots); here both repair with them.
     */
    public static final Tier DESH = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1024, 5.0F, 2.5F, 10,
            () -> Ingredient.of(GCItems.DESH_INGOT.get()));

    /**
     * 1.12.2 TOOL_TITANIUM: 760 uses, efficiency 14, +4 damage, enchantability 16; AsteroidsItems#registerHarvestLevels
     * gave the pickaxe and shovel level 5, above anything (netherite's place here). No repair item in 1.12.2; Titanium
     * Ingots here.
     */
    public static final Tier TITANIUM = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 760, 14.0F, 4.0F, 16,
            () -> Ingredient.of(GCItems.TITANIUM_INGOT.get()));

    /**
     * 1.12.2 ItemAxeAsteroids was built on TOOL_STEEL (768 uses, efficiency 5, enchantability 8, repaired with Compressed
     * Steel), then given axe level 5: the Titanium Axe keeps those numbers.
     */
    public static final Tier TITANIUM_AXE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 768, 5.0F, 2.0F, 8,
            () -> Ingredient.of(GCItems.COMPRESSED_STEEL.get()));

    private GCTiers() {
    }
}
