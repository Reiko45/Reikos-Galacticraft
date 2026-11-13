package io.github.reiko45.galacticraft.compat.emi;

import dev.emi.emi.api.EmiApi;

/** Screenshot tour: open an EMI page (kept apart so the tour never loads EMI classes without EMI). */
public final class EmiTour {
    private EmiTour() {
    }

    public static void showCompressing() {
        EmiApi.displayRecipeCategory(GalacticraftEmiPlugin.COMPRESSING);
    }

    public static void showRefining() {
        EmiApi.displayRecipeCategory(GalacticraftEmiPlugin.REFINING);
    }
}
