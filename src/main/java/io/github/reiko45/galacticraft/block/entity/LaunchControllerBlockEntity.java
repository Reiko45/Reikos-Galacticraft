/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController and
 * EventHandlerMars#onLandingPadRemoved). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.entity.CargoRocketEntity;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.LaunchControllerMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.jetbrains.annotations.Nullable;

/**
 * Launch Controller (1.12.2 TileEntityLaunchController), 6 gJ/t: beside a launch pad it gives the pad a frequency and
 * tells a Cargo Rocket on it where to fly (the controller with the target frequency, on any world) and when to go by
 * itself: when its cargo is unloaded or full, when fully fuelled, at once, after 10 s / 30 s / 1 min, or on a redstone
 * signal. While powered it can also stop rockets taking the pad with them. It keeps its own chunk loaded (1.12.2
 * launchControllerChunkLoad), so rockets can find it. Only its owner can change it.
 */
public class LaunchControllerBlockEntity extends MachineBlockEntity {
    public static final float ENERGY_USE = 6.0F;
    public static final int MAX_FREQUENCY = 999_999;
    public static final TicketController TICKETS = new TicketController(GalacticraftCore.id("launch_controller"), (level, helper) -> {
        // Forget chunks whose controller has gone (broken while its chunk was unloaded), and all of them once
        // launchControllerChunkLoad is off
        final boolean chunkLoading = io.github.reiko45.galacticraft.GCConfig.server(io.github.reiko45.galacticraft.GCConfig.LAUNCH_CONTROLLER_CHUNK_LOAD);
        for (BlockPos owner : Set.copyOf(helper.getBlockTickets().keySet())) {
            if (!chunkLoading || !(level.getBlockEntity(owner) instanceof LaunchControllerBlockEntity)) {
                helper.removeAllTickets(owner);
            }
        }
    });
    /** Controllers loaded on the server, on every world (1.12.2 searched every world's loaded tile entities). */
    private static final Set<LaunchControllerBlockEntity> LOADED = Collections.newSetFromMap(new IdentityHashMap<>());

    /** 1.12.2 EnumAutoLaunch, in its dropdown's order. */
    public enum AutoLaunch {
        CARGO_IS_UNLOADED("cargo_unloaded"), CARGO_IS_FULL("cargo_full"), ROCKET_IS_FUELED("fully_fueled"), INSTANT("instant"),
        TIME_10_SECONDS("ten_sec"), TIME_30_SECONDS("thirty_sec"), TIME_1_MINUTE("one_min"), REDSTONE_SIGNAL("redstone_sig");

        private final String key;

        AutoLaunch(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "gui.galacticraftcore.auto_launch." + this.key;
        }
    }

    private int frequency = -1;
    private int destFrequency = -1;
    private boolean frequencyValid;
    private boolean destFrequencyValid;
    private AutoLaunch launchSelection = AutoLaunch.CARGO_IS_UNLOADED;
    private boolean launchSchedulingEnabled;
    private boolean padRemovalDisabled = true;
    private boolean hideDestination = true;
    private boolean disabled;
    private int disableCooldown;
    private boolean hasEnoughEnergyToRun;
    private boolean controlEnabled;
    @Nullable
    private UUID ownerId;
    private String ownerName = "";
    private int ticks;
    private boolean chunkForced;

