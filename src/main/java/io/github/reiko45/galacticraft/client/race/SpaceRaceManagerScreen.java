/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.screen.GuiNewSpaceRace). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.race;

import io.github.reiko45.galacticraft.race.FlagData;
import io.github.reiko45.galacticraft.race.RaceNetwork;
import io.github.reiko45.galacticraft.race.SpaceRace;
import io.github.reiko45.galacticraft.race.SpaceRaces;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The Space Race Manager (1.12.2 GuiNewSpaceRace), opened from the pause menu. Its main page shows the team's flag and
 * colour, its name, and for the owner the Add / Remove Player(s) pages; "View Server Stats" and "View Global Stats" say
 * "Coming Soon" as they did in 1.12.2. The owner clicks the flag to draw it (a paintbrush and an eraser of radius 1-11,
 * a rectangle to paint within, a colour picker, a grid, and red, green and blue sliders) and the colour box to choose
 * the team colour. Changes go to the server every 5 seconds and when the screen closes.
 */
public class SpaceRaceManagerScreen extends Screen {
    private enum State {
        MAIN, ADD_PLAYER, REMOVE_PLAYER, DESIGN_FLAG, CHANGE_TEAM_COLOR
    }

    private static final String KEY = "gui.galacticraftcore.space_race.";

    private State state = State.MAIN;
    private int ticks;
    private boolean initialized;
    private boolean dirty = true;
    private boolean canEdit = true;
    private String name = SpaceRace.DEFAULT_NAME;
    private FlagData flag = new FlagData();
    private int color = 0xFFFFFF;
    private List<String> members = new ArrayList<>();
    /** 1.12.2 recentlyInvited: names invited in the last minute, shown in orange. */
    private final Map<String, Integer> recentlyInvited = new HashMap<>();

    private EditBox rename;
    private RaceWidgets.NameList list;
    private RaceWidgets.GradientButton listAction;
    private RaceWidgets.Slider red;
    private RaceWidgets.Slider green;
    private RaceWidgets.Slider blue;
    private RaceWidgets.Slider brushSize;
    private RaceWidgets.Slider eraserSize;
    private RaceWidgets.ToolBox paintbrush;
    private RaceWidgets.ToolBox eraser;
    private RaceWidgets.ToolBox selector;
    private RaceWidgets.ToolBox picker;
    private RaceWidgets.ToolBox grid;
    private float brushValue = 0.0F;
    private float eraserValue = 0.0F;
    private final float[] paint = {0.0F, 0.0F, 0.0F};

    // flag designer canvas
    private float canvasX;
    private float canvasY;
    private float scaleX;
    private float scaleY;
    private int selectionMinX = -1;
    private int selectionMinY = -1;
    private int selectionMaxX = -1;
    private int selectionMaxY = -1;
    private boolean selecting;

    // main page buttons drawn by hand
    private int flagX;
    private int flagY;
    private int colorX;
    private int colorY;

    public SpaceRaceManagerScreen() {
        super(Component.translatable(KEY + "create.title"));
        loadFromCache(true);
    }

    private UUID self() {
        return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID()
                : net.minecraft.client.Minecraft.getInstance().player.getUUID();
    }

    /** 1.12.2 updateSpaceRaceData: take the server's copy unless there are unsent changes. */
    private void loadFromCache(boolean force) {
        final RaceNetwork.RaceData race = ClientSpaceRaces.of(self());
        if (race == null) {
            if (force) {
                this.members = new ArrayList<>(List.of(net.minecraft.client.Minecraft.getInstance().player.getGameProfile().getName()));
                this.canEdit = true;
            }
            return;
        }
        if (!force && this.dirty) {
            return;
        }
        this.name = race.name();
        this.flag = race.flag().copy();
        this.color = race.color();
        this.members = new ArrayList<>(race.members());
        this.canEdit = race.isOwner(self());
        if (this.rename != null && !this.rename.isFocused()) {
            this.rename.setValue(SpaceRaces.displayName(this.name).getString());
        }
    }

