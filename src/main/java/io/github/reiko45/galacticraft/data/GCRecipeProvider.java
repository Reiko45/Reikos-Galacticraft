package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.recipe.CircuitFabricatorRecipe;
import io.github.reiko45.galacticraft.recipe.CompressingRecipe;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

/**
 * Furnace and stonecutter recipes. 1.12.2 registered smelting in code ({@code RecipeManagerGC}); crafting
 * table recipes were JSON already and are converted by {@code tools/port_legacy_assets.py}.
 */
public class GCRecipeProvider extends RecipeProvider {
    public GCRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        compressing(output);
        fabricating(output);

        // RecipeManagerGC#addSmeltingRecipes (experience values kept); blasting is new in this port.
        ore(output, GCBlocks.TIN_ORE.get(), GCItems.TIN_INGOT.get(), 0.5F);
        ore(output, GCBlocks.ALUMINUM_ORE.get(), GCItems.ALUMINUM_INGOT.get(), 0.5F);
        ore(output, GCBlocks.MOON_COPPER_ORE.get(), Items.COPPER_INGOT, 1.0F);
        ore(output, GCBlocks.MOON_TIN_ORE.get(), GCItems.TIN_INGOT.get(), 1.0F);
        ore(output, GCBlocks.MOON_CHEESE_ORE.get(), GCItems.CHEESE_CURD.get(), 1.0F);
        ore(output, GCBlocks.MOON_SAPPHIRE_ORE.get(), GCItems.LUNAR_SAPPHIRE.get(), 1.0F);
        ore(output, GCItems.METEORIC_IRON_RAW.get(), GCItems.METEORIC_IRON_INGOT.get(), 1.0F);
        // RecipeManagerMars#addUniversalRecipes
        ore(output, GCItems.RAW_DESH.get(), GCItems.DESH_INGOT.get(), 0.2F);
        ore(output, GCBlocks.DESH_ORE.get(), GCItems.DESH_INGOT.get(), 0.2F);
        ore(output, GCBlocks.MARS_COPPER_ORE.get(), Items.COPPER_INGOT, 1.0F);
        ore(output, GCBlocks.MARS_TIN_ORE.get(), GCItems.TIN_INGOT.get(), 1.0F);
        ore(output, GCBlocks.MARS_IRON_ORE.get(), Items.IRON_INGOT, 0.2F);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(GCBlocks.MARS_COBBLESTONE.get()), RecipeCategory.BUILDING_BLOCKS, GCBlocks.MARS_STONE.get(), 0.0F, 200)
                .unlockedBy(getHasName(GCBlocks.MARS_COBBLESTONE.get()), has(GCBlocks.MARS_COBBLESTONE.get()))
                .save(output, GalacticraftCore.id("smelting/mars_stone"));

        // Canisters melt back down to 3 ingots.
        output.accept(GalacticraftCore.id("smelting/tin_ingot_from_tin_canister"), new SmeltingRecipe("", CookingBookCategory.MISC,
                Ingredient.of(GCItems.TIN_CANISTER.get()), new ItemStack(GCItems.TIN_INGOT.get(), 3), 1.0F, 200), null);
        output.accept(GalacticraftCore.id("smelting/copper_ingot_from_copper_canister"), new SmeltingRecipe("", CookingBookCategory.MISC,
                Ingredient.of(GCItems.COPPER_CANISTER.get()), new ItemStack(Items.COPPER_INGOT, 3), 1.0F, 200), null);

        // Beef patty: furnace like 1.12.2, plus smoker and campfire like vanilla meat.
        final Ingredient patty = Ingredient.of(GCItems.RAW_BEEF_PATTY.get());
        SimpleCookingRecipeBuilder.smelting(patty, RecipeCategory.FOOD, GCItems.COOKED_BEEF_PATTY.get(), 1.0F, 200)
                .unlockedBy("has_raw_beef_patty", has(GCItems.RAW_BEEF_PATTY.get()))
                .save(output, GalacticraftCore.id("smelting/cooked_beef_patty"));
        SimpleCookingRecipeBuilder.smoking(patty, RecipeCategory.FOOD, GCItems.COOKED_BEEF_PATTY.get(), 1.0F, 100)
                .unlockedBy("has_raw_beef_patty", has(GCItems.RAW_BEEF_PATTY.get()))
                .save(output, GalacticraftCore.id("smoking/cooked_beef_patty"));
        SimpleCookingRecipeBuilder.campfireCooking(patty, RecipeCategory.FOOD, GCItems.COOKED_BEEF_PATTY.get(), 1.0F, 600)
                .unlockedBy("has_raw_beef_patty", has(GCItems.RAW_BEEF_PATTY.get()))
                .save(output, GalacticraftCore.id("campfire_cooking/cooked_beef_patty"));

        // Stonecutter variants of the 1.12.2 stair/slab/wall crafting recipes (new in this port).
        cutting(output, GCBlocks.TIN_DECORATION_BLOCK_1.get(), GCBlocks.TIN_DECORATION_STAIRS_1.get(), GCBlocks.TIN_DECORATION_SLAB_1.get(), GCBlocks.TIN_DECORATION_WALL_1.get());
        cutting(output, GCBlocks.TIN_DECORATION_BLOCK_2.get(), GCBlocks.TIN_DECORATION_STAIRS_2.get(), GCBlocks.TIN_DECORATION_SLAB_2.get(), GCBlocks.TIN_DECORATION_WALL_2.get());
        cutting(output, GCBlocks.MOON_ROCK.get(), GCBlocks.MOON_ROCK_STAIRS.get(), GCBlocks.MOON_ROCK_SLAB.get(), GCBlocks.MOON_ROCK_WALL.get());
        cutting(output, GCBlocks.MOON_DUNGEON_BRICK.get(), GCBlocks.MOON_DUNGEON_BRICK_STAIRS.get(), GCBlocks.MOON_DUNGEON_BRICK_SLAB.get(), GCBlocks.MOON_DUNGEON_BRICK_WALL.get());
        cutting(output, GCBlocks.MARS_COBBLESTONE.get(), GCBlocks.MARS_COBBLESTONE_STAIRS.get(), GCBlocks.MARS_COBBLESTONE_SLAB.get(), GCBlocks.MARS_COBBLESTONE_WALL.get());
        cutting(output, GCBlocks.MARS_DUNGEON_BRICK.get(), GCBlocks.MARS_DUNGEON_BRICK_STAIRS.get(), GCBlocks.MARS_DUNGEON_BRICK_SLAB.get(), GCBlocks.MARS_DUNGEON_BRICK_WALL.get());
    }

    /** 1.12.2 RecipeManagerGC compressor recipes (CompressorRecipes). */
    private static void compressing(RecipeOutput output) {
        compressShapeless(output, "compressed_copper", GCItems.COMPRESSED_COPPER.get(), tag("ingots/copper"), tag("ingots/copper"));
        compressShapeless(output, "compressed_tin", GCItems.COMPRESSED_TIN.get(), tag("ingots/tin"), tag("ingots/tin"));
        compressShapeless(output, "compressed_aluminum", GCItems.COMPRESSED_ALUMINUM.get(), tag("ingots/aluminum"), tag("ingots/aluminum"));
        compressShapeless(output, "compressed_iron", GCItems.COMPRESSED_IRON.get(), Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.IRON_INGOT));
        compressShapeless(output, "compressed_meteoric_iron", GCItems.COMPRESSED_METEORIC_IRON.get(), Ingredient.of(GCItems.METEORIC_IRON_INGOT.get()));
        compressShapeless(output, "compressed_steel_from_coal", GCItems.COMPRESSED_STEEL.get(),
                Ingredient.of(Items.COAL), Ingredient.of(GCItems.COMPRESSED_IRON.get()), Ingredient.of(Items.COAL));
        compressShapeless(output, "compressed_bronze_from_plates", GCItems.COMPRESSED_BRONZE.get(),
                Ingredient.of(GCItems.COMPRESSED_COPPER.get()), Ingredient.of(GCItems.COMPRESSED_TIN.get()));
        // Only when another mod provides steel/bronze ingots, like 1.12.2's OreDictionary check.
        compressShapeless(output.withConditions(new NotCondition(new TagEmptyCondition("c", "ingots/bronze"))),
                "compressed_bronze", GCItems.COMPRESSED_BRONZE.get(), tag("ingots/bronze"), tag("ingots/bronze"));
        compressShapeless(output.withConditions(new NotCondition(new TagEmptyCondition("c", "ingots/steel"))),
                "compressed_steel", GCItems.COMPRESSED_STEEL.get(), tag("ingots/steel"), tag("ingots/steel"));
        output.accept(GalacticraftCore.id("compressing/heavy_plating"), CompressingRecipe.shaped(
                ShapedRecipePattern.of(Map.of(
                        'X', Ingredient.of(GCItems.COMPRESSED_STEEL.get()),
                        'Y', Ingredient.of(GCItems.COMPRESSED_ALUMINUM.get()),
                        'Z', Ingredient.of(GCItems.COMPRESSED_BRONZE.get())), "XYZ", "XYZ"),
                new ItemStack(GCItems.HEAVY_PLATING.get(), 2)), null);
        // RecipeManagerMars: the Tier 2 Rocket's plate and Compressed Desh.
        compressShapeless(output, "heavy_duty_plate", GCItems.HEAVY_DUTY_PLATE.get(),
                Ingredient.of(GCItems.HEAVY_PLATING.get()), Ingredient.of(GCItems.COMPRESSED_METEORIC_IRON.get()));
        compressShapeless(output, "compressed_desh", GCItems.COMPRESSED_DESH.get(), tag("ingots/desh"));
    }

    private static void compressShapeless(RecipeOutput output, String name, ItemLike result, Ingredient... ingredients) {
        output.accept(GalacticraftCore.id("compressing/" + name),
                CompressingRecipe.shapeless(NonNullList.of(Ingredient.EMPTY, ingredients), new ItemStack(result)), null);
    }

    /** 1.12.2 CircuitFabricatorRecipes: diamond, silicon, silicon, redstone, plus one recipe-specific item. */
    private static void fabricating(RecipeOutput output) {
        fabricate(output, GCItems.SOLAR_WAFER.get(), 9, tag("gems/lapis"));
        fabricate(output, GCItems.BASIC_WAFER.get(), 3, Ingredient.of(Items.REDSTONE_TORCH));
        fabricate(output, GCItems.ADVANCED_WAFER.get(), 1, Ingredient.of(Items.REPEATER));
    }

    private static void fabricate(RecipeOutput output, ItemLike result, int count, Ingredient extra) {
        output.accept(GalacticraftCore.id("circuit_fabricating/" + getItemName(result)), new CircuitFabricatorRecipe(
                NonNullList.of(Ingredient.EMPTY, tag("gems/diamond"), tag("silicon"), tag("silicon"), tag("dusts/redstone"), extra),
                new ItemStack(result, count)), null);
    }

    private static Ingredient tag(String path) {
        return Ingredient.of(GCTags.item(path));
    }

    private static void ore(RecipeOutput output, ItemLike input, ItemLike result, float experience) {
        final String name = getItemName(result) + "_from_" + getItemName(input);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, experience, 200)
                .unlockedBy(getHasName(input), has(input))
                .save(output, GalacticraftCore.id("smelting/" + name));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, experience, 100)
                .unlockedBy(getHasName(input), has(input))
                .save(output, GalacticraftCore.id("blasting/" + name));
    }

    private static void cutting(RecipeOutput output, ItemLike base, ItemLike stairs, ItemLike slab, ItemLike wall) {
        cut(output, base, stairs, 1);
        cut(output, base, slab, 2);
        cut(output, base, wall, 1);
    }

    private static void cut(RecipeOutput output, ItemLike base, ItemLike result, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(base), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy(getHasName(base), has(base))
                .save(output, GalacticraftCore.id("stonecutting/" + getItemName(result) + "_from_" + getItemName(base)));
    }
}
