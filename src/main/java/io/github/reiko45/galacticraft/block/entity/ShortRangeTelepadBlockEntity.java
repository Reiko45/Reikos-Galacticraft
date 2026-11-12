/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityShortRangeTelepad).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.ShortRangeTelepadMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.world.TelepadRegistry;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Short Range Telepad (1.12.2 TileEntityShortRangeTelepad), 50 gJ/t while on: give it a unique address and the address
 * of another telepad on the same world within 256 blocks. Living things standing on it for 7.5 seconds (150 ticks) are
 * sent there, for 2,500 gJ from each telepad. Only the owner can change the addresses.
 */
public class ShortRangeTelepadBlockEntity extends MachineBlockEntity {
    /** 1.12.2 EnumTelepadSearchResult. */
    public enum SearchResult {
        VALID, NOT_FOUND, TOO_FAR, WRONG_DIM, TARGET_DISABLED
    }

    public static final int MAX_TELEPORT_TIME = 150;
    public static final int TELEPORTER_RANGE = 256;
    public static final float ENERGY_USE_ON_TELEPORT = 2500.0F;
    /** 1.12.2 storage.setMaxExtract(50) (115 in hard mode): used each tick while enabled. */
    public static final float ENERGY_USE = 50.0F;
    public static final int MAX_ADDRESS = 999_999;

    private int address = -1;
    private boolean addressValid;
    private int targetAddress = -1;
    private SearchResult targetResult = SearchResult.NOT_FOUND;
    private int teleportTime;
    private boolean teleporting;
    private boolean disabled;
    private int disableCooldown;
    private int ticks;
    @Nullable
    private UUID ownerId;
    private String ownerName = "";

