package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.CommsDishBlock;
import io.github.reiko45.galacticraft.block.DisplayScreenBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.MagneticCraftingTableBlock;
import io.github.reiko45.galacticraft.block.entity.DisplayScreenBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MagneticCraftingTableBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TelemetryBlockEntity;
import io.github.reiko45.galacticraft.item.FrequencyModuleItem;
import io.github.reiko45.galacticraft.item.SensorGlassesItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.network.DisplayNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Phase 7g part 2: sensor glasses, magnetic crafting table, compact workbench, telemetry, display screens, comms dish. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseEquipmentTests {
    private BaseEquipmentTests() {
    }

    /** The glasses: a helmet with no protection that can't be enchanted; the ores they point at are tagged. */
    @GameTest(template = "empty")
    public static void sensorGlassesAreAHelmet(GameTestHelper helper) {
        final ItemStack glasses = new ItemStack(GCItems.SENSOR_GLASSES.get());
        helper.assertTrue(glasses.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD && armor.getDefense() == 0,
                "a helmet with no defence");
        helper.assertValueEqual(glasses.getMaxDamage(), 2200, "1.12.2 durability factor 200");
        helper.assertFalse(glasses.isEnchantable(), "not enchantable");
        helper.assertTrue(GCBlocks.DESH_ORE.get().defaultBlockState().is(GCModTags.SENSOR_VALUABLE)
                && Blocks.COPPER_ORE.defaultBlockState().is(GCModTags.SENSOR_VALUABLE)
                && !Blocks.STONE.defaultBlockState().is(GCModTags.SENSOR_VALUABLE), "ores are valuable, stone isn't");
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, glasses);
        helper.assertTrue(SensorGlassesItem.isWorn(player), "worn");
        helper.succeed();
    }

    /** A sealer with too much room traces the way its air gets out, from its vent outwards, for Sensor Glasses. */
    @GameTest(template = "air_room", timeoutTicks = 500)
    public static void unsealedSealerTracesItsLeak(GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 1, 6), GCBlocks.OXYGEN_SEALER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final OxygenSealerBlockEntity sealer = helper.getBlockEntity(new BlockPos(6, 1, 6));
        sealer.energy().setStored(32_000.0F);
        sealer.setOxygenStored(10_000);
        helper.succeedWhen(() -> {
            final List<BlockPos> trace = sealer.leakTrace();
            helper.assertFalse(sealer.isSealed(), "unsealed");
            helper.assertTrue(trace.size() >= 2, "a leak trace: " + trace.size());
            helper.assertValueEqual(trace.get(0), sealer.getBlockPos().above(), "it starts at the vent");
            for (int i = 1; i < trace.size(); i++) {
                helper.assertValueEqual(trace.get(i).distManhattan(trace.get(i - 1)), 1, "each step one block on");
            }
        });
    }

    /**
     * The magnetic table remembers the last recipe crafted; then hoppers may only put that recipe's items into the
     * slots it wants them in, and taking the result crafts it, keeping the rest of the batch for the next take.
     */
    @GameTest(template = "empty")
    public static void magneticTableRemembersAndAutoCrafts(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GCBlocks.MAGNETIC_CRAFTING_TABLE.get().defaultBlockState().setValue(MagneticCraftingTableBlock.FACING, Direction.UP));
        final MagneticCraftingTableBlockEntity table = helper.getBlockEntity(pos);
        // two planks, one above the other: four sticks
        table.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        table.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(table.result().is(Items.STICK) && table.result().getCount() == 4, "sticks from planks");
        table.updateMemory(table.result());
        helper.assertTrue(table.memoryResult().is(Items.STICK), "it remembers sticks");
        table.matrix().clearContent();
        helper.assertTrue(table.canPlaceItemThroughFace(1, new ItemStack(Items.OAK_PLANKS), Direction.UP), "planks go where the recipe wants them");
        helper.assertFalse(table.canPlaceItemThroughFace(0, new ItemStack(Items.OAK_PLANKS), Direction.UP), "but not elsewhere");
        helper.assertFalse(table.canPlaceItemThroughFace(1, new ItemStack(Items.COBBLESTONE), Direction.UP), "nor other items");
        final IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(handler != null, "an item handler for hoppers");
        int inserted = 0;
        for (int slot = 0; slot < 9; slot++) {
            if (handler.insertItem(slot, new ItemStack(Items.OAK_PLANKS), false).isEmpty()) {
                inserted++;
            }
        }
        helper.assertValueEqual(inserted, 2, "a plank into each slot of the recipe, and no more on this pass");
        helper.assertTrue(table.matrix().getItem(1).getCount() == 1 && table.matrix().getItem(4).getCount() == 1, "one in each of the two slots");
        helper.assertTrue(table.canTakeItemThroughFace(9, ItemStack.EMPTY, Direction.DOWN), "the result can be taken");
        final ItemStack taken = handler.extractItem(9, 1, false);
        helper.assertTrue(taken.is(Items.STICK) && taken.getCount() == 1, "one stick out");
        helper.assertTrue(table.matrix().getItem(1).isEmpty() && table.matrix().getItem(4).isEmpty(), "the planks were used");
        helper.assertTrue(table.getItem(9).is(Items.STICK) && table.getItem(9).getCount() == 3, "three sticks kept for later");
        helper.succeed();
    }

    /** The compact workbench crafts from the big one and back, and seals. */
    @GameTest(template = "empty")
    public static void compactWorkbenchConverts(GameTestHelper helper) {
        final var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(GalacticraftCore.id("compact_nasa_workbench")).isPresent(), "NASA Workbench to compact");
        helper.assertTrue(recipes.byKey(GalacticraftCore.id("nasa_workbench_2")).isPresent(), "and back");
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.COMPACT_NASA_WORKBENCH.get());
        helper.assertTrue(io.github.reiko45.galacticraft.oxygen.OxygenUtil.canBlockPassAir(helper.getLevel(), helper.getBlockState(new BlockPos(1, 1, 1)),
                helper.absolutePos(new BlockPos(1, 1, 1)), Direction.UP) == false, "air doesn't pass it");
        helper.succeed();
    }

    /** A Telemetry Unit linked to a pig reports its type and health; a worn, linked frequency module links its wearer. */
    @GameTest(template = "empty")
    public static void telemetryReportsItsLink(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GCBlocks.TELEMETRY.get());
        final TelemetryBlockEntity telemetry = helper.getBlockEntity(pos);
        DisplayNetwork.TelemetryData data = telemetry.snapshot();
        helper.assertValueEqual(data.name(), "", "nothing linked yet");
        final Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        final ItemStack module = new ItemStack(GCItems.FREQUENCY_MODULE.get());
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        module.getItem().onLeftClickEntity(module, player, pig);
        helper.assertValueEqual(FrequencyModuleItem.linkedEntity(module), pig.getUUID(), "the module tracks the pig");
        telemetry.addTrackedEntity(FrequencyModuleItem.linkedEntity(module));
        data = telemetry.snapshot();
        helper.assertValueEqual(data.name(), "minecraft:pig", "a pig");
        helper.assertValueEqual(data.data1(), 100, "at full health");
        helper.assertValueEqual(data.data2(), 40, "a resting pulse (400 / 10)");
        FrequencyModuleItem.setTelemetry(module, helper.getLevel(), helper.absolutePos(pos));
        GCPlayerData.get(player).setGear(GearSlot.FREQUENCY_MODULE, module);
        TelemetryBlockEntity.frequencyModuleTick(player);
        helper.assertValueEqual(telemetry.linked(), player.getUUID(), "wearing it links the wearer");
        GCPlayerData.get(player).setGear(GearSlot.FREQUENCY_MODULE, ItemStack.EMPTY);
        TelemetryBlockEntity.frequencyModuleTick(player);
        helper.assertTrue(telemetry.linked() == null, "taking it off unlinks them");
        helper.succeed();
    }

    /**
     * Four screens facing north: clicking joins the bottom pair, then the top pair, then the two pairs, into one 2 x 2
     * screen drawn from its top left block; breaking one block breaks the big screen up.
     */
    @GameTest(template = "empty")
    public static void displayScreensJoin(GameTestHelper helper) {
        final BlockPos a = new BlockPos(1, 1, 2);
        final BlockPos b = new BlockPos(2, 1, 2);
        final BlockPos c = new BlockPos(1, 2, 2);
        final BlockPos d = new BlockPos(2, 2, 2);
        for (BlockPos pos : List.of(a, b, c, d)) {
            helper.setBlock(pos, GCBlocks.DISPLAY_SCREEN.get().defaultBlockState().setValue(DisplayScreenBlock.FACING, Direction.NORTH));
        }
        final DisplayScreenBlockEntity bottomLeft = helper.getBlockEntity(a);
        final DisplayScreenBlockEntity topLeft = helper.getBlockEntity(c);
        bottomLeft.changeChannel();
        helper.assertTrue(bottomLeft.isMultiscreen && bottomLeft.width() == 2 && bottomLeft.height() == 1, "the bottom pair joined");
        helper.assertValueEqual(bottomLeft.imageType, 1, "showing picture 1");
        topLeft.changeChannel();
        helper.assertTrue(topLeft.isMultiscreen && topLeft.width() == 2, "the top pair joined");
        bottomLeft.changeChannel();
        helper.assertTrue(bottomLeft.width() == 2 && bottomLeft.height() == 2, "one 2 x 2 screen: " + bottomLeft.width() + " x " + bottomLeft.height());
        helper.assertTrue(topLeft.drawsScreen() && !bottomLeft.drawsScreen(), "drawn from the top left");
        helper.assertValueEqual(((DisplayScreenBlockEntity) helper.getBlockEntity(d)).imageType, bottomLeft.imageType, "all show the same picture");
        helper.assertTrue(helper.getBlockState(a).getValue(DisplayScreenBlock.UP) && helper.getBlockState(a).getValue(DisplayScreenBlock.RIGHT)
                && !helper.getBlockState(a).getValue(DisplayScreenBlock.LEFT), "the frame edges follow the joins");
        helper.destroyBlock(d);
        helper.assertTrue(bottomLeft.width() * bottomLeft.height() < 4, "no longer 2 x 2");
        helper.succeed();
    }

    /** The dish fills 132 parts above it; breaking any part takes the whole dish down. */
    @GameTest(template = "air_room")
    public static void commsDishBuildsAndBreaks(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 0, 6);
        helper.setBlock(pos, GCBlocks.COMMS_DISH.get());
        CommsDishBlock.placeParts(helper.getLevel(), helper.absolutePos(pos));
        int parts = 0;
        for (BlockPos part : CommsDishBlock.partPositions(helper.absolutePos(pos))) {
            if (helper.getLevel().getBlockState(part).is(GCBlocks.COMMS_DISH_PART.get())) {
                parts++;
            }
        }
        helper.assertValueEqual(parts, 132, "parts");
        helper.destroyBlock(new BlockPos(6, 7, 3));
        helper.assertBlockNotPresent(GCBlocks.COMMS_DISH.get(), pos);
        helper.assertBlockNotPresent(GCBlocks.COMMS_DISH_PART.get(), new BlockPos(6, 1, 6));
        helper.succeed();
    }
}
