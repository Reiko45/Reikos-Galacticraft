/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.tile.TileEntitySolarArrayController,
 * TileEntitySolarArrayModule, SolarModuleNetwork). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.SolarArrayControllerMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solar Array Controller (1.12.2 TileEntitySolarArrayController). It gathers every Solar Array Module joined (side by
 * side) to the modules on its left, right and back, and once a second, by day (rain only matters off Galacticraft
 * worlds), counts those within 16 blocks across at its own height, and of them the ones that see the sky. Output is
 * floor(6.3 gJ/t * modules in the sun * boost), at most 1,000 gJ/t, sent out of its front; it stores 50,000 gJ and
 * charges a battery. The boost is the world's solar multiplier, a tenth more on a space station, and on Venus a
 * thousandth more per block above y = 90.
 *
 * <p>Kept from 1.12.2: before counting a module it also checks the column over the controller, but starting as far
 * above the controller as the controller is above the bottom of the world, so only a roof high up turns the array off.
 */
public class SolarArrayControllerBlockEntity extends MachineBlockEntity {
    public static final int MAX_GENERATE_WATTS = 1000;
    public static final int RANGE = 16;
    /** A bound on the module search, far more than can be in range (33 x 33 - 1). */
    private static final int SEARCH_LIMIT = 4096;

    private int solarStrength;
    private int possible;
    private boolean disabled;
    private int disableCooldown;
    private int generateWatts;
    private int ticks;

    public SolarArrayControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.SOLAR_ARRAY_CONTROLLER.get(), pos, state, 1, 50_000.0F, MAX_GENERATE_WATTS, MAX_GENERATE_WATTS);
    }

    /** 1.12.2 getSolarBoost. */
    public static float solarBoost(Level level, BlockPos pos) {
        float result = SolarPanelBlockEntity.solarBoost(level);
        if (level.dimension().equals(GCDimensions.ORBIT)) {
            result *= 1.1F;
        }
        if (level.dimension().equals(GCDimensions.VENUS) && pos.getY() > 90) {
            result += (pos.getY() - 90) / 1000.0F;
        }
        return result;
    }

    /** The modules joined to this controller (1.12.2: the networks of the modules beside it, except at its front). */
    public Set<BlockPos> findModules() {
        final Set<BlockPos> found = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        final Direction front = getBlockState().getValue(MachineBlock.FACING);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            final BlockPos next = this.worldPosition.relative(side);
            if (side != front && isModule(next) && found.add(next)) {
                open.add(next);
            }
        }
        while (!open.isEmpty() && found.size() < SEARCH_LIMIT) {
            final BlockPos pos = open.poll();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                final BlockPos next = pos.relative(side);
                if (this.level.isLoaded(next) && isModule(next) && found.add(next)) {
                    open.add(next);
                }
            }
        }
        return found;
    }

    private boolean isModule(BlockPos pos) {
        return this.level.getBlockState(pos).is(GCBlocks.SOLAR_ARRAY_MODULE.get());
    }

    /** 1.12.2: the column at getPos().add(0, y, 0) for y from getPos().getY() + 1 up holds nothing opaque. */
    private boolean controllerColumnClear() {
        final int start = this.worldPosition.getY() + (this.worldPosition.getY() - this.level.getMinBuildHeight()) + 1;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = start; y < this.level.getMaxBuildHeight(); y++) {
            pos.set(this.worldPosition.getX(), y, this.worldPosition.getZ());
            if (this.level.getBlockState(pos).isSolidRender(this.level, pos)) {
                return false;
            }
        }
        return true;
    }

    private void countModules() {
        this.solarStrength = 0;
        this.possible = 0;
        final Level level = this.level;
        final boolean gcWorld = CelestialPhysics.of(level.dimension()) != null;
        if (CelestialPhysics.isDaytime(level) && (gcWorld || !level.isRaining() && !level.isThundering())) {
            Boolean column = null;
            for (BlockPos module : findModules()) {
                if (Math.abs(module.getX() - this.worldPosition.getX()) <= RANGE && module.getY() == this.worldPosition.getY()
                        && Math.abs(module.getZ() - this.worldPosition.getZ()) <= RANGE) {
                    this.possible++;
                    if (level.canSeeSkyFromBelowWater(module)) {
                        if (column == null) {
                            column = controllerColumnClear();
                        }
                        if (column) {
                            this.solarStrength++;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void tick() {
        this.energy.generate(this.generateWatts);
        chargeBattery(0);
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        if (!this.disabled && this.ticks++ % 20 == 0) {
            countModules();
        }
        final int generated = this.disabled ? 0 : Mth.floor(this.solarStrength * 6.3F * solarBoost(this.level, this.worldPosition));
        this.generateWatts = generated > 0 ? Math.min(generated, MAX_GENERATE_WATTS) : 0;
        pushEnergy();
        setChanged();
    }

    /** Disable/Enable button (1.12.2: 20 tick cooldown). */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 20;
            setChanged();
        }
    }

    /** Modules in range that see the sun (1.12.2 getActualArraySize). */
    public int solarStrength() {
        return this.solarStrength;
    }

    /** Modules in range (1.12.2 getPossibleArraySize). */
    public int possibleSize() {
        return this.possible;
    }

    public int generating() {
        return this.generateWatts;
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.generateWatts);
        data.addShort(() -> this.solarStrength);
        data.addShort(() -> this.possible);
        data.addShort(() -> this.disabled ? 1 : 0);
        data.addShort(() -> this.disableCooldown);
        // the boost in tenths of a percent, as the screen shows it (the client doesn't know the controller's world height)
        data.addShort(() -> Math.round((solarBoost(this.level, this.worldPosition) - 1.0F) * 1000.0F));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ElectricItem;
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.FRONT ? EnergyAccess.OUTPUT : EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0};
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SolarArrayControllerMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Disabled", this.disabled);
        tag.putInt("DisableCooldown", this.disableCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.disabled = tag.getBoolean("Disabled");
        this.disableCooldown = tag.getInt("DisableCooldown");
    }
}