    public LaunchControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.LAUNCH_CONTROLLER.get(), pos, state, 1, 16_000.0F, 2.5F * ENERGY_USE, 0.0F);
    }

    public static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    // ------------------------------------------------------------------ lookups

    /** The loaded controller (on any world) with this frequency, or null. */
    @Nullable
    public static LaunchControllerBlockEntity withFrequency(int frequency) {
        if (frequency < 0) {
            return null;
        }
        for (LaunchControllerBlockEntity controller : LOADED) {
            if (!controller.isRemoved() && controller.frequency == frequency) {
                return controller;
            }
        }
        return null;
    }

    /** The launch controller beside the joined launch pad centred at {@code centre} (1.12.2 the pad's connected tiles). */
    @Nullable
    public static LaunchControllerBlockEntity attachedTo(Level level, BlockPos centre) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 0, 2))) {
            if (level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller && centre.equals(PadVehicles.findPad(level, pos))) {
                return controller;
            }
        }
        return null;
    }

    /** 1.12.2 onLandingPadRemoved: a powered, enabled controller set to keep the pad stops a launch taking it. */
    public static boolean keepsPad(Level level, BlockPos centre) {
        final LaunchControllerBlockEntity controller = attachedTo(level, centre);
        return controller != null && controller.energy.getStoredGJ() > 0.0F && controller.padRemovalDisabled && !controller.disabled;
    }

    /** 1.12.2 setTarget: the launch pad within 2 blocks of this controller (its centre), or null. */
    @Nullable
    public BlockPos landingPad() {
        if (this.level == null) {
            return null;
        }
        for (BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-2, 0, -2), this.worldPosition.offset(2, 0, 2))) {
            final BlockPos centre = LandingPadBlock.findLaunchPadCentre(this.level, pos);
            if (centre != null) {
                return centre.immutable();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            LOADED.add(this);
            // 1.12.2 ConfigManagerMars#launchControllerChunkLoad: it keeps its chunk loaded unless that is turned off
            forceChunk(serverLevel, io.github.reiko45.galacticraft.GCConfig.server(io.github.reiko45.galacticraft.GCConfig.LAUNCH_CONTROLLER_CHUNK_LOAD));
        }
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        LOADED.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    public void onBroken() {
        LOADED.remove(this);
        if (this.level instanceof ServerLevel serverLevel) {
            forceChunk(serverLevel, false);
        }
        super.onBroken();
    }

    private void forceChunk(ServerLevel level, boolean add) {
        if (add != this.chunkForced) {
            TICKETS.forceChunk(level, this.worldPosition, this.worldPosition.getX() >> 4, this.worldPosition.getZ() >> 4, add, true);
            this.chunkForced = add;
        }
    }

    @Override
    protected void tick() {
        this.ticks++;
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        dischargeBattery(0);
        this.hasEnoughEnergyToRun = this.energy.getStoredGJ() > ENERGY_USE;
        if (this.hasEnoughEnergyToRun && !this.disabled) {
            this.energy.consume(ENERGY_USE);
        } else if (this.ticks % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
        this.controlEnabled = this.launchSchedulingEnabled && this.hasEnoughEnergyToRun && !this.disabled;
        if (this.ticks % 40 == 1) {
            checkFrequencies();
        }
    }

    /** 1.12.2 setFrequency / checkDestFrequencyValid: own frequency unique, target frequency in use elsewhere. */
    private void checkFrequencies() {
        this.frequencyValid = this.frequency >= 0;
        if (this.frequencyValid) {
            for (LaunchControllerBlockEntity other : LOADED) {
                if (other != this && !other.isRemoved() && other.frequency == this.frequency) {
                    this.frequencyValid = false;
                    break;
                }
            }
        }
        final LaunchControllerBlockEntity target = withFrequency(this.destFrequency);
        this.destFrequencyValid = target != null && target != this;
    }

    /** 1.12.2 validFrequency: enabled, powered and both frequencies good. */
    public boolean validFrequency() {
        checkFrequencies();
        return !this.disabled && this.hasEnoughEnergyToRun && this.frequencyValid && this.destFrequencyValid;
    }

    /** 1.12.2 GuiLaunchController#getStatus. */
    public MachineStatus status() {
        if (!this.frequencyValid) {
            return MachineStatus.INVALID_FREQUENCY;
        }
        if (this.energy.getStoredGJ() <= 0.0F) {
            return MachineStatus.NO_ENERGY;
        }
        return this.disabled ? MachineStatus.CONTROLLER_DISABLED : MachineStatus.CONTROLLER_ACTIVE;
    }

    // ------------------------------------------------------------------ settings

    public int frequency() {
        return this.frequency;
    }

    public int destFrequency() {
        return this.destFrequency;
    }

    public boolean controlEnabled() {
        return this.controlEnabled;
    }

    public AutoLaunch launchSelection() {
        return this.launchSelection;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public boolean padRemovalDisabled() {
        return this.padRemovalDisabled;
    }

    public boolean isOwner(Player player) {
        return this.ownerId == null || this.ownerId.equals(player.getUUID());
    }

    @Nullable
    public UUID ownerId() {
        return this.ownerId;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public void setOwner(Player player) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public void setFrequency(int frequency) {
        this.frequency = Math.max(-1, Math.min(MAX_FREQUENCY, frequency));
        checkFrequencies();
        setChanged();
    }

    public void setDestFrequency(int frequency) {
        final int value = Math.max(-1, Math.min(MAX_FREQUENCY, frequency));
        if (value != this.destFrequency) {
            this.destFrequency = value;
            checkFrequencies();
            updateRocketOnPad();
            setChanged();
        }
    }

    public void setLaunchSelection(AutoLaunch selection) {
        if (selection != this.launchSelection) {
            this.launchSelection = selection;
            updateRocketOnPad();
            setChanged();
        }
    }

    public void setLaunchSchedulingEnabled(boolean enabled) {
        if (enabled != this.launchSchedulingEnabled) {
            this.launchSchedulingEnabled = enabled;
            this.controlEnabled = enabled && this.hasEnoughEnergyToRun && !this.disabled;
            updateRocketOnPad();
            setChanged();
        }
    }

    public void togglePadRemoval() {
        this.padRemovalDisabled = !this.padRemovalDisabled;
        setChanged();
    }

    /** 1.12.2 setDisabled 0 / 2: 10 tick cooldown between presses. */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 10;
            setChanged();
        }
    }

    public void toggleHideDestination() {
        if (this.disableCooldown == 0) {
            this.hideDestination = !this.hideDestination;
            this.disableCooldown = 10;
            setChanged();
        }
    }

    /** 1.12.2 updateRocketOnDockSettings: the Cargo Rocket on the pad rereads the settings. */
    private void updateRocketOnPad() {
        if (this.level != null && !this.level.isClientSide) {
            final BlockPos pad = PadVehicles.findPad(this.level, this.worldPosition);
            if (PadVehicles.dockedRocket(this.level, pad) instanceof CargoRocketEntity rocket) {
                rocket.updateControllerSettings(this);
            }
        }
    }

    // ------------------------------------------------------------------ menu, saving

    /** Synced: frequency, target (ints), flags, the auto-launch choice, the button cooldown and the status. */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addInt(() -> this.frequency);
        data.addInt(() -> this.destFrequency);
        data.addShort(() -> (this.frequencyValid ? 1 : 0) | (this.destFrequencyValid ? 2 : 0) | (this.disabled ? 4 : 0)
                | (this.launchSchedulingEnabled ? 8 : 0) | (this.padRemovalDisabled ? 16 : 0) | (this.hideDestination ? 32 : 0));
        data.addShort(() -> this.launchSelection.ordinal());
        data.addShort(() -> this.disableCooldown);
        data.addShort(() -> status().ordinal());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ElectricItem;
    }

    /** 1.12.2 getElectricInputDirection: front.rotateY(), the left face. */
    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return isItemValid(slot, stack);
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new LaunchControllerMenu(id, playerInventory, this);
    }

    /** Opens the GUI; the client menu also needs the owner's name and whether this player is the owner. */
    public void openMenu(net.minecraft.server.level.ServerPlayer player) {
        player.openMenu(this, buf -> {
            buf.writeBlockPos(this.worldPosition);
            buf.writeUtf(this.ownerName);
            buf.writeBoolean(isOwner(player));
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putString("OwnerName", this.ownerName);
        tag.putInt("LaunchSelection", this.launchSelection.ordinal());
        tag.putInt("ControllerFrequency", this.frequency);
        tag.putInt("TargetFrequency", this.destFrequency);
        tag.putBoolean("LaunchPadRemovalDisabled", this.padRemovalDisabled);
        tag.putBoolean("LaunchPadSchedulingEnabled", this.launchSchedulingEnabled);
        tag.putBoolean("HideTargetDestination", this.hideDestination);
        tag.putBoolean("Disabled", this.disabled);
        tag.putBoolean("ChunkForced", this.chunkForced);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.ownerName = tag.getString("OwnerName");
        this.launchSelection = AutoLaunch.values()[Math.max(0, Math.min(AutoLaunch.values().length - 1, tag.getInt("LaunchSelection")))];
        this.frequency = tag.contains("ControllerFrequency") ? tag.getInt("ControllerFrequency") : -1;
        this.destFrequency = tag.contains("TargetFrequency") ? tag.getInt("TargetFrequency") : -1;
        this.padRemovalDisabled = !tag.contains("LaunchPadRemovalDisabled") || tag.getBoolean("LaunchPadRemovalDisabled");
        this.launchSchedulingEnabled = tag.getBoolean("LaunchPadSchedulingEnabled");
        this.hideDestination = !tag.contains("HideTargetDestination") || tag.getBoolean("HideTargetDestination");
        this.disabled = tag.getBoolean("Disabled");
        this.chunkForced = tag.getBoolean("ChunkForced");
    }
}
