package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.recipe.CircuitFabricatorRecipe;
import io.github.reiko45.galacticraft.recipe.CompressingRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Machine recipe types. 1.12.2 kept these in code (CompressorRecipes, CircuitFabricatorRecipes); now they are JSON. */
public final class GCRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, GalacticraftCore.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, GalacticraftCore.MOD_ID);

    public static final Supplier<RecipeType<CompressingRecipe>> COMPRESSING = TYPES.register("compressing",
            () -> RecipeType.simple(GalacticraftCore.id("compressing")));
    public static final Supplier<RecipeSerializer<CompressingRecipe>> COMPRESSING_SHAPED = SERIALIZERS.register("compressing_shaped",
            CompressingRecipe.ShapedSerializer::new);
    public static final Supplier<RecipeSerializer<CompressingRecipe>> COMPRESSING_SHAPELESS = SERIALIZERS.register("compressing_shapeless",
            CompressingRecipe.ShapelessSerializer::new);

    public static final Supplier<RecipeType<CircuitFabricatorRecipe>> CIRCUIT_FABRICATING = TYPES.register("circuit_fabricating",
            () -> RecipeType.simple(GalacticraftCore.id("circuit_fabricating")));
    public static final Supplier<RecipeSerializer<CircuitFabricatorRecipe>> CIRCUIT_FABRICATING_SERIALIZER = SERIALIZERS.register("circuit_fabricating",
            CircuitFabricatorRecipe.Serializer::new);

    private GCRecipes() {
    }
}
