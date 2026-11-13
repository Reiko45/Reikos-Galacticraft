package io.github.reiko45.galacticraft.compat.jei;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.recipe.CompressingRecipe;
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
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** Compressor recipes, drawn like the 1.12.2 JEI category (crop of the Compressor GUI). */
public class CompressingCategory implements IRecipeCategory<RecipeHolder<CompressingRecipe>> {
    public static final RecipeType<RecipeHolder<CompressingRecipe>> TYPE = RecipeType.createRecipeHolderType(GalacticraftCore.id("compressing"));
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/ingot_compressor.png");

    private final IDrawable background;
    private final IDrawableAnimated progress;
    private final IDrawable icon;

    public CompressingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 18, 17, 137, 78);
        this.progress = guiHelper.createAnimatedDrawable(guiHelper.createDrawable(TEXTURE, 176, 13, 52, 17), 70,
                IDrawableAnimated.StartDirection.LEFT, false);
        this.icon = guiHelper.createDrawableItemLike(GCBlocks.COMPRESSOR.get());
    }

    @Override
    public RecipeType<RecipeHolder<CompressingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return GCBlocks.COMPRESSOR.get().getName();
    }

    @Override
    public int getWidth() {
        return 137;
    }

    @Override
    public int getHeight() {
        return 78;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CompressingRecipe> holder, IFocusGroup focuses) {
        final CompressingRecipe recipe = holder.value();
        final ShapedRecipePattern pattern = recipe.pattern();
        final int width = pattern != null ? pattern.width() : 3;
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            final Ingredient ingredient = recipe.getIngredients().get(i);
            if (!ingredient.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 1 + i % width * 18, 1 + i / width * 18).addIngredients(ingredient);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 21)
                .addItemStack(io.github.reiko45.galacticraft.machine.CompressorGrid.quickMode(
                        recipe.getResultItem(Minecraft.getInstance().level.registryAccess()), io.github.reiko45.galacticraft.GCConfig.quickMode()));
    }

    @Override
    public void draw(RecipeHolder<CompressingRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.background.draw(graphics);
        this.progress.draw(graphics, 59, 19);
    }
}
