package io.github.reiko45.galacticraft.compat.jei;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Refinery (1.12.2 RefineryRecipeCategory): an oil container in, a fuel container out, with the two tank gauges
 * draining and filling. The oil and fuel fluids are attached as hidden ingredients so looking either up finds it.
 */
public class RefiningCategory implements IRecipeCategory<RefiningCategory.Refining> {
    public static final RecipeType<Refining> TYPE = RecipeType.create(GalacticraftCore.MOD_ID, "refining", Refining.class);
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/refinery_recipe.png");

    /** One container pairing; the refinery itself turns any oil into fuel 1:1. */
    public record Refining(ItemStack input, ItemStack output) {
    }

    private final IDrawable background;
    private final IDrawableAnimated oil;
    private final IDrawableAnimated fuel;
    private final IDrawable icon;

    public RefiningCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 3, 4, 168, 64);
        this.oil = guiHelper.createAnimatedDrawable(guiHelper.createDrawable(TEXTURE, 176, 0, 16, 38), 70,
                IDrawableAnimated.StartDirection.TOP, true);
        this.fuel = guiHelper.createAnimatedDrawable(guiHelper.createDrawable(TEXTURE, 192, 0, 16, 38), 70,
                IDrawableAnimated.StartDirection.BOTTOM, false);
        this.icon = guiHelper.createDrawableItemLike(GCBlocks.REFINERY.get());
    }

    /** 1.12.2 RefineryRecipeMaker: oil canister or bucket in, fuel canister or bucket out. */
    public static List<Refining> recipes() {
        final ItemStack oilCanister = LiquidCanisterItem.full(false);
        final ItemStack fuelCanister = LiquidCanisterItem.full(true);
        final ItemStack oilBucket = new ItemStack(GCFluids.OIL_BUCKET.get());
        final ItemStack fuelBucket = new ItemStack(GCFluids.FUEL_BUCKET.get());
        return List.of(new Refining(oilCanister, fuelCanister), new Refining(oilBucket, fuelCanister),
                new Refining(oilCanister, fuelBucket), new Refining(oilBucket, fuelBucket));
    }

    @Override
    public RecipeType<Refining> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return GCBlocks.REFINERY.get().getName();
    }

    @Override
    public int getWidth() {
        return 168;
    }

    @Override
    public int getHeight() {
        return 64;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Refining recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 3).addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 3).addItemStack(recipe.output());
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(GCFluids.OIL.get(), 1000);
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addFluidStack(GCFluids.FUEL.get(), 1000);
    }

    @Override
    public void draw(Refining recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.background.draw(graphics);
        this.oil.draw(graphics, 40, 24);
        this.fuel.draw(graphics, 114, 24);
    }
}
