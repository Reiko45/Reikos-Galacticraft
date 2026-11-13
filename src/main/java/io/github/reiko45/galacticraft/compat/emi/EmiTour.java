package io.github.reiko45.galacticraft.compat.emi;

import dev.emi.emi.api.EmiApi;

/** Screenshot tour: open an EMI page (kept apart so the tour never loads EMI classes without EMI). */
public final class EmiTour {
    private EmiTour() {
    }

    public static void showCompressing() {
        EmiApi.displayRecipeCategory(GalacticraftEmiPlugin.COMPRESSING);
    }

    /** A NASA Workbench page or 1.12.2 machine page, by its category id's path. */
    public static void show(String path) {
        final dev.emi.emi.api.recipe.EmiRecipeCategory category = EmiApi.getRecipeManager().getCategories().stream()
                .filter(c -> c.getId().equals(io.github.reiko45.galacticraft.GalacticraftCore.id(path))).findFirst().orElse(null);
        if (category != null) {
            EmiApi.displayRecipeCategory(category);
        }
    }

    public static void showRefining() {
        EmiApi.displayRecipeCategory(GalacticraftEmiPlugin.REFINING);
    }
}
