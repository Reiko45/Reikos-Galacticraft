package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.menu.SolarPanelMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/** 1.12.2 GuiSolar: generation, status, environmental boost, sun indicator and an Enable/Disable button. */
public class SolarPanelScreen extends MachineScreen<SolarPanelMenu> {
    private Button toggle;

    public SolarPanelScreen(SolarPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "solar", 176, 201);
        energyTooltipRegion(96, 24, 56, 9);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 7;
        this.toggle = addRenderableWidget(Button.builder(buttonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, SolarPanelMenu.BUTTON_TOGGLE);
            }
        }).bounds(this.width / 2 - 36, this.height / 2 - 19, 72, 20).build());
    }

    private Component buttonLabel() {
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.enable" : "gui.galacticraftcore.button.disable");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.toggle.setMessage(buttonLabel());
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 83, this.topPos + 24, 176, 0, 11, 10);
        }
        if (this.menu.strength() > 0) {
            graphics.blit(this.texture, this.leftPos + 48, this.topPos + 21, 176, 10, 16, 16);
        }
        blitHorizontal(graphics, 97, 25, 187, 0, 54, 7, energyFraction());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        if (x >= 47 && x < 65 && y >= 20 && y < 38) {
            final int strength = this.menu.strength();
            graphics.renderComponentTooltip(this.font, List.of(strength > 0
                    ? Component.translatable("gui.galacticraftcore.status.sun_visible").append(": " + Math.round(strength / 9.0F * 100) + "%")
                    : Component.translatable("gui.galacticraftcore.status.blocked_fully")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final int generating = this.menu.generating();
        centered(graphics, Component.translatable("gui.galacticraftcore.generating").append(": ")
                .append(generating > 0 ? Component.literal(EnergyDisplay.format(generating) + "/t")
                        : Component.translatable("gui.galacticraftcore.status.not_generating")), 46);
        centered(graphics, Component.translatable("gui.galacticraftcore.status").append(": ").append(status()), 57);
        final Level level = this.minecraft.level;
        final int boost = Math.round((SolarPanelBlockEntity.solarBoost(level) - 1.0F) * 100.0F);
        centered(graphics, Component.translatable("gui.galacticraftcore.environment").append(": " + boost + "%"), 68);
    }

    /** 1.12.2 GuiSolar#getStatus. */
    private Component status() {
        final Level level = this.minecraft.level;
        if (this.menu.isDisabled()) {
            return Component.translatable("gui.galacticraftcore.status.disabled").withStyle(ChatFormatting.GOLD);
        }
        if (!level.isDay()) {
            return Component.translatable("gui.galacticraftcore.status.blocked_fully").withStyle(ChatFormatting.DARK_RED);
        }
        if (level.isRaining() || level.isThundering()) {
            return Component.translatable("gui.galacticraftcore.status.raining").withStyle(ChatFormatting.DARK_RED);
        }
        if (this.menu.strength() == 0) {
            return Component.translatable("gui.galacticraftcore.status.blocked_fully").withStyle(ChatFormatting.DARK_RED);
        }
        if (this.menu.strength() < 9) {
            return Component.translatable("gui.galacticraftcore.status.blocked_partial").withStyle(ChatFormatting.DARK_RED);
        }
        if (this.menu.generating() > 0) {
            return Component.translatable("gui.galacticraftcore.status.collecting_energy").withStyle(ChatFormatting.DARK_GREEN);
        }
        return Component.translatable("gui.galacticraftcore.status.unknown").withStyle(ChatFormatting.GOLD);
    }

    private void centered(GuiGraphics graphics, Component text, int y) {
        graphics.drawString(this.font, text, (this.imageWidth - this.font.width(text)) / 2, y, 0x404040, false);
    }
}
