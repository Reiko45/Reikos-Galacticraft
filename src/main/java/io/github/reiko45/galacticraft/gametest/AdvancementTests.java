package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCTriggers;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 8: the 28 1.12.2 advancements and their three Galacticraft criteria. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdvancementTests {
    private static final String AIR = "air_room";
    private static final String[] NAMES = {"root", "coal_generator", "circuit_fabricator", "basic_wafer", "advanced_wafer", "basic_solar",
            "advanced_solar", "compressor", "electric_compressor", "oil", "refinery", "rocket_fuel", "oxygen_collector", "oxygen_compressor",
            "fill_tank", "fill_all_tanks", "oxygen_gear", "rocket_workbench", "rocket_t1", "fuel_loader", "launch_rocket", "moon",
            "create_space_station", "spin_thruster", "moon_dungeon", "key_t1", "moon_buggy_schematic", "moon_buggy"};

    private AdvancementTests() {
    }

    private static AdvancementHolder advancement(GameTestHelper helper, String name) {
        final AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(GalacticraftCore.id("galacticraft/" + name));
        helper.assertTrue(holder != null, "advancement " + name);
        return holder;
    }

    private static boolean done(GameTestHelper helper, ServerPlayer player, String name) {
        return player.getAdvancements().getOrStartProgress(advancement(helper, name)).isDone();
    }

    /** All 28, in 1.12.2's tree. */
    @GameTest(template = "empty")
    public static void advancementTree(GameTestHelper helper) {
        helper.assertValueEqual(NAMES.length, 28, "1.12.2 had 28");
        for (String name : NAMES) {
            advancement(helper, name);
        }
        helper.assertTrue(advancement(helper, "moon").value().parent().orElseThrow().equals(GalacticraftCore.id("galacticraft/launch_rocket")),
                "the Moon follows the launch");
        helper.assertTrue(advancement(helper, "root").value().display().orElseThrow().getBackground().isPresent(), "the tab has its background");
        helper.succeed();
    }

    /** Building a space station is an advancement of its own. */
    @GameTest(template = "empty")
    public static void createSpaceStationCriterion(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertFalse(done(helper, player, "create_space_station"), "not yet");
        GCTriggers.CREATE_SPACE_STATION.get().trigger(player);
        helper.assertTrue(done(helper, player, "create_space_station"), "built one");
        helper.succeed();
    }

    /** Coming within 20 blocks of a Skeleton Boss finds the Moon dungeon's boss. */
    @GameTest(template = AIR, timeoutTicks = 60)
    public static void findMoonBossCriterion(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5D, 1.0D, 2.5D)));
        final Mob boss = helper.spawn(GCEntities.SKELETON_BOSS.get(), new BlockPos(10, 1, 10));
        boss.setNoAi(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(done(helper, player, "moon_dungeon"), "found the boss");
            boss.discard();
        });
    }

    /** Riding a rocket as it lifts off. */
    @GameTest(template = AIR, timeoutTicks = 60)
    public static void launchRocketCriterion(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), 0, 0, 0, 0, Tier1RocketEntity.FUEL_CAPACITY);
        final BlockPos at = helper.absolutePos(new BlockPos(6, 1, 6));
        rocket.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(rocket);
        player.startRiding(rocket, true);
        rocket.ignite();
        final CompoundTag tag = new CompoundTag();
        rocket.saveWithoutId(tag);
        tag.putInt("Countdown", 1); // skip the 400 tick countdown
        rocket.load(tag);
        helper.succeedWhen(() -> {
            helper.assertTrue(rocket.launched(), "launched");
            helper.assertTrue(done(helper, player, "launch_rocket"), "the rider launched a rocket");
            player.stopRiding();
            rocket.discard();
        });
    }

    /** A full tank of any size fills one; it takes all three for the stockpile. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fullTankCriteria(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().add(new ItemStack(GCItems.LIGHT_OXYGEN_TANK.get())); // empty: doesn't count
        player.getInventory().add(GCItems.MEDIUM_OXYGEN_TANK.get().full());
        helper.succeedWhen(() -> {
            helper.assertTrue(done(helper, player, "fill_tank"), "a full tank");
            helper.assertFalse(done(helper, player, "fill_all_tanks"), "not all three");
        });
    }
}
