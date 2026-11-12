/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiLaunchController,
 * GuiLaunchControllerAdvanced and core.client.gui.element.GuiElementDropdown / GuiElementCheckbox). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity;
import io.github.reiko45.galacticraft.menu.LaunchControllerMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

/**
 * Launch Controller GUI. The main page: this controller's frequency, the target frequency (shown scrambled to anyone but
 * the owner while hidden), Enable / Disable, Hide / Unhide and Advanced. The advanced page: Remove Pad On Launch, Auto
 * Launch When and the launch criterion dropdown. Values the owner types go to the server as they change.
 */
public class LaunchControllerScreen extends MachineScreen<LaunchControllerMenu> {
    private static final ResourceLocation WIDGETS = GalacticraftCore.id("textures/gui/gui.png");
    private static final int LABEL = 0x404040;
    private static final int DROPDOWN_X = 52;
    private static final int DROPDOWN_Y = 52;
    private static final int OPTION_HEIGHT = 13;

    private boolean advanced;
    private EditBox frequency;
    private EditBox destination;
    private Button enable;
    private Button hide;
    private Button openAdvanced;
    private Button back;
    private boolean dropdownOpen;
    private int dropdownWidth;
    private int lastFrequency = Integer.MIN_VALUE;
    private int lastDestination = Integer.MIN_VALUE;
    private int cannotEditTimer;

