/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.inventory.ContainerSlimeling). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.entity.SlimelingInventory;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Slimeling's inventory (1.12.2 ContainerSlimeling): the bag slot, the player's inventory, and the bag's 27 slots.
 * 1.12.2 added and removed the bag's slots as the bag came and went; here they are always there but only usable (and
 * drawn) while the bag is in.
 */
public class SlimelingMenu extends AbstractContainerMenu {
    public static final int BAG = 0;
    public static final int PLAYER_START = 1;
    public static final int PLAYER_END = 37;
    public static final int CARGO_START = 37;
    public static final int CARGO_END = 64;

    private final SlimelingInventory inventory;
    @Nullable
    private final SlimelingEntity slimeling;

    public SlimelingMenu(int id, Inventory playerInventory, SlimelingInventory inventory, @Nullable SlimelingEntity slimeling) {
        super(GCMenus.SLIMELING.get(), id);
        this.inventory = inventory;
        this.slimeling = slimeling;
        addSlot(new Slot(inventory, SlimelingInventory.BAG_SLOT, 9, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SlimelingInventory.isBag(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 129 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 187));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, SlimelingInventory.CARGO_START + col + row * 9, 8 + col * 18, 54 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return SlimelingMenu.this.inventory.hasBag();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return isActive();
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return isActive();
                    }
                });
            }
        }
    }

    /** Client side: {@code buf} carries the slimeling's entity id. */
    public static SlimelingMenu client(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        final var entity = playerInventory.player.level().getEntity(buf.readVarInt());
        return new SlimelingMenu(id, playerInventory, new SlimelingInventory(null), entity instanceof SlimelingEntity slimeling ? slimeling : null);
    }

    /** 1.12.2 MarsUtil#openSlimelingInventory. */
    public static void open(ServerPlayer player, SlimelingEntity slimeling) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SlimelingMenu(id, inventory, slimeling.inventory(), slimeling),
                Component.translatable("container.galacticraftcore.slimeling_inventory")), buf -> buf.writeVarInt(slimeling.getId()));
    }

    @Nullable
    public SlimelingEntity slimeling() {
        return this.slimeling;
    }

    public boolean hasBag() {
        return this.inventory.hasBag();
    }

    /** 1.12.2 transferStackInSlot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index == BAG || index >= CARGO_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (SlimelingInventory.isBag(stack) && !this.slots.get(BAG).hasItem()) {
            if (!moveItemStackTo(stack, BAG, BAG + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasBag() && moveItemStackTo(stack, CARGO_START, CARGO_END, false)) {
            // into the bag
        } else if (index < PLAYER_END - 9) {
            if (!moveItemStackTo(stack, PLAYER_END - 9, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END - 9, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.slimeling == null ? this.inventory.stillValid(player) : this.slimeling.inventory().stillValid(player);
    }
}
