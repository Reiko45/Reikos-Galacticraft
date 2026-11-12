package io.github.reiko45.galacticraft.player;

import io.github.reiko45.galacticraft.item.gear.GearItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.registry.GCAttachments;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
    // --- Space travel (1.12.2 GCPlayerStats rocketStacks, fuelLevel, launchpadStack, coordsTeleportedFrom, ...) ---
    /** The rocket's cargo while travelling: storage slots, then the launch pads, then the rocket itself. */
    public NonNullList<ItemStack> rocketCargo = NonNullList.create();
    public int rocketFuel;
    public int rocketStorage;
    /** The tier of the rocket being travelled in (1.12.2 GCPlayerStats rocketItem): picks destinations and the rocket item. */
    public int rocketTier = 1;
    public int launchpadCount;
    public double launchedFromX;
    public double launchedFromZ;
    /** Choosing a destination: held in place at {@link #spacePos} until a world is picked. */
    public boolean inSpace;
    public net.minecraft.world.phys.Vec3 spacePos = net.minecraft.world.phys.Vec3.ZERO;
    public int launchAttempts;
    public boolean usingParachute;
    public int chestSpawnCooldown;
    /** 1.12.2 GCPlayerStats thermalLevel (-22 freezing to 22 overheating) and thermalLevelNormalising. */
    public int thermalLevel;
    public boolean thermalNormalising = true;
    /** Ticks until a Cryogenic Chamber can be used again (1.12.2 GCPlayerStats#cryogenicChamberCooldown). */
    public int cryogenicChamberCooldown;
    public net.minecraft.world.phys.Vec3 chestSpawnPos;

    // --- Footprints (1.12.2 distanceSinceLastStep / lastStep; not saved) ---
    public double lastFootX;
    public double lastFootZ;
    public double distanceSinceLastStep;
    public int lastStep;

    /** NASA Workbench pages unlocked with schematics (1.12.2 GCPlayerStats#unlockedSchematics); kept through death. */
    private final java.util.Set<String> unlockedSchematics = new java.util.LinkedHashSet<>();

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

    public boolean hasSchematic(String id) {
        return this.unlockedSchematics.contains(id);
    }

    /** @return whether it was newly unlocked */
    public boolean unlockSchematic(String id) {
        return this.unlockedSchematics.add(id);
    }

    /** Copy everything from a previous player entity (respawn, End exit). */
    public void copyFrom(GCPlayerData other, boolean keepItems) {
        this.unlockedSchematics.clear();
        this.unlockedSchematics.addAll(other.unlockedSchematics);
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
        tag.putInt("CryogenicChamberCooldown", this.cryogenicChamberCooldown);
        tag.putInt("ThermalLevel", this.thermalLevel);
        final CompoundTag travel = new CompoundTag();
        travel.putInt("CargoSize", this.rocketCargo.size());
        net.minecraft.world.ContainerHelper.saveAllItems(travel, this.rocketCargo, provider);
        travel.putInt("Fuel", this.rocketFuel);
        travel.putInt("Storage", this.rocketStorage);
        travel.putInt("Tier", this.rocketTier);
        travel.putInt("Launchpads", this.launchpadCount);
        travel.putDouble("FromX", this.launchedFromX);
        travel.putDouble("FromZ", this.launchedFromZ);
        travel.putBoolean("InSpace", this.inSpace);
        travel.putDouble("SpaceX", this.spacePos.x);
        travel.putDouble("SpaceY", this.spacePos.y);
        travel.putDouble("SpaceZ", this.spacePos.z);
        travel.putBoolean("Parachute", this.usingParachute);
        travel.putInt("ChestCooldown", this.chestSpawnCooldown);
        if (this.chestSpawnPos != null) {
            travel.putDouble("ChestX", this.chestSpawnPos.x);
            travel.putDouble("ChestY", this.chestSpawnPos.y);
            travel.putDouble("ChestZ", this.chestSpawnPos.z);
        }
        tag.put("Travel", travel);
        final net.minecraft.nbt.ListTag schematics = new net.minecraft.nbt.ListTag();
        for (String id : this.unlockedSchematics) {
            schematics.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        tag.put("Schematics", schematics);
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
        this.cryogenicChamberCooldown = tag.getInt("CryogenicChamberCooldown");
        this.thermalLevel = tag.getInt("ThermalLevel");
        final CompoundTag travel = tag.getCompound("Travel");
        this.rocketCargo = NonNullList.withSize(travel.getInt("CargoSize"), ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(travel, this.rocketCargo, provider);
        this.rocketFuel = travel.getInt("Fuel");
        this.rocketStorage = travel.getInt("Storage");
        this.rocketTier = Math.max(1, travel.getInt("Tier"));
        this.launchpadCount = travel.getInt("Launchpads");
        this.launchedFromX = travel.getDouble("FromX");
        this.launchedFromZ = travel.getDouble("FromZ");
        this.inSpace = travel.getBoolean("InSpace");
        this.spacePos = new net.minecraft.world.phys.Vec3(travel.getDouble("SpaceX"), travel.getDouble("SpaceY"), travel.getDouble("SpaceZ"));
        this.usingParachute = travel.getBoolean("Parachute");
        this.chestSpawnCooldown = travel.getInt("ChestCooldown");
        this.chestSpawnPos = travel.contains("ChestX")
                ? new net.minecraft.world.phys.Vec3(travel.getDouble("ChestX"), travel.getDouble("ChestY"), travel.getDouble("ChestZ")) : null;
        this.unlockedSchematics.clear();
        for (net.minecraft.nbt.Tag id : tag.getList("Schematics", net.minecraft.nbt.Tag.TAG_STRING)) {
            this.unlockedSchematics.add(id.getAsString());
        }
        this.gearChanged = true;
    }
}