    public LaunchControllerScreen(LaunchControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "launch_controller", 176, 209);
        energyTooltipRegion(98, 113, 52, 9);
        this.inventoryLabelY = 115;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -1000;
        this.frequency = numberBox(66, 16);
        this.frequency.setResponder(text -> send(LaunchControllerMenu.SET_FREQUENCY, text, true));
        this.destination = numberBox(45, 38);
        this.destination.setResponder(text -> send(LaunchControllerMenu.SET_DEST_FREQUENCY, text, false));
        this.enable = addRenderableWidget(Button.builder(Component.empty(), b -> press(LaunchControllerMenu.BUTTON_ENABLE))
                .bounds(this.leftPos + 70 + 124 - 72, this.topPos + 16, 48, 20).build());
        this.hide = addRenderableWidget(Button.builder(Component.empty(), b -> press(LaunchControllerMenu.BUTTON_HIDE_DESTINATION))
                .bounds(this.leftPos + 95, this.topPos + 38, 39, 20).build());
        this.openAdvanced = addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.launch_controller.advanced").append("..."),
                b -> setAdvanced(true)).bounds(this.leftPos + 48, this.topPos + 62, 80, 20).build());
        this.back = addRenderableWidget(Button.builder(Component.literal("<"), b -> setAdvanced(false))
                .bounds(this.leftPos + 5, this.topPos + 5, 20, 20).build());
        this.dropdownWidth = 0;
        for (LaunchControllerBlockEntity.AutoLaunch option : LaunchControllerBlockEntity.AutoLaunch.values()) {
            this.dropdownWidth = Math.max(this.dropdownWidth, this.font.width(Component.translatable(option.translationKey())) + 8 + 15);
        }
        setAdvanced(this.advanced);
        refresh();
    }

    private EditBox numberBox(int x, int y) {
        final EditBox box = new EditBox(this.font, this.leftPos + x, this.topPos + y, 48, 20, Component.empty());
        box.setMaxLength(6);
        box.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        box.setEditable(this.menu.isOwner());
        return addRenderableWidget(box);
    }

    private void setAdvanced(boolean advanced) {
        this.advanced = advanced;
        this.dropdownOpen = false;
        this.frequency.visible = !advanced;
        this.destination.visible = !advanced;
        this.enable.visible = !advanced;
        this.hide.visible = !advanced;
        this.openAdvanced.visible = !advanced;
        this.back.visible = advanced;
    }

    private void press(int id) {
        if (!this.menu.isOwner()) {
            this.cannotEditTimer = 50;
            return;
        }
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void send(int base, String text, boolean own) {
        if (!this.menu.isOwner() || !(own ? this.frequency : this.destination).isFocused()) {
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

    /** Show the server's values in boxes that aren't being typed in (scrambled targets for non-owners, as 1.12.2 did). */
    private void refresh() {
        if (!this.frequency.isFocused() && this.menu.frequency() != this.lastFrequency) {
            this.lastFrequency = this.menu.frequency();
            this.frequency.setValue(String.valueOf(this.lastFrequency));
        }
        if (!this.destination.isFocused()) {
            if (!this.menu.isOwner() && this.menu.hideDestination()) {
                if (this.lastDestination != Integer.MAX_VALUE || RandomSource.create().nextFloat() < 0.025F) {
                    final RandomSource random = RandomSource.create();
                    final StringBuilder fake = new StringBuilder();
                    for (int i = 0; i < 6; i++) {
                        fake.append((char) (random.nextInt(126 - 33) + 33));
                    }
                    this.destination.setFilter(text -> true);
                    this.destination.setValue(fake.toString());
                    this.lastDestination = Integer.MAX_VALUE;
                }
            } else if (this.menu.destFrequency() != this.lastDestination) {
                this.lastDestination = this.menu.destFrequency();
                this.destination.setValue(String.valueOf(this.lastDestination));
            }
        }
        this.frequency.setTextColor(this.menu.frequencyValid() ? 0xFF14FF14 : 0xFFFF1919);
        this.destination.setTextColor(this.menu.destFrequencyValid() ? 0xFF14FF14 : 0xFFFF1919);
        this.enable.setMessage(Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.enable" : "gui.galacticraftcore.button.disable"));
        this.hide.setMessage(Component.translatable(this.menu.hideDestination() ? "gui.galacticraftcore.button.unhide_destination"
                : "gui.galacticraftcore.button.hide_destination"));
        final boolean active = this.menu.canToggle() && this.menu.isOwner();
        this.enable.active = active;
        this.hide.active = active;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
        if (this.cannotEditTimer > 0) {
            this.cannotEditTimer--;
        }
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.menu.energy() > 0) {
            blitHorizontal(graphics, 99, 114, 176, 0, 54, 7, energyFraction());
        }
        if (this.advanced) {
            checkbox(graphics, 27, 20, !this.menu.padRemovalDisabled());
            checkbox(graphics, 27, 38, this.menu.scheduling());
        }
    }

    private void checkbox(GuiGraphics graphics, int x, int y, boolean ticked) {
        graphics.blit(WIDGETS, this.leftPos + x, this.topPos + y, ticked ? 33 : 20, 24, 13, 13);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final int ownerColor = this.cannotEditTimer > 0 && this.cannotEditTimer % 30 < 15 ? 0xFFFF6464 : LABEL;
        if (this.advanced) {
            final Component owner = Component.translatable("gui.galacticraftcore.launch_controller.owner").append(": " + this.menu.ownerName());
            graphics.drawString(this.font, owner, this.imageWidth - this.font.width(owner) - 5, 5, ownerColor, false);
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.remove_pad"), 27 + 16, 23, LABEL, false);
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.launch_when").append(": "), 27 + 16, 41, LABEL, false);
        } else {
            final Component title = Component.literal(this.menu.ownerName() + "'s ").append(this.title);
            graphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 5, ownerColor, false);
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.frequency").append(":"), 7, 22, LABEL, false);
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.destination_frequency").append(":"), 7, 44, LABEL, false);
            final Component status = this.menu.status().text();
            graphics.drawString(this.font, status, this.imageWidth / 2 - this.font.width(status) / 2, 86, LABEL, false);
        }
    }

    /** 1.12.2 GuiElementDropdown: a black-edged grey box with an arrow box on its right, opening downwards. */
    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        final LaunchControllerBlockEntity.AutoLaunch[] options = LaunchControllerBlockEntity.AutoLaunch.values();
        final int x = this.leftPos + DROPDOWN_X;
        final int y = this.topPos + DROPDOWN_Y;
        final int w = this.dropdownWidth;
        final int h = this.dropdownOpen ? OPTION_HEIGHT * options.length : OPTION_HEIGHT;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(x, y, x + w - 15, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 16, y + h - 1, 0xFF969696);
        graphics.fill(x + w - 15, y, x + w - 1, y + OPTION_HEIGHT, 0xFF000000);
        graphics.fill(x + w - 15, y + 1, x + w - 2, y + OPTION_HEIGHT - 1, 0xFF969696);
        graphics.drawString(this.font, "v", x + w - 11, y + 3, 0xFFFFFFFF, true);
        if (this.dropdownOpen) {
            if (mouseX >= x && mouseX < x + w - 15 && mouseY >= y && mouseY < y + h) {
                final int hover = (mouseY - y) / OPTION_HEIGHT;
                graphics.fill(x + 1, y + OPTION_HEIGHT * hover + 1, x + w - 16, y + OPTION_HEIGHT * (hover + 1) - 1, 0xFFAFAFAF);
            }
            for (int i = 0; i < options.length; i++) {
                final Component text = Component.translatable(options[i].translationKey());
                graphics.drawString(this.font, text, x + w / 2 - 7 - this.font.width(text) / 2, y + (OPTION_HEIGHT - 6) / 2 + OPTION_HEIGHT * i, 0xFFFFFFFF, true);
            }
        } else {
            final Component text = Component.translatable(this.menu.selection().translationKey());
            graphics.drawString(this.font, text, x + w / 2 - 7 - this.font.width(text) / 2, y + (OPTION_HEIGHT - 6) / 2, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.advanced) {
            final double x = mouseX - this.leftPos;
            final double y = mouseY - this.topPos;
            final int options = LaunchControllerBlockEntity.AutoLaunch.values().length;
            if (this.dropdownOpen) {
                if (x >= DROPDOWN_X && x < DROPDOWN_X + this.dropdownWidth && y >= DROPDOWN_Y && y < DROPDOWN_Y + OPTION_HEIGHT * options) {
                    press(LaunchControllerMenu.BUTTON_SELECTION + (int) ((y - DROPDOWN_Y) / OPTION_HEIGHT));
                }
                this.dropdownOpen = false;
                return true;
            }
            if (x >= DROPDOWN_X && x < DROPDOWN_X + this.dropdownWidth && y >= DROPDOWN_Y && y < DROPDOWN_Y + OPTION_HEIGHT) {
                if (this.menu.isOwner()) {
                    this.dropdownOpen = true;
                } else {
                    this.cannotEditTimer = 50;
                }
                return true;
            }
            if (x >= 27 && x < 40 && y >= 20 && y < 33) {
                press(LaunchControllerMenu.BUTTON_PAD_REMOVAL);
                return true;
            }
            if (x >= 27 && x < 40 && y >= 38 && y < 51) {
                press(LaunchControllerMenu.BUTTON_SCHEDULING);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Typing digits must not trigger the inventory key and close the screen
        final EditBox focused = this.frequency.isFocused() ? this.frequency : this.destination.isFocused() ? this.destination : null;
        if (focused != null && keyCode != 256) {
            return focused.keyPressed(keyCode, scanCode, modifiers) || focused.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.advanced) {
            renderDropdown(graphics, mouseX, mouseY);
        }
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        final List<Component> tooltip = new ArrayList<>();
        if (x >= 151 && x < 169 && y >= 104 && y < 122 && this.menu.getCarried().isEmpty()) {
            tooltip.add(Component.translatable("gui.galacticraftcore.battery_slot.desc.0"));
            tooltip.add(Component.translatable("gui.galacticraftcore.battery_slot.desc.1"));
        } else if (!this.advanced && x >= 5 && x < 114 && y >= 20 && y < 33) {
            lines(tooltip, "gui.galacticraftcore.launch_controller.desc.0");
        } else if (!this.advanced && x >= 5 && x < 42 && y >= 42 && y < 55) {
            lines(tooltip, "gui.galacticraftcore.launch_controller.desc.1");
        } else if (!this.advanced && x >= 95 && x < 133 && y >= 38 && y < 58) {
            lines(tooltip, "gui.galacticraftcore.launch_controller.desc.4");
        } else if (this.advanced && !this.dropdownOpen && x >= 27 && x < 40 && y >= 20 && y < 33) {
            lines(tooltip, "gui.galacticraftcore.launch_controller.desc.2");
        } else if (this.advanced && !this.dropdownOpen && x >= DROPDOWN_X && x < DROPDOWN_X + 99 && y >= DROPDOWN_Y + 1 && y < DROPDOWN_Y + 14) {
            lines(tooltip, "gui.galacticraftcore.launch_controller.desc.3");
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
