package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiElectricFurnace / GuiElectricArcFurnace. */
public class ElectricFurnaceScreen extends MachineScreen<ElectricFurnaceMenu> {
    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.isArc() ? "electric_arc_furnace" : "electric_furnace", 176, 166);
        this.titleLabelX = 45;
        energyTooltipRegion(39, 52, 56, 9);
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        blitHorizontal(graphics, 78, 24, 176, 0, 23, 15, this.menu.progressFraction());
        blitHorizontal(graphics, 40, 53, 176, 15, 54, 7, energyFraction());
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 26, this.topPos + 52, 176, 22, 11, 10);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.status").append(": "), 97, 52, 0x404040, false);
        graphics.drawString(this.font, this.menu.status().text(), 107, 62, 0x404040, false);
    }
}
