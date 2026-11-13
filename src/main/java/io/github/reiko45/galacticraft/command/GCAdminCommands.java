/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.command.CommandGCKit, CommandGCInv,
 * CommandPlanetTeleport, CommandGCAstroMiner, CommandGCEnergyUnits and CommandGCHelp). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The rest of 1.12.2's commands:
 * <ul>
 *     <li>{@code /gckit [player]} (operators): an Emergency Kit</li>
 *     <li>{@code /gcinv save|restore|drop|clear <player>} (operators): put a player's Galacticraft inventory away and
 *     give it back (for map makers), drop it or clear it</li>
 *     <li>{@code /dimensiontp [player]} (operators): straight into space, every world open, as if from a Tier 1 Rocket</li>
 *     <li>{@code /gcastrominer show|reset|set<number> [player]} (operators): a player's count of Astro Miners out</li>
 *     <li>{@code /gcenergyunits gJ|FE}: the energy unit your client shows (1.12.2 also offered RF, MJ, EU and J)</li>
 *     <li>{@code /gchelp}: where to find help</li>
 * </ul>
 * 1.12.2's {@code /gckeeploaded} kept a world loaded when nobody was in it; 1.21 never unloads a world, so it has no
 * work left to do and is not ported.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class GCAdminCommands {
    /** 1.12.2 CommandGCHelp's link. */
    public static final String WIKI = "https://wiki.galacticraft.com/wiki";

    private GCAdminCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("gckit").requires(source -> source.hasPermission(2))
                .executes(context -> kit(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> kit(context, EntityArgument.getPlayer(context, "player")))));
        dispatcher.register(Commands.literal("gcinv").requires(source -> source.hasPermission(2))
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"save", "restore", "drop", "clear"}, builder))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.GameProfileArgument.gameProfile())
                                .executes(GCAdminCommands::inventory))));
        dispatcher.register(Commands.literal("dimensiontp").requires(source -> source.hasPermission(2))
                .executes(context -> toSpace(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> toSpace(context, EntityArgument.getPlayer(context, "player")))));
        dispatcher.register(Commands.literal("gcastrominer").requires(source -> source.hasPermission(2))
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"show", "reset", "set1"}, builder))
                        .executes(context -> astroMiners(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> astroMiners(context, EntityArgument.getPlayer(context, "player"))))));
        dispatcher.register(Commands.literal("gcenergyunits")
                .executes(context -> {
                    throw fail(Component.translatable("commands.galacticraftcore.gcenergyunits.no_units", "/gcenergyunits [gJ|FE]"));
                })
                .then(Commands.argument("units", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"gJ", "FE"}, builder))
                        .executes(GCAdminCommands::energyUnits)));
        dispatcher.register(Commands.literal("gchelp").executes(GCAdminCommands::help));
    }

    private static CommandSyntaxException fail(Component message) {
        return new SimpleCommandExceptionType(message).create();
    }

    private static int kit(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(GCItems.EMERGENCY_KIT.get()));
        context.getSource().sendSuccess(() -> Component.translatable("commands.galacticraftcore.emergencykit", player.getDisplayName()), true);
        return 1;
    }

    private static int inventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int done = 0;
        for (com.mojang.authlib.GameProfile profile : net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles(context, "player")) {
            inventory(context, profile.getName(), context.getSource().getServer().getPlayerList().getPlayer(profile.getId()));
            done++;
        }
        return done;
    }

    /** One player's /gcinv: by name (it may be offline, for restore), and the player if online. */
    private static void inventory(CommandContext<CommandSourceStack> context, String name, @Nullable ServerPlayer player) throws CommandSyntaxException {
        final String action = StringArgumentType.getString(context, "action").toLowerCase(Locale.ROOT);
        final GCInvData data = GCInvData.get(context.getSource().getServer());
        switch (action) {
            case "save", "drop", "clear" -> {
                if (player == null) {
                    throw fail(Component.translatable("commands.galacticraftcore.gcinv.not_found", name));
                }
                if (action.equals("save")) {
                    data.save(player);
                } else {
                    final ItemStackHandler inventory = GCPlayerData.get(player).inventory();
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        if (action.equals("drop") && !inventory.getStackInSlot(i).isEmpty()) {
                            player.drop(inventory.getStackInSlot(i), true, false);
                        }
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                    GCPlayerData.get(player).markGearChanged();
                }
            }
            case "restore" -> {
                if (!data.restore(name, player)) {
                    throw fail(Component.translatable("commands.galacticraftcore.gcinv.nothing_saved", name));
                }
            }
            default -> throw fail(Component.translatable("commands.galacticraftcore.gcinv.usage"));
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.galacticraftcore.gcinv." + action, name), true);
    }

    private static int toSpace(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        SpaceTravel.sendToSpace(player);
        context.getSource().sendSuccess(() -> Component.translatable("commands.galacticraftcore.dimensionteleport", player.getDisplayName(), ""), true);
        return 1;
    }

    /** 1.12.2 CommandGCAstroMiner: show, reset, or set{@code <n>} (a positive number). */
    private static int astroMiners(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        final String action = StringArgumentType.getString(context, "action").toLowerCase(Locale.ROOT);
        final GCPlayerData data = GCPlayerData.get(player);
        if (action.equals("reset")) {
            data.astroMinerCount = 0;
        } else if (action.startsWith("set") && action.length() > 3) {
            final int count = parse(action.substring(3));
            if (count <= 0) {
                throw fail(Component.translatable("commands.galacticraftcore.gcastrominer.usage"));
            }
            data.astroMinerCount = count;
        } else if (!action.equals("show")) {
            throw fail(Component.translatable("commands.galacticraftcore.gcastrominer.usage"));
        }
        final int count = data.astroMinerCount;
        context.getSource().sendSuccess(() -> Component.translatable("command.galacticraftcore.gcastrominer.count", player.getDisplayName(), count), false);
        return count;
    }

    private static int parse(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 1.12.2 CommandGCEnergyUnits: gJ, or RF (here FE, which it is now); the client switches its display. */
    private static int energyUnits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final GCConfig.EnergyUnit unit = units(StringArgumentType.getString(context, "units"));
        if (unit == null) {
            throw fail(Component.translatable("commands.galacticraftcore.gcenergyunits.invalid_units", "/gcenergyunits [gJ|FE]"));
        }
        GCNetwork.sendToPlayer(player, new GCNetwork.EnergyUnits(unit.ordinal()));
        return 1;
    }

    @Nullable
    public static GCConfig.EnergyUnit units(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "gj" -> GCConfig.EnergyUnit.GJ;
            case "fe", "rf" -> GCConfig.EnergyUnit.FE;
            default -> null;
        };
    }

    /** 1.12.2 CommandGCHelp: a line with the (clickable) wiki address. */
    private static int help(CommandContext<CommandSourceStack> context) {
        final Component link = Component.literal(" " + WIKI).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, WIKI))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gui.galacticraftcore.message.clicklink")
                        .withStyle(ChatFormatting.YELLOW))));
        context.getSource().sendSuccess(() -> Component.translatable("gui.galacticraftcore.message.help1").append(":").append(link), false);
        return 1;
    }
}
