package io.github.reiko45.galacticraft.player;

import io.github.reiko45.galacticraft.item.gear.GearItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.registry.GCAttachments;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Per-player Galacticraft state (the parts of 1.12.2 GCPlayerStats that phase 3 needs): the 11-slot Galacticraft
 * inventory and the breathing state. Stored as a NeoForge data attachment on the player.
 */
public class GCPlayerData implements INBTSerializable<CompoundTag> {
    private final ItemStackHandler inventory = new ItemStackHandler(GearSlot.COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            GCPlayerData.this.gearChanged = true;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(GearSlot.values()[slot], stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    /** Oxygen left in each tank (1.12.2 airRemaining/airRemaining2, raw tank units). */
    private int airRemaining;
    private int airRemaining2;
    private boolean oxygenSetupValid = true;
    private boolean lastOxygenSetupValid = true;
    private int damageCounter;
    private int incrementalDamage;
    /** Last HUD state sent to the client (not saved). */
    io.github.reiko45.galacticraft.network.GCNetwork.OxygenState lastSentState;
    /** Set when the gear changes, so the server sends the new look to everyone watching this player. */
    private boolean gearChanged = true;

    public static GCPlayerData get(Player player) {
        return player.getData(GCAttachments.PLAYER_DATA);
    }

    /** 1.12.2 OxygenUtil#isItemValidForPlayerTankInv and SlotExtendedInventory#isItemValid. */
    public static boolean accepts(GearSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case FREQUENCY_MODULE -> stack.is(GCItems.FREQUENCY_MODULE.get());
            case THERMAL_HELMET -> stack.is(GCModTags.THERMAL_HELMETS);
            case THERMAL_CHESTPLATE -> stack.is(GCModTags.THERMAL_CHESTPLATES);
            case THERMAL_LEGGINGS -> stack.is(GCModTags.THERMAL_LEGGINGS);
            case THERMAL_BOOTS -> stack.is(GCModTags.THERMAL_BOOTS);
            case SHIELD_CONTROLLER -> stack.is(GCModTags.SHIELD_CONTROLLERS);
            default -> stack.getItem() instanceof GearItem gear && gear.fits(slot);
        };
    }

    public ItemStackHandler inventory() {
        return this.inventory;
    }

    public ItemStack gear(GearSlot slot) {
        return this.inventory.getStackInSlot(slot.index());
    }

    public void setGear(GearSlot slot, ItemStack stack) {
        this.inventory.setStackInSlot(slot.index(), stack);
    }

    public int airRemaining() {
        return this.airRemaining;
    }

    public int airRemaining2() {
        return this.airRemaining2;
    }

    public void setAirRemaining(int airRemaining) {
        this.airRemaining = airRemaining;
    }

    public void setAirRemaining2(int airRemaining2) {
        this.airRemaining2 = airRemaining2;
    }

    public boolean oxygenSetupValid() {
        return this.oxygenSetupValid;
    }

    public void setOxygenSetupValid(boolean valid) {
        this.oxygenSetupValid = valid;
    }

    public boolean lastOxygenSetupValid() {
        return this.lastOxygenSetupValid;
    }

    public void setLastOxygenSetupValid(boolean valid) {
        this.lastOxygenSetupValid = valid;
    }

    public int damageCounter() {
        return this.damageCounter;
    }

    public void setDamageCounter(int damageCounter) {
        this.damageCounter = damageCounter;
    }

    public int incrementalDamage() {
        return this.incrementalDamage;
    }

    public void setIncrementalDamage(int incrementalDamage) {
        this.incrementalDamage = incrementalDamage;
    }

    /** Returns and clears the gear-changed flag. */
    public boolean takeGearChanged() {
        final boolean changed = this.gearChanged;
        this.gearChanged = false;
        return changed;
    }

    public void markGearChanged() {
        this.gearChanged = true;
    }

    /** Copy everything from a previous player entity (respawn, End exit). */
    public void copyFrom(GCPlayerData other, boolean keepItems) {
        if (keepItems) {
            for (int i = 0; i < GearSlot.COUNT; i++) {
                this.inventory.setStackInSlot(i, other.inventory.getStackInSlot(i).copy());
            }
            this.airRemaining = other.airRemaining;
            this.airRemaining2 = other.airRemaining2;
        }
        this.gearChanged = true;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.put("ExtendedInventory", this.inventory.serializeNBT(provider));
        tag.putInt("AirRemaining", this.airRemaining);
        tag.putInt("AirRemaining2", this.airRemaining2);
        tag.putInt("DamageCounter", this.damageCounter);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        final CompoundTag inventoryTag = tag.getCompound("ExtendedInventory");
        inventoryTag.remove("Size"); // always GearSlot.COUNT slots
        this.inventory.deserializeNBT(provider, inventoryTag);
        this.airRemaining = tag.getInt("AirRemaining");
        this.airRemaining2 = tag.getInt("AirRemaining2");
        this.damageCounter = tag.getInt("DamageCounter");
        this.gearChanged = true;
    }
}
