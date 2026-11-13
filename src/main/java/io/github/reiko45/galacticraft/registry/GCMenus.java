package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.CargoMenu;
import io.github.reiko45.galacticraft.menu.CircuitFabricatorMenu;
import io.github.reiko45.galacticraft.menu.FuelLoaderMenu;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import io.github.reiko45.galacticraft.menu.CoalGeneratorMenu;
import io.github.reiko45.galacticraft.menu.CompressorMenu;
import io.github.reiko45.galacticraft.menu.ElectricCompressorMenu;
import io.github.reiko45.galacticraft.menu.ElectricFurnaceMenu;
import io.github.reiko45.galacticraft.menu.EnergyStorageMenu;
import io.github.reiko45.galacticraft.menu.ExtendedInventoryMenu;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.menu.RefineryMenu;
import io.github.reiko45.galacticraft.menu.SolarPanelMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GalacticraftCore.MOD_ID);

    public static final Supplier<MenuType<CoalGeneratorMenu>> COAL_GENERATOR = MENUS.register("coal_generator",
            () -> IMenuTypeExtension.create(CoalGeneratorMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.DeconstructorMenu>> DECONSTRUCTOR = MENUS.register("deconstructor",
            () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.DeconstructorMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.ChromaticApplicatorMenu>> CHROMATIC_APPLICATOR = MENUS.register("chromatic_applicator",
            () -> new MenuType<>(io.github.reiko45.galacticraft.menu.ChromaticApplicatorMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.MagneticCraftingMenu>> MAGNETIC_CRAFTING = MENUS.register("magnetic_crafting",
            () -> new MenuType<>(io.github.reiko45.galacticraft.menu.MagneticCraftingMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<EnergyStorageMenu>> ENERGY_STORAGE = MENUS.register("energy_storage",
            () -> IMenuTypeExtension.create(EnergyStorageMenu::new));
    public static final Supplier<MenuType<CompressorMenu>> COMPRESSOR = MENUS.register("compressor",
            () -> IMenuTypeExtension.create(CompressorMenu::new));
    public static final Supplier<MenuType<ElectricCompressorMenu>> ELECTRIC_COMPRESSOR = MENUS.register("electric_compressor",
            () -> IMenuTypeExtension.create(ElectricCompressorMenu::normal));
    public static final Supplier<MenuType<ElectricCompressorMenu>> ADVANCED_COMPRESSOR = MENUS.register("advanced_compressor",
            () -> IMenuTypeExtension.create(ElectricCompressorMenu::advanced));
    public static final Supplier<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE = MENUS.register("electric_furnace",
            () -> IMenuTypeExtension.create(ElectricFurnaceMenu::furnace));
    public static final Supplier<MenuType<ElectricFurnaceMenu>> ELECTRIC_ARC_FURNACE = MENUS.register("electric_arc_furnace",
            () -> IMenuTypeExtension.create(ElectricFurnaceMenu::arcFurnace));
    public static final Supplier<MenuType<CircuitFabricatorMenu>> CIRCUIT_FABRICATOR = MENUS.register("circuit_fabricator",
            () -> IMenuTypeExtension.create(CircuitFabricatorMenu::new));

    public static final Supplier<MenuType<SolarPanelMenu>> SOLAR_PANEL = MENUS.register("solar_panel",
            () -> IMenuTypeExtension.create(SolarPanelMenu::new));
    public static final Supplier<MenuType<RefineryMenu>> REFINERY = MENUS.register("refinery",
            () -> IMenuTypeExtension.create(RefineryMenu::new));

    public static final Supplier<MenuType<OxygenMachineMenu>> OXYGEN_COLLECTOR = oxygen("oxygen_collector", OxygenMachineMenu.Kind.COLLECTOR);
    public static final Supplier<MenuType<OxygenMachineMenu>> OXYGEN_COMPRESSOR = oxygen("oxygen_compressor", OxygenMachineMenu.Kind.COMPRESSOR);
    public static final Supplier<MenuType<OxygenMachineMenu>> OXYGEN_DECOMPRESSOR = oxygen("oxygen_decompressor", OxygenMachineMenu.Kind.DECOMPRESSOR);
    public static final Supplier<MenuType<OxygenMachineMenu>> OXYGEN_DISTRIBUTOR = oxygen("oxygen_distributor", OxygenMachineMenu.Kind.DISTRIBUTOR);
    public static final Supplier<MenuType<OxygenMachineMenu>> OXYGEN_SEALER = oxygen("oxygen_sealer", OxygenMachineMenu.Kind.SEALER);
    public static final Supplier<MenuType<OxygenMachineMenu>> OXYGEN_STORAGE_MODULE = oxygen("oxygen_storage_module", OxygenMachineMenu.Kind.STORAGE_MODULE);
    public static final Supplier<MenuType<ExtendedInventoryMenu>> EXTENDED_INVENTORY = MENUS.register("extended_inventory",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new ExtendedInventoryMenu(id, inventory)));

    public static final Supplier<MenuType<FuelLoaderMenu>> FUEL_LOADER = MENUS.register("fuel_loader",
            () -> IMenuTypeExtension.create(FuelLoaderMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.CargoLoaderMenu>> CARGO_LOADER = MENUS.register("cargo_loader",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.CargoLoaderMenu(true, id, inventory, buf)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.CargoLoaderMenu>> CARGO_UNLOADER = MENUS.register("cargo_unloader",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.CargoLoaderMenu(false, id, inventory, buf)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.GasMachineMenu>> GAS_LIQUEFIER = MENUS.register("gas_liquefier",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.GasMachineMenu(
                    io.github.reiko45.galacticraft.menu.GasMachineMenu.Kind.GAS_LIQUEFIER, id, inventory, buf)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.GasMachineMenu>> METHANE_SYNTHESIZER = MENUS.register("methane_synthesizer",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.GasMachineMenu(
                    io.github.reiko45.galacticraft.menu.GasMachineMenu.Kind.METHANE_SYNTHESIZER, id, inventory, buf)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.GasMachineMenu>> ELECTROLYZER = MENUS.register("electrolyzer",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.GasMachineMenu(
                    io.github.reiko45.galacticraft.menu.GasMachineMenu.Kind.ELECTROLYZER, id, inventory, buf)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.LaunchControllerMenu>> LAUNCH_CONTROLLER = MENUS.register("launch_controller",
            () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.LaunchControllerMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.AstroMinerDockMenu>> ASTRO_MINER_DOCK = MENUS.register(
            "astro_miner_dock", () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.AstroMinerDockMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.AstroMinerBenchMenu>> ASTRO_MINER_BENCH = MENUS.register(
            "astro_miner_bench", () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.AstroMinerBenchMenu(id, inventory)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.CrashedProbeMenu>> CRASHED_PROBE = MENUS.register("crashed_probe",
            () -> new MenuType<>(io.github.reiko45.galacticraft.menu.CrashedProbeMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.GeothermalGeneratorMenu>> GEOTHERMAL_GENERATOR = MENUS.register(
            "geothermal_generator", () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.GeothermalGeneratorMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.SolarArrayControllerMenu>> SOLAR_ARRAY_CONTROLLER = MENUS.register(
            "solar_array_controller", () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.SolarArrayControllerMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.ShortRangeTelepadMenu>> SHORT_RANGE_TELEPAD = MENUS.register(
            "short_range_telepad", () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.ShortRangeTelepadMenu::new));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.TerraformerMenu>> TERRAFORMER = MENUS.register("terraformer",
            () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.TerraformerMenu::new));
    public static final Supplier<MenuType<CargoMenu>> CARGO = MENUS.register("cargo",
            () -> IMenuTypeExtension.create(CargoMenu::client));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.SlimelingMenu>> SLIMELING = MENUS.register("slimeling",
            () -> IMenuTypeExtension.create(io.github.reiko45.galacticraft.menu.SlimelingMenu::client));
    public static final Supplier<MenuType<NasaWorkbenchMenu>> NASA_WORKBENCH = MENUS.register("nasa_workbench",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new NasaWorkbenchMenu(id, inventory)));

    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.Tier3RocketBenchMenu>> TIER_3_ROCKET_BENCH = MENUS.register("tier_3_rocket_bench",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.Tier3RocketBenchMenu(id, inventory)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.Tier2RocketBenchMenu>> TIER_2_ROCKET_BENCH = MENUS.register("tier_2_rocket_bench",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.Tier2RocketBenchMenu(id, inventory)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.CargoRocketBenchMenu>> CARGO_ROCKET_BENCH = MENUS.register("cargo_rocket_bench",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.CargoRocketBenchMenu(id, inventory)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.BuggyBenchMenu>> BUGGY_BENCH = MENUS.register("buggy_bench",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.BuggyBenchMenu(id, inventory)));
    public static final Supplier<MenuType<io.github.reiko45.galacticraft.menu.SchematicInputMenu>> SCHEMATIC_INPUT = MENUS.register("schematic_input",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new io.github.reiko45.galacticraft.menu.SchematicInputMenu(id, inventory)));

    private static Supplier<MenuType<OxygenMachineMenu>> oxygen(String name, OxygenMachineMenu.Kind kind) {
        return MENUS.register(name, () -> IMenuTypeExtension.create((id, inventory, buf) -> new OxygenMachineMenu(kind, id, inventory, buf)));
    }

    private GCMenus() {
    }
}
