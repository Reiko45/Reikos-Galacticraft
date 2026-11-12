package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.item.SchematicItem;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * NASA Workbench pages (1.12.2 SchematicRegistry and ISchematicPage). The Tier 1 Rocket and "Add New Schematic" pages
 * are always there; others are unlocked per player by putting their schematic into the "Add New Schematic" page. Pages
 * are ordered like the 1.12.2 page ids: rocket (0), Moon Buggy (1), ..., Add New Schematic last.
 */
public final class Schematics {
    /** One page: its id (also the {@link SchematicItem#schematic()} that unlocks it), position and menu. */
    public record Page(String id, int order, @Nullable Supplier<? extends Item> unlockedBy, String title, Factory factory) {
        public Component titleComponent() {
            return Component.translatable(this.title);
        }
    }

    @FunctionalInterface
    public interface Factory {
        AbstractContainerMenu create(int id, Inventory inventory, ContainerLevelAccess access);
    }

    public static final Page ROCKET_T1 = new Page("rocket_t1", 0, null, "schematic.galacticraftcore.rocket_t1", NasaWorkbenchMenu::new);
    public static final Page MOON_BUGGY = new Page("moon_buggy", 1, GCItems.BUGGY_SCHEMATIC, "schematic.galacticraftcore.moon_buggy", BuggyBenchMenu::new);
    /** 1.12.2 SchematicTier2Rocket (planets config id 2). */
    public static final Page ROCKET_T2 = new Page("rocket_t2", 2, GCItems.TIER_2_ROCKET_SCHEMATIC, "schematic.galacticraftcore.rocket_t2",
            Tier2RocketBenchMenu::new);
    /** 1.12.2 SchematicCargoRocket (planets config id 3). */
    public static final Page CARGO_ROCKET = new Page("cargo_rocket", 3, GCItems.CARGO_ROCKET_SCHEMATIC, "schematic.galacticraftcore.cargo_rocket",
            CargoRocketBenchMenu::new);
    /** 1.12.2 SchematicTier3Rocket (asteroids config id 4). */
    public static final Page ROCKET_T3 = new Page("rocket_t3", 4, GCItems.TIER_3_ROCKET_SCHEMATIC, "schematic.galacticraftcore.rocket_t3",
            Tier3RocketBenchMenu::new);
    public static final Page ADD_SCHEMATIC = new Page("add_schematic", Integer.MAX_VALUE, null, "gui.galacticraftcore.add_new_schematic",
            SchematicInputMenu::new);
    private static final List<Page> PAGES = List.of(ROCKET_T1, MOON_BUGGY, ROCKET_T2, CARGO_ROCKET, ROCKET_T3, ADD_SCHEMATIC).stream()
            .sorted(Comparator.comparingInt(Page::order)).toList();

    public static final int ACTION_BACK = 0;
    public static final int ACTION_NEXT = 1;
    public static final int ACTION_UNLOCK = 2;

    private Schematics() {
    }

    /** The pages this player can see, in order. */
    public static List<Page> unlocked(Player player) {
        final GCPlayerData data = GCPlayerData.get(player);
        final List<Page> pages = new ArrayList<>();
        for (Page page : PAGES) {
            if (page.unlockedBy() == null || data.hasSchematic(page.id())) {
                pages.add(page);
            }
        }
        return pages;
    }

    /** Bit 0: there is a page before this one; bit 1: there is one after it. */
    public static int neighbours(Player player, Page page) {
        final List<Page> pages = unlocked(player);
        final int index = pages.indexOf(page);
        return (index > 0 ? 1 : 0) | (index >= 0 && index < pages.size() - 1 ? 2 : 0);
    }

    /** The page a schematic item unlocks, if that page exists yet. */
    @Nullable
    public static Page forItem(ItemStack stack) {
        if (stack.getItem() instanceof SchematicItem schematic) {
            for (Page page : PAGES) {
                if (page.id().equals(schematic.schematic())) {
                    return page;
                }
            }
        }
        return null;
    }

    public static void open(ServerPlayer player, Page page, ContainerLevelAccess access) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> page.factory().create(id, inventory, access), page.titleComponent()));
    }

    public static void open(ServerPlayer player, Page page, net.minecraft.world.level.Level level, BlockPos pos) {
        open(player, page, ContainerLevelAccess.create(level, pos));
    }

    /** A Back, Next or Unlock Schematic button (1.12.2 S_OPEN_SCHEMATIC_PAGE, S_UNLOCK_NEW_SCHEMATIC). */
    public static void action(ServerPlayer player, int action) {
        if (!(player.containerMenu instanceof SchematicPageMenu menu) || !menu.stillValid(player)) {
            return;
        }
        if (action == ACTION_UNLOCK) {
            if (menu instanceof SchematicInputMenu input) {
                unlock(player, input.schematicStack());
                input.refreshNeighbours(player);
                input.broadcastChanges();
            }
            return;
        }
        final List<Page> pages = unlocked(player);
        final int index = pages.indexOf(menu.page()) + (action == ACTION_NEXT ? 1 : -1);
        if (index >= 0 && index < pages.size()) {
            open(player, pages.get(index), menu.access());
        }
    }

    /**
     * 1.12.2 SchematicRegistry#unlockNewPage: unlocks the schematic's page (using it up) unless already unlocked, and
     * for the player's Space Race teammates (SpaceRaceManager#teamUnlockSchematic).
     */
    public static boolean unlock(Player player, ItemStack stack) {
        final Page page = forItem(stack);
        if (page == null || page.unlockedBy() == null || !GCPlayerData.get(player).unlockSchematic(page.id())) {
            return false;
        }
        stack.shrink(1);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            io.github.reiko45.galacticraft.race.SpaceRaces.get(serverPlayer.server).teamUnlock(serverPlayer, page.id());
        }
        return true;
    }
}
