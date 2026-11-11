/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiSlimelingFeed). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.network.GCNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What a meal of its favourite food does (1.12.2 GuiSlimelingFeed): grow it (5,000 ticks of age), make it ready to
 * breed, strengthen its attack (+0.1) or heal it (5 health). Escape only closes it when none of these can be done.
 */
public class SlimelingFeedScreen extends Screen {
    private static final ResourceLocation PANEL = GalacticraftCore.id("textures/gui/slimeling_panel1.png");
    private static final int WIDTH = 138;
    private static final int HEIGHT = 51;
    private static final int[] ACTIONS = {SlimelingEntity.ACTION_GROW, SlimelingEntity.ACTION_BREED, SlimelingEntity.ACTION_STRENGTHEN, SlimelingEntity.ACTION_HEAL};
    private static final String[] LABELS = {"grow", "breed", "strengthen", "heal"};

    private final SlimelingEntity slimeling;
    private final Button[] buttons = new Button[4];

    public SlimelingFeedScreen(SlimelingEntity slimeling) {
        super(slimeling.getDisplayName());
        this.slimeling = slimeling;
    }

    @Override
    protected void init() {
        final int top = (this.height - HEIGHT) / 2;
        for (int i = 0; i < 4; i++) {
            final int action = ACTIONS[i];
            this.buttons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.slimeling." + LABELS[i]), button -> {
                PacketDistributor.sendToServer(new GCNetwork.SlimelingAction(this.slimeling.getId(), action, ""));
                this.minecraft.setScreen(null);
            }).bounds(this.width / 2 + (i % 2 == 0 ? -65 : 1), top + (i < 2 ? -15 : 7), 64, 20).build());
        }
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < 4; i++) {
            this.buttons[i].active = this.slimeling.canFeed(ACTIONS[i]);
        }
    }

    @Override
    public void tick() {
        updateButtons();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        for (Button button : this.buttons) {
            if (button.active) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(PANEL, (this.width - WIDTH) / 2, (this.height - HEIGHT) / 2 - 20, 0, 0, WIDTH, HEIGHT);
    }
}
