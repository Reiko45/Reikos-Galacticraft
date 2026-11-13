package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.RocketItem;
import io.github.reiko45.galacticraft.menu.AstroMinerBenchMenu;
import io.github.reiko45.galacticraft.menu.MachinePages;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import io.github.reiko45.galacticraft.menu.PartBenchMenu;
import io.github.reiko45.galacticraft.menu.Workbenches;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 10 part 2: the NASA Workbench pages and 1.12.2's machine pages for JEI and EMI. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkbenchPageTests {
    private static final String AIR = "air_room";

    private WorkbenchPageTests() {
    }

    /**
     * 1.12.2's recipe makers: a Tier 1 Rocket (and Tier 2, Tier 3, buggy) with none to three chests, a cargo rocket with
     * one to three, one Astro Miner; every slot of a page filled.
     */
    @GameTest(template = AIR)
    public static void benchPagesListEachChestCount(GameTestHelper helper) {
        final List<Workbenches.Recipe> rocket = Workbenches.recipes(Workbenches.ROCKET_T1);
        helper.assertValueEqual(rocket.size(), 4, "Tier 1: 0-3 chests");
        for (int chests = 0; chests < 4; chests++) {
            final Workbenches.Recipe recipe = rocket.get(chests);
            helper.assertTrue(recipe.result().is(GCItems.TIER_1_ROCKET.get()), "a Tier 1 Rocket");
            helper.assertValueEqual(RocketItem.storage(recipe.result()), chests, "with " + chests + " chests");
            helper.assertValueEqual((int) recipe.parts().stream().filter(p -> !p.items().isEmpty()).count(), 14 + chests, "parts shown");
        }
        helper.assertValueEqual(Workbenches.recipes(Workbenches.MOON_BUGGY).size(), 4, "buggy: 0-3 boxes");
        helper.assertValueEqual(Workbenches.recipes(Workbenches.ROCKET_T2).size(), 4, "Tier 2");
        helper.assertValueEqual(Workbenches.recipes(Workbenches.ROCKET_T3).size(), 4, "Tier 3");
        helper.assertValueEqual(Workbenches.recipes(Workbenches.CARGO_ROCKET).size(), 3, "cargo rocket: 1-3 chests");
        final List<Workbenches.Recipe> miner = Workbenches.recipes(Workbenches.ASTRO_MINER);
        helper.assertValueEqual(miner.size(), 1, "one Astro Miner");
        helper.assertTrue(miner.get(0).result().is(GCItems.ASTRO_MINER.get()), "an Astro Miner");
        helper.assertTrue(miner.get(0).parts().stream().noneMatch(p -> p.items().isEmpty()), "every slot filled");
        for (Workbenches.Bench bench : Workbenches.ALL) {
            final int[] area = bench.area();
            for (Workbenches.Recipe recipe : Workbenches.recipes(bench)) {
                for (Workbenches.Part part : recipe.parts()) {
                    helper.assertTrue(part.y() >= area[1] && part.y() + 16 <= area[1] + area[3], bench.id() + ": every slot inside the page");
                }
            }
        }
        helper.succeed();
    }

    /** The pages read the menus' own layouts: the menus still have their part, result and inventory slots. */
    @GameTest(template = AIR)
    public static void menusKeepTheirSlots(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final int[] parts = new int[1];
        final PartBenchMenu.Layout count = new PartBenchMenu.Layout() {
            @Override
            public void result(int x, int y) {
            }

            @Override
            public void part(int index, int x, int y) {
                parts[0]++;
            }
        };
        NasaWorkbenchMenu.layout(count);
        helper.assertValueEqual(parts[0], 17, "Tier 1: 17 part slots");
        helper.assertValueEqual(new NasaWorkbenchMenu(0, player.getInventory()).slots.size(), 1 + 17 + 36, "Tier 1 menu");
        parts[0] = 0;
        AstroMinerBenchMenu.layout(count);
        helper.assertValueEqual(new AstroMinerBenchMenu(0, player.getInventory()).slots.size(), 1 + parts[0] + 36, "Astro Miner menu");
        helper.succeed();
    }

    /** 1.12.2's Oxygen Compressor, Gas Liquefier and Methane Synthesizer pages. */
    @GameTest(template = AIR)
    public static void machinePages(GameTestHelper helper) {
        helper.assertValueEqual(MachinePages.conversions(MachinePages.OXYGEN_COMPRESSING).size(), 3, "three tanks");
        helper.assertValueEqual(MachinePages.conversions(MachinePages.GAS_LIQUEFYING).size(), 5, "five liquefier recipes");
        helper.assertValueEqual(MachinePages.conversions(MachinePages.METHANE_SYNTHESIZING).size(), 2, "air or carbon");
        for (MachinePages.Page page : MachinePages.ALL) {
            for (MachinePages.Conversion conversion : MachinePages.conversions(page)) {
                helper.assertFalse(conversion.input().isEmpty() || conversion.output().isEmpty(), page.id() + ": something in and out");
            }
        }
        helper.assertValueEqual(io.github.reiko45.galacticraft.item.gear.OxygenTankItem.oxygen(
                MachinePages.conversions(MachinePages.OXYGEN_COMPRESSING).get(0).input()), 0, "an empty tank in");
        helper.succeed();
    }
}
