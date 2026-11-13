/*
 * Derived from Galacticraft Legacy (assets/galacticraftcore/advancements/galacticraft/*.json). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCTriggers;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * 1.12.2's 28 Galacticraft advancements, under the same names ({@code galacticraftcore:galacticraft/...}), parents,
 * icons, frames and criteria. "Full" oxygen tanks are tanks holding their whole capacity (1.12.2: the tank items at
 * damage 0); a rocket of any size counts.
 */
public class GCAdvancementProvider extends AdvancementProvider {
    public GCAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFiles) {
        super(output, registries, existingFiles, List.of(new Advancements()));
    }

    private static final class Advancements implements AdvancementProvider.AdvancementGenerator {
        private static final ResourceLocation BACKGROUND = GalacticraftCore.id("textures/gui/galacticraft_background.png");

        private Consumer<AdvancementHolder> saver;
        private ExistingFileHelper existingFiles;

        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFiles) {
            this.saver = saver;
            this.existingFiles = existingFiles;
            final AdvancementHolder root = net.minecraft.advancements.Advancement.Builder.advancement()
                    .display(new ItemStack(GCItems.TIER_1_ROCKET.get()), title("root").copy().withStyle(net.minecraft.ChatFormatting.GRAY,
                            net.minecraft.ChatFormatting.BOLD), description("root"), BACKGROUND, AdvancementType.TASK, false, false, false)
                    .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                    .save(saver, GalacticraftCore.id("galacticraft/root"), existingFiles);

            final AdvancementHolder coalGenerator = has(root, "coal_generator", "coal_generator", GCBlocks.COAL_GENERATOR.get(), AdvancementType.TASK);
            final AdvancementHolder circuitFabricator = has(coalGenerator, "circuit_fabricator", "circuit_fabricator", GCBlocks.CIRCUIT_FABRICATOR.get(),
                    AdvancementType.TASK);
            final AdvancementHolder basicWafer = has(circuitFabricator, "basic_wafer", "basic_wafer", GCItems.BASIC_WAFER.get(), AdvancementType.TASK);
            has(basicWafer, "advanced_wafer", "advanced_wafer", GCItems.ADVANCED_WAFER.get(), AdvancementType.TASK);
            final AdvancementHolder basicSolar = has(basicWafer, "basic_solar", "solar", GCBlocks.BASIC_SOLAR_PANEL.get(), AdvancementType.TASK);
            has(basicSolar, "advanced_solar", "advanced_solar", GCBlocks.ADVANCED_SOLAR_PANEL.get(), AdvancementType.TASK);
            final AdvancementHolder compressor = has(basicWafer, "compressor", "compressor", GCBlocks.COMPRESSOR.get(), AdvancementType.TASK);
            has(compressor, "electric_compressor", "electric_compressor", GCBlocks.ELECTRIC_COMPRESSOR.get(), AdvancementType.TASK);

            final AdvancementHolder oil = builder(compressor, GCFluids.OIL_BUCKET.get(), "oil", AdvancementType.TASK)
                    .addCriterion("in_oil", EnterBlockTrigger.TriggerInstance.entersBlock(GCFluids.OIL_BLOCK.get()))
                    .save(this.saver, id("oil"), this.existingFiles);
            final AdvancementHolder refinery = has(oil, "refinery", "refiery", GCBlocks.REFINERY.get(), AdvancementType.TASK);
            builder(refinery, GCFluids.FUEL_BUCKET.get(), "rocket_fuel", AdvancementType.GOAL)
                    .addCriterion("fuel_bucket", InventoryChangeTrigger.TriggerInstance.hasItems(GCFluids.FUEL_BUCKET.get()))
                    .save(this.saver, id("rocket_fuel"), this.existingFiles);

            final AdvancementHolder collector = has(compressor, "oxygen_collector", "oxygen_collector", GCBlocks.OXYGEN_COLLECTOR.get(), AdvancementType.TASK);
            final AdvancementHolder oxygenCompressor = has(collector, "oxygen_compressor", "oxygen_compressor", GCBlocks.OXYGEN_COMPRESSOR.get(),
                    AdvancementType.TASK);
            final AdvancementHolder fillTank = tanks(builder(oxygenCompressor, GCItems.MEDIUM_OXYGEN_TANK.get().full(), "fill_tank", AdvancementType.TASK))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(this.saver, id("fill_tank"), this.existingFiles);
            tanks(builder(fillTank, GCItems.HEAVY_OXYGEN_TANK.get().full(), "fill_all_tanks", AdvancementType.GOAL))
                    .requirements(AdvancementRequirements.Strategy.AND)
                    .save(this.saver, id("fill_all_tanks"), this.existingFiles);
            tanks(builder(fillTank, GCItems.OXYGEN_MASK.get(), "all_oxygen_gear", AdvancementType.GOAL))
                    .addCriterion("oxygen_mask", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.OXYGEN_MASK.get()))
                    .addCriterion("oxygen_gear", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.OXYGEN_GEAR.get()))
                    .requirements(new AdvancementRequirements(List.of(List.of("small_tank", "medium_tank", "heavy_tank"), List.of("oxygen_mask"),
                            List.of("oxygen_gear"))))
                    .save(this.saver, id("oxygen_gear"), this.existingFiles);

            final AdvancementHolder workbench = has(compressor, "rocket_workbench", "rocket_workbench", GCBlocks.NASA_WORKBENCH.get(), AdvancementType.TASK);
            final AdvancementHolder rocket = builder(workbench, GCItems.TIER_1_ROCKET.get(), "rocket_t1", AdvancementType.TASK)
                    .addCriterion("rocket_crafted", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_ROCKET.get()))
                    .save(this.saver, id("rocket_t1"), this.existingFiles);
            final AdvancementHolder fuelLoader = has(rocket, "fuel_loader", "fuel_loader", GCBlocks.FUEL_LOADER.get(), AdvancementType.TASK);
            final AdvancementHolder launch = builder(fuelLoader, Items.FLINT_AND_STEEL, "launch_rocket", AdvancementType.GOAL)
                    .addCriterion("launched_rocket", GCTriggers.LAUNCH_ROCKET.get().createCriterion(new PlayerTrigger.TriggerInstance(java.util.Optional.empty())))
                    .save(this.saver, id("launch_rocket"), this.existingFiles);
            final AdvancementHolder moon = builder(launch, GCBlocks.MOON_TURF.get(), "moon", AdvancementType.CHALLENGE)
                    .addCriterion("traveled_to_moon", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(GCDimensions.MOON))
                    .save(this.saver, id("moon"), this.existingFiles);
            final AdvancementHolder station = builder(moon, GCItems.FULL_SOLAR_PANEL.get(), "create_space_station", AdvancementType.GOAL)
                    .addCriterion("create_space_station", GCTriggers.CREATE_SPACE_STATION.get().createCriterion(
                            new PlayerTrigger.TriggerInstance(java.util.Optional.empty())))
                    .save(this.saver, id("create_space_station"), this.existingFiles);
            has(station, "spin_thruster", "spin_thruster", GCBlocks.SPIN_THRUSTER.get(), AdvancementType.TASK);
            final AdvancementHolder dungeon = builder(moon, GCBlocks.MOON_DUNGEON_BRICK.get(), "moon_dungeon", AdvancementType.GOAL)
                    .addCriterion("found_boss", GCTriggers.FIND_MOON_BOSS.get().createCriterion(new PlayerTrigger.TriggerInstance(java.util.Optional.empty())))
                    .save(this.saver, id("moon_dungeon"), this.existingFiles);
            final AdvancementHolder key = builder(dungeon, GCItems.TIER_1_DUNGEON_KEY.get(), "dungeon_key_moon", AdvancementType.CHALLENGE)
                    .addCriterion("got_key", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_DUNGEON_KEY.get()))
                    .save(this.saver, id("key_t1"), this.existingFiles);
            final AdvancementHolder schematic = builder(key, GCItems.BUGGY_SCHEMATIC.get(), "moon_buggy_schematic", AdvancementType.TASK)
                    .addCriterion("got_schematic", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BUGGY_SCHEMATIC.get()))
                    .save(this.saver, id("moon_buggy_schematic"), this.existingFiles);
            builder(schematic, GCItems.BUGGY.get(), "moon_buggy", AdvancementType.GOAL)
                    .addCriterion("got_buggy", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BUGGY.get()))
                    .save(this.saver, id("moon_buggy"), this.existingFiles);
        }

        private static ResourceLocation id(String name) {
            return GalacticraftCore.id("galacticraft/" + name);
        }

        private static Component title(String key) {
            return Component.translatable("advancements.galacticraftcore." + key + ".title");
        }

        private static Component description(String key) {
            return Component.translatable("advancements.galacticraftcore." + key + ".description");
        }

        /** 1.12.2's display: shown as a toast and in chat, never hidden. {@code key} is its lang name, which isn't always its id. */
        private static net.minecraft.advancements.Advancement.Builder builder(AdvancementHolder parent, ItemLike icon, String key, AdvancementType type) {
            return builder(parent, new ItemStack(icon), key, type);
        }

        private static net.minecraft.advancements.Advancement.Builder builder(AdvancementHolder parent, ItemStack icon, String key, AdvancementType type) {
            return net.minecraft.advancements.Advancement.Builder.advancement().parent(parent)
                    .display(icon, title(key), description(key), null, type, true, true, false);
        }

        /** The common shape: have the item in your inventory (1.12.2 inventory_changed). */
        private AdvancementHolder has(AdvancementHolder parent, String name, String key, ItemLike item, AdvancementType type) {
            return builder(parent, item, key, type)
                    .addCriterion(name, InventoryChangeTrigger.TriggerInstance.hasItems(item))
                    .save(this.saver, id(name), this.existingFiles);
        }

        /** 1.12.2 small_tank, medium_tank and heavy_tank: a full tank of each size. */
        private static net.minecraft.advancements.Advancement.Builder tanks(net.minecraft.advancements.Advancement.Builder builder) {
            return builder.addCriterion("small_tank", fullTank(GCItems.LIGHT_OXYGEN_TANK.get()))
                    .addCriterion("medium_tank", fullTank(GCItems.MEDIUM_OXYGEN_TANK.get()))
                    .addCriterion("heavy_tank", fullTank(GCItems.HEAVY_OXYGEN_TANK.get()));
        }

        private static Criterion<InventoryChangeTrigger.TriggerInstance> fullTank(OxygenTankItem tank) {
            return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tank)
                    .hasComponents(DataComponentPredicate.builder().expect(GCDataComponents.OXYGEN.get(), tank.capacity()).build()));
        }
    }
}
