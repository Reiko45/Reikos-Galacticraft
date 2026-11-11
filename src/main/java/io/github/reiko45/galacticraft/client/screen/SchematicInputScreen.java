package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.SchematicInputMenu;
import io.github.reiko45.galacticraft.menu.Schematics;
import io.github.reiko45.galacticraft.network.GCNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.12.2 GuiSchematicInput (textures/gui/schematicpage.png): the schematic slot with its explanation, and the Unlock
 * Schematic button. The 1.12.2 GUI was 166 high with the texture's top 27 pixels above it; here the GUI is the texture.
 */
public class SchematicInputScreen extends SchematicPageScreen<SchematicInputMenu> {
    public SchematicInputScreen(SchematicInputMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GalacticraftCore.id("textures/gui/schematicpage.png"), 220);
        this.titleLabelX = 7;
        this.titleLabelY = -22 + 27;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 56 + 27;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.unlock_schematic"),
                button -> PacketDistributor.sendToServer(new GCNetwork.SchematicAction(Schematics.ACTION_UNLOCK)))
                .bounds(this.width / 2 - 92 / 2, this.topPos + 27 + 83 - 52, 92, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos - 79;
        final int y = mouseY - this.topPos - 27;
        if (x >= 0 && x < 18 && y >= 0 && y < 18 && this.menu.getCarried().isEmpty() && !this.menu.getSlot(0).hasItem()) {
            final List<Component> lines = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                lines.add(Component.translatable("gui.galacticraftcore.new_schematic.slot.desc." + i));
            }
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }
}
