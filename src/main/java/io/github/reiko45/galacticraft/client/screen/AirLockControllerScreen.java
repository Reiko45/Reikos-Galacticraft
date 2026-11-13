/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.container.GuiAirLockController and
 * core.client.gui.element.GuiElementCheckbox / GuiElementDropdown / GuiElementTextBox). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.airlock.AirLockControllerBlock;
import io.github.reiko45.galacticraft.block.airlock.AirLockControllerBlockEntity;
import io.github.reiko45.galacticraft.network.AirLockNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.12.2 GuiAirLockController: who the lock opens for (redstone, a player within 1-10 blocks, a named player), Invert
 * Selection and Horizontal Mode, with the lock's state underneath. Anyone can look; only the owner can change it (the
 * owner's name flashes for anyone else who tries).
 */
public class AirLockControllerScreen extends Screen {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/air_lock_controller.png");
    private static final ResourceLocation WIDGETS = GalacticraftCore.id("textures/gui/gui.png");
    private static final int WIDTH = 181;
    private static final int HEIGHT = 139;
    private static final int LABEL = 0x404040;
    private static final int OPTION_HEIGHT = 13;

    private final BlockPos pos;
    private final String owner;
    private final boolean[] checks = new boolean[6];
    private int distanceSelection;
    private String playerName;
    private EditBox nameBox;
    private boolean dropdownOpen;
    private int dropdownWidth;
    private int cannotEditTimer;
    private int left;
    private int top;

    public AirLockControllerScreen(BlockPos pos, CompoundTag settings) {
        super(Component.translatable("block.galacticraftcore.air_lock_controller"));
        this.pos = pos;
        this.owner = settings.getString("OwnerName");
        this.checks[AirLockControllerBlockEntity.SET_REDSTONE] = settings.getBoolean("RedstoneActivation");
        this.checks[AirLockControllerBlockEntity.SET_PLAYER_DISTANCE] = settings.getBoolean("PlayerDistanceActivation");
        this.checks[AirLockControllerBlockEntity.SET_PLAYER_NAME_MATCHES] = settings.getBoolean("PlayerNameMatches");
        this.checks[AirLockControllerBlockEntity.SET_INVERT] = settings.getBoolean("InvertSelection");
        this.checks[AirLockControllerBlockEntity.SET_HORIZONTAL] = settings.getBoolean("HorizontalModeEnabled");
        this.distanceSelection = settings.getInt("PlayerDistanceSelection");
        this.playerName = settings.getString("PlayerToOpenFor");
    }

