package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.menu.EnergyStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiEnergyStorageModule: stored/capacity/max output text and a 72px bar at (87, 52). */
public class EnergyStorageScreen extends MachineScreen<EnergyStorageMenu> {
    public EnergyStorageScreen(EnergyStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "energy_storage_module", 176, 166);
        energyTooltipRegion(87, 52, 72, 3);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        final float capacity = this.menu.capacity();
        if (capacity > 0) {
            final int width = (int) ((this.menu.energy() + 49) / capacity * 72);
            graphics.blit(this.texture, this.leftPos + 87, this.topPos + 52, 176, 0, Math.min(width, 72), 3);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        float energy = this.menu.energy();
        if (energy + 49 > this.menu.capacity()) {
            energy = this.menu.capacity();
        }
        drawCentered(graphics, Component.literal(EnergyDisplay.format(energy)), 122, 25);
        drawCentered(graphics, Component.translatable("gui.galacticraftcore.of").append(" " + EnergyDisplay.format(this.menu.capacity())), 122, 34);
        drawCentered(graphics, Component.translatable("gui.galacticraftcore.max_output")
                .append(": " + EnergyDisplay.format(this.menu.maxOutput()) + "/t"), 114, 64);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y) {
        graphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, 0x404040, false);
    }
}
