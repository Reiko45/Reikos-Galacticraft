/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.gui.GuiGeothermal). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.GeothermalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.menu.GeothermalGeneratorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiGeothermal: output, status (spout missing), a flickering flame while it has a spout, Enable/Disable. */
public class GeothermalGeneratorScreen extends MachineScreen<GeothermalGeneratorMenu> {
    private Button toggle;
    private int ticks;

    public GeothermalGeneratorScreen(GeothermalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "geothermal", 176, 201);
        energyTooltipRegion(96, 24, 56, 9);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 7;
        this.toggle = addRenderableWidget(Button.builder(buttonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, GeothermalGeneratorMenu.BUTTON_TOGGLE);
            }
        }).bounds(this.width / 2 - 36, this.height / 2 - 19, 72, 20).build());
    }

    private Component buttonLabel() {
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.enable" : "gui.galacticraftcore.button.disable");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
        this.toggle.setMessage(buttonLabel());
        this.toggle.active = !this.menu.coolingDown();
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 83, this.topPos + 24, 176, 0, 11, 10);
        }
        if (this.menu.hasValidSpout()) {
            // the flame: seven frames of 16 px, three ticks each, as tall as the output is high
            final int height = 16 * this.menu.generating() / (GeothermalGeneratorBlockEntity.MAX_GENERATE_GJ_PER_TICK - 1);
            graphics.blit(this.texture, this.leftPos + 33, this.topPos + 21 + (16 - height), 176, 10 + this.ticks / 3 % 7 * 16 + (16 - height), 46,
                    height);
        }
        blitHorizontal(graphics, 97, 25, 187, 0, 54, 7, energyFraction());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final int generating = this.menu.generating();
        centered(graphics, Component.translatable("gui.galacticraftcore.generating").append(": ")
                .append(generating > 0 ? Component.literal(EnergyDisplay.format(generating) + "/t")
                        : Component.translatable("gui.galacticraftcore.status.not_generating")), 46);
        centered(graphics, Component.translatable("gui.galacticraftcore.status").append(": ").append(status()), 57);
        if (!this.menu.isDisabled() && generating <= 0 && !this.menu.hasValidSpout()) {
            centered(graphics, Component.translatable("gui.galacticraftcore.status.invalid_spout_2").withStyle(ChatFormatting.RED), 68);
        }
    }

    /** 1.12.2 GuiGeothermal#getStatus. */
    private Component status() {
        if (this.menu.isDisabled()) {
            return Component.translatable("gui.galacticraftcore.status.disabled").withStyle(ChatFormatting.GOLD);
        }
        if (this.menu.generating() > 0) {
            return Component.translatable("gui.galacticraftcore.status.collecting_energy").withStyle(ChatFormatting.DARK_GREEN);
        }
        if (!this.menu.hasValidSpout()) {
            return Component.translatable("gui.galacticraftcore.status.invalid_spout_1").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.galacticraftcore.status.unknown").withStyle(ChatFormatting.GOLD);
    }

    private void centered(GuiGraphics graphics, Component text, int y) {
        graphics.drawString(this.font, text, (this.imageWidth - this.font.width(text)) / 2, y, 0x404040, false);
    }
}
