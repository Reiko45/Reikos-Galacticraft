package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.menu.SchematicPageMenu;
import io.github.reiko45.galacticraft.menu.Schematics;
import io.github.reiko45.galacticraft.network.GCNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A NASA Workbench page with the 1.12.2 Back and Next buttons to the left of it (width / 2 - 130, height / 2 - 110),
 * greyed out where there is no page to go to.
 */
public abstract class SchematicPageScreen<T extends SchematicPageMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation texture;
    private Button back;
    private Button next;

    protected SchematicPageScreen(T menu, Inventory playerInventory, Component title, ResourceLocation texture, int imageHeight) {
        super(menu, playerInventory, title);
        this.texture = texture;
        this.imageWidth = 176;
        this.imageHeight = imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        this.back = addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.back"),
                button -> PacketDistributor.sendToServer(new GCNetwork.SchematicAction(Schematics.ACTION_BACK)))
                .bounds(this.width / 2 - 130, this.height / 2 - 110, 40, 20).build());
        this.next = addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.next"),
                button -> PacketDistributor.sendToServer(new GCNetwork.SchematicAction(Schematics.ACTION_NEXT)))
                .bounds(this.width / 2 - 130, this.height / 2 - 110 + 25, 40, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.back.active = this.menu.hasPrevious();
        this.next.active = this.menu.hasNext();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The 1.12.2 Tier 1 and buggy pages drew 220 rows of their 221 tall GUIs; the Tier 2 page draws all 238.
        graphics.blit(this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight > 230 ? this.imageHeight : Math.min(this.imageHeight, 220));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
