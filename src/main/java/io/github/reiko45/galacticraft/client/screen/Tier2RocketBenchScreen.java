package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.Tier2RocketBenchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 GuiSchematicTier2Rocket (planets textures/gui/schematic_rocket_t2.png, 238 tall). */
public class Tier2RocketBenchScreen extends SchematicPageScreen<Tier2RocketBenchMenu> {
    public Tier2RocketBenchScreen(Tier2RocketBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GalacticraftCore.id("textures/gui/schematic_rocket_t2.png"), 238);
        this.titleLabelX = 7;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 220 - 104 + 2 + 27;
    }
}
