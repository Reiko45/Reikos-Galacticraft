package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.menu.MachineMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Base screen for machines, drawing the 1.12.2 GUI sheets ({@code textures/gui/*.png}, 256x256) unchanged:
 * the panel at (0,0) and progress/energy sprites from the same sheet.
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    protected final ResourceLocation texture;
    private final List<Region> energyRegions = new ArrayList<>();

    private record Region(int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    protected MachineScreen(M menu, Inventory playerInventory, Component title, String textureName, int width, int height) {
        super(menu, playerInventory, title);
        this.texture = GalacticraftCore.id("textures/gui/" + textureName + ".png");
        this.imageWidth = width;
        this.imageHeight = height;
        this.inventoryLabelY = height - 94;
    }

    /** Hovering this GUI-relative rectangle shows the energy tooltip. */
    protected void energyTooltipRegion(int x, int y, int width, int height) {
        this.energyRegions.add(new Region(x, y, width, height));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (Region region : this.energyRegions) {
            if (region.contains(mouseX - this.leftPos, mouseY - this.topPos)) {
                graphics.renderComponentTooltip(this.font, energyTooltip(), mouseX, mouseY);
            }
        }
    }

    protected List<Component> energyTooltip() {
        return List.of(EnergyDisplay.energy(this.menu.energy()), EnergyDisplay.maxEnergy(this.menu.capacity()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        renderOverlays(graphics);
    }

    /** Draw bars and arrows on top of the panel. */
    protected abstract void renderOverlays(GuiGraphics graphics);

    /** Blit a sprite from the sheet, cropped to {@code fraction} of its width (left to right). */
    protected void blitHorizontal(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, float fraction) {
        final int w = Math.round(width * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (w > 0) {
            graphics.blit(this.texture, this.leftPos + x, this.topPos + y, u, v, w, height);
        }
    }

    /** Blit a sprite from the sheet, cropped to {@code fraction} of its height (bottom to top). */
    protected void blitVertical(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, float fraction) {
        final int h = Math.round(height * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (h > 0) {
            graphics.blit(this.texture, this.leftPos + x, this.topPos + y + height - h, u, v + height - h, width, h);
        }
    }

    protected float energyFraction() {
        return this.menu.capacity() <= 0 ? 0.0F : this.menu.energy() / this.menu.capacity();
    }
}
