package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.DescribedItem;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.gear.GearItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
import io.github.reiko45.galacticraft.item.WrenchItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry. The 1.12.2 {@code basic_item}, {@code food}, {@code item_basic_moon}, {@code canister},
 * {@code engine} and {@code buggymat} metadata items are split into individual items.
 *
 * <p>Many of these are crafting intermediates whose recipes depend on machines that are not
 * ported yet (compressor, circuit fabricator, NASA workbench). They are registered now so later
 * phases only need to add recipes and behaviour.
 */
public final class GCItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GalacticraftCore.MOD_ID);

    // --- Raw materials and ingots ---
    public static final DeferredItem<Item> RAW_SILICON = simple("raw_silicon");
    public static final DeferredItem<Item> TIN_INGOT = simple("tin_ingot");
    public static final DeferredItem<Item> ALUMINUM_INGOT = simple("aluminum_ingot");
    public static final DeferredItem<Item> METEORIC_IRON_RAW = simple("meteoric_iron_raw");
    public static final DeferredItem<Item> METEORIC_IRON_INGOT = simple("meteoric_iron_ingot");
    public static final DeferredItem<Item> LUNAR_SAPPHIRE = simple("lunar_sapphire");

    // --- Compressed plates (made in the Compressor, a later phase) ---
    public static final DeferredItem<Item> COMPRESSED_COPPER = simple("compressed_copper");
    public static final DeferredItem<Item> COMPRESSED_TIN = simple("compressed_tin");
    public static final DeferredItem<Item> COMPRESSED_ALUMINUM = simple("compressed_aluminum");
    public static final DeferredItem<Item> COMPRESSED_STEEL = simple("compressed_steel");
    public static final DeferredItem<Item> COMPRESSED_BRONZE = simple("compressed_bronze");
    public static final DeferredItem<Item> COMPRESSED_IRON = simple("compressed_iron");
    public static final DeferredItem<Item> COMPRESSED_METEORIC_IRON = simple("compressed_meteoric_iron");
    public static final DeferredItem<Item> HEAVY_PLATING = simple("heavy_plating");

    // --- Electronics (made in the Circuit Fabricator, a later phase) ---
    public static final DeferredItem<Item> SOLAR_WAFER = simple("solar_wafer");
    public static final DeferredItem<Item> BASIC_WAFER = simple("basic_wafer");
    public static final DeferredItem<Item> ADVANCED_WAFER = simple("advanced_wafer");
    public static final DeferredItem<Item> SINGLE_SOLAR_MODULE = simple("single_solar_module");
    public static final DeferredItem<Item> FULL_SOLAR_PANEL = simple("full_solar_panel");
    public static final DeferredItem<Item> FREQUENCY_MODULE = simple("frequency_module");
    public static final DeferredItem<Item> AMBIENT_THERMAL_CONTROLLER = simple("ambient_thermal_controller");

    // --- Rocket, oxygen and vehicle parts ---
    public static final DeferredItem<Item> OXYGEN_VENT = simple("oxygen_vent");
    public static final DeferredItem<Item> OXYGEN_FAN = simple("oxygen_fan");
    public static final DeferredItem<Item> OXYGEN_CONCENTRATOR = simple("oxygen_concentrator");
    public static final DeferredItem<Item> TIER_1_ROCKET_ENGINE = simple("tier_1_rocket_engine");
    public static final DeferredItem<Item> TIER_1_BOOSTER = simple("tier_1_booster");
    public static final DeferredItem<Item> ROCKET_FINS = simple("rocket_fins");
    public static final DeferredItem<Item> NOSE_CONE = simple("nose_cone");
    public static final DeferredItem<Item> SENSOR_LENS = simple("sensor_lens");
    public static final DeferredItem<Item> CANVAS = simple("canvas");
    public static final DeferredItem<Item> STEEL_POLE = simple("steel_pole");
    public static final DeferredItem<Item> BUGGY_WHEEL = simple("buggy_wheel");
    public static final DeferredItem<Item> BUGGY_SEAT = simple("buggy_seat");
    public static final DeferredItem<Item> BUGGY_STORAGE_BOX = simple("buggy_storage_box");
    public static final DeferredItem<Item> TIN_CANISTER = simple("tin_canister");
    public static final DeferredItem<Item> COPPER_CANISTER = simple("copper_canister");

    // --- Electrical ---
    /** 1.12.2 ItemBattery: 15,000 gJ, 200 gJ/t in and out; empty batteries stack to 4. */
    public static final DeferredItem<ElectricItem> BATTERY = ITEMS.register("battery",
            () -> new ElectricItem(new Item.Properties().stacksTo(4), 15_000.0F, 200.0F));
    /** 1.12.2 ItemUniversalWrench: 256 uses, rotates machines. */
    public static final DeferredItem<WrenchItem> STANDARD_WRENCH = ITEMS.register("standard_wrench",
            () -> new WrenchItem(new Item.Properties().durability(256)));

    // --- Liquid canisters (1.12.2 oil_canister_partial / fuel_canister_partial) ---
    public static final DeferredItem<LiquidCanisterItem> OIL_CANISTER = ITEMS.register("oil_canister",
            () -> new LiquidCanisterItem(new Item.Properties(), false));
    public static final DeferredItem<LiquidCanisterItem> FUEL_CANISTER = ITEMS.register("fuel_canister",
            () -> new LiquidCanisterItem(new Item.Properties(), true));

    // --- Oxygen gear (1.12.2 ItemOxygenMask, ItemOxygenGear, ItemOxygenTank, ItemCanisterOxygenInfinite, ItemParaChute) ---
    public static final DeferredItem<GearItem> OXYGEN_MASK = ITEMS.register("oxygen_mask",
            () -> new GearItem(new Item.Properties(), GearSlot.MASK));
    public static final DeferredItem<GearItem> OXYGEN_GEAR = ITEMS.register("oxygen_gear",
            () -> new GearItem(new Item.Properties(), GearSlot.GEAR));
    public static final DeferredItem<OxygenTankItem> LIGHT_OXYGEN_TANK = ITEMS.register("light_oxygen_tank",
            () -> new OxygenTankItem(new Item.Properties(), 1));
    public static final DeferredItem<OxygenTankItem> MEDIUM_OXYGEN_TANK = ITEMS.register("medium_oxygen_tank",
            () -> new OxygenTankItem(new Item.Properties(), 2));
    public static final DeferredItem<OxygenTankItem> HEAVY_OXYGEN_TANK = ITEMS.register("heavy_oxygen_tank",
            () -> new OxygenTankItem(new Item.Properties(), 3));
    public static final DeferredItem<InfiniteOxygenTankItem> INFINITE_OXYGEN = ITEMS.register("infinite_oxygen",
            () -> new InfiniteOxygenTankItem(new Item.Properties()));
    public static final List<DeferredItem<ParachuteItem>> PARACHUTES = parachutes();

    // --- Food (values from 1.12.2 ItemFood#getHealAmount / #getSaturationModifier) ---
    // Canned food returns an empty Tin Canister when eaten, like 1.12.2 did. TIN_CANISTER must be registered first.
    public static final DeferredItem<Item> DEHYDRATED_APPLE = food("dehydrated_apple", 8, 0.3F, true);
    public static final DeferredItem<Item> DEHYDRATED_CARROT = food("dehydrated_carrot", 8, 0.6F, true);
    public static final DeferredItem<Item> DEHYDRATED_MELON = food("dehydrated_melon", 4, 0.3F, true);
    public static final DeferredItem<Item> DEHYDRATED_POTATO = food("dehydrated_potato", 2, 0.3F, true);
    public static final DeferredItem<Item> CANNED_BEEF = food("canned_beef", 8, 0.6F, true);
    public static final DeferredItem<Item> CHEESE_CURD = food("cheese_curd", 1, 0.1F, false);
    public static final DeferredItem<Item> CHEESE_SLICE = food("cheese_slice", 2, 0.1F, false);
    public static final DeferredItem<Item> BURGER_BUN = food("burger_bun", 4, 0.8F, false);
    public static final DeferredItem<Item> RAW_BEEF_PATTY = food("raw_beef_patty", 2, 0.3F, false);
    public static final DeferredItem<Item> COOKED_BEEF_PATTY = food("cooked_beef_patty", 4, 0.6F, false);
    public static final DeferredItem<DescribedItem> CHEESEBURGER = ITEMS.register("cheeseburger", () -> new DescribedItem(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(14).saturationModifier(1.0F).build())));

    // --- Heavy Duty (steel) tools and armor ---
    public static final DeferredItem<PickaxeItem> HEAVY_DUTY_PICKAXE = ITEMS.register("heavy_duty_pickaxe",
            () -> new PickaxeItem(GCTiers.HEAVY_DUTY, new Item.Properties().attributes(PickaxeItem.createAttributes(GCTiers.HEAVY_DUTY, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> HEAVY_DUTY_AXE = ITEMS.register("heavy_duty_axe",
            () -> new AxeItem(GCTiers.HEAVY_DUTY, new Item.Properties().attributes(AxeItem.createAttributes(GCTiers.HEAVY_DUTY, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> HEAVY_DUTY_SHOVEL = ITEMS.register("heavy_duty_shovel",
            () -> new ShovelItem(GCTiers.HEAVY_DUTY, new Item.Properties().attributes(ShovelItem.createAttributes(GCTiers.HEAVY_DUTY, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> HEAVY_DUTY_HOE = ITEMS.register("heavy_duty_hoe",
            () -> new HoeItem(GCTiers.HEAVY_DUTY, new Item.Properties().attributes(HoeItem.createAttributes(GCTiers.HEAVY_DUTY, -2.0F, -1.0F))));
    public static final DeferredItem<SwordItem> HEAVY_DUTY_SWORD = ITEMS.register("heavy_duty_sword",
            () -> new SwordItem(GCTiers.HEAVY_DUTY, new Item.Properties().attributes(SwordItem.createAttributes(GCTiers.HEAVY_DUTY, 3, -2.4F))));

    public static final DeferredItem<ArmorItem> HEAVY_DUTY_HELMET = armor("heavy_duty_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> HEAVY_DUTY_CHESTPLATE = armor("heavy_duty_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> HEAVY_DUTY_LEGGINGS = armor("heavy_duty_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> HEAVY_DUTY_BOOTS = armor("heavy_duty_boots", ArmorItem.Type.BOOTS);

    private static List<DeferredItem<ParachuteItem>> parachutes() {
        final List<DeferredItem<ParachuteItem>> list = new java.util.ArrayList<>();
        for (int color = 0; color < ParachuteItem.IDS.length; color++) {
            final int c = color;
            list.add(ITEMS.register(ParachuteItem.IDS[color], () -> new ParachuteItem(new Item.Properties(), c)));
        }
        return List.copyOf(list);
    }

    private GCItems() {
    }

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.registerSimpleItem(name);
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation, boolean canned) {
        return ITEMS.register(name, () -> {
            final FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
            if (canned) {
                food.usingConvertsTo(TIN_CANISTER.get());
            }
            return new Item(new Item.Properties().food(food.build()));
        });
    }

    /** ARMOR_STEEL in 1.12.2 used durability factor 30. */
    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(GCArmorMaterials.HEAVY_DUTY, type,
                new Item.Properties().durability(type.getDurability(30))));
    }
}
