/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.screen.GuiJoinSpaceRace). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.race;

import io.github.reiko45.galacticraft.race.FlagData;
import io.github.reiko45.galacticraft.race.RaceNetwork;
import io.github.reiko45.galacticraft.race.SpaceRaces;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Join Space Race (1.12.2 GuiJoinSpaceRace, from /joinrace): the team's flag, name, owner and size, and Join. */
public class JoinSpaceRaceScreen extends Screen {
    private static final String KEY = "gui.galacticraftcore.space_race.";

    private final int raceId;
    private int ticks;
    private boolean initialized;

    public JoinSpaceRaceScreen(int raceId) {
        super(Component.translatable(KEY + "join.title"));
        this.raceId = raceId;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int flagX() {
        return this.width / 2 - 81 / 2;
    }

    private int flagY() {
        return this.height / 2 - this.height / 3 + 10;
    }

    @Override
    protected void init() {
        clearWidgets();
        if (!this.initialized) {
            return;
        }
        final RaceNetwork.RaceData race = ClientSpaceRaces.byId(this.raceId);
        addRenderableWidget(new RaceWidgets.GradientButton(this.width / 2 - this.width / 3 + 15, this.height / 2 - this.height / 4 - 15, 50, 15,
                Component.translatable(KEY + "create.close"), this::onClose));
        final int buttonWidth = (this.width - this.width / 4) / 2;
        addRenderableWidget(new RaceWidgets.GradientButton(this.width / 2 - buttonWidth / 2, flagY() + 58 + 60, buttonWidth, 20,
                Component.translatable(KEY + "join", race == null ? Component.empty() : SpaceRaces.displayName(race.name())), () -> {
                    PacketDistributor.sendToServer(new RaceNetwork.JoinRace(this.raceId));
                    onClose();
                }));
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
        if (!this.initialized && Math.min(this.ticks * 14, this.width / 3) == this.width / 3 && Math.min(this.ticks * 14, this.height / 3) == this.height / 3) {
            this.initialized = true;
            init();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        final int scaleX = Math.min(this.ticks * 14, this.width / 3);
        final int scaleY = Math.min(this.ticks * 14, this.height / 3);
        graphics.fillGradient(this.width / 2 - scaleX, this.height / 2 - scaleY, this.width / 2 + scaleX, this.height / 2 + scaleY, 0xC0101010, 0xD0101010);
        // Screen#render draws the background, then the buttons: the team's details go in between
        final RaceNetwork.RaceData race = ClientSpaceRaces.byId(this.raceId);
        if (this.initialized && race != null) {
            graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - this.height / 3 - 15, 0xFFFFFF);
            final FlagData flag = race.flag();
            graphics.fill(flagX() + 4, flagY() + 3, flagX() + 6, flagY() + 56, 0xFF8A8A8A);
            RaceWidgets.drawFlag(graphics, flag, flagX() + 6, flagY() + 4, 72, 48);
            graphics.drawCenteredString(this.font, SpaceRaces.displayName(race.name()), this.width / 2, flagY() + 58 + 5, RaceWidgets.argb(255, 100, 150, 20));
            graphics.drawCenteredString(this.font, Component.translatable(KEY + "join.owner").append(": " + (race.members().isEmpty() ? "" : race.members().get(0))),
                    this.width / 2, flagY() + 58 + 25, RaceWidgets.argb(255, 150, 150, 150));
            graphics.drawCenteredString(this.font, Component.translatable(KEY + "join.member_count", race.members().size()), this.width / 2, flagY() + 58 + 40,
                    RaceWidgets.argb(255, 150, 150, 150));
        }
    }
}
