package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.CargoMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12.2 GuiRocketInventory (textures/gui/rocket_N.png) and GuiParaChest (textures/gui/chest_N.png), N being the
 * number of cargo slots.
 */
public class CargoScreen extends AbstractContainerScreen<CargoMenu> {
    private final ResourceLocation texture;
    private final int size;
    @org.jetbrains.annotations.Nullable
    private net.minecraft.client.gui.components.Button launch;

    public CargoScreen(CargoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.size = menu.cargoSize();
        this.imageWidth = 176;
        if (menu.layout() == CargoMenu.Layout.BUGGY) {
            // 1.12.2 GuiBuggy: the buggy's name over the storage, no inventory label
            this.texture = GalacticraftCore.id("textures/gui/buggy_" + this.size + ".png");
            this.imageHeight = 145 + this.size * 2;
            this.titleLabelX = 8;
            this.titleLabelY = this.size == 0 ? 50 : 39;
            this.inventoryLabelY = -1000;
        } else if (menu.layout() == CargoMenu.Layout.CARGO_ROCKET) {
            // 1.12.2 GuiCargoRocket: the player inventory's name below the cargo grid
            this.texture = GalacticraftCore.id("textures/gui/rocket_" + (this.size - 2) / 18 * 18 + ".png");
            this.imageHeight = this.size <= 3 ? 132 : 145 + this.size * 2;
            this.titleLabelY = -1000;
            this.inventoryLabelX = 8;
            this.inventoryLabelY = this.size == 2 ? 76 : 89 + (this.size - 20) / 9 * 18;
        } else if (menu.layout() == CargoMenu.Layout.ROCKET) {
            this.texture = GalacticraftCore.id("textures/gui/rocket_" + (this.size - 2) / 18 * 18 + ".png");
            this.imageHeight = this.size <= 3 ? 132 : 145 + this.size * 2;
            // 1.12.2 drew the player inventory's name above the cargo grid, not the rocket's (which would cover the gauge)
            this.titleLabelY = -1000;
            this.inventoryLabelX = 8;
            this.inventoryLabelY = 39;
        } else {
            this.texture = GalacticraftCore.id("textures/gui/chest_" + (this.size - 3) / 18 * 18 + ".png");
            this.imageHeight = 146 + this.size * 2;
            this.titleLabelX = 8;
            this.titleLabelY = 6;
            this.inventoryLabelX = 8;
            this.inventoryLabelY = this.imageHeight - 103 + (this.size == 3 ? 2 : 4);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (this.menu.layout() == CargoMenu.Layout.CARGO_ROCKET) {
            this.launch = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.galacticraftcore.message.launch"),
                    button -> {
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                        }
                    }).bounds(this.leftPos + 116, this.topPos + 26, 50, 20).build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.launch != null) {
            this.launch.active = this.menu.value(5) == 0;
            // 1.12.2: the GUI closes itself as a launch that passed its checks begins
            final int cooldown = this.menu.value(3);
            if (this.menu.value(4) != 0 && cooldown > 0 && cooldown < 4) {
                onClose();
            }
        }
    }

    /** 1.12.2 GuiCargoRocket: the status lines (split at '#'), or waiting / launched. */
    private void renderCargoRocketLabels(GuiGraphics graphics) {
        final Component fuel = Component.translatable("gui.galacticraftcore.message.fuel").append(":");
        graphics.drawString(this.font, fuel, 140 - this.font.width(fuel) / 2, 5, 0x404040, false);
        final int percentage = fuelScaled(100);
        final ChatFormatting color = percentage > 80 ? ChatFormatting.GREEN : percentage > 40 ? ChatFormatting.GOLD : ChatFormatting.RED;
        final Component full = Component.literal(percentage + "% ").append(Component.translatable("gui.galacticraftcore.message.full")).withStyle(color);
        graphics.drawString(this.font, full, 140 - this.font.width(full) / 2, 15, 0x404040, false);
        final Component status = Component.translatable("gui.galacticraftcore.status").append(":");
        graphics.drawString(this.font, status, 40 - this.font.width(status) / 2, 9, 0x404040, false);
        final List<Component> lines;
        if (this.menu.value(3) == 0 || this.menu.value(2) == io.github.reiko45.galacticraft.entity.CargoRocketEntity.STATUS_NONE) {
            lines = this.menu.value(5) != 0
                    ? List.of(Component.translatable("gui.galacticraftcore.cargo_rocket.launched.0"), Component.translatable("gui.galacticraftcore.cargo_rocket.launched.1"))
                    : List.of(Component.translatable("gui.galacticraftcore.cargo_rocket.waiting.0"), Component.translatable("gui.galacticraftcore.cargo_rocket.waiting.1"));
        } else {
            lines = switch (this.menu.value(2)) {
                case io.github.reiko45.galacticraft.entity.CargoRocketEntity.STATUS_FREQUENCY_NOT_SET -> List.of(
                        Component.translatable("gui.galacticraftcore.frequency").withStyle(ChatFormatting.RED),
                        Component.translatable("gui.galacticraftcore.message.not_set").withStyle(ChatFormatting.RED));
                case io.github.reiko45.galacticraft.entity.CargoRocketEntity.STATUS_NOT_ENOUGH_FUEL -> List.of(
                        Component.translatable("gui.galacticraftcore.message.not_enough").withStyle(ChatFormatting.RED),
                        Component.translatable("gui.galacticraftcore.message.fuel").withStyle(ChatFormatting.RED));
                default -> List.of(Component.translatable("gui.galacticraftcore.message.success").withStyle(ChatFormatting.GREEN));
            };
        }
        int y = 18;
        for (Component line : lines) {
            final Component styled = line.getStyle().getColor() == null ? line.copy().withStyle(ChatFormatting.YELLOW) : line;
            graphics.drawString(this.font, styled, 35 - this.font.width(styled) / 2, y, 0x404040, false);
            y += 9;
        }
    }

    private int fuelScaled(int scale) {
        return this.menu.fuelCapacity() <= 0 ? 0 : this.menu.fuel() * scale / this.menu.fuelCapacity();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.layout() != CargoMenu.Layout.CHEST) {
            final int level = fuelScaled(38);
            if (level > 0) {
                graphics.blit(this.texture, this.leftPos + (this.menu.layout() != CargoMenu.Layout.BUGGY && this.size == 2 ? 71 : 72), this.topPos + 45 - level, 176, 38 - level, 42, level);
            }
        } else {
            final int level = fuelScaled(28);
            if (level > 0) {
                graphics.blit(this.texture, this.leftPos + 17, this.topPos + (this.size == 3 ? 40 : 42) - level + this.size * 2,
                        176, 28 - level, 34, level);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (this.menu.layout() == CargoMenu.Layout.CARGO_ROCKET) {
            renderCargoRocketLabels(graphics);
        } else if (this.menu.layout() != CargoMenu.Layout.CHEST) {
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.message.fuel"), 8, 5, 0x404040, false);
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.message.fuel").append(":"), 125, 18, 0x404040, false);
            final int percentage = fuelScaled(100);
            final ChatFormatting color = percentage > 80 ? ChatFormatting.GREEN : percentage > 40 ? ChatFormatting.GOLD : ChatFormatting.RED;
            final Component text = Component.literal(percentage + "% ").append(Component.translatable("gui.galacticraftcore.message.full")).withStyle(color);
            graphics.drawString(this.font, text, 117 - text.getString().length() / 2, 28, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (this.menu.layout() != CargoMenu.Layout.CHEST) {
            final int x = mouseX - this.leftPos - (this.menu.layout() != CargoMenu.Layout.BUGGY && this.size == 2 ? 70 : 71);
            final int y = mouseY - this.topPos - 6;
            if (x >= 0 && x < 36 && y >= 0 && y < 40) {
                graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.fuel_tank.desc.0"),
                        Component.translatable("gui.galacticraftcore.fuel_tank.desc.1")), mouseX, mouseY);
            }
        }
    }
}
