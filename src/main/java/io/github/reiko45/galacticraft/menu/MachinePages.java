/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.jei.oxygencompressor and the planets'
 * gasliquefier and methanesynth JEI recipe makers). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The machines 1.12.2 gave a JEI page of what goes in and what comes out (its recipe makers' lists): the Oxygen
 * Compressor filling tanks, the Gas Liquefier and the Methane Synthesizer.
 */
public final class MachinePages {
    public record Page(String id, Supplier<? extends Block> machine) {
    }

    public record Conversion(Page page, ItemStack input, ItemStack output) {
    }

    public static final Page OXYGEN_COMPRESSING = new Page("oxygen_compressing", GCBlocks.OXYGEN_COMPRESSOR);
    public static final Page GAS_LIQUEFYING = new Page("gas_liquefying", GCBlocks.GAS_LIQUEFIER);
    public static final Page METHANE_SYNTHESIZING = new Page("methane_synthesizing", GCBlocks.METHANE_SYNTHESIZER);
    public static final List<Page> ALL = List.of(OXYGEN_COMPRESSING, GAS_LIQUEFYING, METHANE_SYNTHESIZING);

    private MachinePages() {
    }

    public static List<Conversion> conversions(Page page) {
        if (page == OXYGEN_COMPRESSING) {
            // OxygenCompressorRecipeMaker: every oxygen tank, empty to full
            return List.of(GCItems.LIGHT_OXYGEN_TANK, GCItems.MEDIUM_OXYGEN_TANK, GCItems.HEAVY_OXYGEN_TANK).stream()
                    .map(tank -> new Conversion(page, empty(tank.get()), tank.get().full())).toList();
        }
        if (page == GAS_LIQUEFYING) {
            return List.of(
                    new Conversion(page, LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE), LiquidCanisterItem.full(LiquidCanisterItem.Kind.FUEL)),
                    new Conversion(page, new ItemStack(GCItems.ATMOSPHERIC_VALVE.get()), LiquidCanisterItem.full(LiquidCanisterItem.Kind.LIQUID_NITROGEN)),
                    new Conversion(page, new ItemStack(GCItems.ATMOSPHERIC_VALVE.get()), LiquidCanisterItem.full(LiquidCanisterItem.Kind.LIQUID_OXYGEN)),
                    new Conversion(page, half(LiquidCanisterItem.Kind.LIQUID_NITROGEN), LiquidCanisterItem.full(LiquidCanisterItem.Kind.LIQUID_NITROGEN)),
                    new Conversion(page, half(LiquidCanisterItem.Kind.LIQUID_OXYGEN), LiquidCanisterItem.full(LiquidCanisterItem.Kind.LIQUID_OXYGEN)));
        }
        return List.of(
                new Conversion(page, new ItemStack(GCItems.ATMOSPHERIC_VALVE.get()), LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE)),
                new Conversion(page, new ItemStack(GCItems.CARBON_FRAGMENTS.get(), 25), LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE)));
    }

    private static ItemStack empty(OxygenTankItem tank) {
        final ItemStack stack = new ItemStack(tank);
        OxygenTankItem.setOxygen(stack, 0);
        return stack;
    }

    /** 1.12.2's half-full canister (damage 501). */
    private static ItemStack half(LiquidCanisterItem.Kind kind) {
        final FluidStack full = LiquidCanisterItem.contents(LiquidCanisterItem.full(kind));
        return LiquidCanisterItem.filled(kind.item(), full.copyWithAmount(full.getAmount() / 2));
    }
}
