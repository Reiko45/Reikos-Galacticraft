/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.container.GuiCrafting). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.MagneticCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12.2 GuiCrafting: the crafting table screen with the remembered result drawn under the result slot; hovering it
 * says whether a recipe is remembered.
 */
public class MagneticCraftingScreen extends AbstractContainerScreen<MagneticCraftingMenu> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/magnetic_crafting_table.png");

    public MagneticCraftingScreen(MagneticCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.crafting"));
        this.titleLabelX = 28;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isHovering(123, 59, 18, 18, mouseX, mouseY) && this.menu.getCarried().isEmpty()) {
            graphics.renderTooltip(this.font, Component.translatable(this.menu.hasMemory() ? "gui.galacticraftcore.crafting_memory.desc.0"
                    : "gui.galacticraftcore.crafting_memory.desc.1"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
