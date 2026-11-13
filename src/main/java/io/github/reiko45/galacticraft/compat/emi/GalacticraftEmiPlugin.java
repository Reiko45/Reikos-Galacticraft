package io.github.reiko45.galacticraft.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.menu.MagneticCraftingMenu;
import io.github.reiko45.galacticraft.recipe.CircuitFabricatorRecipe;
import io.github.reiko45.galacticraft.recipe.CompressingRecipe;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCMenus;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * EMI (optional; 1.12.2 had only JEI): the same Compressor, Circuit Fabricator and Refinery pages as the JEI plugin,
 * drawn on the 1.12.2 GUI sheets, with the machines as workstations, the Magnetic Crafting Table's note and its
 * recipe filling.
 */
@EmiEntrypoint
public class GalacticraftEmiPlugin implements EmiPlugin {
    private static final ResourceLocation COMPRESSOR_GUI = GalacticraftCore.id("textures/gui/ingot_compressor.png");
    private static final ResourceLocation FABRICATOR_GUI = GalacticraftCore.id("textures/gui/circuit_fabricator.png");
    private static final ResourceLocation REFINERY_GUI = GalacticraftCore.id("textures/gui/refinery_recipe.png");
    /** 1.12.2's arrows take 70 ticks in the JEI pages. */
    private static final int ANIMATION_MS = 3500;
    private static final int[][] FABRICATOR_INPUTS = {{12, 13}, {71, 42}, {71, 60}, {119, 42}, {142, 16}};

