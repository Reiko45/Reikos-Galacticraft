package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.client.screen.ExtendedInventoryScreen;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Inventory tabs above the survival inventory and the Galacticraft inventory (1.12.2 TabRegistry with
 * InventoryTabVanilla and InventoryTabGalacticraft), drawn with the creative-tab sprites.
 */
public final class InventoryTabs {
    private static final ResourceLocation SELECTED = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1");
    private static final ResourceLocation UNSELECTED = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1");

    private InventoryTabs() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(new Tab(screen, 0, true, new ItemStack(Items.CRAFTING_TABLE), Component.translatable("container.crafting"), () -> {
            }));
            event.addListener(new Tab(screen, 1, false, new ItemStack(GCItems.OXYGEN_MASK.get()),
                    Component.translatable("gui.galacticraftcore.galacticraft_inventory"),
                    () -> PacketDistributor.sendToServer(new GCNetwork.OpenExtendedInventory())));
        } else if (event.getScreen() instanceof ExtendedInventoryScreen screen) {
            event.addListener(new Tab(screen, 0, false, new ItemStack(Items.CRAFTING_TABLE), Component.translatable("container.crafting"), () -> {
                final Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.closeContainer();
                    minecraft.setScreen(new InventoryScreen(minecraft.player));
                }
            }));
            event.addListener(new Tab(screen, 1, true, new ItemStack(GCItems.OXYGEN_MASK.get()),
                    Component.translatable("gui.galacticraftcore.galacticraft_inventory"), () -> {
            }));
        }
    }

    /** A 28x32 tab sitting on top of the screen's panel; follows the panel when the recipe book moves it. */
    private static final class Tab extends AbstractButton {
        private final AbstractContainerScreen<?> screen;
        private final int index;
        private final boolean selected;
        private final ItemStack icon;
        private final Runnable action;

        Tab(AbstractContainerScreen<?> screen, int index, boolean selected, ItemStack icon, Component tooltip, Runnable action) {
            super(screen.getGuiLeft() + index * 29, screen.getGuiTop() - 28, 26, 32, tooltip);
            this.screen = screen;
            this.index = index;
            this.selected = selected;
            this.icon = icon;
            this.action = action;
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
        }

        @Override
        public void onPress() {
            if (!this.selected) {
                this.action.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setX(this.screen.getGuiLeft() + this.index * 29);
            setY(this.screen.getGuiTop() - 28);
            // The selected tab runs into the panel; an unselected one stops at the panel's edge (creative-tab style)
            if (this.selected) {
                graphics.blitSprite(SELECTED, getX(), getY(), 26, 32);
            } else {
                graphics.blitSprite(UNSELECTED, 26, 32, 0, 0, getX(), getY(), 26, 28);
            }
            graphics.renderItem(this.icon, getX() + 5, getY() + 9);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
