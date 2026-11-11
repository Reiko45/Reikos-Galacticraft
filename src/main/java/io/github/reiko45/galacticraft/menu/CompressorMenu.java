package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 ContainerIngotCompressor (192 px tall): grid (19 + 18c, 18 + 18r), fuel (55, 75), output (138, 38). */
public class CompressorMenu extends MachineMenu {
    public static final int PROGRESS = EXTRA;
    public static final int BURN_TIME = EXTRA + 1;
    public static final int CURRENT_BURN_TIME = EXTRA + 2;

    public CompressorMenu(int id, Inventory playerInventory, CompressorBlockEntity machine) {
        super(GCMenus.COMPRESSOR.get(), id, playerInventory, machine);
    }

    public CompressorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.COMPRESSOR.get(), id, playerInventory, buf, 11, EXTRA + 3);
    }

    @Override
    protected void addMachineSlots() {
        addMachineSlot(0, 55, 75, stack -> CompressorBlockEntity.burnTime(stack) > 0);
        addOutputSlot(1, 138, 38);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addMachineSlot(CompressorBlockEntity.GRID_START + col + row * 3, 19 + col * 18, 18 + row * 18);
            }
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 110};
    }

    public int progress() {
        return value(PROGRESS);
    }

    public float burnFraction() {
        final int current = value(CURRENT_BURN_TIME);
        return current <= 0 ? 0.0F : (float) value(BURN_TIME) / current;
    }
}
