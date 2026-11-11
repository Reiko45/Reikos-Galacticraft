package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.menu.ElectricCompressorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiElectricIngotCompressor (shared by the Advanced Compressor). */
public class ElectricCompressorScreen extends MachineScreen<ElectricCompressorMenu> {
    public ElectricCompressorScreen(ElectricCompressorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "electric_ingot_compressor", 176, 199);
        this.titleLabelX = 10;
        energyTooltipRegion(17, 95, 56, 9);
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        blitHorizontal(graphics, 77, 38, 176, 13, 54, 17, this.menu.progressFraction());
        blitHorizontal(graphics, 18, 96, 176, 30, 54, 7, energyFraction());
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 4, this.topPos + 95, 176, 37, 11, 10);
        }
        if (this.menu.progress() > this.menu.processTime() / 2) {
            graphics.blit(this.texture, this.leftPos + 101, this.topPos + 30, 176, 0, 15, 13);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final Component status = Component.translatable("gui.galacticraftcore.status").append(": ").append(this.menu.status().text());
        graphics.drawString(this.font, status, 120 - this.font.width(status) / 2, 75, 0x404040, false);
    }
}
