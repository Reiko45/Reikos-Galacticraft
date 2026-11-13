/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.jei.tier1rocket, .buggy and the planets'
 * tier2rocket, cargorocket, tier3rocket and astrominer JEI categories and recipe makers). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The NASA Workbench pages as recipe pages for JEI and EMI (1.12.2 showed each as its own JEI category): every part slot
 * where the page has it, each with the items it takes, and one recipe for each number of chests (or storage boxes) the
 * page allows, as 1.12.2's recipe makers listed them.
 */
public final class Workbenches {
    /**
     * A page: its GUI texture (and how far down the texture its GUI starts), its slot rules from its menu, and the matrix
     * slots that hold the optional chests (the first {@code minStorage} of them needed).
     */
    public record Bench(String id, Schematics.Page page, ResourceLocation texture, int textureTop, int matrixSize,
                        BiPredicate<Integer, ItemStack> isPart, Function<Container, ItemStack> craft, Consumer<PartBenchMenu.Layout> layout,
                        int[] storageSlots, int minStorage) {
        /** The area of the GUI the recipe page shows: the slots, with 1.12.2's margins (x 3-171 of the 176 wide GUI). */
        public int[] area() {
            final int[] box = {Integer.MAX_VALUE, Integer.MIN_VALUE};
            this.layout.accept(new PartBenchMenu.Layout() {
                @Override
                public void result(int x, int y) {
                    include(y);
                }

                @Override
                public void part(int index, int x, int y) {
                    include(y);
                }

                private void include(int y) {
                    box[0] = Math.min(box[0], y);
                    box[1] = Math.max(box[1], y + 16);
                }
            });
            final int top = Math.max(0, box[0] - 8);
            return new int[] {3, top, 168, box[1] + 8 - top};
        }
    }

    /** A slot of a recipe page: where it is in the GUI and what it shows. */
    public record Part(int x, int y, List<ItemStack> items) {
    }

    public record Recipe(Bench bench, List<Part> parts, int resultX, int resultY, ItemStack result) {
    }

    public static final Bench ROCKET_T1 = new Bench("rocket_t1", Schematics.ROCKET_T1, GalacticraftCore.id("textures/gui/rocketbench.png"), 0,
            NasaWorkbenchMenu.MATRIX_SIZE, NasaWorkbenchMenu::isPart, NasaWorkbenchMenu::craft, NasaWorkbenchMenu::layout, new int[] {15, 16, 17}, 0);
    public static final Bench MOON_BUGGY = new Bench("moon_buggy", Schematics.MOON_BUGGY, GalacticraftCore.id("textures/gui/buggybench.png"), 0,
            BuggyBenchMenu.MATRIX_SIZE, BuggyBenchMenu::isPart, BuggyBenchMenu::craft, BuggyBenchMenu::layout, new int[] {17, 18, 19}, 0);
    public static final Bench ROCKET_T2 = new Bench("rocket_t2", Schematics.ROCKET_T2, GalacticraftCore.id("textures/gui/schematic_rocket_t2.png"), 0,
            Tier2RocketBenchMenu.MATRIX_SIZE, Tier2RocketBenchMenu::isPart, Tier2RocketBenchMenu::craft, Tier2RocketBenchMenu::layout,
            new int[] {19, 20, 21}, 0);
    public static final Bench CARGO_ROCKET = new Bench("cargo_rocket", Schematics.CARGO_ROCKET,
            GalacticraftCore.id("textures/gui/schematic_rocket_cargo.png"), 0, CargoRocketBenchMenu.MATRIX_SIZE, CargoRocketBenchMenu::isPart,
            CargoRocketBenchMenu::craft, CargoRocketBenchMenu::layout, new int[] {14, 15, 16}, 1);
    public static final Bench ROCKET_T3 = new Bench("rocket_t3", Schematics.ROCKET_T3, GalacticraftCore.id("textures/gui/schematic_rocket_t3.png"), 0,
            Tier3RocketBenchMenu.MATRIX_SIZE, Tier3RocketBenchMenu::isPart, Tier3RocketBenchMenu::craft, Tier3RocketBenchMenu::layout,
            new int[] {19, 20, 21}, 0);
    /** Its screen draws the texture from 26 pixels down. */
    public static final Bench ASTRO_MINER = new Bench("astro_miner", Schematics.ASTRO_MINER,
            GalacticraftCore.id("textures/gui/schematic_astro_miner.png"), 26, AstroMinerBenchMenu.MATRIX_SIZE, AstroMinerBenchMenu::isPart,
            AstroMinerBenchMenu::craft, AstroMinerBenchMenu::layout, new int[] {}, 0);

    public static final List<Bench> ALL = List.of(ROCKET_T1, MOON_BUGGY, ROCKET_T2, CARGO_ROCKET, ROCKET_T3, ASTRO_MINER);

    private Workbenches() {
    }

    /** A recipe for each number of chests the page allows, every slot showing all the items it takes. */
    public static List<Recipe> recipes(Bench bench) {
        final List<List<ItemStack>> candidates = new ArrayList<>();
        for (int i = 0; i < bench.matrixSize(); i++) {
            candidates.add(new ArrayList<>());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            final ItemStack stack = new ItemStack(item);
            for (int i = 1; i < bench.matrixSize(); i++) {
                if (bench.isPart().test(i, stack)) {
                    candidates.get(i).add(stack);
                }
            }
        }
        final List<Recipe> recipes = new ArrayList<>();
        for (int storage = bench.minStorage(); storage <= bench.storageSlots().length; storage++) {
            final SimpleContainer matrix = new SimpleContainer(bench.matrixSize());
            final java.util.Set<Integer> filled = new java.util.HashSet<>();
            for (int i = 1; i < bench.matrixSize(); i++) {
                if (!isStorage(bench, i) && !candidates.get(i).isEmpty()) {
                    filled.add(i);
                }
            }
            for (int s = 0; s < storage; s++) {
                filled.add(bench.storageSlots()[s]);
            }
            for (int i : filled) {
                if (!candidates.get(i).isEmpty()) {
                    matrix.setItem(i, candidates.get(i).get(0).copy());
                }
            }
            final ItemStack result = bench.craft().apply(matrix);
            if (result.isEmpty()) {
                continue;
            }
            final List<Part> parts = new ArrayList<>();
            final int[] resultAt = new int[2];
            bench.layout().accept(new PartBenchMenu.Layout() {
                @Override
                public void result(int x, int y) {
                    resultAt[0] = x;
                    resultAt[1] = y;
                }

                @Override
                public void part(int index, int x, int y) {
                    parts.add(new Part(x, y, filled.contains(index) ? List.copyOf(candidates.get(index)) : List.of()));
                }
            });
            recipes.add(new Recipe(bench, parts, resultAt[0], resultAt[1], result));
        }
        return recipes;
    }

    private static boolean isStorage(Bench bench, int index) {
        for (int slot : bench.storageSlots()) {
            if (slot == index) {
                return true;
            }
        }
        return false;
    }
}
