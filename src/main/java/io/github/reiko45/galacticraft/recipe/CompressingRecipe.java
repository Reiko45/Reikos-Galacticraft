package io.github.reiko45.galacticraft.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import org.jetbrains.annotations.Nullable;

/**
 * Compressor recipe (1.12.2 CompressorRecipes): a 3x3 grid, shaped or shapeless, using the same JSON shape as
 * vanilla crafting recipes but its own recipe type so it never appears in a crafting table. Used by the
 * Compressor and the Electric Compressor.
 */
public final class CompressingRecipe implements Recipe<CraftingInput> {
    @Nullable
    private final ShapedRecipePattern pattern;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    private CompressingRecipe(@Nullable ShapedRecipePattern pattern, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.pattern = pattern;
        this.ingredients = ingredients;
        this.result = result;
    }

    public static CompressingRecipe shaped(ShapedRecipePattern pattern, ItemStack result) {
        return new CompressingRecipe(pattern, pattern.ingredients(), result);
    }

    public static CompressingRecipe shapeless(NonNullList<Ingredient> ingredients, ItemStack result) {
        return new CompressingRecipe(null, ingredients, result);
    }

    public boolean isShaped() {
        return this.pattern != null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (this.pattern != null) {
            return this.pattern.matches(input);
        }
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        final List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                items.add(input.getItem(i));
            }
        }
        return RecipeMatcher.findMatches(items, this.ingredients) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return this.pattern != null ? width >= this.pattern.width() && height >= this.pattern.height() : width * height >= this.ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Nullable
    public ShapedRecipePattern pattern() {
        return this.pattern;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.pattern != null ? GCRecipes.COMPRESSING_SHAPED.get() : GCRecipes.COMPRESSING_SHAPELESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GCRecipes.COMPRESSING.get();
    }

    @Override
    public boolean isSpecial() {
        return true; // no recipe book
    }

    public static final class ShapedSerializer implements RecipeSerializer<CompressingRecipe> {
        private static final MapCodec<CompressingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ShapedRecipePattern.MAP_CODEC.forGetter(CompressingRecipe::pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, CompressingRecipe::shaped));
        private static final StreamCodec<RegistryFriendlyByteBuf, CompressingRecipe> STREAM_CODEC = StreamCodec.composite(
                ShapedRecipePattern.STREAM_CODEC, CompressingRecipe::pattern,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                CompressingRecipe::shaped);

        @Override
        public MapCodec<CompressingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompressingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static final class ShapelessSerializer implements RecipeSerializer<CompressingRecipe> {
        private static final MapCodec<CompressingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
                    if (list.isEmpty()) {
                        return DataResult.error(() -> "No ingredients for compressing recipe");
                    }
                    if (list.size() > 9) {
                        return DataResult.error(() -> "Too many ingredients for compressing recipe (max 9)");
                    }
                    return DataResult.success(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)));
                }, DataResult::success).forGetter(CompressingRecipe::getIngredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, CompressingRecipe::shapeless));
        private static final StreamCodec<RegistryFriendlyByteBuf, CompressingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
                recipe -> recipe.ingredients,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                (list, result) -> CompressingRecipe.shapeless(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)), result));

        @Override
        public MapCodec<CompressingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompressingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
