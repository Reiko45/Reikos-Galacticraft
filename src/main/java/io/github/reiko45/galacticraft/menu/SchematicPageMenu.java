package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

/** A NASA Workbench page; tells the client whether the Back and Next buttons lead anywhere. */
public abstract class SchematicPageMenu extends AbstractContainerMenu {
    private final Schematics.Page page;
    private final ContainerLevelAccess access;
    private final DataSlot neighbours = DataSlot.standalone();

    protected SchematicPageMenu(@Nullable MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access, Schematics.Page page) {
        super(type, id);
        this.page = page;
        this.access = access;
        if (!inventory.player.level().isClientSide) {
            this.neighbours.set(Schematics.neighbours(inventory.player, page));
        }
        addDataSlot(this.neighbours);
    }

    /** After unlocking a page the buttons may lead somewhere new. */
    public void refreshNeighbours(Player player) {
        this.neighbours.set(Schematics.neighbours(player, this.page));
    }

    public Schematics.Page page() {
        return this.page;
    }

    public ContainerLevelAccess access() {
        return this.access;
    }

    public boolean hasPrevious() {
        return (this.neighbours.get() & 1) != 0;
    }

    public boolean hasNext() {
        return (this.neighbours.get() & 2) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, GCBlocks.NASA_WORKBENCH.get());
    }
}
