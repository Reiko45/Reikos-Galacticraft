/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiTerraformer). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity;
import io.github.reiko45.galacticraft.menu.TerraformerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * Terraformer GUI (1.12.2 GuiTerraformer): water gauge, energy bar, the status on the right, Enable / Disable Trees and
 * Grass buttons and the Show Bubble checkbox.
 */
public class TerraformerScreen extends MachineScreen<TerraformerMenu> {
    private static final ResourceLocation WIDGETS = GalacticraftCore.id("textures/gui/gui.png");
    private static final int LABEL = 0x404040;
    private Button trees;
    private Button grass;

    public TerraformerScreen(TerraformerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "terraformer", 176, 237);
        energyTooltipRegion(44, 47, 52, 9);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 5;
        this.inventoryLabelY = 144;
        this.trees = addRenderableWidget(Button.builder(label(true), button -> press(TerraformerMenu.BUTTON_TREES))
                .bounds(this.leftPos + 98, this.topPos + 85, 72, 20).build());
        this.grass = addRenderableWidget(Button.builder(label(false), button -> press(TerraformerMenu.BUTTON_GRASS))
                .bounds(this.leftPos + 98, this.topPos + 109, 72, 20).build());
    }

    private Component label(boolean trees) {
        final boolean disabled = trees ? this.menu.treesDisabled() : this.menu.grassDisabled();
        return Component.translatable(disabled ? "gui.galacticraftcore.button.enable" : "gui.galacticraftcore.button.disable").append(" ")
                .append(Component.translatable(trees ? "gui.galacticraftcore.message.trees" : "gui.galacticraftcore.message.grass"));
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.trees.setMessage(label(true));
        this.grass.setMessage(label(false));
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        blitHorizontal(graphics, 45, 48, 176, 26, 54, 7, energyFraction());
        final int level = this.menu.water() * 26 / TerraformerBlockEntity.WATER_CAPACITY;
        if (level > 0) {
            graphics.blit(this.texture, this.leftPos + 56, this.topPos + 17 + 27 - level, 176, 26 - level, 39, level);
        }
        // 1.12.2 GuiElementCheckbox: 13x13 at (20, 24) of gui.png, the ticked box 13 to the right
        graphics.blit(WIDGETS, this.leftPos + 85, this.topPos + 132, this.menu.bubbleVisible() ? 33 : 20, 24, 13, 13);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int y = 24;
        for (FormattedCharSequence line : this.font.split(this.menu.status(), this.imageWidth - 105)) {
            graphics.drawString(this.font, line, 105, y, LABEL, false);
            y += this.font.lineHeight;
        }
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.bubble_visible"), 85 + 13 + 3, 132 + 3, LABEL, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        if (x >= 85 && x < 98 && y >= 132 && y < 145) {
            press(TerraformerMenu.BUTTON_BUBBLE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        if (x >= 55 && x < 96 && y >= 17 && y < 45) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.water_tank")
                    .append(": " + this.menu.water() * 100 / TerraformerBlockEntity.WATER_CAPACITY + "%")), mouseX, mouseY);
        } else if (x >= 85 && x < 170 && y >= 132 && y < 145) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.show_bubble")), mouseX, mouseY);
        }
    }
}
