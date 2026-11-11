package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.menu.CoalGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiCoalGenerator. */
public class CoalGeneratorScreen extends MachineScreen<CoalGeneratorMenu> {
    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "coal_generator", 176, 166);
        this.titleLabelX = 55;
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final float heat = this.menu.heat();
        final int min = CoalGeneratorBlockEntity.MIN_GENERATE_GJ_PER_TICK;
        final Component status = Component.translatable(heat < min
                ? "gui.galacticraftcore.status.not_generating" : "gui.galacticraftcore.status.generating");
        drawCentered(graphics, status, 33);
        final Component detail = heat < min
                ? Component.translatable("gui.galacticraftcore.status.hull_heat").append(": " + (int) (heat / min * 100) + "%")
                : Component.literal(EnergyDisplay.format(heat - min) + "/t");
        drawCentered(graphics, detail, 45);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int y) {
        graphics.drawString(this.font, text, 122 - this.font.width(text) / 2, y, 0x404040, false);
    }
}
