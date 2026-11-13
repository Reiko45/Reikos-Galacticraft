/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.container.GuiPainter). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.ChromaticApplicatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiPainter: 176 x 186, the Mix, Reset and Apply buttons and a swatch of the colour being mixed. */
public class ChromaticApplicatorScreen extends AbstractContainerScreen<ChromaticApplicatorMenu> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/chromatic_applicator.png");

    public ChromaticApplicatorScreen(ChromaticApplicatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 39;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.paintapply"), b -> click(0))
                .bounds(this.width / 2 + 4, this.height / 2 - 48, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.paintmix"), b -> click(1))
                .bounds(this.width / 2 - 80, this.height / 2 - 48, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.paintreset"), b -> click(2))
                .bounds(this.width / 2 - 80, this.height / 2 - 26, 76, 20).build());
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        final int x = this.leftPos + this.imageWidth / 2 - 9;
        final int y = this.topPos + this.imageHeight / 2 - 69;
        graphics.fill(x, y, x + 18, y + 18, 0xFF000000 | this.menu.color());
    }
}
