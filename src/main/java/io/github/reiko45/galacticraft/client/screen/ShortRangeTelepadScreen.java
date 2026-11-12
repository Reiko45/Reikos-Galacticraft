/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.gui.GuiShortRangeTelepad and
 * tile.TileEntityShortRangeTelepad#getReceivingStatus / getSendingStatus). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import io.github.reiko45.galacticraft.menu.ShortRangeTelepadMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Short Range Telepad GUI: its address (green when it is its own, red when taken), the target's address (green when it
 * can be reached, orange when switched off, red otherwise), Enable / Disable, and how it stands for receiving and
 * sending. The owner's typing goes to the server as it changes.
 */
public class ShortRangeTelepadScreen extends MachineScreen<ShortRangeTelepadMenu> {
    private static final int LABEL = 0x404040;

    private EditBox address;
    private EditBox target;
    private Button enable;
    private int lastAddress = Integer.MIN_VALUE;
    private int lastTarget = Integer.MIN_VALUE;

    public ShortRangeTelepadScreen(ShortRangeTelepadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "short_range_telepad", 176, 209);
        energyTooltipRegion(98, 113, 52, 9);
        this.inventoryLabelY = 115;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -1000;
        this.address = numberBox(66, 16);
        this.address.setResponder(text -> send(ShortRangeTelepadMenu.SET_ADDRESS, text, this.address));
        this.target = numberBox(122, 38);
        this.target.setResponder(text -> send(ShortRangeTelepadMenu.SET_TARGET, text, this.target));
        this.enable = addRenderableWidget(Button.builder(Component.empty(), b -> press(ShortRangeTelepadMenu.BUTTON_ENABLE))
                .bounds(this.leftPos + 70 + 124 - 72, this.topPos + 16, 48, 20).build());
        refresh();
    }

    private EditBox numberBox(int x, int y) {
        final EditBox box = new EditBox(this.font, this.leftPos + x, this.topPos + y, 48, 20, Component.empty());
        box.setMaxLength(6);
        box.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        box.setEditable(this.menu.isOwner());
        return addRenderableWidget(box);
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void send(int base, String text, EditBox box) {
        if (!this.menu.isOwner() || !box.isFocused()) {
            return;
        }
        int value = -1;
        try {
            value = text.isEmpty() ? -1 : Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            // the filter only lets digits through
        }
        press(base + value + 1);
    }

    private void refresh() {
        if (!this.address.isFocused() && this.menu.address() != this.lastAddress) {
            this.lastAddress = this.menu.address();
            this.address.setValue(String.valueOf(this.lastAddress));
        }
        if (!this.target.isFocused() && this.menu.targetAddress() != this.lastTarget) {
            this.lastTarget = this.menu.targetAddress();
            this.target.setValue(String.valueOf(this.lastTarget));
        }
        this.address.setTextColor(this.menu.addressValid() ? 0xFF14FF14 : 0xFFFF1919);
        this.target.setTextColor(switch (this.menu.targetResult()) {
            case VALID -> 0xFF14FF14;
            case TARGET_DISABLED -> 0xFFFFAA19;
            default -> 0xFFFF1919;
        });
        this.enable.setMessage(Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.enable" : "gui.galacticraftcore.button.disable"));
        this.enable.active = this.menu.canToggle();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.menu.energy() > 0) {
            blitHorizontal(graphics, 99, 114, 176, 0, 54, 7, energyFraction());
        }
    }

    /** 1.12.2 getReceivingStatus. */
    private Component receivingStatus() {
        if (!this.menu.addressValid()) {
            return Component.translatable("gui.galacticraftcore.telepad.invalid_address").withStyle(ChatFormatting.RED);
        }
        return energyOrActive("gui.galacticraftcore.telepad.receiving_active");
    }

    /** 1.12.2 getSendingStatus. */
    private Component sendingStatus() {
        if (!this.menu.addressValid()) {
            return Component.translatable("gui.galacticraftcore.telepad.invalid_target_address").withStyle(ChatFormatting.RED);
        }
        return switch (this.menu.targetResult()) {
            case TOO_FAR -> Component.translatable("gui.galacticraftcore.telepad.too_far", ShortRangeTelepadBlockEntity.TELEPORTER_RANGE).withStyle(ChatFormatting.RED);
            case WRONG_DIM -> Component.translatable("gui.galacticraftcore.telepad.wrong_dim").withStyle(ChatFormatting.RED);
            case NOT_FOUND -> Component.translatable("gui.galacticraftcore.telepad.not_found").withStyle(ChatFormatting.RED);
            case TARGET_DISABLED -> Component.translatable("gui.galacticraftcore.telepad.target_disabled").withStyle(ChatFormatting.GOLD);
            case VALID -> energyOrActive("gui.galacticraftcore.telepad.sending_active");
        };
    }

    private Component energyOrActive(String active) {
        if (this.menu.energy() <= 0) {
            return Component.translatable("gui.galacticraftcore.telepad.no_energy").withStyle(ChatFormatting.RED);
        }
        if (this.menu.energy() <= ShortRangeTelepadBlockEntity.ENERGY_USE_ON_TELEPORT) {
            return Component.translatable("gui.galacticraftcore.telepad.not_enough_energy").withStyle(ChatFormatting.RED);
        }
        if (this.menu.isDisabled()) {
            return Component.translatable("gui.galacticraftcore.status.disabled").withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable(active).withStyle(ChatFormatting.GREEN);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 5, LABEL, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.telepad.address").append(":"), 7, 22, LABEL, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.telepad.dest_address").append(":"), 7, 44, LABEL, false);
        final Component receiving = receivingStatus();
        final Component sending = sendingStatus();
        graphics.drawString(this.font, receiving, 7, 66, LABEL, false);
        if (!receiving.getString().equals(sending.getString())) {
            graphics.drawString(this.font, sending, 7, 88, LABEL, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        final EditBox focused = this.address.isFocused() ? this.address : this.target.isFocused() ? this.target : null;
        if (focused != null && keyCode != 256) {
            return focused.keyPressed(keyCode, scanCode, modifiers) || focused.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        final List<Component> tooltip = new ArrayList<>();
        if (x >= 151 && x < 169 && y >= 104 && y < 122 && this.menu.getCarried().isEmpty()) {
            tooltip.add(Component.translatable("gui.galacticraftcore.battery_slot.desc.0"));
            tooltip.add(Component.translatable("gui.galacticraftcore.battery_slot.desc.1"));
        } else if (x >= 5 && x < 64 && y >= 20 && y < 33) {
            lines(tooltip, "gui.galacticraftcore.telepad.desc.0");
        } else if (x >= 5 && x < 122 && y >= 42 && y < 55) {
            lines(tooltip, "gui.galacticraftcore.telepad.desc.1");
        }
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /** 1.12.2 translateWithSplit: "$" breaks lines. */
    private static void lines(List<Component> tooltip, String key) {
        for (String line : Component.translatable(key).getString().split("\\$")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.WHITE));
        }
    }
}
