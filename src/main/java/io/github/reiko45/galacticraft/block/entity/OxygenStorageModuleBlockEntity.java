package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.OxygenStorageModuleBlock;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.machine.StorageSides;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Oxygen Storage Module (1.12.2 TileEntityOxygenStorageModule): 60,000 mB, no power. Oxygen in through the input face
 * (80 mB/t), out through the output face (500 mB/t); sneak + wrench moves them like the energy storage blocks.
 * Slot 0 drains an oxygen supply item.
 */
public class OxygenStorageModuleBlockEntity extends OxygenMachineBlockEntity {
    public static final int CAPACITY = 60_000;
    public static final int OUTPUT_PER_TICK = 500;

    public OxygenStorageModuleBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_STORAGE_MODULE.get(), pos, state, 1, CAPACITY, 40, 0.0F, 0.0F);
    }

    private StorageSides sides() {
        final BlockState state = getBlockState();
        return state.hasProperty(OxygenStorageModuleBlock.SIDES) ? state.getValue(OxygenStorageModuleBlock.SIDES) : StorageSides.LEFT_RIGHT;
    }

    @Override
    protected int oxygenSupplySlot() {
        return 0;
    }

    /** StorageSides names the input first; the 1.12.2 default is in on the left, out on the right (LEFT_RIGHT). */
    @Override
    protected Set<RelativeSide> oxygenInputs() {
        return Set.of(sides().input());
    }

    @Override
    protected Set<RelativeSide> oxygenOutputs() {
        return Set.of(sides().output());
    }

    @Override
    protected int oxygenOutputRate() {
        return OUTPUT_PER_TICK;
    }

    @Override
    protected boolean shouldUseEnergy() {
        return false;
    }

    @Override
    protected void work() {
    }

    @Override
    public MachineStatus status() {
        return MachineStatus.ACTIVE;
    }

    @Override
    public OxygenMachineMenu.Kind menuKind() {
        return OxygenMachineMenu.Kind.STORAGE_MODULE;
    }
}
