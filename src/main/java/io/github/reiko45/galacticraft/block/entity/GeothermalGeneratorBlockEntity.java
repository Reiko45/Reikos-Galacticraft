/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.tile.TileEntityGeothermalGenerator).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.GeothermalGeneratorBlock;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.GeothermalGeneratorMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCParticles;
import net.minecraft.core.BlockPos;
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
 * Geothermal Generator (1.12.2 TileEntityGeothermalGenerator). Set on a Vapor Spout with Sulphuric Acid under it (up to
 * 19 blocks below the generator, through air only), it makes 30 to 200 gJ/t, rising and falling along a sine wave with
 * a period of about 314 ticks. It stores 16,000 gJ, sends up to 200 gJ/t out of its left side (seen from the front)
 * and charges a battery in its slot. The spout is checked once a second; the block looks active while it has a spout
 * and is switched on.
 *
 * <p>1.12.2 timed the wave by the generator's own tick counter, which only the server knew; the client was told the
 * output every few ticks. Here the wave runs on the world clock, so the client can puff the exhaust without being told.
 */
public class GeothermalGeneratorBlockEntity extends MachineBlockEntity {
    public static final int MAX_GENERATE_GJ_PER_TICK = 200;
    public static final int MIN_GENERATE_GJ_PER_TICK = 30;

    private boolean validSpout;
    private boolean disabled;
    private int disableCooldown;
    private int generateWatts;
    private int ticks;

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.GEOTHERMAL_GENERATOR.get(), pos, state, 1, 16_000.0F, MAX_GENERATE_GJ_PER_TICK, MAX_GENERATE_GJ_PER_TICK);
    }

    /** 1.12.2 update: the spout check (every 20 ticks) under the generator. */
    public static boolean hasSpout(Level level, BlockPos pos) {
        final BlockPos below = pos.below();
        if (!level.getBlockState(below).is(GCBlocks.VAPOR_SPOUT.get())) {
            return false;
        }
        for (BlockPos check = below.below(); pos.getY() - check.getY() < 20; check = check.below()) {
            final BlockState state = level.getBlockState(check);
            if (state.is(GCFluids.SULPHURIC_ACID_BLOCK.get())) {
                return true;
            }
            if (!state.isAir()) {
                return false;
            }
        }
        return false;
    }

    /** 1.12.2 getGenerate (without the disabled and spout checks): 30 + 170 * (sin(t / 50) / 2 + 1/2), rounded down. */
    public static int output(long time) {
        final int diff = MAX_GENERATE_GJ_PER_TICK - MIN_GENERATE_GJ_PER_TICK;
        return Mth.floor((Math.sin(time / 50.0F) * 0.5F + 0.5F) * diff + MIN_GENERATE_GJ_PER_TICK);
    }

    @Override
    protected void tick() {
        this.energy.generate(this.generateWatts);
        if (this.ticks++ % 20 == 0) {
            this.validSpout = hasSpout(this.level, this.worldPosition);
        }
        chargeBattery(0);
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        this.generateWatts = this.disabled || !this.validSpout ? 0
                : Mth.clamp(output(this.level.getGameTime()), 0, MAX_GENERATE_GJ_PER_TICK);
        updateActive();
        pushEnergy();
        setChanged();
    }

    /** 1.12.2 getActualState: active = a valid spout and switched on. */
    private void updateActive() {
        final BlockState state = getBlockState();
        final boolean active = this.validSpout && !this.disabled;
        if (state.hasProperty(GeothermalGeneratorBlock.ACTIVE) && state.getValue(GeothermalGeneratorBlock.ACTIVE) != active) {
            this.level.setBlock(this.worldPosition, state.setValue(GeothermalGeneratorBlock.ACTIVE, active), 3);
        }
    }

    /**
     * 1.12.2 client update: while it works, nine puffs of acid exhaust over its top every 5 * (200 / (output + 1)) + 1
     * ticks, more often the more it makes.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(GeothermalGeneratorBlock.ACTIVE)) {
            return;
        }
        final long time = level.getGameTime();
        final int watts = output(time);
        if (time % ((int) ((float) MAX_GENERATE_GJ_PER_TICK / (watts + 1)) * 5 + 1) == 0) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    level.addParticle(GCParticles.ACID_EXHAUST.get(), pos.getX() + 0.5D + i * 0.25D, pos.getY() + 1.0D,
                            pos.getZ() + 0.5D + j * 0.25D, 0.0D, 0.025D, 0.0D);
                }
            }
        }
    }

    /** Disable/Enable button (1.12.2: 20 tick cooldown). */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 20;
            setChanged();
        }
    }

    public boolean hasValidSpout() {
        return this.validSpout;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public int generating() {
        return this.generateWatts;
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.generateWatts);
        data.addShort(() -> this.validSpout ? 1 : 0);
        data.addShort(() -> this.disabled ? 1 : 0);
        data.addShort(() -> this.disableCooldown);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ElectricItem;
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.OUTPUT : EnergyAccess.NONE;
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
        return new GeothermalGeneratorMenu(id, playerInventory, this);
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