    /** 1.12.2 sendSpaceRaceData (S_START_NEW_SPACE_RACE): only the owner's changes count. */
    private void send() {
        if (this.canEdit) {
            PacketDistributor.sendToServer(new RaceNetwork.UpdateRace(this.name, this.flag.copy(), this.color));
        }
        this.dirty = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ layout (1.12.2 initGui)

    @Override
    protected void init() {
        clearWidgets();
        if (!this.initialized) {
            return;
        }
        final int left = this.width / 2 - this.width / 3;
        addRenderableWidget(new RaceWidgets.GradientButton(left + 15, this.height / 2 - this.height / 4 - 15, 50, 15,
                Component.translatable(this.state == State.MAIN ? KEY + "create.close" : KEY + "create.back"), () -> exit(true)));
        switch (this.state) {
            case MAIN -> initMain();
            case ADD_PLAYER, REMOVE_PLAYER -> initList();
            case DESIGN_FLAG -> initDesigner();
            case CHANGE_TEAM_COLOR -> initTeamColor();
        }
    }

    private void initMain() {
        this.flagX = this.width / 2 - 81 / 2;
        this.flagY = this.height / 2 - this.height / 3 + 10;
        this.colorX = this.flagX + 81 + 10;
        this.colorY = this.flagY + 58 / 2 - 45 / 2;
        this.rename = new EditBox(this.font, this.width / 2 - 75, this.flagY + 58 + 10, 150, 16, Component.translatable(KEY + "create.rename"));
        this.rename.setMaxLength(SpaceRaces.MAX_NAME_LENGTH);
        this.rename.setValue(SpaceRaces.displayName(this.name).getString());
        this.rename.setEditable(this.canEdit);
        this.rename.setResponder(text -> {
            if (this.canEdit && !text.equals(SpaceRaces.displayName(this.name).getString())) {
                this.name = text;
                this.dirty = true;
            }
        });
        addRenderableWidget(this.rename);
        final int rowY = this.rename.getY() + this.height / 10;
        final int rowH = this.height / 10;
        if (this.canEdit) {
            addRenderableWidget(new RaceWidgets.GradientButton(this.width / 2 - 120, rowY, 100, rowH, Component.translatable(KEY + "create.add_players"),
                    () -> switchTo(State.ADD_PLAYER)));
            addRenderableWidget(new RaceWidgets.GradientButton(this.width / 2 - 120, rowY + rowH + this.height / 50, 100, rowH,
                    Component.translatable(KEY + "create.rem_players"), () -> switchTo(State.REMOVE_PLAYER)));
        }
        final int statsX = this.width / 2 + (this.canEdit ? 20 : -50);
        addRenderableWidget(new StatsButton(statsX, rowY, rowH, KEY + "create.server_stats"));
        addRenderableWidget(new StatsButton(statsX, rowY + rowH + this.height / 50, rowH, KEY + "create.global_stats"));
    }

    /** 1.12.2's stats buttons, never finished there: "Coming Soon" under the mouse. */
    private final class StatsButton extends RaceWidgets.GradientButton {
        private final Component label;

        StatsButton(int x, int y, int height, String key) {
            super(x, y, 100, height, Component.translatable(key), () -> {
            });
            this.label = Component.translatable(key);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setMessage(isHovered() ? Component.translatable(KEY + "coming_soon") : this.label);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void initList() {
        final boolean add = this.state == State.ADD_PLAYER;
        this.listAction = new RaceWidgets.GradientButton(this.width / 2 - this.width / 3 + 7, this.height / 2 + this.height / 4 - 15, 64, 15,
                Component.translatable(add ? KEY + "create.send_invite" : KEY + "create.remove"), this::listAction);
        this.listAction.active = false;
        addRenderableWidget(this.listAction);
        final int x0 = this.width / 2 - this.width / 3 + 15 + 50 + 10;
        final int x1 = this.width / 2 + this.width / 3 - 10;
        final int y0 = this.height / 2 - this.height / 3 + 10;
        final int y1 = this.height / 2 + this.height / 3 - 10;
        this.list = new RaceWidgets.NameList(x0, y0, x1 - x0, y1 - y0);
        addRenderableWidget(this.list);
        refreshList();
    }

    /** Players within 25 blocks who aren't in the team (to invite), or the members but the owner (to remove). */
    private void refreshList() {
        final List<String> names = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        if (this.state == State.ADD_PLAYER) {
            for (AbstractClientPlayer player : this.minecraft.level.players()) {
                if (player.distanceToSqr(this.minecraft.player) <= 25.0D * 25.0D) {
                    final String playerName = player.getGameProfile().getName();
                    if (!this.members.contains(playerName)) {
                        names.add(playerName);
                        colors.add(this.recentlyInvited.containsKey(playerName) ? RaceWidgets.argb(255, 250, 120, 0) : RaceWidgets.argb(255, 190, 190, 190));
                    }
                }
            }
        } else {
            for (int i = 1; i < this.members.size(); i++) {
                names.add(this.members.get(i));
                colors.add(RaceWidgets.argb(255, 190, 190, 190));
            }
        }
        this.list.setContents(names, colors);
    }

    private void listAction() {
        final String selected = this.list.selectedName();
        if (selected == null) {
            return;
        }
        if (this.state == State.ADD_PLAYER) {
            if (!this.recentlyInvited.containsKey(selected)) {
                PacketDistributor.sendToServer(new RaceNetwork.RacePlayer(selected, true));
                this.recentlyInvited.put(selected, 20 * 60);
            }
        } else {
            PacketDistributor.sendToServer(new RaceNetwork.RacePlayer(selected, false));
        }
    }

    private void initDesigner() {
        final int guiBottom = this.height / 2 + this.height / 4;
        final int guiTop = this.height / 2 - this.height / 4;
        final int guiRight = this.width / 2 + this.width / 3;
        this.scaleX = this.width / 130.0F;
        this.scaleY = this.height / 70.0F;
        this.canvasX = this.width / 2.0F - this.flag.width() * this.scaleX / 2.0F;
        this.canvasY = this.height / 2.0F - this.flag.height() * this.scaleY / 2.0F;
        final int canvasRight = (int) (this.canvasX + this.flag.width() * this.scaleX);
        final int availWidth = (int) ((guiRight - 10 - (canvasRight + 10)) / 3.0F);
        final float x1 = canvasRight + 10;
        final float x2 = guiRight - 10;
        final float y1 = guiBottom - 10 - (x2 - x1);
        final int sliderHeight = Math.max(10, (int) (y1 - 10 - (guiTop + 10)));
        this.red = new RaceWidgets.Slider(canvasRight + 10, guiTop + 10, availWidth, sliderHeight, true, 0x000000, 0xFF0000, this.paint[0],
                Component.empty(), value -> this.paint[0] = value);
        this.green = new RaceWidgets.Slider(canvasRight + 11 + availWidth, guiTop + 10, availWidth, sliderHeight, true, 0x000000, 0x00FF00, this.paint[1],
                Component.empty(), value -> this.paint[1] = value);
        this.blue = new RaceWidgets.Slider(canvasRight + 12 + availWidth * 2, guiTop + 10, availWidth, sliderHeight, true, 0x000000, 0x0000FF, this.paint[2],
                Component.empty(), value -> this.paint[2] = value);
        addRenderableWidget(this.red);
        addRenderableWidget(this.green);
        addRenderableWidget(this.blue);
        final int toolX = (int) (this.canvasX - 15);
        final int toolY = this.height / 2 - this.height / 4;
        this.paintbrush = new RaceWidgets.ToolBox(toolX, toolY + 10, 0, true, Component.empty(), this::toolChanged);
        this.eraser = new RaceWidgets.ToolBox(toolX, toolY + 25, 52, false, Component.empty(), this::toolChanged);
        this.selector = new RaceWidgets.ToolBox(toolX, toolY + 40, 78, false, Component.empty(), this::toolChanged);
        this.picker = new RaceWidgets.ToolBox(toolX, toolY + 55, 104, false, Component.empty(), this::toolChanged);
        this.grid = new RaceWidgets.ToolBox(toolX, toolY + 90, 26, false, Component.empty(), box -> {
        });
        this.brushSize = new RaceWidgets.Slider(toolX - 40, toolY + 10, 35, 13, false, 0x575757, 0x575757, this.brushValue,
                Component.translatable(KEY + "create.brush_size"), value -> this.brushValue = value);
        this.eraserSize = new RaceWidgets.Slider(toolX - 40, toolY + 25, 35, 13, false, 0x575757, 0x575757, this.eraserValue,
                Component.translatable(KEY + "create.eraser_size"), value -> this.eraserValue = value);
        this.eraserSize.visible = false;
        for (var widget : List.of(this.paintbrush, this.grid, this.eraser, this.selector, this.picker)) {
            addRenderableWidget(widget);
        }
        addRenderableWidget(this.brushSize);
        addRenderableWidget(this.eraserSize);
    }

    /** 1.12.2 onSelectionChanged: one tool at a time; the brush and eraser show their size sliders. */
    private void toolChanged(RaceWidgets.ToolBox box) {
        if (box.selected()) {
            for (RaceWidgets.ToolBox other : List.of(this.paintbrush, this.eraser, this.selector, this.picker)) {
                if (other != box) {
                    other.setSelected(false);
                }
            }
        }
        this.brushSize.visible = this.paintbrush.selected();
        this.eraserSize.visible = this.eraser.selected();
    }

    private void initTeamColor() {
        final int guiBottom = this.height / 2 + this.height / 4;
        final int guiTop = this.height / 2 - this.height / 4;
        final int guiRight = this.width / 2 + this.width / 3;
        final int guiLeft = this.width / 2 - this.width / 3;
        final int availWidth = (guiRight - guiLeft - 100) / 3;
        final int sliderHeight = guiBottom - 10 - (guiTop + 30);
        final int x = guiLeft + 25;
        this.red = new RaceWidgets.Slider(x, guiTop + 30, availWidth, sliderHeight, true, 0x000000, 0xFF0000, (this.color >> 16 & 255) / 255.0F,
                Component.empty(), value -> setColor());
        this.green = new RaceWidgets.Slider(x + availWidth + 25, guiTop + 30, availWidth, sliderHeight, true, 0x000000, 0x00FF00, (this.color >> 8 & 255) / 255.0F,
                Component.empty(), value -> setColor());
        this.blue = new RaceWidgets.Slider(x + availWidth * 2 + 50, guiTop + 30, availWidth, sliderHeight, true, 0x000000, 0x0000FF, (this.color & 255) / 255.0F,
                Component.empty(), value -> setColor());
        addRenderableWidget(this.red);
        addRenderableWidget(this.green);
        addRenderableWidget(this.blue);
    }

    private void setColor() {
        this.color = this.red.colorValue() << 16 | this.green.colorValue() << 8 | this.blue.colorValue();
        this.dirty = true;
    }

    private void switchTo(State state) {
        this.state = state;
        init();
    }

    /** 1.12.2 exitCurrentScreen: back to the main page, or from there send any changes and close. */
    private void exit(boolean close) {
        if (this.state == State.MAIN) {
            if (this.dirty) {
                send();
            }
            if (close) {
                onClose();
            }
        } else {
            switchTo(State.MAIN);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            exit(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (this.dirty) {
            send();
        }
        super.removed();
    }

    // ------------------------------------------------------------------ ticking (1.12.2 updateScreen)

    @Override
    public void tick() {
        super.tick();
        if (this.ticks % 100 == 0) {
            if (this.dirty || this.ticks == 0) {
                send();
            } else {
                loadFromCache(false);
            }
        }
        this.ticks++;
        this.recentlyInvited.replaceAll((key, left) -> left - 1);
        this.recentlyInvited.values().removeIf(left -> left < 0);
        if (!this.initialized && Math.min(this.ticks * 14, this.width / 3) == this.width / 3 && Math.min(this.ticks * 14, this.height / 3) == this.height / 3) {
            this.initialized = true;
            init();
        }
        if ((this.state == State.ADD_PLAYER || this.state == State.REMOVE_PLAYER) && this.list != null) {
            if (this.ticks % 20 == 0) {
                refreshList();
            }
            this.listAction.active = this.list.selectedName() != null;
        }
    }

    // ------------------------------------------------------------------ the flag designer's mouse

    private boolean onCanvas(double mouseX, double mouseY) {
        return mouseX >= this.canvasX && mouseY >= this.canvasY && mouseX <= this.canvasX + this.flag.width() * this.scaleX
                && mouseY <= this.canvasY + this.flag.height() * this.scaleY;
    }

    private int cellX(double mouseX) {
        return Mth.floor((mouseX - this.canvasX) / this.scaleX);
    }

    private int cellY(double mouseY) {
        return Mth.floor((mouseY - this.canvasY) / this.scaleY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.initialized && this.state == State.MAIN && button == 0 && this.canEdit) {
            if (mouseX >= this.flagX && mouseY >= this.flagY && mouseX < this.flagX + 81 && mouseY < this.flagY + 58) {
                switchTo(State.DESIGN_FLAG);
                return true;
            }
            if (mouseX >= this.colorX && mouseY >= this.colorY && mouseX < this.colorX + 45 && mouseY < this.colorY + 45) {
                switchTo(State.CHANGE_TEAM_COLOR);
                return true;
            }
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.state == State.DESIGN_FLAG && button == 0) {
            if (this.selector.selected()) {
                this.selecting = true;
                if (onCanvas(mouseX, mouseY)) {
                    this.selectionMinX = cellX(mouseX);
                    this.selectionMinY = cellY(mouseY);
                } else {
                    this.selectionMinX = this.selectionMinY = -1;
                }
                this.selectionMaxX = this.selectionMaxY = -1;
                return true;
            }
            return paintAt(mouseX, mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return this.state == State.DESIGN_FLAG && button == 0 && !this.selector.selected() && paintAt(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.state == State.DESIGN_FLAG && this.selecting && button == 0) {
            this.selecting = false;
            if (this.selectionMinX != -1 && this.selectionMinY != -1 && onCanvas(mouseX, mouseY)) {
                this.selectionMaxX = Math.min(cellX(mouseX) + 1, this.flag.width());
                this.selectionMaxY = Math.min(cellY(mouseY) + 1, this.flag.height());
                if (this.selectionMinX > this.selectionMaxX) {
                    final int temp = this.selectionMaxX - 1;
                    this.selectionMaxX = this.selectionMinX + 1;
                    this.selectionMinX = temp;
                }
                if (this.selectionMinY > this.selectionMaxY) {
                    final int temp = this.selectionMaxY - 1;
                    this.selectionMaxY = this.selectionMinY + 1;
                    this.selectionMinY = temp;
                }
            } else {
                this.selectionMaxX = this.selectionMaxY = -1;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 1.12.2 updateScreen's painting: the brush, the eraser (white) or the colour picker under the mouse. */
    private boolean paintAt(double mouseX, double mouseY) {
        if (!this.canEdit || !onCanvas(mouseX, mouseY)) {
            return false;
        }
        final int x = cellX(mouseX);
        final int y = cellY(mouseY);
        if (this.eraser.selected()) {
            brush(x, y, FlagData.WHITE, Mth.floor(this.eraserValue * 10) + 1);
        } else if (this.picker.selected()) {
            final int picked = this.flag.colorAt(x, y);
            this.red.setValue((picked >> 16 & 255) / 255.0F);
            this.green.setValue((picked >> 8 & 255) / 255.0F);
            this.blue.setValue((picked & 255) / 255.0F);
            this.paint[0] = this.red.value();
            this.paint[1] = this.green.value();
            this.paint[2] = this.blue.value();
        } else if (this.paintbrush.selected()) {
            brush(x, y, this.red.colorValue() << 16 | this.green.colorValue() << 8 | this.blue.colorValue(), Mth.floor(this.brushValue * 10) + 1);
        }
        return true;
    }

    /** 1.12.2 setColorWithBrushSize: a round brush, kept inside the selection if there is one. */
    private void brush(int cx, int cy, int rgb, int size) {
        for (int x = cx - size + 1; x < cx + size; x++) {
            for (int y = cy - size + 1; y < cy + size; y++) {
                if (x < 0 || y < 0 || x >= this.flag.width() || y >= this.flag.height()) {
                    continue;
                }
                final float dx = x - cx;
                final float dy = y - cy;
                if (Math.sqrt(dx * dx + dy * dy) > size) {
                    continue;
                }
                final boolean selection = this.selectionMaxX - this.selectionMinX > 0 && this.selectionMaxY - this.selectionMinY > 0;
                if (!selection || x >= this.selectionMinX && x < this.selectionMaxX && y >= this.selectionMinY && y < this.selectionMaxY) {
                    this.flag.setColorAt(x, y, rgb);
                    this.dirty = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------ drawing (1.12.2 drawScreen)

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.12.2 drawWorldBackground: a dark box that grows from the middle
        final int scaleX = Math.min(this.ticks * 14, this.width / 3);
        final int scaleY = Math.min(this.ticks * 14, this.height / 3);
        graphics.fillGradient(this.width / 2 - scaleX, this.height / 2 - scaleY, this.width / 2 + scaleX, this.height / 2 + scaleY, 0xC0101010, 0xD0101010);
        // Screen#render draws the background first and the widgets over it: the hand-drawn parts go in between
        if (this.initialized) {
            final int titleY = this.height / 2 - this.height / 3 - 15;
            switch (this.state) {
                case MAIN -> {
                    graphics.drawCenteredString(this.font, Component.translatable(KEY + "create.title"), this.width / 2, titleY, 0xFFFFFF);
                    drawFlagButton(graphics, mouseX, mouseY);
                    drawColorButton(graphics, mouseX, mouseY);
                }
                case ADD_PLAYER -> {
                    graphics.drawCenteredString(this.font, Component.translatable(KEY + "create.invite_player"), this.width / 2, titleY, 0xFFFFFF);
                    graphics.drawCenteredString(this.font, Component.translatable(KEY + "create.player_radius"), this.width / 2,
                            this.height / 2 + this.height / 3 + 3, RaceWidgets.argb(255, 180, 40, 40));
                }
                case REMOVE_PLAYER -> graphics.drawCenteredString(this.font, Component.translatable(KEY + "create.remove_player"), this.width / 2, titleY, 0xFFFFFF);
                case DESIGN_FLAG -> drawDesigner(graphics);
                case CHANGE_TEAM_COLOR -> {
                    float x1 = this.green.getX();
                    float x2 = this.green.getX() + this.green.getWidth();
                    final float y1 = this.height / 2.0F - this.height / 3.0F + 5;
                    float y2 = this.green.getY() - 5;
                    if (x2 - x1 > y2 - y1) {
                        final float mid = this.green.getX() + this.green.getWidth() / 2.0F;
                        x1 = mid - (y2 - y1) / 2;
                        x2 = mid + (y2 - y1) / 2;
                    } else {
                        y2 = y1 + (x2 - x1);
                    }
                    graphics.fill((int) x1 + 1, (int) y1 + 1, (int) x2 - 1, (int) y2 - 1, 0xFF000000 | this.color);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.state == State.DESIGN_FLAG && this.initialized) {
            if (this.brushSize.visible) {
                this.brushSize.setMessage(Component.translatable(KEY + "create.brush_radius").append(": " + (Mth.floor(this.brushValue * 10) + 1)));
            }
            if (this.eraserSize.visible) {
                this.eraserSize.setMessage(Component.translatable(KEY + "create.eraser_radius").append(": " + (Mth.floor(this.eraserValue * 10) + 1)));
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFlagButton(GuiGraphics graphics, int mouseX, int mouseY) {
        final boolean hover = this.canEdit && mouseX >= this.flagX && mouseY >= this.flagY && mouseX < this.flagX + 81 && mouseY < this.flagY + 58;
        // the pole, then the flag hanging from its top
        graphics.fill(this.flagX + 4, this.flagY + 3, this.flagX + 6, this.flagY + 56, 0xFF8A8A8A);
        RaceWidgets.drawFlag(graphics, this.flag, this.flagX + 6, this.flagY + 4, 72, 48);
        if (hover) {
            graphics.fill(this.flagX, this.flagY, this.flagX + 81, this.flagY + 58, RaceWidgets.argb(150, 50, 50, 50));
        }
        if (this.canEdit) {
            final int shade = hover ? 170 : 100;
            final Component text = Component.translatable(KEY + "create.customize");
            graphics.drawString(this.font, text, this.flagX + 81 / 2 - this.font.width(text) / 2, this.flagY + 58 / 2 - 5,
                    RaceWidgets.argb(255, shade, shade, shade), hover);
        }
        graphics.renderOutline(this.flagX, this.flagY, 81, 58, 0xFF000000);
    }

    private void drawColorButton(GuiGraphics graphics, int mouseX, int mouseY) {
        final boolean hover = this.canEdit && mouseX >= this.colorX && mouseY >= this.colorY && mouseX < this.colorX + 45 && mouseY < this.colorY + 45;
        graphics.fill(this.colorX + 2, this.colorY + 2, this.colorX + 43, this.colorY + 43, 0xFF000000 | this.color);
        final int shade = hover ? 170 : 100;
        final int text = RaceWidgets.argb(255, shade, shade, shade);
        final int centre = this.colorX + 45 / 2;
        if (this.canEdit) {
            final Component change = Component.translatable(KEY + "create.change_color.name.0");
            graphics.drawString(this.font, change, centre - this.font.width(change) / 2, this.colorY + 45 / 2 - 13, text, hover);
        }
        final Component team = Component.translatable(KEY + "create.change_color.name.1");
        final Component colour = Component.translatable(KEY + "create.change_color.name.2");
        graphics.drawString(this.font, team, centre - this.font.width(team) / 2, this.colorY + 45 / 2 - (this.canEdit ? 3 : 9), text, hover);
        graphics.drawString(this.font, colour, centre - this.font.width(colour) / 2, this.colorY + 45 / 2 + (this.canEdit ? 7 : 1), text, hover);
        if (hover) {
            graphics.fill(this.colorX, this.colorY, this.colorX + 45, this.colorY + 45, RaceWidgets.argb(55, 50, 50, 50));
        }
        graphics.renderOutline(this.colorX, this.colorY, 45, 45, 0xFF000000);
    }

    private void drawDesigner(GuiGraphics graphics) {
        final float width = this.flag.width() * this.scaleX;
        final float height = this.flag.height() * this.scaleY;
        RaceWidgets.drawFlag(graphics, this.flag, this.canvasX, this.canvasY, width, height);
        if (this.grid.selected()) {
            graphics.pose().pushPose();
            graphics.pose().translate(this.canvasX, this.canvasY, 0.0F);
            for (int x = 0; x <= this.flag.width(); x++) {
                final int px = Math.round(x * this.scaleX);
                graphics.fill(px, 0, px + 1, Math.round(height), 0xFF000000);
            }
            for (int y = 0; y <= this.flag.height(); y++) {
                final int py = Math.round(y * this.scaleY);
                graphics.fill(0, py, Math.round(width), py + 1, 0xFF000000);
            }
            graphics.pose().popPose();
        }
        if (!this.selecting && this.selectionMaxX - this.selectionMinX > 0 && this.selectionMaxY - this.selectionMinY > 0) {
            final int shade = (int) ((Math.sin(this.ticks * 0.3) * 0.4 + 0.1) * 255) & 255;
            graphics.renderOutline(Math.round(this.canvasX + this.selectionMinX * this.scaleX), Math.round(this.canvasY + this.selectionMinY * this.scaleY),
                    Math.round((this.selectionMaxX - this.selectionMinX) * this.scaleX), Math.round((this.selectionMaxY - this.selectionMinY) * this.scaleY),
                    RaceWidgets.argb(255, shade, shade, shade));
        }
        // the paint colour, under the sliders
        final int guiRight = this.width / 2 + this.width / 3;
        final int guiBottom = this.height / 2 + this.height / 4;
        final int x1 = this.red.getX();
        final int x2 = guiRight - 10;
        final int y1 = guiBottom - 10 - (x2 - x1);
        final int y2 = guiBottom - 10;
        graphics.fill(x1, y1, x2, y2, 0xFF000000);
        graphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF000000 | this.red.colorValue() << 16 | this.green.colorValue() << 8 | this.blue.colorValue());
    }

    /** For the tour: open a page. */
    public void showDesigner() {
        if (!this.initialized) {
            this.initialized = true;
        }
        switchTo(State.DESIGN_FLAG);
    }

    public void showTeamColor() {
        this.initialized = true;
        switchTo(State.CHANGE_TEAM_COLOR);
    }
}
