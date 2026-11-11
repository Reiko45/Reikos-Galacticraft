package io.github.reiko45.galacticraft.menu;

import com.mojang.datafixers.util.Pair;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Galacticraft inventory tab (1.12.2 ContainerExtendedInventory): armour, the player inventory and the 11 gear
 * slots (mask, gear, two tanks, parachute, frequency module, four thermal padding slots, shield controller) at the
 * 1.12.2 positions.
 */
public class ExtendedInventoryMenu extends AbstractContainerMenu {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};
    /** 1.12.2 slot positions, indexed by GearSlot. */
    private static final int[][] GEAR_POSITIONS = {{125, 26}, {125, 44}, {116, 62}, {134, 62}, {143, 26}, {107, 26},
            {79, 8}, {79, 26}, {79, 44}, {79, 62}, {125, 8}};
    private static final int PLAYER_START = 0;
    private static final int ARMOR_START = 36;
    private static final int GEAR_START = 40;
    private static final int END = GEAR_START + GearSlot.COUNT;

    public ExtendedInventoryMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new ItemStackHandler(GearSlot.COUNT));
    }

    public ExtendedInventoryMenu(int id, Inventory playerInventory, IItemHandler gear) {
        super(GCMenus.EXTENDED_INVENTORY.get(), id);
        final Player player = playerInventory.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new ArmorSlot(playerInventory, player, ARMOR[i], 39 - i, 61, 8 + i * 18, ARMOR_ICONS[i]));
        }
        for (GearSlot slot : GearSlot.values()) {
            final int[] pos = GEAR_POSITIONS[slot.index()];
            addSlot(new SlotItemHandler(gear, slot.index(), pos[0], pos[1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GCPlayerData.accepts(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
    }

    /** Server: open the tab for {@code player}. */
    public static void open(Player player) {
        final GCPlayerData data = GCPlayerData.get(player);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ExtendedInventoryMenu(id, inventory, data.inventory()),
                Component.translatable("container.inventory")));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index >= ARMOR_START) {
            if (!moveItemStackTo(stack, PLAYER_START, ARMOR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = ARMOR_START; i < END && !stack.isEmpty(); i++) {
                if (this.slots.get(i).mayPlace(stack) && !this.slots.get(i).hasItem() && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                if (index < 27) {
                    if (!moveItemStackTo(stack, 27, ARMOR_START, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
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
        return true;
    }

    /** Armour slot like vanilla's inventory: only items for that body part, no taking off cursed items. */
    private static final class ArmorSlot extends Slot {
        private final Player owner;
        private final EquipmentSlot equipmentSlot;
        private final ResourceLocation icon;

        ArmorSlot(Container container, Player owner, EquipmentSlot equipmentSlot, int index, int x, int y, ResourceLocation icon) {
            super(container, index, x, y);
            this.owner = owner;
            this.equipmentSlot = equipmentSlot;
            this.icon = icon;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(this.equipmentSlot, this.owner);
        }

        @Override
        public boolean mayPickup(Player player) {
            final ItemStack stack = getItem();
            return (stack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
                    && super.mayPickup(player);
        }

        @Nullable
        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, this.icon);
        }
    }
}