    private boolean isOwner() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.getGameProfile().getName().equals(this.owner);
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        this.nameBox = new EditBox(this.font, this.width / 2 - 61, this.top + 64, 110, 15, Component.empty());
        this.nameBox.setMaxLength(16);
        this.nameBox.setValue(this.playerName);
        this.nameBox.setTextColor(0xFFC8C8C8);
        this.nameBox.setEditable(isOwner());
        this.nameBox.setResponder(text -> {
            if (isOwner() && !text.equals(this.playerName)) {
                this.playerName = text;
                send(AirLockControllerBlockEntity.SET_PLAYER_NAME, 0, text);
            }
        });
        addRenderableWidget(this.nameBox);
        this.dropdownWidth = 0;
        for (int i = 0; i < 4; i++) {
            this.dropdownWidth = Math.max(this.dropdownWidth, this.font.width(distance(i)) + 8 + 15);
        }
    }

    private static Component distance(int i) {
        return Component.translatable("gui.galacticraftcore.dropbox.player_distance." + i);
    }

    private void send(int setting, int value, String text) {
        PacketDistributor.sendToServer(new AirLockNetwork.AirLockSetting(this.pos, setting, value, text));
    }

    /** Checkbox positions (screen x, y) and labels, as 1.12.2 laid them out. */
    private int checkX(int id) {
        return this.width / 2 + (id == AirLockControllerBlockEntity.SET_PLAYER_NAME_MATCHES ? -68 : -84);
    }

    private int checkY(int id) {
        return this.top + switch (id) {
            case AirLockControllerBlockEntity.SET_REDSTONE -> 18;
            case AirLockControllerBlockEntity.SET_PLAYER_DISTANCE -> 33;
            case AirLockControllerBlockEntity.SET_PLAYER_NAME_MATCHES -> 49;
            case AirLockControllerBlockEntity.SET_INVERT -> 80;
            default -> 96;
        };
    }

    private static Component checkLabel(int id) {
        return switch (id) {
            case AirLockControllerBlockEntity.SET_REDSTONE -> Component.translatable("gui.galacticraftcore.checkbox.redstone_signal");
            case AirLockControllerBlockEntity.SET_PLAYER_DISTANCE -> Component.translatable("gui.galacticraftcore.checkbox.player_within").append(": ");
            case AirLockControllerBlockEntity.SET_PLAYER_NAME_MATCHES -> Component.translatable("gui.galacticraftcore.checkbox.player_name").append(": ");
            case AirLockControllerBlockEntity.SET_INVERT -> Component.translatable("gui.galacticraftcore.checkbox.invert");
            default -> Component.translatable("gui.galacticraftcore.checkbox.horizontal");
        };
    }

    private static final int[] CHECKBOXES = {AirLockControllerBlockEntity.SET_REDSTONE, AirLockControllerBlockEntity.SET_PLAYER_DISTANCE,
            AirLockControllerBlockEntity.SET_PLAYER_NAME_MATCHES, AirLockControllerBlockEntity.SET_INVERT, AirLockControllerBlockEntity.SET_HORIZONTAL};

    private boolean active() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return false;
        }
        final BlockState state = this.minecraft.level.getBlockState(this.pos);
        return state.hasProperty(AirLockControllerBlock.ACTIVE) && state.getValue(AirLockControllerBlock.ACTIVE);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, this.left, this.top, 0, 0, WIDTH, HEIGHT);
        graphics.blit(TEXTURE, this.left + 11, this.top + 51, 181, 0, 7, 9);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final Component title = Component.translatable("gui.galacticraftcore.title.air_lock", this.owner);
        final int titleColor = this.cannotEditTimer > 0 && this.cannotEditTimer % 30 < 15 ? 0xFFFF6464 : LABEL;
        graphics.drawString(this.font, title, this.width / 2 - this.font.width(title) / 2, this.height / 2 - 65, titleColor, false);
        for (int id : CHECKBOXES) {
            final int x = checkX(id);
            final int y = checkY(id);
            final boolean hover = mouseX >= x && mouseX < x + 13 && mouseY >= y && mouseY < y + 13;
            graphics.blit(WIDGETS, x, y, this.checks[id] ? 33 : 20, hover ? 37 : 24, 13, 13);
            graphics.drawString(this.font, checkLabel(id), x + 16, y + 3, LABEL, false);
        }
        final Component status = Component.translatable("gui.galacticraftcore.status").append(":");
        graphics.drawString(this.font, status, this.width / 2 - this.font.width(status) / 2, this.height / 2 + 45, LABEL, false);
        final Component lock = active() ? Component.translatable("gui.galacticraftcore.status.air_lock_closed").withStyle(ChatFormatting.RED)
                : Component.translatable("gui.galacticraftcore.status.air_lock_open").withStyle(ChatFormatting.GREEN);
        graphics.drawString(this.font, lock, this.width / 2 - this.font.width(lock) / 2, this.height / 2 + 55, LABEL, false);
        renderDropdown(graphics, mouseX, mouseY);
    }

    /** 1.12.2 GuiElementDropdown: a black-edged grey box with an arrow box on its right, opening downwards. */
    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        final int x = this.left + 99;
        final int y = this.top + 32;
        final int w = this.dropdownWidth;
        final int h = this.dropdownOpen ? OPTION_HEIGHT * 4 : OPTION_HEIGHT;
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
            for (int i = 0; i < 4; i++) {
                final Component text = distance(i);
                graphics.drawString(this.font, text, x + w / 2 - 7 - this.font.width(text) / 2, y + (OPTION_HEIGHT - 6) / 2 + OPTION_HEIGHT * i, 0xFFFFFFFF, true);
            }
        } else {
            final Component text = distance(this.distanceSelection);
            graphics.drawString(this.font, text, x + w / 2 - 7 - this.font.width(text) / 2, y + (OPTION_HEIGHT - 6) / 2, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int x = this.left + 99;
        final int y = this.top + 32;
        if (this.dropdownOpen) {
            if (mouseX >= x && mouseX < x + this.dropdownWidth && mouseY >= y && mouseY < y + OPTION_HEIGHT * 4) {
                this.distanceSelection = (int) ((mouseY - y) / OPTION_HEIGHT);
                send(AirLockControllerBlockEntity.SET_DISTANCE_SELECTION, this.distanceSelection, "");
            }
            this.dropdownOpen = false;
            return true;
        }
        if (mouseX >= x && mouseX < x + this.dropdownWidth && mouseY >= y && mouseY < y + OPTION_HEIGHT) {
            if (isOwner()) {
                this.dropdownOpen = true;
            } else {
                this.cannotEditTimer = 50;
            }
            return true;
        }
        for (int id : CHECKBOXES) {
            final int cx = checkX(id);
            final int cy = checkY(id);
            if (mouseX >= cx && mouseX < cx + 13 && mouseY >= cy && mouseY < cy + 13) {
                if (isOwner()) {
                    this.checks[id] = !this.checks[id];
                    send(id, this.checks[id] ? 1 : 0, "");
                } else {
                    this.cannotEditTimer = 50;
                }
                return true;
            }
        }
        if (!isOwner() && this.nameBox.isMouseOver(mouseX, mouseY)) {
            this.cannotEditTimer = 50;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.cannotEditTimer > 0) {
            this.cannotEditTimer--;
        }
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.distanceToSqr(this.pos.getCenter()) > 64.0D) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
