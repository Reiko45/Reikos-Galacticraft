package io.github.reiko45.galacticraft.compat.jei;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.MachinePages;
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

/** What a machine turns into what (1.12.2's Oxygen Compressor, Gas Liquefier and Methane Synthesizer JEI pages). */
public class MachinePageCategory implements IRecipeCategory<MachinePages.Conversion> {
    private final MachinePages.Page page;
    private final RecipeType<MachinePages.Conversion> type;
    private final IDrawable arrow;
    private final IDrawable slot;
    private final IDrawable icon;

    public MachinePageCategory(IGuiHelper guiHelper, MachinePages.Page page) {
        this.page = page;
        this.type = type(page);
        this.arrow = guiHelper.getRecipeArrow();
        this.slot = guiHelper.getSlotDrawable();
        this.icon = guiHelper.createDrawableItemLike(page.machine().get());
    }

    public static RecipeType<MachinePages.Conversion> type(MachinePages.Page page) {
        return RecipeType.create(GalacticraftCore.MOD_ID, page.id(), MachinePages.Conversion.class);
    }

    @Override
    public RecipeType<MachinePages.Conversion> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return this.page.machine().get().getName();
    }

    @Override
    public int getWidth() {
        return 82;
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachinePages.Conversion recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 2).addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 62, 2).addItemStack(recipe.output());
    }

    @Override
    public void draw(MachinePages.Conversion recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.slot.draw(graphics, 1, 1);
        this.arrow.draw(graphics, 28, 2);
        this.slot.draw(graphics, 61, 1);
    }
}
