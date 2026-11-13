package io.github.reiko45.galacticraft.compat.jei;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.Workbenches;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A NASA Workbench page (1.12.2 Tier1RocketRecipeCategory and its kin): a crop of the page's own GUI with every part slot
 * and the result, one recipe for each number of chests.
 */
public class WorkbenchCategory implements IRecipeCategory<Workbenches.Recipe> {
    private final Workbenches.Bench bench;
    private final RecipeType<Workbenches.Recipe> type;
    private final int[] area;
    private final IDrawable background;
    private final IDrawable icon;

    public WorkbenchCategory(IGuiHelper guiHelper, Workbenches.Bench bench) {
        this.bench = bench;
        this.type = type(bench);
        this.area = bench.area();
        this.background = guiHelper.createDrawable(bench.texture(), this.area[0], this.area[1] + bench.textureTop(), this.area[2], this.area[3]);
        this.icon = guiHelper.createDrawableItemLike(GCBlocks.NASA_WORKBENCH.get());
    }

    public static RecipeType<Workbenches.Recipe> type(Workbenches.Bench bench) {
        return RecipeType.create(GalacticraftCore.MOD_ID, "workbench_" + bench.id(), Workbenches.Recipe.class);
    }

    @Override
    public RecipeType<Workbenches.Recipe> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(this.bench.page().title());
    }

    @Override
    public int getWidth() {
        return this.area[2];
    }

    @Override
    public int getHeight() {
        return this.area[3];
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Workbenches.Recipe recipe, IFocusGroup focuses) {
        for (Workbenches.Part part : recipe.parts()) {
            if (!part.items().isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, part.x() - this.area[0], part.y() - this.area[1]).addItemStacks(part.items());
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, recipe.resultX() - this.area[0], recipe.resultY() - this.area[1]).addItemStack(recipe.result());
    }

    @Override
    public void draw(Workbenches.Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.background.draw(graphics);
    }
}