    public static final EmiRecipeCategory COMPRESSING = new EmiRecipeCategory(GalacticraftCore.id("compressing"),
            EmiStack.of(GCBlocks.COMPRESSOR.get()));
    public static final EmiRecipeCategory CIRCUIT_FABRICATING = new EmiRecipeCategory(GalacticraftCore.id("circuit_fabricating"),
            EmiStack.of(GCBlocks.CIRCUIT_FABRICATOR.get()));
    public static final EmiRecipeCategory REFINING = new EmiRecipeCategory(GalacticraftCore.id("refining"), EmiStack.of(GCBlocks.REFINERY.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(COMPRESSING);
        registry.addCategory(CIRCUIT_FABRICATING);
        registry.addCategory(REFINING);
        registry.addWorkstation(COMPRESSING, EmiStack.of(GCBlocks.COMPRESSOR.get()));
        registry.addWorkstation(COMPRESSING, EmiStack.of(GCBlocks.ELECTRIC_COMPRESSOR.get()));
        registry.addWorkstation(COMPRESSING, EmiStack.of(GCBlocks.ADVANCED_COMPRESSOR.get()));
        registry.addWorkstation(CIRCUIT_FABRICATING, EmiStack.of(GCBlocks.CIRCUIT_FABRICATOR.get()));
        registry.addWorkstation(REFINING, EmiStack.of(GCBlocks.REFINERY.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(GCBlocks.ELECTRIC_FURNACE.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(GCBlocks.ELECTRIC_ARC_FURNACE.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(GCBlocks.MAGNETIC_CRAFTING_TABLE.get()));

        for (RecipeHolder<CompressingRecipe> holder : registry.getRecipeManager().getAllRecipesFor(GCRecipes.COMPRESSING.get())) {
            registry.addRecipe(new Compressing(holder));
        }
        for (RecipeHolder<CircuitFabricatorRecipe> holder : registry.getRecipeManager().getAllRecipesFor(GCRecipes.CIRCUIT_FABRICATING.get())) {
            registry.addRecipe(new Fabricating(holder));
        }
        // 1.12.2 RefineryRecipeMaker: oil canister or bucket in, fuel canister or bucket out
        final ItemStack[] oil = {LiquidCanisterItem.full(false), new ItemStack(GCFluids.OIL_BUCKET.get())};
        final ItemStack[] fuel = {LiquidCanisterItem.full(true), new ItemStack(GCFluids.FUEL_BUCKET.get())};
        final String[] names = {"canister", "bucket"};
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                registry.addRecipe(new Refining(GalacticraftCore.id("/refining/" + names[i] + "_to_" + names[j]), oil[i], fuel[j]));
            }
        }

        // the NASA Workbench pages and 1.12.2's machine pages
        for (io.github.reiko45.galacticraft.menu.Workbenches.Bench bench : io.github.reiko45.galacticraft.menu.Workbenches.ALL) {
            final EmiRecipeCategory category = named(GalacticraftCore.id("workbench_" + bench.id()), EmiStack.of(GCBlocks.NASA_WORKBENCH.get()),
                    Component.translatable(bench.page().title()));
            registry.addCategory(category);
            registry.addWorkstation(category, EmiStack.of(GCBlocks.NASA_WORKBENCH.get()));
            final List<io.github.reiko45.galacticraft.menu.Workbenches.Recipe> recipes = io.github.reiko45.galacticraft.menu.Workbenches.recipes(bench);
            for (int i = 0; i < recipes.size(); i++) {
                registry.addRecipe(new Workbench(category, GalacticraftCore.id("/workbench/" + bench.id() + "/" + i), recipes.get(i)));
            }
        }
        for (io.github.reiko45.galacticraft.menu.MachinePages.Page page : io.github.reiko45.galacticraft.menu.MachinePages.ALL) {
            final EmiRecipeCategory category = named(GalacticraftCore.id(page.id()), EmiStack.of(page.machine().get()), page.machine().get().getName());
            registry.addCategory(category);
            registry.addWorkstation(category, EmiStack.of(page.machine().get()));
            final List<io.github.reiko45.galacticraft.menu.MachinePages.Conversion> conversions = io.github.reiko45.galacticraft.menu.MachinePages.conversions(page);
            for (int i = 0; i < conversions.size(); i++) {
                registry.addRecipe(new Conversion(category, GalacticraftCore.id("/" + page.id() + "/" + i), conversions.get(i)));
            }
        }

        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(GCBlocks.MAGNETIC_CRAFTING_TABLE.get())),
                List.of(Component.translatable("jei.galacticraftcore.magnetic_crafting.info")), GalacticraftCore.id("/info/magnetic_crafting_table")));
        registry.addRecipeHandler(GCMenus.MAGNETIC_CRAFTING.get(), new MagneticCrafting());
    }

    private static ItemStack result(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        return recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
    }

    /** The Compressor page: the 3 x 3 grid, the arrow filling, the result. */
    static final class Compressing extends BasicEmiRecipe {
        private final int gridWidth;

        Compressing(RecipeHolder<CompressingRecipe> holder) {
            super(COMPRESSING, holder.id(), 137, 78);
            final CompressingRecipe recipe = holder.value();
            final ShapedRecipePattern pattern = recipe.pattern();
            this.gridWidth = pattern != null ? pattern.width() : 3;
            for (Ingredient ingredient : recipe.getIngredients()) {
                this.inputs.add(EmiIngredient.of(ingredient));
            }
            this.outputs.add(EmiStack.of(io.github.reiko45.galacticraft.machine.CompressorGrid.quickMode(result(recipe),
                    io.github.reiko45.galacticraft.GCConfig.quickMode())));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(COMPRESSOR_GUI, 0, 0, 137, 78, 18, 17);
            widgets.addAnimatedTexture(COMPRESSOR_GUI, 59, 19, 52, 17, 176, 13, ANIMATION_MS, true, false, false);
            for (int i = 0; i < this.inputs.size(); i++) {
                if (!this.inputs.get(i).isEmpty()) {
                    widgets.addSlot(this.inputs.get(i), i % this.gridWidth * 18, i / this.gridWidth * 18).drawBack(false);
                }
            }
            widgets.addSlot(this.outputs.get(0), 119, 20).drawBack(false).recipeContext(this);
        }
    }

    /** The Circuit Fabricator page, on the machine's own sheet. */
    static final class Fabricating extends BasicEmiRecipe {
        Fabricating(RecipeHolder<CircuitFabricatorRecipe> holder) {
            super(CIRCUIT_FABRICATING, holder.id(), 168, 101);
            for (Ingredient ingredient : holder.value().getIngredients()) {
                this.inputs.add(EmiIngredient.of(ingredient));
            }
            this.outputs.add(EmiStack.of(result(holder.value())));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(FABRICATOR_GUI, 0, 0, 168, 101, 3, 4);
            widgets.addAnimatedTexture(FABRICATOR_GUI, 85, 16, 51, 10, 176, 17, ANIMATION_MS, true, false, false);
            for (int i = 0; i < FABRICATOR_INPUTS.length && i < this.inputs.size(); i++) {
                if (!this.inputs.get(i).isEmpty()) {
                    widgets.addSlot(this.inputs.get(i), FABRICATOR_INPUTS[i][0] - 1, FABRICATOR_INPUTS[i][1] - 1).drawBack(false);
                }
            }
            widgets.addSlot(this.outputs.get(0), 148, 81).drawBack(false).recipeContext(this);
        }
    }

    /** The Refinery page: a container of oil in, fuel out, the gauges draining and filling. */
    static final class Refining extends BasicEmiRecipe {
        Refining(ResourceLocation id, ItemStack oil, ItemStack fuel) {
            super(REFINING, id, 168, 64);
            this.inputs.add(EmiStack.of(oil));
            this.inputs.add(EmiStack.of(GCFluids.OIL.get(), 1000));
            this.outputs.add(EmiStack.of(fuel));
            this.outputs.add(EmiStack.of(GCFluids.FUEL.get(), 1000));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(REFINERY_GUI, 0, 0, 168, 64, 3, 4);
            widgets.addAnimatedTexture(REFINERY_GUI, 40, 24, 16, 38, 176, 0, ANIMATION_MS, false, false, true);
            widgets.addAnimatedTexture(REFINERY_GUI, 114, 24, 16, 38, 192, 0, ANIMATION_MS, false, true, false);
            widgets.addSlot(this.inputs.get(0), 39, 2).drawBack(false);
            widgets.addSlot(this.outputs.get(0), 113, 2).drawBack(false).recipeContext(this);
        }
    }

    /** A category named by a translation of our own (EMI would look for an "emi.category" key). */
    private static EmiRecipeCategory named(ResourceLocation id, EmiStack icon, Component name) {
        return new EmiRecipeCategory(id, icon) {
            @Override
            public Component getName() {
                return name;
            }
        };
    }

    /** A NASA Workbench page: a crop of its GUI with the parts and the result where the page has them. */
    static final class Workbench extends BasicEmiRecipe {
        private final io.github.reiko45.galacticraft.menu.Workbenches.Recipe recipe;
        private final int[] area;
        private final List<Integer> partSlots = new ArrayList<>();

        Workbench(EmiRecipeCategory category, ResourceLocation id, io.github.reiko45.galacticraft.menu.Workbenches.Recipe recipe) {
            super(category, id, recipe.bench().area()[2], recipe.bench().area()[3]);
            this.recipe = recipe;
            this.area = recipe.bench().area();
            for (int i = 0; i < recipe.parts().size(); i++) {
                final var part = recipe.parts().get(i);
                if (!part.items().isEmpty()) {
                    this.partSlots.add(i);
                    this.inputs.add(EmiIngredient.of(part.items().stream().map(EmiStack::of).toList()));
                }
            }
            this.outputs.add(EmiStack.of(recipe.result()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(this.recipe.bench().texture(), 0, 0, this.area[2], this.area[3], this.area[0],
                    this.area[1] + this.recipe.bench().textureTop());
            for (int i = 0; i < this.partSlots.size(); i++) {
                final var part = this.recipe.parts().get(this.partSlots.get(i));
                widgets.addSlot(this.inputs.get(i), part.x() - this.area[0] - 1, part.y() - this.area[1] - 1).drawBack(false);
            }
            widgets.addSlot(this.outputs.get(0), this.recipe.resultX() - this.area[0] - 1, this.recipe.resultY() - this.area[1] - 1)
                    .drawBack(false).recipeContext(this);
        }
    }

    /** What a machine turns into what. */
    static final class Conversion extends BasicEmiRecipe {
        Conversion(EmiRecipeCategory category, ResourceLocation id, io.github.reiko45.galacticraft.menu.MachinePages.Conversion conversion) {
            super(category, id, 82, 20);
            this.inputs.add(EmiStack.of(conversion.input()));
            this.outputs.add(EmiStack.of(conversion.output()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(this.inputs.get(0), 1, 1);
            widgets.addTexture(dev.emi.emi.api.render.EmiTexture.EMPTY_ARROW, 28, 2);
            widgets.addSlot(this.outputs.get(0), 61, 1).recipeContext(this);
        }
    }

    /** 1.12.2 MagneticCraftingTransferInfo: crafting recipes fill the table's grid (slots 1-9) from the player's 36. */
    static final class MagneticCrafting implements StandardRecipeHandler<MagneticCraftingMenu> {
        @Override
        public List<Slot> getInputSources(MagneticCraftingMenu menu) {
            return new ArrayList<>(menu.slots.subList(1, 46));
        }

        @Override
        public List<Slot> getCraftingSlots(MagneticCraftingMenu menu) {
            return new ArrayList<>(menu.slots.subList(1, 10));
        }

        @Override
        public Slot getOutputSlot(MagneticCraftingMenu menu) {
            return menu.slots.get(0);
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
        }
    }
}
