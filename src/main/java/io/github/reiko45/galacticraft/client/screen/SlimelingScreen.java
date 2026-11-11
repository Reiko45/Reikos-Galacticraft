/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiSlimeling). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.entity.SlimelingRenderer;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.network.GCNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A Slimeling's panel (1.12.2 GuiSlimeling): the slimeling itself, its colour, name (editable by the owner), owner,
 * kills, growth, attack damage and favourite food; Sit / Follow, and the button to its inventory.
 */
public class SlimelingScreen extends Screen {
    static final ResourceLocation PANEL = GalacticraftCore.id("textures/gui/slimeling_panel0.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 147;

    private final SlimelingEntity slimeling;
    private EditBox nameField;
    private Button stayButton;
    private int left;
    private int top;

    public SlimelingScreen(SlimelingEntity slimeling) {
        super(slimeling.getDisplayName());
        this.slimeling = slimeling;
    }

    private boolean owner() {
        return this.minecraft != null && this.minecraft.player != null && this.slimeling.isOwnedBy(this.minecraft.player);
    }

    private static void send(SlimelingEntity slimeling, int action, String text) {
        PacketDistributor.sendToServer(new GCNetwork.SlimelingAction(slimeling.getId(), action, text));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        this.stayButton = addRenderableWidget(Button.builder(stayLabel(), button -> send(this.slimeling, SlimelingEntity.ACTION_SIT, ""))
                .bounds(this.left + 120, this.top + 122, 50, 20).build());
        this.stayButton.active = owner();
        this.nameField = new EditBox(this.font, this.left + 44, this.top + 59, 103, 12, Component.translatable("gui.galacticraftcore.slimeling.name"));
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(SlimelingEntity.MAX_NAME_LENGTH);
        this.nameField.setValue(this.slimeling.hasCustomName() ? this.slimeling.getCustomName().getString() : "");
        this.nameField.setHint(Component.translatable("gui.galacticraftcore.slimeling.unnamed"));
        this.nameField.setEditable(owner());
        this.nameField.setResponder(name -> send(this.slimeling, SlimelingEntity.ACTION_RENAME, name));
        addRenderableWidget(this.nameField);
        if (owner()) {
            setInitialFocus(this.nameField);
        }
    }

    private Component stayLabel() {
        return Component.translatable(this.slimeling.isOrderedToSit() ? "gui.galacticraftcore.slimeling.button.follow" : "gui.galacticraftcore.slimeling.button.sit");
    }

    @Override
    public void tick() {
        if (!this.slimeling.isAlive()) {
            onClose();
            return;
        }
        this.stayButton.setMessage(stayLabel());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean overInventoryButton(double mouseX, double mouseY) {
        final int x = this.left + 151;
        final int y = this.top + 76;
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overInventoryButton(mouseX, mouseY)) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            send(this.slimeling, SlimelingEntity.ACTION_OPEN_INVENTORY, "");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 1.12.2 drawSlimelingOnGui at scale 70 (35 pixels a block), its head turned towards the mouse, on black, behind
     * the panel (which has a window for it). {@code z} is where the black goes: below what the screen draws later.
     */
    static void drawSlimeling(GuiGraphics graphics, SlimelingEntity slimeling, int left, int top, int width, int height, int mouseX, int mouseY,
                              float z) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, z);
        graphics.fill(left, top, left + width, top + height, 0xFF000000);
        SlimelingRenderer.renderingOnGui = true;
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, left + 60, top + 12, left + 116, top + 60, 35, 0.0625F, mouseX, mouseY, slimeling);
        } finally {
            SlimelingRenderer.renderingOnGui = false;
            graphics.pose().popPose();
        }
    }

    /** The red, green and blue swatches with the slimeling's share of each. */
    static void drawColors(GuiGraphics graphics, Font font, ResourceLocation panel, SlimelingEntity slimeling, int left, int top) {
        final float[] colors = {slimeling.red(), slimeling.green(), slimeling.blue()};
        final int[] text = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        for (int i = 0; i < 3; i++) {
            graphics.blit(panel, left + WIDTH - 15, top + 9 + i * 13, 176 + i * 9, 0, 9, 9);
            final String percent = Math.round(colors[i] * 1000) / 10.0F + "% ";
            graphics.drawString(font, percent, left + WIDTH - 15 - font.width(percent), top + 10 + i * 13, text[i]);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawSlimeling(graphics, this.slimeling, this.left, this.top, WIDTH, HEIGHT - 20, mouseX, mouseY, -150.0F);
        graphics.blit(PANEL, this.left, this.top, 0, 0, WIDTH, HEIGHT);
        drawColors(graphics, this.font, PANEL, this.slimeling, this.left, this.top);
        graphics.blit(PANEL, this.left + 151, this.top + 76, 176, 9, 18, 18);
        graphics.fill(this.left + 40, this.top + 55, this.left + 156, this.top + 70, 0xFFA0A0A0);
        graphics.fill(this.left + 41, this.top + 56, this.left + 155, this.top + 69, 0xFF000000);
        final int x = this.left + 10;
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.slimeling.name").append(": "), x, this.top + 59, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.slimeling.owner").append(": " + this.slimeling.ownerName()), x, this.top + 72, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.slimeling.kills").append(": " + this.slimeling.kills()), x, this.top + 85, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.slimeling.scale")
                .append(": " + Math.round(this.slimeling.slimelingAge() / (float) SlimelingEntity.MAX_AGE * 1000.0F) / 10.0F + "%"), x, this.top + 98, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.slimeling.damage")
                .append(": " + Math.round(this.slimeling.damage() * 100.0F) / 100.0F), x, this.top + 111, 0x404040, false);
        final Component status = Component.translatable(this.slimeling.isOrderedToSit() ? "gui.galacticraftcore.slimeling.sitting" : "gui.galacticraftcore.slimeling.following");
        graphics.drawString(this.font, status, this.left + 145 - this.font.width(status) / 2, this.top + 112, 0x404040, false);
        final Component food = Component.translatable("gui.galacticraftcore.slimeling.food").append(": ");
        graphics.drawString(this.font, food, x, this.top + 124, 0x404040, false);
        final ItemStack favourite = new ItemStack(this.slimeling.favouriteFood());
        final int foodX = x + this.font.width(food);
        graphics.renderItem(favourite, foodX, this.top + 120);
        for (var widget : this.renderables) {
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
        if (mouseX >= foodX && mouseX < foodX + 16 && mouseY >= this.top + 120 && mouseY < this.top + 136) {
            graphics.renderTooltip(this.font, favourite, mouseX, mouseY);
        }
    }
}
