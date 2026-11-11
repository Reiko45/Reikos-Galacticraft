package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.SolarPanelMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Basic (tier 1, 16,000 gJ) and Advanced (tier 2, 30,000 gJ) Solar Panel, from 1.12.2 TileEntitySolar.
 *
 * <p>Every 20 ticks it counts how many of the 9 columns over its 3x3 panel see the sky (the advanced panel looks
 * along the sun's direction instead of straight up). The panel turns towards its target angle by 1/20 of the gap each
 * tick: the basic panel faces straight up by day and flips over at night or in rain; the advanced panel tracks the sun.
 * Output = floor(0.01 d^2 * strength * |d| * 500 * boost) gJ/t (max 200), where d is how well the panel faces the sun.
 * Energy leaves through the front face only; slot 0 charges a battery.
 */
public class SolarPanelBlockEntity extends MachineBlockEntity {
    public static final int MAX_GENERATE_WATTS = 200;

    private final boolean advanced;
    private int solarStrength;
    private float currentAngle;
    private float targetAngle;
    private boolean disabled;
    private int disableCooldown;
    private int generateWatts;
    private int ticks;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state, boolean advanced) {
        super(advanced ? GCBlockEntities.ADVANCED_SOLAR_PANEL.get() : GCBlockEntities.BASIC_SOLAR_PANEL.get(), pos, state, 1,
                advanced ? 30_000.0F : 16_000.0F, MAX_GENERATE_WATTS, MAX_GENERATE_WATTS);
        this.advanced = advanced;
        this.energy.setTier(advanced ? 2 : 1);
    }

    public static SolarPanelBlockEntity basic(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state, false);
    }

    public static SolarPanelBlockEntity advanced(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state, true);
    }

    public boolean isAdvanced() {
        return this.advanced;
    }

    public float currentAngle() {
        return this.currentAngle;
    }

    // ---------------------------------------------------------------- sun maths (1.12.2 constants kept)

    /** 1.12.2 celestial angle used for output: 0 at sunrise, 90 at noon, 180 at sunset. */
    private static float sunAngle(Level level, float offsetConstant) {
        final float time = level.getTimeOfDay(1.0F);
        final float offset = time - offsetConstant < 0 ? 1.0F - offsetConstant : -offsetConstant;
        return ((time + offset) * 360.0F + 12.5F) % 360.0F;
    }

    public static float solarBoost(Level level) {
        if (level.dimension().equals(io.github.reiko45.galacticraft.world.GCDimensions.ORBIT)) {
            return io.github.reiko45.galacticraft.GCConfig.SPACE_STATION_ENERGY_SCALAR.get().floatValue();
        }
        final CelestialPhysics physics = CelestialPhysics.of(level.dimension());
        return physics != null ? physics.solarMultiplier() : 1.0F;
    }

    private void updateAngles() {
        if (this.level == null) {
            return;
        }
        final float sun = sunAngle(this.level, 0.7845194F);
        final boolean day = CelestialPhysics.isDaytime(this.level) && (sun < 180.5F || sun > 359.5F);
        final boolean raining = this.level.isRaining() || this.level.isThundering();
        if (!day || raining) {
            this.targetAngle = 77.5F + 180.0F;
        } else if (!this.advanced) {
            this.targetAngle = 77.5F;
        } else if (sun > 27.5F && sun < 152.5F) {
            this.targetAngle -= (this.targetAngle - sun + 12.5F) / 20.0F;
        } else if (sun <= 27.5F || sun > 270.0F) {
            this.targetAngle = 15.0F;
        } else {
            this.targetAngle = 140.0F;
        }
        this.currentAngle += (this.targetAngle - this.currentAngle) / 20.0F;
    }

    private int computeStrength() {
        final Level level = this.level;
        final boolean gcWorld = CelestialPhysics.of(level.dimension()) != null;
        if (!CelestialPhysics.isDaytime(level) || !gcWorld && (level.isRaining() || level.isThundering())) {
            return 0;
        }
        int strength = 0;
        final double sinA = -Math.sin(Math.toRadians(this.currentAngle - 77.5D));
        final double cosA = Math.abs(Math.cos(Math.toRadians(this.currentAngle - 77.5D)));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (this.advanced ? sunRayClear(level, x, z, sinA, cosA) : skyColumnClear(level, x, z)) {
                    strength++;
                }
            }
        }
        return strength;
    }

    /** Basic panel: the panel block sees the sky and nothing opaque is above it. */
    private boolean skyColumnClear(Level level, int x, int z) {
        final BlockPos panel = this.worldPosition.offset(x, 2, z);
        if (level.getBrightness(LightLayer.SKY, panel) < level.getMaxLightLevel()) {
            return false;
        }
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = this.worldPosition.getY() + 3; y < level.getMaxBuildHeight(); y++) {
            pos.set(panel.getX(), y, panel.getZ());
            if (level.getBlockState(pos).isSolidRender(level, pos)) {
                return false;
            }
        }
        return true;
    }

    /** Advanced panel: a 100-block ray towards the sun from just above the panel. */
    private boolean sunRayClear(Level level, int x, int z, double sinA, double cosA) {
        final BlockPos start = this.worldPosition.offset(x, 3, z);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double d = 0.0D; d < 100.0D; d++) {
            pos.set(start.getX() + (int) (d * sinA), start.getY() + (int) (d * cosA), start.getZ());
            if (!level.isInWorldBounds(pos)) {
                break;
            }
            if (level.getBlockState(pos).isSolidRender(level, pos)) {
                return false;
            }
        }
        return true;
    }

    private int computeGenerate() {
        if (this.disabled) {
            return 0;
        }
        final float sun = sunAngle(this.level, 0.784690560F);
        final float difference = (180.0F - Math.abs((this.currentAngle + 12.5F) % 180.0F - sun)) / 180.0F;
        return Mth.floor(0.01F * difference * difference * (this.solarStrength * (Math.abs(difference) * 500.0F)) * solarBoost(this.level));
    }

    // ---------------------------------------------------------------- ticking

    @Override
    protected void tick() {
        this.energy.generate(this.generateWatts);
        chargeBattery(0);
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        if (!this.disabled && this.ticks++ % 20 == 0) {
            this.solarStrength = computeStrength();
        }
        updateAngles();
        this.generateWatts = Mth.clamp(computeGenerate(), 0, MAX_GENERATE_WATTS);
        pushEnergy();
        setChanged();
    }

    public void clientTick() {
        updateAngles();
    }

    /** Disable/Enable button (1.12.2: 20 tick cooldown). */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 20;
            setChanged();
        }
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.generateWatts);
        data.addShort(() -> this.solarStrength);
        data.addShort(() -> this.disabled ? 1 : 0);
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
        return new SolarPanelMenu(id, playerInventory, this);
    }

    /** The panel is drawn up to 2 blocks out and 4 blocks up. */
    public AABB renderBounds() {
        return new AABB(this.worldPosition).inflate(1.0D, 0.0D, 1.0D).expandTowards(0.0D, 4.0D, 0.0D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Disabled", this.disabled);
        tag.putFloat("CurrentAngle", this.currentAngle);
        tag.putFloat("TargetAngle", this.targetAngle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.disabled = tag.getBoolean("Disabled");
        this.currentAngle = tag.getFloat("CurrentAngle");
        this.targetAngle = tag.getFloat("TargetAngle");
    }
}
