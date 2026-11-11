package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.BuggyBenchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiSchematicBuggy (textures/gui/buggybench.png). */
public class BuggyBenchScreen extends SchematicPageScreen<BuggyBenchMenu> {
    public BuggyBenchScreen(BuggyBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GalacticraftCore.id("textures/gui/buggybench.png"), 221);
        this.titleLabelX = 7;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 202 - 104 + 2 + 27;
    }
}
