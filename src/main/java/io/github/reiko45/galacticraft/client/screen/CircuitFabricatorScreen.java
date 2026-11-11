package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.menu.CircuitFabricatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiCircuitFabricator, including the three-frame animated progress bar. */
public class CircuitFabricatorScreen extends MachineScreen<CircuitFabricatorMenu> {
    public CircuitFabricatorScreen(CircuitFabricatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "circuit_fabricator", 176, 192);
        this.titleLabelX = 10;
        energyTooltipRegion(17, 88, 56, 9);
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        graphics.blit(this.texture, this.leftPos + 5, this.topPos + 68, 176, 47, 18, 18);
        graphics.blit(this.texture, this.leftPos + 5, this.topPos + 89, 194, 47, 9, 8);
        graphics.blit(this.texture, this.leftPos + 17, this.topPos + 88, 176, 65, 56, 9);
        final int progress = this.menu.progress();
        if (progress > 0) {
            final int frame = (progress % 9) / 3;
            final int width = progress * 51 / this.menu.processTime();
            graphics.blit(this.texture, this.leftPos + 88, this.topPos + 20, 176, 17 + frame * 10, width, 10);
        }
        blitHorizontal(graphics, 18, 89, 176, 0, 54, 7, energyFraction());
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 4, this.topPos + 88, 176, 7, 11, 10);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final Component status = Component.translatable("gui.galacticraftcore.status").append(":");
        graphics.drawString(this.font, status, 115 - this.font.width(status) / 2, 80, 0x404040, false);
        final Component state = this.menu.status().text();
        graphics.drawString(this.font, state, 115 - this.font.width(state) / 2, 90, 0x404040, false);
    }
}
