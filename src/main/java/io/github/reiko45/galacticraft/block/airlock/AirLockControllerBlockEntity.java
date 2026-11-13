/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityAirLockController). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.airlock;

import io.github.reiko45.galacticraft.network.AirLockNetwork;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Air Lock Controller (1.12.2 TileEntityAirLockController). Every tick it decides whether the lock should be shut: by
 * default it opens when any player is within 1, 2, 5 or 10 blocks (or only a named player), optionally only while it
 * gets a redstone signal, and the choice can be inverted. Every 5 ticks, while shut, it checks the frame around it
 * ({@link AirLockProtocol}) and fills the opening inside the frame with Air Lock Seals, re-filling when the frame
 * changes; opening takes the seals away again (into breathable air where the room behind them was sealed). Only the
 * owner (who placed it) can change its settings.
 */
public class AirLockControllerBlockEntity extends BlockEntity {
    public static final int SET_REDSTONE = 0;
    public static final int SET_PLAYER_DISTANCE = 1;
    public static final int SET_DISTANCE_SELECTION = 2;
    public static final int SET_PLAYER_NAME_MATCHES = 3;
    public static final int SET_INVERT = 4;
    public static final int SET_HORIZONTAL = 5;
    public static final int SET_PLAYER_NAME = 6;
    private static final double[] DISTANCES = {1.0D, 2.0D, 5.0D, 10.0D};

    boolean redstoneActivation;
    boolean playerDistanceActivation = true;
    int playerDistanceSelection;
    boolean playerNameMatches;
    String playerToOpenFor = "";
    boolean invertSelection;
    boolean horizontalModeEnabled;
    private boolean lastHorizontalModeEnabled;
    private String ownerName = "";
    private boolean active;
    private boolean lastActive;
    private int otherAirLocks;
    private int lastOtherAirLocks;
    @Nullable
    private AirLockProtocol protocol;
    private int ticks;

