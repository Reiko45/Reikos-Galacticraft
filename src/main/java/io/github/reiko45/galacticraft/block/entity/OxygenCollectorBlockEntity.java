package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Oxygen Collector (1.12.2 TileEntityOxygenCollector): 6,000 mB tank, 10 gJ/t. About once every 10 ticks it collects
 * 0.75 mB per leaf or crop block within 5 blocks; in a world with breathable air it collects 93 mB regardless. Oxygen
 * leaves through the face opposite the power port, up to 500 mB/t.
 */
public class OxygenCollectorBlockEntity extends OxygenMachineBlockEntity {
    public static final float OXYGEN_PER_PLANT = 0.75F;
    private boolean producedLastTick;
    /** mB collected in the last collection (1.12.2 lastOxygenCollected x 10). */
    private int lastCollected;

    public OxygenCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_COLLECTOR.get(), pos, state, 1, 6000, 0, 16_000.0F, 10.0F);
        this.noRedstoneControl = true;
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    @Override
    protected Set<RelativeSide> oxygenInputs() {
        return Set.of();
    }

    @Override
    protected Set<RelativeSide> oxygenOutputs() {
        return Set.of(RelativeSide.RIGHT);
    }

    @Override
    protected int oxygenOutputRate() {
        return 500;
    }

    @Override
    protected boolean acceptsOxygen() {
        return false;
    }

    @Override
    protected boolean shouldUseEnergy() {
        return this.oxygen.getFluidAmount() > 0 && this.producedLastTick;
    }

    @Override
    protected void work() {
        this.producedLastTick = this.oxygen.getFluidAmount() < this.oxygen.getCapacity();
        if (this.level == null || this.level.random.nextInt(10) != 0) {
            return;
        }
        if (!this.hasEnoughEnergyToRun) {
            this.lastCollected = 0;
            return;
        }
        final float collected = CelestialPhysics.isAirless(this.level) ? countPlants() * OXYGEN_PER_PLANT : 93.0F;
        this.lastCollected = Mth.floor(collected);
        setOxygenStored(Math.min(this.oxygen.getFluidAmount() + this.lastCollected, this.oxygen.getCapacity()));
    }

    /** Leaves and crops in the 11x11x11 cube around the collector. */
    private int countPlants() {
        int plants = 0;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -5; y <= 5; y++) {
                    pos.setWithOffset(this.worldPosition, x, y, z);
                    if (this.level.isInWorldBounds(pos)) {
                        final BlockState state = this.level.getBlockState(pos);
                        if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof CropBlock) {
                            plants++;
                        }
                    }
                }
            }
        }
        return plants;
    }

    public int lastCollected() {
        return this.lastCollected;
    }

    @Override
    public MachineStatus status() {
        final MachineStatus status = electricStatus();
        return status == MachineStatus.ACTIVE && this.lastCollected <= 0 ? MachineStatus.MISSING_LEAVES : status;
    }

    @Override
    protected int guiValue1() {
        return this.lastCollected;
    }

    @Override
    public OxygenMachineMenu.Kind menuKind() {
        return OxygenMachineMenu.Kind.COLLECTOR;
    }
}
