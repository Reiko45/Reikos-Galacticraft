package io.github.reiko45.galacticraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.station.SpaceStations;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Galacticraft commands (1.12.2 CommandSpaceStationAddOwner, CommandSpaceStationRemoveOwner,
 * CommandSpaceStationChangeOwner and CommandGCHouston):
 * <ul>
 *     <li>{@code /ssinvite <player> | +all | -all}: let a player onto your space station, or open/close it to all</li>
 *     <li>{@code /ssuninvite <player>}: take a player off your station's list</li>
 *     <li>{@code /ssnewowner <station#> <player>} (operators): hand a station to another player</li>
 *     <li>{@code /gchouston [player]}: from space, abandon the mission and go home to Earth (type it twice to confirm;
 *     operators can send other players)</li>
 * </ul>
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class GCCommands {
    /** Players who typed /gchouston once, with the game time they did it (1.12.2: 250 ticks to confirm). */
    private static final Map<UUID, Long> HOUSTON = new HashMap<>();
    private static final long HOUSTON_CONFIRM_TICKS = 250L;

    private GCCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("ssinvite")
                .then(Commands.argument("player", StringArgumentType.word()).executes(GCCommands::invite)));
        dispatcher.register(Commands.literal("ssuninvite")
                .then(Commands.argument("player", StringArgumentType.word()).executes(GCCommands::uninvite)));
        dispatcher.register(Commands.literal("ssnewowner").requires(source -> source.hasPermission(2))
                .then(Commands.argument("station", IntegerArgumentType.integer(0))
                        .then(Commands.argument("player", EntityArgument.player()).executes(GCCommands::newOwner))));
        dispatcher.register(Commands.literal("gchouston").executes(context -> houston(context, null))
                .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                        .executes(context -> houston(context, EntityArgument.getPlayer(context, "player")))));
    }

    private static CommandSyntaxException fail(Component message) {
        return new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(message).create();
    }

    private static SpaceStations.Station ownStation(ServerPlayer player) throws CommandSyntaxException {
        final SpaceStations.Station station = SpaceStations.get(player.server).ownedBy(player.getUUID());
        if (station == null) {
            throw fail(Component.translatable("commands.galacticraftcore.ssinvite.not_found"));
        }
        return station;
    }

    private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final SpaceStations stations = SpaceStations.get(player.server);
        final SpaceStations.Station station = ownStation(player);
        final String name = StringArgumentType.getString(context, "player");
        if (name.equalsIgnoreCase("+all")) {
            stations.setAllowAll(station, true);
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.spacestation.allow_all_true"));
            return 1;
        }
        if (name.equalsIgnoreCase("-all")) {
            stations.setAllowAll(station, false);
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.spacestation.allow_all_false"));
            return 1;
        }
        stations.allow(station, name);
        final ServerPlayer invited = player.server.getPlayerList().getPlayerByName(name);
        if (invited != null) {
            invited.sendSystemMessage(Component.translatable("gui.galacticraftcore.spacestation.added", player.getGameProfile().getName()));
        }
        player.sendSystemMessage(Component.translatable("gui.galacticraftcore.spacestation.addsuccess", name));
        return 1;
    }

    private static int uninvite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final SpaceStations stations = SpaceStations.get(player.server);
        final SpaceStations.Station station = ownStation(player);
        final String name = StringArgumentType.getString(context, "player");
        if (!stations.disallow(station, name)) {
            throw fail(Component.translatable("commands.galacticraftcore.ssuninvite.no_player", "\"" + name + "\""));
        }
        player.sendSystemMessage(Component.translatable("gui.galacticraftcore.spacestation.removesuccess", name));
        return 1;
    }

    private static int newOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final SpaceStations stations = SpaceStations.get(context.getSource().getServer());
        final SpaceStations.Station station = stations.byId(IntegerArgumentType.getInteger(context, "station"));
        if (station == null) {
            throw fail(Component.translatable("commands.galacticraftcore.ssnewowner.wrong_usage",
                    "/ssnewowner <station#> <player>"));
        }
        final ServerPlayer newOwner = EntityArgument.getPlayer(context, "player");
        final String oldName = station.ownerName();
        stations.setOwner(station, newOwner.getUUID(), newOwner.getGameProfile().getName());
        context.getSource().sendSuccess(() -> Component.translatable("gui.galacticraftcore.spacestation.changesuccess", oldName,
                newOwner.getGameProfile().getName()), true);
        return 1;
    }

    /** 1.12.2 CommandGCHouston: back to the bed or world spawn on Earth, with the rocket's cargo given up. */
    private static int houston(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        final ServerPlayer player;
        if (target == null) {
            player = source.getPlayerOrException();
            if (!player.getAbilities().instabuild && CelestialPhysics.of(player.level().dimension()) == null) {
                source.sendFailure(Component.translatable("commands.galacticraftcore.gchouston.fail"));
                return 0;
            }
            final long now = player.server.overworld().getGameTime();
            final Long asked = HOUSTON.remove(player.getUUID());
            if (asked == null || now - asked > HOUSTON_CONFIRM_TICKS) {
                HOUSTON.put(player.getUUID(), now);
                source.sendSystemMessage(Component.translatable("commands.galacticraftcore.gchouston.confirm.1").withStyle(ChatFormatting.YELLOW)
                        .append(" ").append(Component.translatable("commands.galacticraftcore.gchouston.confirm.2").withStyle(ChatFormatting.WHITE)));
                return 1;
            }
        } else {
            player = target;
        }
        final ServerLevel earth = player.server.overworld();
        BlockPos spawn = null;
        if (player.getRespawnDimension().equals(earth.dimension()) && player.getRespawnPosition() != null) {
            spawn = player.getRespawnPosition();
        }
        if (spawn == null) {
            spawn = earth.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, earth.getSharedSpawnPos());
        }
        final GCPlayerData data = GCPlayerData.get(player);
        data.rocketCargo = NonNullList.withSize(0, ItemStack.EMPTY);
        data.rocketFuel = 0;
        data.rocketStorage = 0;
        data.launchpadCount = 0;
        data.inSpace = false;
        data.launchedFromX = spawn.getX();
        data.launchedFromZ = spawn.getZ();
        player.setNoGravity(false);
        player.closeContainer();
        player.teleportTo(earth, spawn.getX() + 0.5D, spawn.getY() + 0.25D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        final String name = player.getGameProfile().getName();
        source.sendSuccess(() -> Component.translatable("commands.galacticraftcore.gchouston.success", name), true);
        return 1;
    }
}
