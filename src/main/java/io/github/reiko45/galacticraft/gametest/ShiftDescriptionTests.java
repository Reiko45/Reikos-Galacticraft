package io.github.reiko45.galacticraft.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.item.DungeonLocatorItem;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 10 part 1: the "more info" texts, machines' power draw, the Infinite Battery and the Dungeon Locator. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShiftDescriptionTests {
    private static final String AIR = "air_room";
    /** Texts several blocks share (ShiftDescribed). */
    private static final Set<String> SHARED = Set.of("enclosed", "space_glass", "space_glass.clear", "space_glass.strong", "slimeling_egg",
            "lighting_panel");

    private ShiftDescriptionTests() {
    }

    /** Every {@code .description} text in the English file belongs to a block or item there is (or is a shared one). */
    @GameTest(template = AIR)
    public static void descriptionsBelongToSomething(GameTestHelper helper) throws Exception {
        final JsonObject lang;
        try (var stream = ShiftDescriptionTests.class.getResourceAsStream("/assets/galacticraftcore/lang/en_us.json")) {
            lang = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        int count = 0;
        for (String key : lang.keySet()) {
            if (!key.endsWith(".description") || !(key.startsWith("block.galacticraftcore.") || key.startsWith("item.galacticraftcore."))) {
                continue;
            }
            final String name = key.substring(key.indexOf("galacticraftcore.") + "galacticraftcore.".length(), key.length() - ".description".length());
            final boolean exists = key.startsWith("block.") ? BuiltInRegistries.BLOCK.containsKey(GalacticraftCore.id(name))
                    : BuiltInRegistries.ITEM.containsKey(GalacticraftCore.id(name));
            helper.assertTrue(exists || SHARED.contains(name), key + " has nothing to describe");
            count++;
        }
        helper.assertTrue(count >= 75, "1.12.2 had about 80 of them; found " + count);
        helper.succeed();
    }

    /** 1.12.2 showed "Power required" from a machine's own use: 20 gJ/t for the fabricator, none for a generator. */
    @GameTest(template = AIR)
    public static void machinesReportTheirDraw(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.CIRCUIT_FABRICATOR.get());
        helper.setBlock(new BlockPos(3, 1, 1), GCBlocks.COAL_GENERATOR.get());
        helper.setBlock(new BlockPos(5, 1, 1), GCBlocks.FUEL_LOADER.get());
        final CircuitFabricatorBlockEntity fabricator = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final MachineBlockEntity generator = helper.getBlockEntity(new BlockPos(3, 1, 1));
        final MachineBlockEntity loader = helper.getBlockEntity(new BlockPos(5, 1, 1));
        helper.assertValueEqual(fabricator.powerDraw(), 20.0F, "fabricator");
        helper.assertValueEqual(generator.powerDraw(), 0.0F, "a generator draws nothing");
        helper.assertValueEqual(loader.powerDraw(), 30.0F, "fuel loader");
        helper.succeed();
    }

    /**
     * In a machine's battery slot the Infinite Battery gives all the machine takes (the fabricator's 50 gJ a tick), as
     * tier 3 power, and never runs down.
     */
    @GameTest(template = AIR)
    public static void infiniteBatteryFillsAMachine(GameTestHelper helper) {
        final ItemStack battery = new ItemStack(GCItems.INFINITE_BATTERY.get());
        final ElectricItem electric = (ElectricItem) battery.getItem();
        helper.assertValueEqual(electric.discharge(battery, 12_345.0F, false), 12_345.0F, "gives whatever is asked");
        helper.assertValueEqual(electric.charge(battery, 100.0F, false), 0.0F, "takes nothing");
        helper.assertValueEqual(electric.tier(), 3, "tier 3");
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.CIRCUIT_FABRICATOR.get());
        final CircuitFabricatorBlockEntity fabricator = helper.getBlockEntity(new BlockPos(1, 1, 1));
        fabricator.inventory().setStackInSlot(0, battery.copy());
        helper.succeedWhen(() -> {
            helper.assertTrue(fabricator.energy().getStoredGJ() >= 1000.0F, "charging at its full rate");
            helper.assertValueEqual(fabricator.energy().poweredTier(), 3, "tier 3 power");
            helper.assertTrue(fabricator.inventory().getStackInSlot(0).is(GCItems.INFINITE_BATTERY.get()), "the battery stays");
        });
    }

    /**
     * 1.12.2 directionToNearestDungeon: ten blocks east of a Moon dungeon's corner the locator points west (270); ten
     * north of it, south (180); the nearest of the dungeons round about, not a farther one.
     */
    @GameTest(template = AIR)
    public static void dungeonLocatorPointsAtTheNearest(GameTestHelper helper) {
        final StructureSet set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceKey.create(Registries.STRUCTURE_SET, GalacticraftCore.id("moon_dungeons")));
        helper.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement, "the Moon's dungeon set");
        final RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
        final long seed = 12345L;
        final ChunkPos dungeon = placement.getPotentialStructureChunk(seed, 0, 0);
        final double cornerX = dungeon.getMinBlockX() + 2;
        final double cornerZ = dungeon.getMinBlockZ() + 2;
        helper.assertTrue(Math.abs(DungeonLocatorItem.bearing(placement, seed, cornerX + 10.0D, cornerZ) - 270.0F) < 0.01F, "west");
        helper.assertTrue(Math.abs(DungeonLocatorItem.bearing(placement, seed, cornerX, cornerZ - 10.0D) - 180.0F) < 0.01F, "south");
        helper.assertTrue(Math.abs(DungeonLocatorItem.bearing(placement, seed, cornerX - 10.0D, cornerZ) - 90.0F) < 0.01F, "east");
        helper.assertTrue(DungeonLocatorItem.direction(helper.getLevel(), 0.0D, 0.0D) == null, "the Overworld has none");
        helper.assertTrue(DungeonLocatorItem.hasDungeons(io.github.reiko45.galacticraft.world.GCDimensions.ASTEROIDS),
                "the asteroids' abandoned bases count");
        helper.succeed();
    }
}
