package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.DescribedItem;
import io.github.reiko45.galacticraft.item.DungeonKeyItem;
import io.github.reiko45.galacticraft.item.SchematicItem;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.Tier1RocketItem;
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

    // --- Mars (1.12.2 item_basic_mars) ---
    public static final DeferredItem<Item> RAW_DESH = simple("raw_desh");
    public static final DeferredItem<Item> DESH_INGOT = simple("desh_ingot");
    public static final DeferredItem<Item> DESH_STICK = simple("desh_stick");
    public static final DeferredItem<Item> COMPRESSED_DESH = simple("compressed_desh");
    /** 1.12.2 reinforced_plate_t2 ("Heavy-Duty Plate"), the Tier 2 Rocket's hull. */
    public static final DeferredItem<Item> HEAVY_DUTY_PLATE = simple("heavy_duty_plate");

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

    // --- Spawn eggs (1.12.2 GCCoreUtil#registerGalacticraftCreature colours) ---
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EVOLVED_ZOMBIE_SPAWN_EGG = egg("evolved_zombie", GCEntities.EVOLVED_ZOMBIE, 44975, 7969893);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EVOLVED_SKELETON_SPAWN_EGG = egg("evolved_skeleton", GCEntities.EVOLVED_SKELETON, 12698049, 4802889);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EVOLVED_SPIDER_SPAWN_EGG = egg("evolved_spider", GCEntities.EVOLVED_SPIDER, 3419431, 11013646);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EVOLVED_CREEPER_SPAWN_EGG = egg("evolved_creeper", GCEntities.EVOLVED_CREEPER, 894731, 0);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EVOLVED_ENDERMAN_SPAWN_EGG = egg("evolved_enderman", GCEntities.EVOLVED_ENDERMAN, 1447446, 0);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> SKELETON_BOSS_SPAWN_EGG = egg("skeleton_boss", GCEntities.SKELETON_BOSS, 12698049, 4802889);

    // --- Dungeon loot (1.12.2 ItemKey, ItemSchematic) ---
    public static final DeferredItem<DungeonKeyItem> TIER_1_DUNGEON_KEY = ITEMS.register("tier_1_dungeon_key",
            () -> new DungeonKeyItem(new Item.Properties(), 1));
    public static final DeferredItem<SchematicItem> BUGGY_SCHEMATIC = ITEMS.register("buggy_schematic",
            () -> new SchematicItem(new Item.Properties(), "moon_buggy"));
    /** Found in Moon dungeons; its workbench page arrives with the Tier 2 Rocket (Mars phase). */
    public static final DeferredItem<SchematicItem> TIER_2_ROCKET_SCHEMATIC = ITEMS.register("tier_2_rocket_schematic",
            () -> new SchematicItem(new Item.Properties(), "rocket_t2"));

    // --- Vehicles ---
    public static final DeferredItem<io.github.reiko45.galacticraft.item.BuggyItem> BUGGY = ITEMS.register("buggy",
            () -> new io.github.reiko45.galacticraft.item.BuggyItem(new Item.Properties()));

    // --- Rockets ---
    public static final DeferredItem<Tier1RocketItem> TIER_1_ROCKET = ITEMS.register("tier_1_rocket",
            () -> new Tier1RocketItem(new Item.Properties()));
    /** 1.12.2 ItemTier2Rocket (rocket_t2 types 0-3); it stands 0.2 lower on the pad (getOnPadYOffset). */
    public static final DeferredItem<io.github.reiko45.galacticraft.item.RocketItem> TIER_2_ROCKET = ITEMS.register("tier_2_rocket",
            () -> new io.github.reiko45.galacticraft.item.RocketItem(new Item.Properties(),
                    io.github.reiko45.galacticraft.entity.Tier2RocketEntity.FUEL_CAPACITY, -0.2D,
                    io.github.reiko45.galacticraft.entity.Tier2RocketEntity::create));

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

    // --- Desh tools and armor (1.12.2 ItemPickaxeMars & co.; attack values from their 1.12.2 ItemTool subclasses) ---
    public static final DeferredItem<PickaxeItem> DESH_PICKAXE = ITEMS.register("desh_pickaxe",
            () -> new PickaxeItem(GCTiers.DESH, new Item.Properties().attributes(PickaxeItem.createAttributes(GCTiers.DESH, 1.0F, -2.8F))));
    public static final DeferredItem<io.github.reiko45.galacticraft.item.StickyPickaxeItem> STICKY_DESH_PICKAXE = ITEMS.register("sticky_desh_pickaxe",
            () -> new io.github.reiko45.galacticraft.item.StickyPickaxeItem(GCTiers.DESH,
                    new Item.Properties().attributes(PickaxeItem.createAttributes(GCTiers.DESH, 1.0F, -2.8F))));
    /** 1.12.2 ItemAxeMars set attack damage 6 and speed -3 directly. */
    public static final DeferredItem<AxeItem> DESH_AXE = ITEMS.register("desh_axe",
            () -> new AxeItem(GCTiers.DESH, new Item.Properties().attributes(AxeItem.createAttributes(GCTiers.DESH, 3.5F, -3.0F))));
    public static final DeferredItem<ShovelItem> DESH_SHOVEL = ITEMS.register("desh_shovel",
            () -> new ShovelItem(GCTiers.DESH, new Item.Properties().attributes(ShovelItem.createAttributes(GCTiers.DESH, 1.5F, -3.0F))));
    /** 1.12.2 ItemHoe: no damage bonus, attack speed (material damage - 3). */
    public static final DeferredItem<HoeItem> DESH_HOE = ITEMS.register("desh_hoe",
            () -> new HoeItem(GCTiers.DESH, new Item.Properties().attributes(HoeItem.createAttributes(GCTiers.DESH, -2.5F, -0.5F))));
    public static final DeferredItem<SwordItem> DESH_SWORD = ITEMS.register("desh_sword",
            () -> new SwordItem(GCTiers.DESH, new Item.Properties().attributes(SwordItem.createAttributes(GCTiers.DESH, 3, -2.4F))));

    public static final DeferredItem<io.github.reiko45.galacticraft.item.DeshArmorItem> DESH_HELMET = deshArmor("desh_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<io.github.reiko45.galacticraft.item.DeshArmorItem> DESH_CHESTPLATE = deshArmor("desh_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<io.github.reiko45.galacticraft.item.DeshArmorItem> DESH_LEGGINGS = deshArmor("desh_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<io.github.reiko45.galacticraft.item.DeshArmorItem> DESH_BOOTS = deshArmor("desh_boots", ArmorItem.Type.BOOTS);

    private static DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> egg(String name,
            java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type, int primary, int secondary) {
        return ITEMS.register(name + "_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(type, primary, secondary, new Item.Properties()));
    }

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

    /** ARMORDESH in 1.12.2 used durability factor 42. */
    private static DeferredItem<io.github.reiko45.galacticraft.item.DeshArmorItem> deshArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new io.github.reiko45.galacticraft.item.DeshArmorItem(type,
                new Item.Properties().durability(type.getDurability(42))));
    }

    /** ARMOR_STEEL in 1.12.2 used durability factor 30. */
    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(GCArmorMaterials.HEAVY_DUTY, type,
                new Item.Properties().durability(type.getDurability(30))));
    }
}
