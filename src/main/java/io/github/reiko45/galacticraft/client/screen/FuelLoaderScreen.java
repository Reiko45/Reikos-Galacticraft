package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.menu.FuelLoaderMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12.2 GuiFuelLoader: fuel gauge, energy bar, status and a Load Fuel / Stop Loading button. The 1.12.2 sheet was
 * drawn 5 px below the GUI's top edge, which is kept here.
 */
public class FuelLoaderScreen extends MachineScreen<FuelLoaderMenu> {
    private Button toggle;

    public FuelLoaderScreen(FuelLoaderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "fuel_loader", 176, 180);
        energyTooltipRegion(112, 65, 56, 9);
        this.titleLabelX = 60;
        this.titleLabelY = 10;
        this.inventoryLabelY = 75;
    }

    @Override
    protected void init() {
        super.init();
        this.toggle = addRenderableWidget(Button.builder(buttonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, FuelLoaderMenu.BUTTON_TOGGLE);
            }
        }).bounds(this.width / 2 + 2, this.height / 2 - 49, 76, 20).build());
    }

    private Component buttonLabel() {
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.load_fuel" : "gui.galacticraftcore.button.stop_loading");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.toggle.setMessage(buttonLabel());
        this.toggle.active = this.menu.canToggle() && this.menu.fuel() > 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.texture, this.leftPos, this.topPos + 5, 0, 0, this.imageWidth, 181);
        renderOverlays(graphics);
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        final int fuel = this.menu.fuel() * 38 / FuelLoaderBlockEntity.TANK_CAPACITY;
        if (fuel > 0) {
            graphics.blit(this.texture, this.leftPos + 7, this.topPos + 71 - fuel, 176, 38 - fuel, 16, fuel);
        }
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 99, this.topPos + 65, 192, 7, 11, 10);
        }
        blitHorizontal(graphics, 113, 66, 192, 0, 54, 7, energyFraction());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        if (x >= 7 && x < 23 && y >= 33 && y < 71) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.galacticraftcore.fuel_tank.desc.2"),
                    Component.translatable("gui.galacticraftcore.fuel_tank.desc.3"),
                    Component.translatable("gui.galacticraftcore.fuel").append(": " + this.menu.fuel() + " / " + FuelLoaderBlockEntity.TANK_CAPACITY)),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.status").append(": ").append(this.menu.status().text()),
                28, 22, 0x404040, false);
    }
}
