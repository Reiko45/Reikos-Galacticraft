/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.container.GuiCargoLoader and
 * GuiCargoUnloader). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.menu.CargoLoaderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Cargo Loader / Unloader GUI (both 1.12.2 GUIs draw cargo_loader.png, 5 px down): status, Load / Unload button, energy bar. */
public class CargoLoaderScreen extends MachineScreen<CargoLoaderMenu> {
    private Button toggle;

    public CargoLoaderScreen(CargoLoaderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "cargo_loader", 176, 201);
        energyTooltipRegion(107, 101, 56, 9);
        this.titleLabelX = 60;
        this.titleLabelY = 12;
        this.inventoryLabelY = 201 - 90;
    }

    @Override
    protected void init() {
        super.init();
        this.toggle = addRenderableWidget(Button.builder(buttonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CargoLoaderMenu.BUTTON_TOGGLE);
            }
        }).bounds(this.width / 2 - 1, this.height / 2 - 23, 76, 20).build());
    }

    private Component buttonLabel() {
        if (this.menu.loads()) {
            return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.load_items" : "gui.galacticraftcore.button.stop_loading");
        }
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.unload_items" : "gui.galacticraftcore.button.stop_unloading");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.toggle.setMessage(buttonLabel());
        this.toggle.active = this.menu.canToggle();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.texture, this.leftPos, this.topPos + 5, 0, 0, this.imageWidth, this.imageHeight);
        renderOverlays(graphics);
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 94, this.topPos + 101, 176, 0, 11, 10);
        }
        blitHorizontal(graphics, 108, 102, 187, 0, 54, 7, energyFraction());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.status").append(": ").append(this.menu.status().text()),
                28 - 17, 45 + 23 - 46 + 45, 0x404040, false);
    }
}
