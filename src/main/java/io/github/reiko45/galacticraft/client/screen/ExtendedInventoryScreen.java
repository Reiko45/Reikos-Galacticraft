package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.ExtendedInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The Galacticraft inventory tab (1.12.2 GuiExtendedInventory) on the 1.12.2 sheet, with the player drawn on the left. */
public class ExtendedInventoryScreen extends AbstractContainerScreen<ExtendedInventoryMenu> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/inventory.png");

    public ExtendedInventoryScreen(ExtendedInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        // 1.12.2 shows neither the title nor the inventory label
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, this.leftPos + 8, this.topPos + 8, this.leftPos + 58,
                this.topPos + 78, 29, 0.0625F, mouseX, mouseY, this.minecraft.player);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
