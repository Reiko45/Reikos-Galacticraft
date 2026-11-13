/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.gui.GuiLaserTurretEditPriority and
 * core.client.gui.element.GuiElementSpinner). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.network.TurretNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 1.12.2 GuiLaserTurretEditPriority: three spinners, Closest, Lowest Health and Highest Health; the target is chosen by
 * the lowest number first. Two can't share a number: moving one onto another's swaps them. Escape goes back.
 */
public class LaserTurretPriorityScreen extends Screen {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/laser_turret_edit.png");
    private static final int WIDTH = 222;
    private static final int HEIGHT = 95;
    private static final String[] LABELS = {"gui.galacticraftcore.message.priority_closest", "gui.galacticraftcore.message.priority_health_low",
            "gui.galacticraftcore.message.priority_health_high"};

    private final LaserTurretBlockEntity turret;
    private final int[] values = new int[3];
    private int left;
    private int top;

    public LaserTurretPriorityScreen(LaserTurretBlockEntity turret) {
        super(turret.getBlockState().getBlock().getName());
        this.turret = turret;
        this.values[0] = turret.priorityClosest();
        this.values[1] = turret.priorityLowestHealth();
        this.values[2] = turret.priorityHighestHealth();
    }

    private boolean owner() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.getUUID().equals(this.turret.ownerId());
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
    }

    private int spinnerX() {
        return this.width / 2 - 95;
    }

    private int spinnerY(int index) {
        return this.top + 21 + 22 * index;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, this.left, this.top, 0, 0, WIDTH, HEIGHT - 10);
        graphics.blit(TEXTURE, this.left, this.top + HEIGHT - 10, 0, 144 - 10, WIDTH, 10);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.message.priority_low"), this.left + 6, this.top + 8,
                LaserTurretScreen.LABEL, false);
        for (int i = 0; i < 3; i++) {
            final int x = spinnerX();
            final int y = spinnerY(i);
            // 1.12.2 GuiElementSpinner: a 20-wide value box and the up and down arrows beside it
            final boolean upper = mouseX >= x + 20 && mouseX < x + 31 && mouseY > y && mouseY < y + 10;
            final boolean lower = mouseX >= x + 20 && mouseX < x + 31 && mouseY > y + 10 && mouseY < y + 20;
            graphics.blit(LaserTurretScreen.WIDGETS, x, y, 214, 0, 20, 20);
            graphics.blit(LaserTurretScreen.WIDGETS, x + 20, y, upper ? 245 : 234, 0, 11, 10);
            graphics.blit(LaserTurretScreen.WIDGETS, x + 20, y + 10, lower ? 245 : 234, 10, 11, 10);
            final String value = Integer.toString(this.values[i]);
            graphics.drawString(this.font, value, x + 11 - this.font.width(value) / 2, y + 7, 0xFFDCDCDC, false);
            graphics.drawString(this.font, Component.translatable(LABELS[i]), x + 35, y + 10 - this.font.lineHeight / 2, LaserTurretScreen.LABEL, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 3; i++) {
            final int x = spinnerX();
            final int y = spinnerY(i);
            if (mouseX >= x + 20 && mouseX < x + 31 && mouseY > y && mouseY < y + 20) {
                if (owner()) {
                    change(i, mouseY < y + 10 ? 1 : -1);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 1.12.2 onSelectionChanged: a spinner already on the new number takes this one's; none goes below 0. */
    private void change(int index, int delta) {
        final int newValue = this.values[index] + delta;
        if (newValue < 0) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (i != index && this.values[i] == newValue) {
                this.values[i] = this.values[index];
            }
        }
        this.values[index] = newValue;
        this.turret.setPriorities(this.values[0], this.values[1], this.values[2]);
        PacketDistributor.sendToServer(new TurretNetwork.Priorities(this.turret.getBlockPos(), this.values[0], this.values[1], this.values[2]));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            PacketDistributor.sendToServer(new TurretNetwork.Open(this.turret.getBlockPos()));
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
