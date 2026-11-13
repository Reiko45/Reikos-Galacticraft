package io.github.reiko45.galacticraft.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.command.GCInvData;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 9 part 2: 1.12.2's remaining commands, run through the server's command dispatcher. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CommandTests {
    private CommandTests() {
    }

    private static int run(GameTestHelper helper, ServerPlayer player, String command) throws CommandSyntaxException {
        final CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static boolean fails(GameTestHelper helper, ServerPlayer player, String command) {
        try {
            run(helper, player, command);
            return false;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    /** /gckit gives an Emergency Kit. */
    @GameTest(template = "empty")
    public static void gckitGivesAKit(GameTestHelper helper) throws CommandSyntaxException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        run(helper, player, "gckit");
        helper.assertTrue(player.getInventory().contains(new ItemStack(GCItems.EMERGENCY_KIT.get())), "a kit");
        helper.succeed();
    }

    /** /gcinv save takes the Galacticraft inventory away; restore gives it back; drop and clear empty it. */
    @GameTest(template = "empty")
    public static void gcinvSavesAndRestores(GameTestHelper helper) throws CommandSyntaxException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final GCPlayerData data = GCPlayerData.get(player);
        final String name = player.getGameProfile().getName();
        data.inventory().setStackInSlot(GearSlot.MASK.index(), new ItemStack(GCItems.OXYGEN_MASK.get()));
        // @p (the nearest player, itself): every mock player has the same name, and a name finds the first of them
        run(helper, player, "gcinv save @p");
        helper.assertTrue(data.gear(GearSlot.MASK).isEmpty(), "put away");
        helper.assertTrue(GCInvData.get(helper.getLevel().getServer()).has(name), "saved");
        GCInvData.get(helper.getLevel().getServer()).onLogin(player);
        helper.assertTrue(data.gear(GearSlot.MASK).isEmpty(), "not given back at login until restored (1.12.2 dontload)");
        run(helper, player, "gcinv restore @p");
        helper.assertTrue(data.gear(GearSlot.MASK).is(GCItems.OXYGEN_MASK.get()), "given back");
        helper.assertTrue(fails(helper, player, "gcinv restore @p"), "nothing saved any more");
        run(helper, player, "gcinv clear @p");
        helper.assertTrue(data.gear(GearSlot.MASK).isEmpty(), "cleared");
        helper.assertTrue(fails(helper, player, "gcinv fly @p"), "unknown action");
        helper.succeed();
    }

    /**
     * /dimensiontp: into space with every world open, even Venus with a Tier 1 Rocket's tier. (The GameTest server has
     * only the vanilla worlds, so the trip ends on Earth.)
     */
    @GameTest(template = "empty")
    public static void dimensiontpOpensEveryWorld(GameTestHelper helper) throws CommandSyntaxException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        run(helper, player, "dimensiontp");
        final GCPlayerData data = GCPlayerData.get(player);
        helper.assertTrue(data.inSpace && data.rocketTier == 1 && data.rocketFuel == 1000, "in space with a Tier 1 Rocket and 1,000 fuel");
        helper.assertValueEqual(SpaceTravel.selectionPayload(player).tier(), Integer.MAX_VALUE, "every world open");
        helper.assertTrue(SpaceTravel.destination(GCDimensions.VENUS).tier() <= data.selectionTier, "Venus is open");
        SpaceTravel.choose(player, net.minecraft.world.level.Level.OVERWORLD);
        helper.assertFalse(data.inSpace, "came down");
        helper.assertValueEqual(data.selectionTier, 0, "back to the rocket's tier");
        if (player.getVehicle() != null) {
            player.getVehicle().discard();
        }
        helper.succeed();
    }

    /** /gcastrominer set5, show, reset. */
    @GameTest(template = "empty")
    public static void gcastrominerCount(GameTestHelper helper) throws CommandSyntaxException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertValueEqual(run(helper, player, "gcastrominer set5"), 5, "set");
        helper.assertValueEqual(GCPlayerData.get(player).astroMinerCount, 5, "stored");
        helper.assertValueEqual(run(helper, player, "gcastrominer show"), 5, "shown");
        run(helper, player, "gcastrominer reset");
        helper.assertValueEqual(GCPlayerData.get(player).astroMinerCount, 0, "reset");
        helper.assertTrue(fails(helper, player, "gcastrominer set0") && fails(helper, player, "gcastrominer setx"), "bad numbers");
        helper.succeed();
    }

    /** /gcenergyunits takes gJ, FE or RF; /gchelp answers. */
    @GameTest(template = "empty")
    public static void energyUnitsAndHelp(GameTestHelper helper) throws CommandSyntaxException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertValueEqual(run(helper, player, "gcenergyunits gJ"), 1, "gJ");
        helper.assertValueEqual(run(helper, player, "gcenergyunits RF"), 1, "RF (now FE)");
        helper.assertTrue(fails(helper, player, "gcenergyunits EU") && fails(helper, player, "gcenergyunits"), "only gJ and FE");
        helper.assertValueEqual(run(helper, player, "gchelp"), 1, "help");
        helper.succeed();
    }
}
