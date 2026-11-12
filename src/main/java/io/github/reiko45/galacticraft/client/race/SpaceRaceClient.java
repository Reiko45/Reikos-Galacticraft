/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tick.TickHandlerClient, the pause menu's Space Race
 * box). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.race;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The Space Race's client hooks: the pause menu's "Space Race Manager" box, the scheduled popup and logout. */
public final class SpaceRaceClient {
    private SpaceRaceClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SpaceRaceClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientSpaceRaces.tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientSpaceRaces.reset());
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof PauseScreen screen && screen.showsPauseMenu()) {
            event.addListener(new ManagerBox(screen.width - 100, screen.height - 35));
        }
    }

    /** 1.12.2 drew a 100 x 35 gradient box in the pause menu's bottom right corner; clicking it opens the manager. */
    private static final class ManagerBox extends AbstractButton {
        ManagerBox(int x, int y) {
            super(x, y, 100, 35, Component.translatable("gui.galacticraftcore.space_race.create.title"));
        }

        @Override
        public void onPress() {
            Minecraft.getInstance().setScreen(new SpaceRaceManagerScreen());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final int shade = 10 + (isHovered() ? 20 : 0);
            graphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, RaceWidgets.argb(150, shade, shade, shade),
                    RaceWidgets.argb(250, shade, shade, shade));
            final var font = Minecraft.getInstance().font;
            final int centre = getX() + 50;
            final int color = RaceWidgets.argb(255, 240, 240, 240);
            final Component first = Component.translatable("gui.galacticraftcore.space_race.create.title.name.0");
            final Component second = Component.translatable("gui.galacticraftcore.space_race.create.title.name.1");
            graphics.drawString(font, first, centre - font.width(first) / 2, getY() + 9, color, false);
            graphics.drawString(font, second, centre - font.width(second) / 2, getY() + 19, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
