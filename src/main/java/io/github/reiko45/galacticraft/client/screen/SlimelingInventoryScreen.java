/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiSlimelingInventory).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.menu.SlimelingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * A Slimeling's inventory (1.12.2 GuiSlimelingInventory): the slimeling, its colour, the bag slot and, with the bag in,
 * the bag's 27 slots. The button on the right goes back to its panel.
 */
public class SlimelingInventoryScreen extends AbstractContainerScreen<SlimelingMenu> {
    private static final ResourceLocation PANEL = GalacticraftCore.id("textures/gui/slimeling_panel2.png");

    public SlimelingInventoryScreen(SlimelingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 210;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int x = this.leftPos + 151;
        final int y = this.topPos + 108;
        final SlimelingEntity slimeling = this.menu.slimeling();
        if (slimeling != null && mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft.player.closeContainer();
            this.minecraft.setScreen(new SlimelingScreen(slimeling));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The darkened background is already drawn (and in the depth buffer) at z 0: the slimeling goes in front of it,
        // the panel in front of the slimeling, and the slots' items (at +150) in front of the panel.
        final SlimelingEntity slimeling = this.menu.slimeling();
        if (slimeling != null) {
            SlimelingScreen.drawSlimeling(graphics, slimeling, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, mouseX, mouseY, 1.0F);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        graphics.blit(PANEL, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (slimeling != null) {
            SlimelingScreen.drawColors(graphics, this.font, PANEL, slimeling, this.leftPos, this.topPos);
        }
        graphics.blit(PANEL, this.leftPos + 151, this.topPos + 108, 176, 27, 18, 18);
        graphics.blit(PANEL, this.leftPos + 8, this.topPos + 8, 176, 9, 18, 18);
        graphics.blit(PANEL, this.leftPos + 8, this.topPos + 29, 176, 9, 18, 18);
        if (this.menu.hasBag()) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    graphics.blit(PANEL, this.leftPos + 7 + col * 18, this.topPos + 53 + row * 18, 176, 9, 18, 18);
                }
            }
        }
        graphics.pose().popPose();
    }

    /** 1.12.2 drew no titles here. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
