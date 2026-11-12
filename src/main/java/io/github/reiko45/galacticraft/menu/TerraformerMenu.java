/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.inventory.ContainerTerraformer and
 * client.gui.GuiTerraformer#getStatus). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Terraformer menu (237 px tall, inventory from y = 155): water container, battery, then rows of 4 bone meal, 4
 * saplings and 4 seeds. Buttons: 0 trees on/off, 1 grass on/off, 2 show bubble.
 */
public class TerraformerMenu extends MachineMenu {
    public static final int WATER = EXTRA;
    public static final int TREES_DISABLED = EXTRA + 1;
    public static final int GRASS_DISABLED = EXTRA + 2;
    public static final int BUBBLE_VISIBLE = EXTRA + 3;
    public static final int BUBBLE_SIZE = EXTRA + 4;
    public static final int TERRAFORMABLE = EXTRA + 5;
    public static final int GRASS = EXTRA + 6;
    public static final int REDSTONE = EXTRA + 7;
    public static final int DATA_COUNT = EXTRA + 8;
    public static final int BUTTON_TREES = 0;
    public static final int BUTTON_GRASS = 1;
    public static final int BUTTON_BUBBLE = 2;

    @Nullable
    private final TerraformerBlockEntity machine;

    public TerraformerMenu(int id, Inventory playerInventory, TerraformerBlockEntity machine) {
        super(GCMenus.TERRAFORMER.get(), id, playerInventory, machine);
        this.machine = machine;
    }

    public TerraformerMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.TERRAFORMER.get(), id, playerInventory, buf, 14, DATA_COUNT);
        this.machine = null;
    }

    @Override
    protected void addMachineSlots() {
        addMachineSlot(0, 25, 19, stack -> stack.is(Items.BUCKET)
                || FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(FluidTags.WATER)).isPresent());
        addBatterySlot(1, 25, 39);
        for (int col = 0; col < 4; col++) {
            addMachineSlot(2 + col, 25 + col * 18, 63, stack -> stack.is(Items.BONE_MEAL));
        }
        for (int col = 0; col < 4; col++) {
            addMachineSlot(6 + col, 25 + col * 18, 87, TerraformerBlockEntity::isPlant);
        }
        for (int col = 0; col < 4; col++) {
            addMachineSlot(10 + col, 25 + col * 18, 111, stack -> stack.is(Items.WHEAT_SEEDS));
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 155};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.machine == null) {
            return false;
        }
        switch (id) {
            case BUTTON_TREES -> this.machine.toggleTrees();
            case BUTTON_GRASS -> this.machine.toggleGrass();
            case BUTTON_BUBBLE -> this.machine.toggleBubbleVisible();
            default -> {
                return false;
            }
        }
        return true;
    }

    public int water() {
        return value(WATER);
    }

    public boolean treesDisabled() {
        return value(TREES_DISABLED) != 0;
    }

    public boolean grassDisabled() {
        return value(GRASS_DISABLED) != 0;
    }

    public boolean bubbleVisible() {
        return value(BUBBLE_VISIBLE) != 0;
    }

    public float bubbleSize() {
        return value(BUBBLE_SIZE) / 10.0F;
    }

    private boolean anyIn(int start, int end) {
        for (int i = start; i < end; i++) {
            if (!this.machineInventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 1.12.2 GuiTerraformer#getStatus. */
    public Component status() {
        if (value(REDSTONE) != 0) {
            return Component.translatable("gui.galacticraftcore.status.off").withStyle(ChatFormatting.RED);
        }
        if (energy() <= 0.0F) {
            return Component.translatable("gui.galacticraftcore.message.no_energy").withStyle(ChatFormatting.RED);
        }
        if (grassDisabled() && treesDisabled()) {
            return Component.translatable("gui.galacticraftcore.status.disabled").withStyle(ChatFormatting.GOLD);
        }
        if (water() <= 0) {
            return Component.translatable("gui.galacticraftcore.message.no_water").withStyle(ChatFormatting.RED);
        }
        if (!anyIn(2, 6)) {
            return Component.translatable("gui.galacticraftcore.message.no_bonemeal").withStyle(ChatFormatting.RED);
        }
        if (!grassDisabled() && !anyIn(10, 14)) {
            return Component.translatable("gui.galacticraftcore.message.no_seeds").withStyle(ChatFormatting.RED);
        }
        if (!treesDisabled() && !anyIn(6, 10)) {
            return Component.translatable("gui.galacticraftcore.message.no_saplings").withStyle(ChatFormatting.RED);
        }
        if (bubbleSize() < TerraformerBlockEntity.MAX_SIZE - 0.5F) {
            return Component.translatable("gui.galacticraftcore.message.bubble_expanding").withStyle(ChatFormatting.YELLOW);
        }
        if (!treesDisabled() && value(GRASS) <= 0) {
            return Component.translatable("gui.galacticraftcore.message.trees").append(": ")
                    .append(Component.translatable("gui.galacticraftcore.message.no_valid_blocks")).withStyle(ChatFormatting.RED);
        }
        if (!grassDisabled() && value(TERRAFORMABLE) <= 0) {
            return Component.translatable("gui.galacticraftcore.message.grass").append(": ")
                    .append(Component.translatable("gui.galacticraftcore.message.no_valid_blocks")).withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.galacticraftcore.message.terraforming").withStyle(ChatFormatting.GREEN);
    }
}
