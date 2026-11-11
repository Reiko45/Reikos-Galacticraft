package io.github.reiko45.galacticraft.compat.jei;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.recipe.CircuitFabricatorRecipe;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Circuit Fabricator recipes, drawn like the 1.12.2 JEI category (crop of the fabricator GUI). */
public class CircuitFabricatingCategory implements IRecipeCategory<RecipeHolder<CircuitFabricatorRecipe>> {
    public static final RecipeType<RecipeHolder<CircuitFabricatorRecipe>> TYPE =
            RecipeType.createRecipeHolderType(GalacticraftCore.id("circuit_fabricating"));
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/circuit_fabricator.png");
    /** Item positions relative to the (3, 4) crop: GUI slots (15,17), (74,46), (74,64), (122,46), (145,20) and output (152,86). */
    private static final int[][] INPUTS = {{12, 13}, {71, 42}, {71, 60}, {119, 42}, {142, 16}};

    private final IDrawable background;
    private final IDrawableAnimated progress;
    private final IDrawable icon;

    public CircuitFabricatingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 3, 4, 168, 101);
        this.progress = guiHelper.createAnimatedDrawable(guiHelper.createDrawable(TEXTURE, 176, 17, 51, 10), 70,
                IDrawableAnimated.StartDirection.LEFT, false);
        this.icon = guiHelper.createDrawableItemLike(GCBlocks.CIRCUIT_FABRICATOR.get());
    }

    @Override
    public RecipeType<RecipeHolder<CircuitFabricatorRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return GCBlocks.CIRCUIT_FABRICATOR.get().getName();
    }

    @Override
    public int getWidth() {
        return 168;
    }

    @Override
    public int getHeight() {
        return 101;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CircuitFabricatorRecipe> holder, IFocusGroup focuses) {
        final CircuitFabricatorRecipe recipe = holder.value();
        for (int i = 0; i < INPUTS.length; i++) {
            final Ingredient ingredient = recipe.getIngredients().get(i);
            if (!ingredient.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, INPUTS[i][0], INPUTS[i][1]).addIngredients(ingredient);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 149, 82)
                .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(RecipeHolder<CircuitFabricatorRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.background.draw(graphics);
        this.progress.draw(graphics, 85, 16);
    }
}
