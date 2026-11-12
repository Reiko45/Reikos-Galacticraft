/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiSchematicCargoRocket).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.CargoRocketBenchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Cargo Rocket page: 1.12.2 schematic_rocket_cargo.png, 220 px tall. */
public class CargoRocketBenchScreen extends SchematicPageScreen<CargoRocketBenchMenu> {
    public CargoRocketBenchScreen(CargoRocketBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GalacticraftCore.id("textures/gui/schematic_rocket_cargo.png"), 220);
        this.titleLabelX = 7;
        this.titleLabelY = -20 + 27;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 220 - 104 + 2 + 9;
    }
}