    public ShortRangeTelepadBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.SHORT_RANGE_TELEPAD.get(), pos, state, 1, 16_000.0F, 2.5F * ENERGY_USE, 0.0F);
    }

    private TelepadRegistry registry() {
        return TelepadRegistry.get(((ServerLevel) this.level).getServer());
    }

    /** The space entities stand in to be sent: the telepad's block and the one above. */
    public AABB sendingArea() {
        return new AABB(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                this.worldPosition.getX() + 1, this.worldPosition.getY() + 2, this.worldPosition.getZ() + 1);
    }

    @Override
    protected void tick() {
        this.ticks++;
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        if (this.energy.getStoredGJ() < this.energy.getCapacityGJ()) {
            dischargeBattery(0);
        }
        if (this.energy.getStoredGJ() > ENERGY_USE && !this.disabled) {
            this.energy.consume(ENERGY_USE);
        } else if (this.ticks % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
        if (this.ticks % 40 == 0) {
            setAddress(this.address);
            setTargetAddress(this.targetAddress);
        }
        if (!this.disabled && this.targetResult == SearchResult.VALID && (this.ticks % 5 == 0 || this.teleporting)) {
            final List<LivingEntity> standing = this.level.getEntitiesOfClass(LivingEntity.class, sendingArea());
            if (!standing.isEmpty() && this.energy.getStoredGJ() >= ENERGY_USE_ON_TELEPORT) {
                if (registry().get(this.targetAddress) != null) {
                    this.teleporting = true;
                }
            } else {
                this.teleporting = false;
            }
        }
        if (this.teleporting) {
            if (++this.teleportTime >= MAX_TELEPORT_TIME) {
                send();
                this.teleportTime = 0;
                this.teleporting = false;
            }
        } else {
            this.teleportTime = Math.max(this.teleportTime - 1, 0);
        }
        // Nearby clients colour the particles by these (1.12.2 sent them as networked fields)
        final int visible = (this.addressValid ? 1 : 0) | this.targetResult.ordinal() << 1 | (this.energy.getStoredGJ() >= ENERGY_USE_ON_TELEPORT ? 16 : 0)
                | (this.energy.getStoredGJ() > 0.0F ? 32 : 0) | (this.disabled ? 64 : 0) | this.teleportTime / 10 << 7;
        if (visible != this.lastVisible) {
            this.lastVisible = visible;
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private int lastVisible = -1;

    /** The countdown is over: everyone still standing here goes to the target telepad, if it can take them. */
    private void send() {
        final TelepadRegistry.Entry entry = registry().get(this.targetAddress);
        if (entry == null || !(this.level.getBlockEntity(entry.pos()) instanceof ShortRangeTelepadBlockEntity target)) {
            return;
        }
        final List<LivingEntity> standing = this.level.getEntitiesOfClass(LivingEntity.class, sendingArea());
        final int result = target.canTeleportHere();
        if (result == 0) {
            final BlockPos to = entry.pos();
            for (LivingEntity entity : standing) {
                if (entity instanceof ServerPlayer player) {
                    player.connection.teleport(to.getX() + 0.5D, to.getY() + 0.08D, to.getZ() + 0.5D, player.getYRot(), player.getXRot());
                } else {
                    entity.teleportTo(to.getX() + 0.5D, to.getY() + 0.08D, to.getZ() + 0.5D);
                }
            }
            if (!standing.isEmpty()) {
                this.energy.setStored(this.energy.getStoredGJ() - ENERGY_USE_ON_TELEPORT);
                target.energy.setStored(target.energy.getStoredGJ() - ENERGY_USE_ON_TELEPORT);
            }
        } else if (result == 2) {
            for (LivingEntity entity : standing) {
                if (entity instanceof Player player) {
                    player.sendSystemMessage(Component.translatable("gui.galacticraftcore.telepad.target_no_energy"));
                }
            }
        }
    }

    /** 1.12.2 canTeleportHere: 0 when it can, 1 for a bad own address, 2 for too little energy. */
    public int canTeleportHere() {
        setAddress(this.address);
        setTargetAddress(this.targetAddress);
        if (!this.addressValid) {
            return 1;
        }
        return this.energy.getStoredGJ() < ENERGY_USE_ON_TELEPORT ? 2 : 0;
    }

    /** 1.12.2 setAddress: an address is valid when nobody else on any world has claimed it. */
    public void setAddress(int address) {
        if (this.level == null || this.level.isClientSide()) {
            this.address = address;
            return;
        }
        final TelepadRegistry registry = registry();
        if (address != this.address) {
            registry.remove(this);
        }
        this.address = address;
        if (this.address >= 0) {
            final TelepadRegistry.Entry entry = registry.get(this.address);
            this.addressValid = entry == null || entry.dimension() == this.level.dimension() && entry.pos().equals(this.worldPosition);
        } else {
            this.addressValid = false;
        }
        registry.add(this);
        setChanged();
    }

    /** 1.12.2 setTargetAddress / updateTarget. */
    public void setTargetAddress(int address) {
        this.targetAddress = address;
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.targetResult = SearchResult.NOT_FOUND;
        if (address >= 0) {
            final TelepadRegistry.Entry entry = registry().get(address);
            if (entry != null) {
                if (entry.dimension() != this.level.dimension()) {
                    this.targetResult = SearchResult.WRONG_DIM;
                } else if (this.worldPosition.getCenter().distanceToSqr(entry.pos().getCenter()) >= (double) TELEPORTER_RANGE * TELEPORTER_RANGE) {
                    this.targetResult = SearchResult.TOO_FAR;
                } else {
                    this.targetResult = entry.enabled() ? SearchResult.VALID : SearchResult.TARGET_DISABLED;
                }
            }
        }
        setChanged();
    }

    /** 1.12.2 setDisabled: 10 ticks between presses; the registry learns the new state. */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 10;
            if (this.level != null && !this.level.isClientSide()) {
                registry().add(this);
            }
            setChanged();
        }
    }

    /** The block is going: give up the address (1.12.2 BlockShortRangeTelepad#breakBlock). */
    public void unregister() {
        if (this.level != null && !this.level.isClientSide()) {
            registry().remove(this);
        }
    }

    public int address() {
        return this.address;
    }

    public boolean addressValid() {
        return this.addressValid;
    }

    public int targetAddress() {
        return this.targetAddress;
    }

    public SearchResult targetResult() {
        return this.targetResult;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public int teleportTime() {
        return this.teleportTime;
    }

    public boolean isOwner(Player player) {
        return this.ownerId == null || this.ownerId.equals(player.getUUID());
    }

    public String ownerName() {
        return this.ownerName;
    }

    public void setOwner(Player player) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        setChanged();
    }

    /** Synced: the addresses, flags, the search result, the countdown and the button cooldown. */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addInt(() -> this.address);
        data.addInt(() -> this.targetAddress);
        data.addShort(() -> (this.addressValid ? 1 : 0) | (this.disabled ? 2 : 0));
        data.addShort(() -> this.targetResult.ordinal());
        data.addShort(() -> this.teleportTime);
        data.addShort(() -> this.disableCooldown);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ElectricItem;
    }

    /** Power comes in through the plate above the middle (see TelepadFakeBlockEntity). */
    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return EnergyAccess.NONE;
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
        return new ShortRangeTelepadMenu(id, playerInventory, this);
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this, buf -> {
            buf.writeBlockPos(this.worldPosition);
            buf.writeUtf(this.ownerName);
            buf.writeBoolean(isOwner(player));
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Address", this.address);
        tag.putInt("TargetAddress", this.targetAddress);
        tag.putBoolean("Disabled", this.disabled);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putString("OwnerName", this.ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.address = tag.contains("Address") ? tag.getInt("Address") : -1;
        this.targetAddress = tag.contains("TargetAddress") ? tag.getInt("TargetAddress") : -1;
        this.disabled = tag.getBoolean("Disabled");
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.ownerName = tag.getString("OwnerName");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("AddressValid", this.addressValid);
        tag.putInt("TargetResult", this.targetResult.ordinal());
        tag.putInt("TeleportTime", this.teleportTime);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
        this.addressValid = tag.getBoolean("AddressValid");
        this.targetResult = SearchResult.values()[Math.floorMod(tag.getInt("TargetResult"), SearchResult.values().length)];
        this.teleportTime = tag.getInt("TeleportTime");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    /**
     * 1.12.2 getParticleColor: red for a telepad that can't send (or receive), orange when short of energy, otherwise
     * blue turning green as the countdown runs. Client side.
     */
    public float[] particleColor(net.minecraft.util.RandomSource random, boolean sending) {
        final float scaled = Math.min(1.0F, this.teleportTime / (float) MAX_TELEPORT_TIME);
        final float f = random.nextFloat() * 0.6F + 0.4F;
        if (sending && this.targetResult != SearchResult.VALID || !sending && !this.addressValid) {
            return new float[]{f, f * 0.3F, f * 0.3F};
        }
        if (this.energy.getStoredGJ() < ENERGY_USE_ON_TELEPORT) {
            return new float[]{f, f * 0.6F, f * 0.3F};
        }
        return new float[]{f * 0.3F, f * (0.3F + scaled * 0.7F), f * (1.0F - scaled * 0.7F)};
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            final int loaded = this.address;
            this.address = Integer.MIN_VALUE;
            setAddress(loaded);
        }
    }
}
