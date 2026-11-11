package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.BreathableAirBlock;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.oxygen.SealFinder;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Oxygen Sealer (1.12.2 TileEntityOxygenSealer): 10,000 mB tank, 32,000 gJ buffer, 5 gJ/t (20 with an Ambient Thermal
 * Controller in slot 2). Oxygen use is 12 mB/t while unsealed and 2 mB/t once sealed. It fills the enclosed space above
 * its vent with breathable air (see {@link SealFinder}); several sealers venting into one space share the work.
 *
 * <p>Like 1.12.2, only one sealer runs a check per server tick, and each waits 75 + (number of sealers) ticks between
 * checks. Blocks changing next to breathable air schedule an edge check: breathable air that no sealer vents into is
 * cleared, otherwise the sealers re-check soon. Slots: 0 battery, 1 oxygen supply, 2 Ambient Thermal Controller.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public class OxygenSealerBlockEntity extends OxygenMachineBlockEntity {
    private static final int UNSEALED_OXYGEN_PER_TICK = 12;
    private static final int SEALED_OXYGEN_PER_TICK = 2;
    private static final Map<Level, Set<OxygenSealerBlockEntity>> LOADED = new WeakHashMap<>();
    private static final Map<Level, Set<BlockPos>> EDGE_CHECKS = new WeakHashMap<>();
    private static int countEntities;
    private static int countThisTick;
    private static boolean checkedThisTick;

    private boolean sealed;
    private boolean active;
    private boolean calculating = true;
    private boolean lastDisabled;
    private int cooldown = 126;
    private int cooldownTotal = 126;

    public OxygenSealerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_SEALER.get(), pos, state, 3, 10_000, UNSEALED_OXYGEN_PER_TICK, 32_000.0F, 5.0F);
        this.noRedstoneControl = true;
        this.energy.setMaxReceive(25.0F);
    }

    // ------------------------------------------------------------------ registry of loaded sealers

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            LOADED.computeIfAbsent(this.level, l -> new LinkedHashSet<>()).add(this);
            this.cooldown = this.cooldownTotal = 126 + countEntities;
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            final Set<OxygenSealerBlockEntity> loaded = LOADED.get(this.level);
            if (loaded != null) {
                loaded.remove(this); // Set.of() would throw, even with nothing to remove
            }
        }
        super.setRemoved();
    }

    /** Broken: the breathable air it was keeping goes, unless another sealer still holds the space (edge check). */
    @Override
    public void onBroken() {
        if (this.level != null && !this.level.isClientSide && this.sealed) {
            requestRecheck(this.level, this.worldPosition.above());
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (this.level != null) {
            final Set<OxygenSealerBlockEntity> loaded = LOADED.get(this.level);
            if (loaded != null) {
                loaded.remove(this); // Set.of() would throw, even with nothing to remove
            }
        }
        super.onChunkUnloaded();
    }

    /** A block next to breathable air changed: re-check the space around {@code pos} soon (1.12.2 edge check). */
    public static void requestRecheck(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            EDGE_CHECKS.computeIfAbsent(level, l -> new LinkedHashSet<>()).add(pos.immutable());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        countEntities = countThisTick;
        countThisTick = 0;
        checkedThisTick = false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final Set<BlockPos> edges = EDGE_CHECKS.remove(level);
        if (edges == null) {
            return;
        }
        final Set<Long> done = new HashSet<>();
        for (BlockPos edge : edges) {
            if (!done.contains(edge.asLong())) {
                edgeCheck(level, edge, done);
            }
        }
    }

    /**
     * Collect the breathable air connected to {@code start}; if no sealer vents into it, clear it, otherwise have those
     * sealers re-check within a few ticks.
     */
    private static void edgeCheck(Level level, BlockPos start, Set<Long> done) {
        if (!(level.getBlockState(start).getBlock() instanceof BreathableAirBlock)) {
            return;
        }
        final List<OxygenSealerBlockEntity> sealers = new ArrayList<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        final Set<Long> seen = new HashSet<>();
        queue.add(start);
        seen.add(start.asLong());
        final int limit = SealFinder.CHECKS_PER_SEALER * 8 * Math.max(1, LOADED.getOrDefault(level, Set.of()).size());
        while (!queue.isEmpty() && seen.size() < limit) {
            final BlockPos pos = queue.poll();
            if (level.getBlockEntity(pos.below()) instanceof OxygenSealerBlockEntity sealer) {
                sealers.add(sealer);
            }
            for (Direction side : Direction.values()) {
                final BlockPos next = pos.relative(side);
                if (level.isLoaded(next) && seen.add(next.asLong()) && level.getBlockState(next).getBlock() instanceof BreathableAirBlock) {
                    queue.add(next);
                }
            }
        }
        done.addAll(seen);
        if (sealers.isEmpty()) {
            SealFinder.unseal(level, start, limit);
        } else {
            for (OxygenSealerBlockEntity sealer : sealers) {
                sealer.cooldown = Math.min(sealer.cooldown, 3);
            }
        }
    }

    // ------------------------------------------------------------------ machine

    @Override
    protected int batterySlot() {
        return 0;
    }

    @Override
    protected int oxygenSupplySlot() {
        return 1;
    }

    /** 1.12.2 thermalControlEnabled: an Ambient Thermal Controller in slot 2, powered and not disabled. */
    public boolean thermalControlEnabled() {
        return this.inventory.getStackInSlot(2).is(GCItems.AMBIENT_THERMAL_CONTROLLER.get()) && this.hasEnoughEnergyToRun && !this.disabled;
    }

    /** 1.12.2 getFindSealChecks: 1,250 while running with an open vent, else 0. */
    public int findSealChecks() {
        if (!this.active || this.oxygen.getFluidAmount() < this.oxygenPerTick || !this.hasEnoughEnergyToRun || ventBlocked()) {
            return 0;
        }
        return SealFinder.CHECKS_PER_SEALER;
    }

    public boolean ventBlocked() {
        final BlockPos above = this.worldPosition.above();
        final BlockState state = this.level.getBlockState(above);
        return !state.isAir() && !OxygenUtil.canBlockPassAir(this.level, state, above, Direction.UP);
    }

    @Override
    protected boolean shouldUseEnergy() {
        return this.oxygen.getFluidAmount() > this.oxygenPerTick && !this.disabled;
    }

    @Override
    protected boolean shouldUseOxygen() {
        return this.hasEnoughEnergyToRun && this.active;
    }

    @Override
    protected void tick() {
        if (thermalControlEnabled()) {
            if (energyUse() != 20.0F) {
                setEnergyUse(20.0F, 50.0F);
            }
        } else if (energyUse() != 5.0F) {
            setEnergyUse(5.0F, 25.0F);
        }
        this.oxygenPerTick = this.sealed ? SEALED_OXYGEN_PER_TICK : UNSEALED_OXYGEN_PER_TICK;
        super.tick();
    }

    @Override
    protected void work() {
        countThisTick++;
        this.active = this.oxygen.getFluidAmount() >= 1 && this.hasEnoughEnergyToRun && !this.disabled;
        if (this.disabled != this.lastDisabled) {
            this.lastDisabled = this.disabled;
            if (!this.disabled) {
                this.cooldown = this.cooldownTotal * 3 / 5;
            }
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        } else if (!checkedThisTick) {
            this.cooldown = this.cooldownTotal = 75 + countEntities;
            if (this.active || this.sealed) {
                checkedThisTick = true;
                this.calculating = false;
                SealFinder.check(this);
            }
        }
    }

    /** Result of a seal check this sealer took part in. Non-head sealers wait longer, so the head keeps control. */
    public void onSealChecked(boolean sealed, boolean head) {
        this.sealed = sealed;
        this.calculating = false;
        if (!head) {
            this.cooldown = this.cooldownTotal = 75 + countEntities + 51;
        }
        setChanged();
    }

    public boolean isSealed() {
        return this.sealed;
    }

    /** 1.12.2 getScaledThreadCooldown(25) while active. */
    public int scaledCooldown() {
        return this.active ? Math.min(25, this.cooldown * 25 / Math.max(1, this.cooldownTotal)) : 0;
    }

    @Override
    public MachineStatus status() {
        if (this.level != null && ventBlocked()) {
            return MachineStatus.SEALER_BLOCKED;
        }
        if (this.disabled) {
            return MachineStatus.DISABLED;
        }
        if (this.energy.getStoredGJ() <= 0.0F) {
            return MachineStatus.MISSING_POWER;
        }
        if (this.energy.getStoredGJ() < energyUse()) {
            return MachineStatus.LOW_MISSING_POWER;
        }
        if (this.oxygen.getFluidAmount() < 1) {
            return MachineStatus.MISSING_OXYGEN;
        }
        if (this.calculating) {
            return MachineStatus.CHECKING_SEAL;
        }
        final int cooldown = scaledCooldown();
        if (cooldown < 15) {
            return cooldown < 4 ? MachineStatus.CHECK_STARTING : MachineStatus.CHECK_PENDING;
        }
        return this.sealed ? MachineStatus.SEALED : MachineStatus.UNSEALED;
    }

    /** GUI: 25 - cooldown (the check-timer bar). */
    @Override
    protected int guiValue1() {
        return 25 - scaledCooldown();
    }

    /** GUI thermal status: 1 on (breathable air above is thermal), 2 not available yet, 0 off. */
    @Override
    protected int guiValue2() {
        final BlockState above = this.level.getBlockState(this.worldPosition.above());
        if (above.getBlock() instanceof BreathableAirBlock && above.getValue(BreathableAirBlock.THERMAL)) {
            return 1;
        }
        return thermalControlEnabled() ? 2 : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 2 ? stack.is(GCItems.AMBIENT_THERMAL_CONTROLLER.get()) : super.isItemValid(slot, stack);
    }

    @Override
    public OxygenMachineMenu.Kind menuKind() {
        return OxygenMachineMenu.Kind.SEALER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Sealed", this.sealed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.sealed = tag.getBoolean("Sealed");
    }
}
