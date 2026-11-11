package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiSchematicTier1Rocket (textures/gui/rocketbench.png). */
public class NasaWorkbenchScreen extends SchematicPageScreen<NasaWorkbenchMenu> {
    public NasaWorkbenchScreen(NasaWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GalacticraftCore.id("textures/gui/rocketbench.png"), 221);
        this.titleLabelX = 7;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 202 - 104 + 2 + 27;
    }
}
