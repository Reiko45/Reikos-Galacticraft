package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.ThermalPaddingItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.player.GearData;
import io.github.reiko45.galacticraft.player.Thermal;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7d: body temperature and Thermal Padding (1.12.2 GCPlayerHandler#checkThermalStatus). */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThermalTests {
    private static final float ASTEROIDS = -1.5F;

    private ThermalTests() {
    }

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // New players are invulnerable for 60 ticks; these never tick
        net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
        return player;
    }

    /** Ticks {@code from} to {@code to} (as tickCount) on a Galacticraft world with this modifier. */
    private static void run(ServerPlayer player, GCPlayerData data, float modifier, int from, int to) {
        for (int t = from; t <= to; t++) {
            player.tickCount = t;
            Thermal.check(player, data, true, modifier);
        }
    }

    private static void wearPadding(GCPlayerData data) {
        data.setGear(GearSlot.THERMAL_HELMET, new ItemStack(GCItems.THERMAL_PADDING_HELMET.get()));
        data.setGear(GearSlot.THERMAL_CHESTPLATE, new ItemStack(GCItems.THERMAL_PADDING_CHESTPLATE.get()));
        data.setGear(GearSlot.THERMAL_LEGGINGS, new ItemStack(GCItems.THERMAL_PADDING_LEGGINGS.get()));
        data.setGear(GearSlot.THERMAL_BOOTS, new ItemStack(GCItems.THERMAL_PADDING_BOOTS.get()));
    }

    /**
     * Unpadded among the asteroids (-1.5): one step colder every floor(floor(200 / 1.5) / 1.5) = 89 ticks; Mars (-1) every
     * 200; on the Moon (0) the level comes back 2 a tick, off Galacticraft worlds 3.
     */
    @GameTest(template = "empty")
    public static void unpaddedPlayersFreeze(GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final GCPlayerData data = GCPlayerData.get(player);
        run(player, data, ASTEROIDS, 1, 890);
        helper.assertValueEqual(data.thermalLevel, -10, "ten steps in 890 ticks among the asteroids");
        helper.assertFalse(data.thermalNormalising, "nothing to bring it back");
        data.thermalLevel = 0;
        run(player, data, -1.0F, 1, 401);
        helper.assertValueEqual(data.thermalLevel, -3, "three steps in 401 ticks on Mars");
        run(player, data, 0.0F, 402, 402);
        helper.assertValueEqual(data.thermalLevel, -1, "2 back a tick on the Moon");
        player.tickCount = 403;
        Thermal.check(player, data, false, 0.0F);
        helper.assertValueEqual(data.thermalLevel, 0, "and 3 a tick at home");
        helper.succeed();
    }

    /** At -22 it hurts (1.5, through armour) every 134 ticks; below -15 the player is slowed; creative players are spared. */
    @GameTest(template = "empty")
    public static void freezingHurtsAndSlows(GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final GCPlayerData data = GCPlayerData.get(player);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        data.thermalLevel = -Thermal.LIMIT;
        final float health = player.getHealth();
        run(player, data, ASTEROIDS, 135, 135);
        helper.assertValueEqual(player.getHealth(), health - Thermal.DAMAGE, "1.5 thermal damage despite the armour");
        helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "slowed");
        helper.assertValueEqual(data.thermalLevel, -Thermal.LIMIT, "no colder than -22");
        // creative (the mock player always says it is) or off Galacticraft worlds
        Thermal.check(player, data);
        helper.assertValueEqual(data.thermalLevel, -19, "creative or at home: back 3 a tick");
        helper.succeed();
    }

    /**
     * A full set of padding (strength 1) among the asteroids: the cold now creeps in every 446 ticks while the padding
     * pulls back every 100, so the level stays at 0 and the player is safe. Three pieces don't count as a set.
     */
    @GameTest(template = "empty")
    public static void paddingKeepsPlayersWarm(GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final GCPlayerData data = GCPlayerData.get(player);
        wearPadding(data);
        run(player, data, ASTEROIDS, 1, 3000);
        helper.assertTrue(data.thermalNormalising, "normalising");
        helper.assertTrue(data.thermalLevel >= -1, "kept warm: " + data.thermalLevel);
        data.thermalLevel = -12;
        run(player, data, ASTEROIDS, 3001, 3400);
        helper.assertTrue(data.thermalLevel > -12, "warming back up: " + data.thermalLevel);
        data.setGear(GearSlot.THERMAL_BOOTS, ItemStack.EMPTY);
        data.thermalLevel = 0;
        run(player, data, ASTEROIDS, 1, 3000);
        helper.assertFalse(data.thermalNormalising, "three pieces don't normalise");
        // four steps of cooldown (134 x 4 = 536) / 1.5 = 357 ticks a step
        helper.assertValueEqual(data.thermalLevel, -9, "but slow the cold down");
        helper.succeed();
    }

    /** The padding items go in their own gear slots, are equipped by use, and show on the player. */
    @GameTest(template = "empty")
    public static void paddingIsGear(GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final GCPlayerData data = GCPlayerData.get(player);
        helper.assertTrue(GCPlayerData.accepts(GearSlot.THERMAL_HELMET, new ItemStack(GCItems.THERMAL_PADDING_HELMET.get())), "helmet slot");
        helper.assertFalse(GCPlayerData.accepts(GearSlot.THERMAL_HELMET, new ItemStack(GCItems.THERMAL_PADDING_BOOTS.get())), "not the boots");
        helper.assertValueEqual(((ThermalPaddingItem) GCItems.THERMAL_PADDING_CHESTPLATE.get()).strength(), 1, "tier 1 strength");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(GCItems.THERMAL_PADDING_LEGGINGS.get()));
        GCItems.THERMAL_PADDING_LEGGINGS.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(data.gear(GearSlot.THERMAL_LEGGINGS).is(GCItems.THERMAL_PADDING_LEGGINGS.get()), "equipped by use");
        helper.assertValueEqual(GearData.of(data).thermalPadding(), 0b0100, "shown: leggings bit");
        helper.succeed();
    }
}
