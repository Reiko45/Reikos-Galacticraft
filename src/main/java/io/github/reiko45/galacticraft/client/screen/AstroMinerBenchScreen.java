/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.gui.GuiSchematicAstroMiner).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.AstroMinerBenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiSchematicAstroMiner (planets textures/gui/schematic_astro_miner.png, drawn from 26 rows down). */
public class AstroMinerBenchScreen extends SchematicPageScreen<AstroMinerBenchMenu> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/schematic_astro_miner.png");

    public AstroMinerBenchScreen(AstroMinerBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, 221);
        this.titleLabelX = 7;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 220 - 104 + 2 - 16;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 26, this.imageWidth, this.imageHeight);
    }
}
