package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.menu.CompressorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiIngotCompressor: progress arrow, fuel flame, no energy bar. */
public class CompressorScreen extends MachineScreen<CompressorMenu> {
    public CompressorScreen(CompressorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "ingot_compressor", 176, 192);
        this.titleLabelX = 10;
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        final int progress = this.menu.progress();
        blitHorizontal(graphics, 77, 36, 176, 13, 54, 17, (float) progress / CompressorBlockEntity.PROCESS_TIME);
        final int flame = (int) (this.menu.burnFraction() * 14);
        if (flame > 0) {
            graphics.blit(this.texture, this.leftPos + 81, this.topPos + 27 + 14 - flame, 176, 30 + 14 - flame, 14, flame);
        }
        if (progress > CompressorBlockEntity.PROCESS_TIME / 2) {
            graphics.blit(this.texture, this.leftPos + 101, this.topPos + 28, 176, 0, 15, 13);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final Component fuel = Component.translatable("gui.galacticraftcore.fuel").append(":");
        graphics.drawString(this.font, fuel, 50 - this.font.width(fuel), 79, 0x404040, false);
        final Component status = Component.translatable("gui.galacticraftcore.status").append(":");
        graphics.drawString(this.font, status, 120 - this.font.width(status) / 2, 70, 0x404040, false);
        final Component state = (this.menu.progress() > 0 ? MachineStatus.COMPRESSING : MachineStatus.IDLE).text();
        graphics.drawString(this.font, state, 120 - this.font.width(state) / 2, 80, 0x404040, false);
    }

    @Override
    protected java.util.List<Component> energyTooltip() {
        return java.util.List.of();
    }
}
