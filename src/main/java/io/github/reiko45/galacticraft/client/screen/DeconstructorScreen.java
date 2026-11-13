/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.container.GuiDeconstructor). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.menu.DeconstructorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiDeconstructor: 176 x 199; the progress bar, the energy bar, and "Status: Running / Idle". */
public class DeconstructorScreen extends MachineScreen<DeconstructorMenu> {
    public DeconstructorScreen(DeconstructorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "deconstructor", 176, 199);
        this.titleLabelX = 10;
        energyTooltipRegion(17, 95, 56, 9);
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        blitHorizontal(graphics, 53, 36, 176, 13, 54, 17, this.menu.progressFraction());
        if (this.menu.progressFraction() > 0.5F) {
            graphics.blit(this.texture, this.leftPos + 77, this.topPos + 28, 176, 0, 15, 13);
        }
        if (this.menu.energy() > 0) {
            blitHorizontal(graphics, 18, 96, 176, 30, 54, 7, energyFraction());
            graphics.blit(this.texture, this.leftPos + 4, this.topPos + 95, 176, 37, 11, 10);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final Component state = this.menu.progressFraction() > 0.0F
                ? Component.translatable("gui.galacticraftcore.status.running").withStyle(ChatFormatting.GREEN)
                : Component.translatable("gui.galacticraftcore.status.idle").withStyle(ChatFormatting.GOLD);
        final Component line = Component.translatable("gui.galacticraftcore.status").append(": ").append(state);
        graphics.drawString(this.font, line, 120 - this.font.width(line) / 2, 75, 0x404040, false);
    }
}