    public AirLockControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.AIR_LOCK_CONTROLLER.get(), pos, state);
    }

    public void setOwner(String owner) {
        this.ownerName = owner;
        setChanged();
    }

    public String ownerName() {
        return this.ownerName;
    }

    /** Shut (sealed) right now. */
    public boolean isActive() {
        return this.active;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AirLockControllerBlockEntity controller) {
        controller.tick();
    }

    private void tick() {
        final Level level = this.level;
        this.active = false;
        if (this.redstoneActivation) {
            this.active = level.getBestNeighborSignal(this.worldPosition) > 0;
        }
        if ((this.active || !this.redstoneActivation) && this.playerDistanceActivation) {
            final double distance = DISTANCES[Math.max(0, Math.min(3, this.playerDistanceSelection))];
            final AABB region = new AABB(this.worldPosition.getX() + 0.5D - distance, this.worldPosition.getY() + 0.5D - distance,
                    this.worldPosition.getZ() + 0.5D - distance, this.worldPosition.getX() + 0.5D + distance, this.worldPosition.getY() + 0.5D + distance,
                    this.worldPosition.getZ() + 0.5D + distance);
            final List<Player> players = level.getEntitiesOfClass(Player.class, region);
            if (this.playerNameMatches) {
                boolean found = false;
                for (Player player : players) {
                    if (player.getGameProfile().getName().equalsIgnoreCase(this.playerToOpenFor)) {
                        found = true;
                        break;
                    }
                }
                this.active = found;
            } else {
                this.active = !players.isEmpty();
            }
        }
        if (!this.invertSelection) {
            this.active = !this.active;
        }
        if (this.protocol == null) {
            this.protocol = new AirLockProtocol(level, this.worldPosition);
        }
        if (this.ticks % 5 == 0) {
            if (this.horizontalModeEnabled != this.lastHorizontalModeEnabled) {
                unsealAirLock();
            } else if (this.active || this.lastActive) {
                this.lastOtherAirLocks = this.otherAirLocks;
                this.otherAirLocks = this.protocol.calculate(this.horizontalModeEnabled);
                if (this.active) {
                    if (this.otherAirLocks != this.lastOtherAirLocks || !this.lastActive) {
                        unsealAirLock();
                        if (this.otherAirLocks >= 0) {
                            sealAirLock();
                        }
                    }
                } else if (this.lastActive) {
                    unsealAirLock();
                }
            }
            if (this.active != this.lastActive) {
                final BlockState state = getBlockState();
                if (state.hasProperty(AirLockControllerBlock.ACTIVE)) {
                    level.setBlock(this.worldPosition, state.setValue(AirLockControllerBlock.ACTIVE, this.active), Block.UPDATE_CLIENTS);
                }
                setChanged();
            }
            this.lastActive = this.active;
            this.lastHorizontalModeEnabled = this.horizontalModeEnabled;
        }
        this.ticks++;
    }

    private void placeSeal(BlockPos pos) {
        if (this.level.getBlockState(pos).isAir()) {
            this.level.setBlock(pos, GCBlocks.AIR_LOCK_SEAL.get().defaultBlockState()
                    .setValue(AirLockSealBlock.CONNECTION, AirLockSealBlock.connection(this.level, pos)), Block.UPDATE_ALL);
        }
    }

    /** 1.12.2 sealAirLock: fill the inside of the frame with seals (a vertical doorway, or a flat hatch). */
    private void sealAirLock() {
        final AirLockProtocol p = this.protocol;
        if (!p.loaded()) {
            return;
        }
        int x = (p.maxX + p.minX) / 2;
        int y = (p.maxY + p.minY) / 2;
        int z = (p.maxZ + p.minZ) / 2;
        if (!this.level.getBlockState(new BlockPos(x, y, z)).is(GCBlocks.AIR_LOCK_SEAL.get())) {
            this.level.playSound(null, x, y, z, GCSounds.OPEN_AIR_LOCK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (this.horizontalModeEnabled) {
            if (p.minY == p.maxY && p.minX != p.maxX && p.minZ != p.maxZ) {
                for (x = p.minX + 1; x <= p.maxX - 1; x++) {
                    for (z = p.minZ + 1; z <= p.maxZ - 1; z++) {
                        placeSeal(new BlockPos(x, y, z));
                    }
                }
            }
        } else if (p.minX != p.maxX) {
            for (x = p.minX + 1; x <= p.maxX - 1; x++) {
                for (y = p.minY + 1; y <= p.maxY - 1; y++) {
                    placeSeal(new BlockPos(x, y, z));
                }
            }
        } else if (p.minZ != p.maxZ) {
            for (z = p.minZ + 1; z <= p.maxZ - 1; z++) {
                for (y = p.minY + 1; y <= p.maxY - 1; y++) {
                    placeSeal(new BlockPos(x, y, z));
                }
            }
        }
    }

    private boolean isSeal(BlockPos pos) {
        return this.level.getBlockState(pos).is(GCBlocks.AIR_LOCK_SEAL.get());
    }

    private boolean isBreathable(BlockPos pos) {
        return this.level.getBlockState(pos).getBlock() instanceof io.github.reiko45.galacticraft.block.BreathableAirBlock;
    }

    /** One opening of the doorway: was the room on either side of it sealed (then the seals turn to breathable air)? */
    private boolean sealedSide(int minA, int maxA, int minB, int maxB, java.util.function.BiFunction<Integer, Integer, BlockPos> at,
                               net.minecraft.core.Direction side) {
        for (int a = minA + 1; a <= maxA - 1; a++) {
            for (int b = minB + 1; b <= maxB - 1; b++) {
                final BlockPos pos = at.apply(a, b);
                if (isBreathable(pos.relative(side))) {
                    if (isSeal(pos)) {
                        return true;
                    }
                    continue;
                }
                if (isBreathable(pos.relative(side.getOpposite())) && isSeal(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void clearSeals(int minA, int maxA, int minB, int maxB, java.util.function.BiFunction<Integer, Integer, BlockPos> at, boolean breathable) {
        for (int a = minA + 1; a <= maxA - 1; a++) {
            for (int b = minB + 1; b <= maxB - 1; b++) {
                final BlockPos pos = at.apply(a, b);
                if (isSeal(pos)) {
                    this.level.setBlock(pos, breathable ? GCBlocks.BREATHABLE_AIR.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** 1.12.2 unsealAirLock. */
    public void unsealAirLock() {
        final AirLockProtocol p = this.protocol;
        if (p == null || this.level == null || !p.loaded()) {
            return;
        }
        final int x = p.minX + (p.maxX - p.minX) / 2;
        final int y = p.minY + (p.maxY - p.minY) / 2;
        final int z = p.minZ + (p.maxZ - p.minZ) / 2;
        if (!this.level.getBlockState(new BlockPos(x, y, z)).isAir()) {
            this.level.playSound(null, x, y, z, GCSounds.CLOSE_AIR_LOCK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (this.lastHorizontalModeEnabled) {
            if (p.minY == p.maxY && p.minX != p.maxX && p.minZ != p.maxZ) {
                final java.util.function.BiFunction<Integer, Integer, BlockPos> at = (a, b) -> new BlockPos(a, y, b);
                clearSeals(p.minX, p.maxX, p.minZ, p.maxZ, at, sealedSide(p.minX, p.maxX, p.minZ, p.maxZ, at, net.minecraft.core.Direction.UP));
            }
        } else if (p.minX != p.maxX) {
            final java.util.function.BiFunction<Integer, Integer, BlockPos> at = (a, b) -> new BlockPos(a, b, z);
            clearSeals(p.minX, p.maxX, p.minY, p.maxY, at, sealedSide(p.minX, p.maxX, p.minY, p.maxY, at, net.minecraft.core.Direction.NORTH));
        } else if (p.minZ != p.maxZ) {
            final java.util.function.BiFunction<Integer, Integer, BlockPos> at = (a, b) -> new BlockPos(x, b, a);
            clearSeals(p.minZ, p.maxZ, p.minY, p.maxY, at, sealedSide(p.minZ, p.maxZ, p.minY, p.maxY, at, net.minecraft.core.Direction.WEST));
        }
    }

    // ---------------------------------------------------------------- settings screen

    public void openScreen(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new AirLockNetwork.OpenAirLock(this.worldPosition, settings()));
    }

    public CompoundTag settings() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("OwnerName", this.ownerName);
        tag.putBoolean("RedstoneActivation", this.redstoneActivation);
        tag.putBoolean("PlayerDistanceActivation", this.playerDistanceActivation);
        tag.putInt("PlayerDistanceSelection", this.playerDistanceSelection);
        tag.putBoolean("PlayerNameMatches", this.playerNameMatches);
        tag.putString("PlayerToOpenFor", this.playerToOpenFor);
        tag.putBoolean("InvertSelection", this.invertSelection);
        tag.putBoolean("HorizontalModeEnabled", this.horizontalModeEnabled);
        return tag;
    }

    /** A change from the settings screen (1.12.2 S_ON_ADVANCED_GUI_CLICKED_INT / _STRING); only the owner may. */
    public void applySetting(ServerPlayer player, int setting, int value, String text) {
        if (!player.getGameProfile().getName().equals(this.ownerName) || player.distanceToSqr(this.worldPosition.getCenter()) > 64.0D) {
            return;
        }
        switch (setting) {
            case SET_REDSTONE -> this.redstoneActivation = value != 0;
            case SET_PLAYER_DISTANCE -> this.playerDistanceActivation = value != 0;
            case SET_DISTANCE_SELECTION -> this.playerDistanceSelection = Math.max(0, Math.min(3, value));
            case SET_PLAYER_NAME_MATCHES -> this.playerNameMatches = value != 0;
            case SET_INVERT -> this.invertSelection = value != 0;
            case SET_HORIZONTAL -> {
                this.lastHorizontalModeEnabled = this.horizontalModeEnabled;
                this.horizontalModeEnabled = value != 0;
            }
            case SET_PLAYER_NAME -> this.playerToOpenFor = text.length() > 16 ? text.substring(0, 16) : text;
            default -> {
                return;
            }
        }
        setChanged();
    }

    /** Settings by hand (tests, structures). */
    public void configure(boolean redstone, boolean playerDistance, int distanceSelection, boolean invert, boolean horizontal) {
        this.redstoneActivation = redstone;
        this.playerDistanceActivation = playerDistance;
        this.playerDistanceSelection = distanceSelection;
        this.invertSelection = invert;
        this.horizontalModeEnabled = horizontal;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ownerName = tag.getString("OwnerName");
        this.redstoneActivation = tag.getBoolean("RedstoneActivation");
        this.playerDistanceActivation = !tag.contains("PlayerDistanceActivation") || tag.getBoolean("PlayerDistanceActivation");
        this.playerDistanceSelection = tag.getInt("PlayerDistanceSelection");
        this.playerNameMatches = tag.getBoolean("PlayerNameMatches");
        this.playerToOpenFor = tag.getString("PlayerToOpenFor");
        this.invertSelection = tag.getBoolean("InvertSelection");
        this.active = tag.getBoolean("active");
        this.lastActive = tag.getBoolean("lastActive");
        this.horizontalModeEnabled = tag.getBoolean("HorizontalModeEnabled");
        this.lastHorizontalModeEnabled = this.horizontalModeEnabled;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.merge(settings());
        tag.putBoolean("active", this.active);
        tag.putBoolean("lastActive", this.lastActive);
    }
}
