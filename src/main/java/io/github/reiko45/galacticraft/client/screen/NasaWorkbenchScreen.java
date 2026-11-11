package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12.2 GuiSchematicTier1Rocket (textures/gui/rocketbench.png). The Back/Next page buttons of 1.12.2 only matter once
 * more schematics (the Moon Buggy) exist, so they are left out for now.
 */
public class NasaWorkbenchScreen extends AbstractContainerScreen<NasaWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/rocketbench.png");

    public NasaWorkbenchScreen(NasaWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 221;
        this.titleLabelX = 7;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 202 - 104 + 2 + 27;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
