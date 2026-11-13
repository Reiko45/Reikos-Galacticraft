/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.gui.GuiLaserTurretEditList and
 * core.client.gui.element.GuiElementGradientList). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.network.TurretNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 1.12.2 GuiLaserTurretEditList: the turret's list (players and entity ids, A to Z) with Add Player, Add Entity and
 * Remove Selected, and the "never fire at your Space Race team" box; adding asks for a name, an entity's going green
 * once it names a known one. Escape goes back to the turret's own screen.
 */
public class LaserTurretListScreen extends Screen {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/laser_turret_edit.png");
    private static final int ROW = 20;

    private enum Mode {
        MAIN, ADD_PLAYER, ADD_ENTITY
    }

    private final LaserTurretBlockEntity turret;
    private Mode mode = Mode.MAIN;
    private int imageWidth;
    private int imageHeight;
    private int left;
    private int top;
    @Nullable
    private EditBox name;
    private final List<String> rows = new ArrayList<>();
    private final List<Boolean> rowIsPlayer = new ArrayList<>();
    private int selected = -1;
    private int scroll;

    public LaserTurretListScreen(LaserTurretBlockEntity turret) {
        super(turret.getBlockState().getBlock().getName());
        this.turret = turret;
    }

    private boolean owner() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.getUUID().equals(this.turret.ownerId());
    }

    @Override
    protected void init() {
        clearWidgets();
        this.name = null;
        if (this.mode == Mode.MAIN) {
            this.imageWidth = 222;
            this.imageHeight = 144;
        } else {
            this.imageWidth = 148;
            this.imageHeight = 70;
        }
        this.left = (this.width - this.imageWidth) / 2;
        this.top = (this.height - this.imageHeight) / 2;
        if (this.mode == Mode.MAIN) {
            addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.add_player"), b -> setMode(Mode.ADD_PLAYER))
                    .bounds(this.left + 4, this.top + 5, 62, 20).build()).active = owner();
            addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.add_entity"), b -> setMode(Mode.ADD_ENTITY))
                    .bounds(this.left + 66, this.top + 5, 62, 20).build()).active = owner();
            addRenderableWidget(Button.builder(Component.translatable("gui.galacticraftcore.button.remove_selected"), b -> removeSelected())
                    .bounds(this.left + 128, this.top + 5, 90, 20).build()).active = owner();
            refreshRows();
        } else {
            this.name = addRenderableWidget(new EditBox(this.font, this.left + 4, this.top + 16, 140, 20, Component.empty()));
            this.name.setMaxLength(LaserTurretBlockEntity.MAX_NAME);
            this.name.setEditable(owner());
            this.name.setResponder(text -> this.name.setTextColor(textColor()));
            this.name.setTextColor(textColor());
            setInitialFocus(this.name);
            addRenderableWidget(Button.builder(Component.translatable(this.turret.blacklistMode()
                    ? "gui.galacticraftcore.button.add_blacklist" : "gui.galacticraftcore.button.add_whitelist"), b -> add())
                    .bounds(this.left + this.imageWidth / 2 - 31, this.top + 40, 62, 20).build());
        }
    }

    private void setMode(Mode mode) {
        this.mode = mode;
        init();
    }

    /** 1.12.2: the players and entity ids together, A to Z, the players marked. */
    private void refreshRows() {
        final List<String> sorted = new ArrayList<>(this.turret.players());
        for (ResourceLocation entity : this.turret.entities()) {
            sorted.add(entity.toString());
        }
        Collections.sort(sorted);
        this.rows.clear();
        this.rowIsPlayer.clear();
        for (String entry : sorted) {
            final boolean player = this.turret.players().contains(entry);
            this.rows.add(entry);
            this.rowIsPlayer.add(player);
        }
        if (this.selected >= this.rows.size()) {
            this.selected = -1;
        }
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll());
    }

    private int listHeight() {
        return this.imageHeight - 45;
    }

    private int maxScroll() {
        return Math.max(0, this.rows.size() * ROW - listHeight());
    }

    private void removeSelected() {
        if (this.selected < 0 || this.selected >= this.rows.size()) {
            return;
        }
        final String entry = this.rows.get(this.selected);
        if (this.rowIsPlayer.get(this.selected)) {
            PacketDistributor.sendToServer(new TurretNetwork.Target(this.turret.getBlockPos(), TurretNetwork.REMOVE_PLAYER, entry));
            this.turret.removePlayer(entry);
        } else {
            final ResourceLocation type = ResourceLocation.tryParse(entry);
            PacketDistributor.sendToServer(new TurretNetwork.Target(this.turret.getBlockPos(), TurretNetwork.REMOVE_ENTITY, entry));
            if (type != null) {
                this.turret.removeEntity(type);
            }
        }
        refreshRows();
    }

    private void add() {
        final String text = this.name == null ? "" : this.name.getValue().trim();
        if (this.mode == Mode.ADD_ENTITY) {
            final ResourceLocation type = entityType(text);
            if (type != null) {
                PacketDistributor.sendToServer(new TurretNetwork.Target(this.turret.getBlockPos(), TurretNetwork.ADD_ENTITY, type.toString()));
                this.turret.addEntity(type);
            }
        } else if (this.mode == Mode.ADD_PLAYER && !text.isEmpty()) {
            PacketDistributor.sendToServer(new TurretNetwork.Target(this.turret.getBlockPos(), TurretNetwork.ADD_PLAYER, text));
            this.turret.addPlayer(text);
        }
        setMode(Mode.MAIN);
    }

    /** 1.12.2 getEntityEntry: a full id, else an id's path, else an entity's name in this language. */
    @Nullable
    static ResourceLocation entityType(String text) {
        if (text.isEmpty()) {
            return null;
        }
        final String lower = text.toLowerCase(Locale.ROOT);
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (id.toString().equals(lower)) {
                return id;
            }
        }
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (id.getPath().equals(lower)) {
                return id;
            }
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getDescription().getString().equalsIgnoreCase(text)) {
                return BuiltInRegistries.ENTITY_TYPE.getKey(type);
            }
        }
        return null;
    }

    private int textColor() {
        if (this.mode == Mode.ADD_ENTITY) {
            return entityType(this.name == null ? "" : this.name.getValue().trim()) != null ? 0xFF50C850 : 0xFFC85050;
        }
        return 0xFFC8C8C8;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.mode == Mode.MAIN) {
            renderList(graphics);
            LaserTurretScreen.checkbox(graphics, this.left + 5, this.top + 126, this.turret.alwaysIgnoreSpaceRace(), !owner());
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.button.never_fire_team"), this.left + 5 + 13 + 3,
                    this.top + 126 + 3, 0xFF404040, false);
        } else {
            final Component prompt = Component.translatable(this.mode == Mode.ADD_PLAYER
                    ? "gui.galacticraftcore.message.enter_player" : "gui.galacticraftcore.message.enter_entity");
            graphics.drawString(this.font, prompt, this.left + this.imageWidth / 2 - this.font.width(prompt) / 2, this.top + 5, 0xFF404040, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, this.left, this.top, 0, this.mode == Mode.MAIN ? 0 : 144, this.imageWidth, this.imageHeight);
    }

    /** 1.12.2 GuiElementGradientList: dark 20-high rows, names centred, a slider on the right. */
    private void renderList(GuiGraphics graphics) {
        final int x = this.left + 4;
        final int y = this.top + 26;
        final int w = this.imageWidth - 8;
        final int h = listHeight();
        final boolean slider = this.rows.size() * ROW > h;
        final int sliderY = y + (slider ? (h - 15) * this.scroll / Math.max(1, maxScroll()) : 1);
        graphics.fill(x, y, x + w - 10, y + h, 0xFF1E1E1E);
        graphics.fill(x + w - 9, y, x + w, y + h, 0xFF323232);
        graphics.fill(x + w - 9, sliderY, x + w, sliderY + 15, slider ? 0xFF5A5A5A : 0xFF282828);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + w - 10, y, x + w - 9, y + h, 0xFF000000);
        graphics.fill(x, y, x + 1, y + h, 0xFF000000);
        graphics.fill(x, y, x + w, y + 1, 0xFF000000);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
        graphics.enableScissor(x + 1, y + 1, x + w - 10, y + h - 1);
        int rowY = y + 1 - this.scroll;
        final Component playerLabel = Component.translatable("gui.galacticraftcore.message.player");
        for (int i = 0; i < this.rows.size(); i++) {
            if (rowY + ROW > y && rowY < y + h) {
                graphics.fill(x + 1, rowY, x + w - 10, rowY + ROW - 1, i == this.selected ? 0xFF232323 : 0xFF191919);
                final String text = this.rowIsPlayer.get(i) ? playerLabel.getString() + ": " + this.rows.get(i) : this.rows.get(i);
                graphics.drawString(this.font, text, x + (w - 10) / 2 - this.font.width(text) / 2, rowY + ROW / 2 - this.font.lineHeight / 2,
                        0xFFF0F0F0, false);
            }
            rowY += ROW;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.mode == Mode.MAIN) {
            final int x = this.left + 4;
            final int y = this.top + 26;
            if (mouseX >= x && mouseX < x + this.imageWidth - 18 && mouseY >= y && mouseY < y + listHeight()) {
                final int row = (int) (mouseY - y - 1 + this.scroll) / ROW;
                this.selected = row >= 0 && row < this.rows.size() ? row : -1;
                return true;
            }
            if (mouseX >= this.left + 5 && mouseX < this.left + 18 && mouseY >= this.top + 126 && mouseY < this.top + 139) {
                if (owner()) {
                    this.turret.setAlwaysIgnoreSpaceRace(!this.turret.alwaysIgnoreSpaceRace());
                    PacketDistributor.sendToServer(new TurretNetwork.Setting(this.turret.getBlockPos(), TurretNetwork.IGNORE_TEAM,
                            this.turret.alwaysIgnoreSpaceRace()));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.mode == Mode.MAIN) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY) * ROW, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        if (this.mode == Mode.MAIN) {
            refreshRows();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.mode == Mode.MAIN) {
                PacketDistributor.sendToServer(new TurretNetwork.Open(this.turret.getBlockPos()));
                onClose();
            } else {
                setMode(Mode.MAIN);
            }
            return true;
        }
        if (this.mode != Mode.MAIN && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            add();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
