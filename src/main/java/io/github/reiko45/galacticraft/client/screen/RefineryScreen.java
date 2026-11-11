package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.menu.RefineryMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiRefinery: oil and fuel gauges, energy bar, status and a Refine/Stop button. */
public class RefineryScreen extends MachineScreen<RefineryMenu> {
    private Button toggle;

    public RefineryScreen(RefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "refinery", 176, 168);
        energyTooltipRegion(62, 16, 56, 9);
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 88 - this.font.width(this.title) / 2;
        this.toggle = addRenderableWidget(Button.builder(buttonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, RefineryMenu.BUTTON_TOGGLE);
            }
        }).bounds(this.width / 2 - 39, this.height / 2 - 56, 76, 20).build());
    }

    private Component buttonLabel() {
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.refine" : "gui.galacticraftcore.button.stop_refine");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.toggle.setMessage(buttonLabel());
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        final int oil = this.menu.oil() * 38 / RefineryBlockEntity.TANK_CAPACITY;
        if (oil > 0) {
            graphics.blit(this.texture, this.leftPos + 7, this.topPos + 66 - oil, 176, 38 - oil, 16, oil);
        }
        final int fuel = this.menu.fuel() * 38 / RefineryBlockEntity.TANK_CAPACITY;
        if (fuel > 0) {
            graphics.blit(this.texture, this.leftPos + 153, this.topPos + 66 - fuel, 192, 38 - fuel, 16, fuel);
        }
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 49, this.topPos + 16, 208, 0, 11, 10);
        }
        blitHorizontal(graphics, 63, 17, 176, 38, 54, 7, energyFraction());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        if (x >= 7 && x < 23 && y >= 28 && y < 66) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.oil")
                    .append(": " + this.menu.oil() + " / " + RefineryBlockEntity.TANK_CAPACITY)), mouseX, mouseY);
        } else if (x >= 153 && x < 169 && y >= 28 && y < 66) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.fuel")
                    .append(": " + this.menu.fuel() + " / " + RefineryBlockEntity.TANK_CAPACITY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.status").append(": "), 60, 50, 0x404040, false);
        graphics.drawString(this.font, this.menu.status().text(), 60, 61, 0x404040, false);
    }
}
