/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.gui.GuiLaserTurret and
 * core.client.gui.element.GuiElementCheckbox). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.menu.LaserTurretMenu;
import io.github.reiko45.galacticraft.network.TurretNetwork;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.12.2 GuiLaserTurret: the name and status, the Blacklist Mode and Target Meteors boxes, Edit Whitelist (or Blacklist)
 * and Edit Priority, Enable / Disable, the battery slot and the energy bar. Anyone may look; a padlock by the name shows
 * a visitor it isn't theirs, and flashes when they try to change something.
 */
public class LaserTurretScreen extends MachineScreen<LaserTurretMenu> {
    static final ResourceLocation WIDGETS = GalacticraftCore.id("textures/gui/gui.png");
    static final int LABEL = 0xFF4B4B4B;

    private Button enable;
    private Button editList;
    private int cannotEditTimer;

    public LaserTurretScreen(LaserTurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "laser_turret", 176, 219);
        energyTooltipRegion(112, 106, 56, 9);
    }

    @Nullable
    LaserTurretBlockEntity turret() {
        return this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof LaserTurretBlockEntity turret ? turret : null;
    }

    private boolean owner() {
        final LaserTurretBlockEntity turret = turret();
        return turret != null && this.minecraft.player != null && this.minecraft.player.getUUID().equals(turret.ownerId());
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -1000;
        this.inventoryLabelY = this.imageHeight - 94;
        this.enable = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (owner()) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, LaserTurretMenu.BUTTON_ENABLE);
            } else {
                this.cannotEditTimer = 50;
            }
        }).bounds(this.leftPos + 7, this.topPos + this.imageHeight / 2 - 9, 72, 20).build());
        this.editList = addRenderableWidget(Button.builder(Component.empty(), b -> {
            final LaserTurretBlockEntity turret = turret();
            if (!owner()) {
                this.cannotEditTimer = 50;
            } else if (turret != null) {
                this.minecraft.setScreen(new LaserTurretListScreen(turret));
            }
        }).bounds(this.leftPos + this.imageWidth / 2 - 41, this.topPos + 55, 82, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.edit_priority"), b -> {
            final LaserTurretBlockEntity turret = turret();
            if (!owner()) {
                this.cannotEditTimer = 50;
            } else if (turret != null) {
                this.minecraft.setScreen(new LaserTurretPriorityScreen(turret));
            }
        }).bounds(this.leftPos + this.imageWidth / 2 - 41, this.topPos + 78, 82, 20).build());
        refresh();
    }

    private void refresh() {
        this.enable.setMessage(Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.enable" : "gui.galacticraftcore.button.disable"));
        this.enable.active = this.menu.cooldown() == 0;
        final LaserTurretBlockEntity turret = turret();
        this.editList.setMessage(Component.translatable(turret != null && turret.blacklistMode()
                ? "gui.galacticraftcore.button.edit_blacklist" : "gui.galacticraftcore.button.edit_whitelist"));
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
        graphics.blit(this.texture, this.leftPos + 101, this.topPos + 106, 176, 51, 67, 9);
        blitHorizontal(graphics, 113, 107, 176, 0, 54, 7, energyFraction());
        graphics.blit(this.texture, this.leftPos + 81, this.topPos + 102, 176, 33, 18, 18);
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 100, this.topPos + 107, 230, 0, 11, 8);
        }
        final LaserTurretBlockEntity turret = turret();
        checkbox(graphics, this.leftPos + 7, this.topPos + 26, turret != null && turret.blacklistMode(), this.cannotEditTimer > 0);
        checkbox(graphics, this.leftPos + 7, this.topPos + 40, turret != null && turret.targetMeteors(), this.cannotEditTimer > 0);
        if (!owner()) {
            // 1.12.2: the padlock beside the name, jumping when a visitor tries to change something
            final boolean off = this.cannotEditTimer > 0 && this.cannotEditTimer % 30 < 15;
            final int x = this.leftPos + this.imageWidth / 2 + this.font.width(this.title) / 2 + (off ? -1 : 0) + 3;
            graphics.blit(this.texture, x, this.topPos + 5 + (off ? -1 : 0), 202 + (off ? 8 : 0), 7, off ? 10 : 8, off ? 12 : 10);
        }
    }

    /** 1.12.2 GuiElementCheckbox: a 13 x 13 box from gui.png, greyed while it can't be used. */
    static void checkbox(GuiGraphics graphics, int x, int y, boolean ticked, boolean greyed) {
        if (greyed) {
            graphics.setColor(0.6F, 0.6F, 0.6F, 1.0F);
        }
        graphics.blit(WIDGETS, x, y, ticked ? 33 : 20, 24, 13, 13);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 7, LABEL, false);
        final Component status = Component.translatable("gui.galacticraftcore.status").append(": ").append(this.menu.status().text());
        graphics.drawString(this.font, status, this.imageWidth / 2 - this.font.width(status) / 2, 18, LABEL, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.message.blacklist_mode"), 7 + 13 + 3, 26 + 3, LABEL, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.message.target_meteors"), 7 + 13 + 3, 40 + 3, LABEL, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final LaserTurretBlockEntity turret = turret();
        if (turret != null && x >= 7 && x < 20 && (y >= 26 && y < 39 || y >= 40 && y < 53)) {
            if (!owner()) {
                this.cannotEditTimer = 50;
            } else if (this.cannotEditTimer == 0) {
                // 1.12.2 onSelectionChanged: the change shows at once, and the boxes rest for a moment
                this.cannotEditTimer = 40;
                if (y < 39) {
                    turret.setBlacklistMode(!turret.blacklistMode());
                    PacketDistributor.sendToServer(new TurretNetwork.Setting(this.menu.pos(), TurretNetwork.BLACKLIST, turret.blacklistMode()));
                } else {
                    turret.setTargetMeteors(!turret.targetMeteors());
                    PacketDistributor.sendToServer(new TurretNetwork.Setting(this.menu.pos(), TurretNetwork.METEORS, turret.targetMeteors()));
                }
                refresh();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
