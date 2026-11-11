package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;

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
        // RecipeManagerGC#addSmeltingRecipes (experience values kept); blasting is new in this port.
        ore(output, GCBlocks.TIN_ORE.get(), GCItems.TIN_INGOT.get(), 0.5F);
        ore(output, GCBlocks.ALUMINUM_ORE.get(), GCItems.ALUMINUM_INGOT.get(), 0.5F);
        ore(output, GCBlocks.MOON_COPPER_ORE.get(), Items.COPPER_INGOT, 1.0F);
        ore(output, GCBlocks.MOON_TIN_ORE.get(), GCItems.TIN_INGOT.get(), 1.0F);
        ore(output, GCBlocks.MOON_CHEESE_ORE.get(), GCItems.CHEESE_CURD.get(), 1.0F);
        ore(output, GCBlocks.MOON_SAPPHIRE_ORE.get(), GCItems.LUNAR_SAPPHIRE.get(), 1.0F);
        ore(output, GCItems.METEORIC_IRON_RAW.get(), GCItems.METEORIC_IRON_INGOT.get(), 1.0F);

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
