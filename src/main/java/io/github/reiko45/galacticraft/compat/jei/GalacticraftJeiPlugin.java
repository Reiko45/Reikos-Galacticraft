package io.github.reiko45.galacticraft.compat.jei;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.screen.CircuitFabricatorScreen;
import io.github.reiko45.galacticraft.client.screen.CompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricCompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricFurnaceScreen;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * JEI integration (1.12.2 GalacticraftJEI): Compressor, Circuit Fabricator and Refinery categories, machine catalysts and
 * GUI click areas. Loaded only when JEI is present.
 */
@JeiPlugin
public class GalacticraftJeiPlugin implements IModPlugin {
    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return GalacticraftCore.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Screenshot tour: open the recipe view of our categories (0 compressing, 1 circuit fabricating, 2 refining). */
    public static boolean showCategory(int index) {
        if (runtime == null) {
            return false;
        }
        runtime.getRecipesGui().showTypes(List.of(switch (index) {
            case 0 -> CompressingCategory.TYPE;
            case 1 -> CircuitFabricatingCategory.TYPE;
            default -> RefiningCategory.TYPE;
        }));
        return true;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        final IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CompressingCategory(guiHelper), new CircuitFabricatingCategory(guiHelper), new RefiningCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        final RecipeManager recipes = Minecraft.getInstance().level.getRecipeManager();
        registration.addRecipes(CompressingCategory.TYPE, recipes.getAllRecipesFor(GCRecipes.COMPRESSING.get()));
        registration.addRecipes(CircuitFabricatingCategory.TYPE, recipes.getAllRecipesFor(GCRecipes.CIRCUIT_FABRICATING.get()));
        registration.addRecipes(RefiningCategory.TYPE, RefiningCategory.recipes());
        registration.addItemStackInfo(new net.minecraft.world.item.ItemStack(GCBlocks.MAGNETIC_CRAFTING_TABLE.get()),
                net.minecraft.network.chat.Component.translatable("jei.galacticraftcore.magnetic_crafting.info"));
    }

    /** 1.12.2 MagneticCraftingTransferInfo: recipes fill the table's grid (slots 1-9) from the player's 36. */
    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(io.github.reiko45.galacticraft.menu.MagneticCraftingMenu.class,
                io.github.reiko45.galacticraft.registry.GCMenus.MAGNETIC_CRAFTING.get(), RecipeTypes.CRAFTING, 1, 9, 10, 36);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CompressingCategory.TYPE,
                GCBlocks.COMPRESSOR.get(), GCBlocks.ELECTRIC_COMPRESSOR.get(), GCBlocks.ADVANCED_COMPRESSOR.get());
        registration.addRecipeCatalysts(CircuitFabricatingCategory.TYPE, GCBlocks.CIRCUIT_FABRICATOR.get());
        registration.addRecipeCatalysts(RefiningCategory.TYPE, GCBlocks.REFINERY.get());
        registration.addRecipeCatalysts(RecipeTypes.SMELTING, GCBlocks.ELECTRIC_FURNACE.get(), GCBlocks.ELECTRIC_ARC_FURNACE.get());
        registration.addRecipeCatalysts(RecipeTypes.CRAFTING, GCBlocks.MAGNETIC_CRAFTING_TABLE.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Progress arrows, as in the 1.12.2 GUIs.
        registration.addRecipeClickArea(CompressorScreen.class, 77, 30, 52, 25, CompressingCategory.TYPE);
        registration.addRecipeClickArea(ElectricCompressorScreen.class, 77, 30, 52, 25, CompressingCategory.TYPE);
        registration.addRecipeClickArea(CircuitFabricatorScreen.class, 87, 19, 53, 12, CircuitFabricatingCategory.TYPE);
        registration.addRecipeClickArea(ElectricFurnaceScreen.class, 78, 24, 23, 15, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(io.github.reiko45.galacticraft.client.screen.MagneticCraftingScreen.class, 88, 32, 28, 23, RecipeTypes.CRAFTING);
    }
}
