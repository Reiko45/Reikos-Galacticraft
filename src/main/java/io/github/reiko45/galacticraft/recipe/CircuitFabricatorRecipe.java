package io.github.reiko45.galacticraft.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Circuit Fabricator recipe (1.12.2 CircuitFabricatorRecipes): five positional ingredients, one per input slot
 * (diamond, silicon, silicon, redstone, and a recipe-specific fifth item), giving one result.
 */
public final class CircuitFabricatorRecipe implements Recipe<CircuitFabricatorRecipe.Input> {
    public static final int SLOTS = 5;

    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    public CircuitFabricatorRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    /** The five fabricator input slots, in order. */
    public record Input(List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return this.items.get(index);
        }

        @Override
        public int size() {
            return this.items.size();
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        for (int i = 0; i < SLOTS; i++) {
            final Ingredient ingredient = this.ingredients.get(i);
            final ItemStack stack = input.getItem(i);
            if (ingredient.isEmpty() ? !stack.isEmpty() : !ingredient.test(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GCRecipes.CIRCUIT_FABRICATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GCRecipes.CIRCUIT_FABRICATING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<CircuitFabricatorRecipe> {
        private static final MapCodec<CircuitFabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.listOf().fieldOf("ingredients").flatXmap(list -> list.size() == SLOTS
                                ? DataResult.success(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)))
                                : DataResult.error(() -> "Circuit fabricator recipes need exactly " + SLOTS + " ingredients"),
                        DataResult::success).forGetter(CircuitFabricatorRecipe::getIngredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, CircuitFabricatorRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, CircuitFabricatorRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), CircuitFabricatorRecipe::getIngredients,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                (list, result) -> new CircuitFabricatorRecipe(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)), result));

        @Override
        public MapCodec<CircuitFabricatorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CircuitFabricatorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
