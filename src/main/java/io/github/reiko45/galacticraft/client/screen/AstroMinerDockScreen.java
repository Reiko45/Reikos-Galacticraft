/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.gui.GuiAstroMinerDock).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.AstroMinerEntity;
import io.github.reiko45.galacticraft.menu.AstroMinerDockMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Astro Miner Dock GUI: the store, the battery, a vertical energy gauge, Recall (while the miner is out), and the
 * miner's status, with where it is relative to the dock and how much it has mined while it is out or stuck.
 */
public class AstroMinerDockScreen extends MachineScreen<AstroMinerDockMenu> {
    private static final ResourceLocation LOADER = GalacticraftCore.id("textures/gui/cargo_loader.png");
    private static final int LABEL = 0x404040;
    private Button recall;

    public AstroMinerDockScreen(AstroMinerDockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_astro_miner_dock", 256, 221);
        energyTooltipRegion(233, 29, 10, 68);
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 7;
        this.titleLabelY = 6;
        this.recall = addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.recall"),
                b -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, AstroMinerDockMenu.BUTTON_RECALL))
                .bounds(this.leftPos + 173, this.topPos + 195, 76, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        final int state = this.menu.minerState();
        this.recall.active = this.menu.linked() && state >= AstroMinerEntity.AISTATE_TRAVELLING && state != AstroMinerEntity.AISTATE_DOCKING;
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.menu.energy() > 0) {
            graphics.blit(LOADER, this.leftPos + 233, this.topPos + 17, 176, 0, 11, 10);
        }
        final int level = Math.min(Math.round(energyFraction() * 66), 66);
        graphics.fill(this.leftPos + 234, this.topPos + 29 + 66 - level, this.leftPos + 242, this.topPos + 29 + 66, 0xFFC1AA24);
    }

    /** 1.12.2 getStatus; true in [1] when the position and count lines are shown too. */
    private Component status(boolean[] extraLines) {
        final int state = this.menu.minerState();
        extraLines[0] = state == AstroMinerEntity.AISTATE_STUCK || state == AstroMinerEntity.AISTATE_TRAVELLING
                || state == AstroMinerEntity.AISTATE_MINING || state == AstroMinerEntity.AISTATE_RETURNING;
        return switch (state) {
            case AstroMinerEntity.AISTATE_OFFLINE -> Component.translatable("gui.galacticraftcore.miner.offline").withStyle(ChatFormatting.GOLD);
            case AstroMinerEntity.AISTATE_STUCK -> Component.translatable("gui.galacticraftcore.miner.stuck").withStyle(ChatFormatting.RED);
            case AstroMinerEntity.AISTATE_ATBASE -> Component.translatable("gui.galacticraftcore.miner.docked").withStyle(ChatFormatting.GREEN);
            case AstroMinerEntity.AISTATE_TRAVELLING -> Component.translatable("gui.galacticraftcore.miner.travelling").withStyle(ChatFormatting.GREEN);
            case AstroMinerEntity.AISTATE_MINING -> Component.translatable("gui.galacticraftcore.miner.mining").withStyle(ChatFormatting.GREEN);
            case AstroMinerEntity.AISTATE_RETURNING -> Component.translatable("gui.galacticraftcore.miner.returning").withStyle(ChatFormatting.GREEN);
            case AstroMinerEntity.AISTATE_DOCKING -> Component.translatable("gui.galacticraftcore.miner.docking").withStyle(ChatFormatting.GREEN);
            default -> Component.empty();
        };
    }

    private static String delta(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final boolean[] extra = new boolean[1];
        graphics.drawString(this.font, status(extra), 177, 141, LABEL, false);
        if (extra[0]) {
            final String[] axes = {"x", "y", "z"};
            for (int i = 0; i < 3; i++) {
                graphics.drawString(this.font, "Δ" + axes[i] + ": " + delta(this.menu.delta(i)), 186, 152 + i * 10, 0x26B51F, false);
            }
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.miner.mined").append(": " + this.menu.mined()), 177, 183, 0x26B51F,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        if (x >= 230 && x < 248 && y >= 108 && y < 126 && this.menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.battery_slot.desc.0"),
                    Component.translatable("gui.galacticraftcore.battery_slot.desc.1")), mouseX, mouseY);
        }
    }
}
