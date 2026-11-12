package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.station.StationRecipe;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Destination choice after a rocket reaches space (1.12.2 GuiCelestialSelection, simplified): the Sol system's Sun,
 * Earth and Moon on their orbits, drawn with the 1.12.2 body icons inside the grey 1.12.2 frame, and the space stations
 * the player may visit circling Earth. Click a body to select it, then LAUNCH. Bodies above the rocket's tier are shown
 * in red and can't be chosen. With Earth selected, a player without a station sees what one costs and can CREATE it;
 * an owner can RENAME their station. The 1.12.2 zoomable map and space-race panel are not ported. Escape does not close
 * it: the player is in space.
 */
public class CelestialSelectionScreen extends Screen {
    private static final ResourceLocation FRAME = GalacticraftCore.id("textures/gui/celestialselection.png");
    private static final int GREY0 = 0xFF282828;
    private static final int GREY1 = 0xFF505050;
    private static final int GREY2 = 0xFF646464;
    private static final int GREY5 = 0xFF969696;
    private static final int WHITE = 0xFFFFFFFF;

    /**
     * A body on the map; {@code parent} null orbits the Sun, {@code orbit} in fractions of the Earth orbit, {@code phase}
     * its starting angle. Space stations carry their station.
     */
    private record Body(String name, @Nullable ResourceKey<Level> dimension, @Nullable Body parent, float orbit,
                        float period, float phase, int size, Component label, @Nullable GCNetwork.StationInfo station) {
        ResourceLocation icon() {
            return GalacticraftCore.id("textures/gui/celestialbodies/" + this.name + ".png");
        }
    }

    private static final Body SUN = new Body("sun", null, null, 0.0F, 1.0F, 0.0F, 24, Component.translatable("star.galacticraftcore.sol"), null);
    private static final Body EARTH = new Body("earth", Level.OVERWORLD, null, 1.0F, 1.0F, 0.0F, 12,
            Component.translatable("planet.galacticraftcore.overworld"), null);
    private static final Body MOON = new Body("moon", GCDimensions.MOON, EARTH, 0.18F, 1.0F / 13.0F, 0.0F, 8,
            Component.translatable("moon.galacticraftcore.moon"), null);
    /** 1.12.2 MarsModule: 1.25 times Earth's distance, a 1.88 year orbit, phase shift 0.1667. */
    private static final Body MARS = new Body("mars", GCDimensions.MARS, null, 1.25F, 1.8811610076670317F, 0.1667F, 10,
            Component.translatable("planet.galacticraftcore.mars"), null);

    /** 1.12.2 AsteroidsModule: 1.375 times Earth's distance, orbit time 45 (a random phase there; a fixed one here). */
    private static final Body ASTEROIDS = new Body("asteroid", GCDimensions.ASTEROIDS, null, 1.375F, 45.0F, 4.0F, 8,
            Component.translatable("planet.galacticraftcore.asteroids"), null);

    /** 1.12.2 VenusModule: 0.75 times Earth's distance, a 0.615 year orbit, phase shift 2. */
    private static final Body VENUS = new Body("venus", GCDimensions.VENUS, null, 0.75F, 0.61527929901423877327491785323111F, 2.0F, 10,
            Component.translatable("planet.galacticraftcore.venus"), null);

    private final List<Body> bodies = new java.util.ArrayList<>();
    private int tier;
    private boolean canCreateStation;
    @Nullable
    private net.minecraft.client.gui.components.EditBox renameBox;
    private final long[] stars = new long[200];
    @Nullable
    private Body selected;
    private int ticks;

    public CelestialSelectionScreen(GCNetwork.OpenCelestialSelection contents) {
        super(Component.translatable("gui.galacticraftcore.message.catalog"));
        final RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < this.stars.length; i++) {
            this.stars[i] = random.nextLong();
        }
        update(contents);
    }

    /** New contents from the server (a station was built or renamed); keeps the selection where it can. */
    public void update(GCNetwork.OpenCelestialSelection contents) {
        this.tier = contents.tier();
        this.canCreateStation = contents.canCreateStation();
        final int selectedStation = this.selected != null && this.selected.station() != null ? this.selected.station().id() : -1;
        this.bodies.clear();
        this.bodies.add(SUN);
        this.bodies.add(EARTH);
        this.bodies.add(MOON);
        this.bodies.add(MARS);
        this.bodies.add(ASTEROIDS);
        this.bodies.add(VENUS);
        final List<GCNetwork.StationInfo> stations = contents.stations();
        for (int i = 0; i < stations.size(); i++) {
            final GCNetwork.StationInfo station = stations.get(i);
            // 1.12.2 satelliteSpaceStation: closer to Earth than the Moon and much faster
            final Body body = new Body("space_station", GCDimensions.ORBIT, EARTH, 0.09F, 1.0F / 20.0F, i * Mth.TWO_PI / stations.size(), 6,
                    Component.literal(station.name()), station);
            this.bodies.add(body);
            if (station.id() == selectedStation) {
                this.selected = body;
            }
        }
        if (this.selected != null && this.selected.station() != null && !this.bodies.contains(this.selected)) {
            this.selected = null;
        }
        closeRename();
    }

    private int border() {
        return this.width / 65;
    }

    private int edge() {
        return border() / 4;
    }

    private float orbitRadius() {
        return Math.min(this.width, this.height) * 0.32F;
    }

    private float[] position(Body body, float partialTick) {
        if (body.parent() == null && body.orbit() == 0.0F) {
            return new float[]{this.width / 2.0F, this.height / 2.0F};
        }
        final float[] centre = body.parent() == null ? new float[]{this.width / 2.0F, this.height / 2.0F} : position(body.parent(), partialTick);
        // 1.12.2: angle = time * relativeOrbitTime; one Earth "year" here is 3,600 ticks.
        final float angle = (this.ticks + partialTick) / 3600.0F / body.period() * Mth.TWO_PI + body.phase();
        final float radius = body.orbit() * orbitRadius();
        return new float[]{centre[0] + Mth.cos(angle) * radius, centre[1] + Mth.sin(angle) * radius};
    }

    private boolean reachable(Body body) {
        if (body.dimension() == null) {
            return false;
        }
        final SpaceTravel.Destination destination = SpaceTravel.destination(body.dimension());
        return destination != null && destination.tier() <= this.tier;
    }

    /** Select the (first) body of {@code dimension}, as clicking it would (used by the screenshot tour). */
    public void select(ResourceKey<Level> dimension) {
        for (Body body : this.bodies) {
            if (dimension.equals(body.dimension())) {
                this.selected = body;
                return;
            }
        }
    }

    @Override
    public void tick() {
        this.ticks++;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
        for (long star : this.stars) {
            final int x = (int) Math.floorMod(star, (long) Math.max(1, this.width));
            final int y = (int) Math.floorMod(star >>> 20, (long) Math.max(1, this.height));
            final int shade = 0x60 + (int) Math.floorMod(star >>> 40, 0x9FL);
            graphics.fill(x, y, x + 1, y + 1, 0xFF000000 | shade << 16 | shade << 8 | shade);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawOrbits(graphics, partialTick);
        for (Body body : this.bodies) {
            final float[] pos = position(body, partialTick);
            final int size = body == this.selected ? body.size() * 3 / 2 : body.size();
            graphics.blit(body.icon(), Math.round(pos[0] - size / 2.0F), Math.round(pos[1] - size / 2.0F), size, size, 0, 0, 16, 16, 16, 16);
            if (body == this.selected || isOver(body, mouseX, mouseY, partialTick)) {
                drawCursor(graphics, Math.round(pos[0]), Math.round(pos[1]), size / 2 + 3, body == this.selected ? 0xFF00A0FF : GREY5);
            }
        }
        drawBorder(graphics);
        drawButtons(graphics, mouseX, mouseY);
        drawStationPanel(graphics, mouseX, mouseY);
        if (this.renameBox != null) {
            this.renameBox.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** 1.12.2 drawCircles: 90-segment orbits around the Sun and around Earth. */
    private void drawOrbits(GuiGraphics graphics, float partialTick) {
        for (Body body : this.bodies) {
            if (body.orbit() <= 0.0F) {
                continue;
            }
            final float[] centre = body.parent() == null ? new float[]{this.width / 2.0F, this.height / 2.0F} : position(body.parent(), partialTick);
            final float radius = body.orbit() * orbitRadius();
            final int segments = Math.max(90, (int) (radius * 4));
            for (int i = 0; i < segments; i++) {
                final float angle = i * Mth.TWO_PI / segments;
                final int x = Math.round(centre[0] + Mth.cos(angle) * radius);
                final int y = Math.round(centre[1] + Mth.sin(angle) * radius);
                graphics.fill(x, y, x + 1, y + 1, 0x80B4B4B4);
            }
        }
    }

    private static void drawCursor(GuiGraphics graphics, int x, int y, int half, int color) {
        final int len = Math.max(2, half / 2);
        graphics.fill(x - half, y - half, x - half + len, y - half + 1, color);
        graphics.fill(x - half, y - half, x - half + 1, y - half + len, color);
        graphics.fill(x + half - len + 1, y - half, x + half + 1, y - half + 1, color);
        graphics.fill(x + half, y - half, x + half + 1, y - half + len, color);
        graphics.fill(x - half, y + half, x - half + len, y + half + 1, color);
        graphics.fill(x - half, y + half - len + 1, x - half + 1, y + half + 1, color);
        graphics.fill(x + half - len + 1, y + half, x + half + 1, y + half + 1, color);
        graphics.fill(x + half, y + half - len + 1, x + half + 1, y + half + 1, color);
    }

    /** 1.12.2 drawBorder. */
    private void drawBorder(GuiGraphics graphics) {
        final int b = border();
        final int e = edge();
        graphics.fill(0, 0, b, this.height, GREY2);
        graphics.fill(this.width - b, 0, this.width, this.height, GREY2);
        graphics.fill(0, 0, this.width, b, GREY2);
        graphics.fill(0, this.height - b, this.width, this.height, GREY2);
        graphics.fill(b, b, b + e, this.height - b, GREY0);
        graphics.fill(b, b, this.width - b, b + e, GREY0);
        graphics.fill(this.width - b - e, b, this.width - b, this.height - b, GREY1);
        graphics.fill(b + e, this.height - b - e, this.width - b, this.height - b, GREY1);
    }

    private int lhs() {
        return border() + edge();
    }

    /** The catalogue header, the selected body's name and the LAUNCH button (1.12.2 drawButtons). */
    private void drawButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        final int lhs = lhs();
        final int rhs = this.width - lhs;
        final int top = lhs;
        graphics.setColor(0.0F, 0.6F, 1.0F, 1.0F);
        graphics.blit(FRAME, this.width / 2 - 43, top, 86, 15, 266, 0, 172, 29, 512, 512);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        final String header = this.title.getString().toUpperCase(Locale.ROOT);
        graphics.drawString(this.font, header, this.width / 2 - this.font.width(header) / 2, top + this.font.lineHeight / 2, WHITE, false);
        if (this.selected == null) {
            final Component hint = Component.translatable("gui.galacticraftcore.message.select_destination");
            graphics.drawString(this.font, hint, this.width / 2 - this.font.width(hint) / 2, this.height - lhs - 14, GREY5, false);
            return;
        }
        // Body name in the left wedge
        graphics.setColor(0.0F, 0.6F, 1.0F, 1.0F);
        graphics.blit(FRAME, lhs, top, 74, 11, 0, 392, 148, 22, 512, 512);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        final String name = this.selected.label().getString().toUpperCase(Locale.ROOT);
        graphics.drawString(this.font, name, lhs + 37 - this.font.width(name) / 2, top + 2, WHITE, false);
        final SpaceTravel.Destination destination = this.selected.dimension() == null ? null : SpaceTravel.destination(this.selected.dimension());
        final Component tierLine = destination == null ? Component.translatable("gui.galacticraftcore.message.not_reachable")
                : Component.translatable("gui.galacticraftcore.message.tier", destination.tier());
        graphics.drawString(this.font, tierLine, lhs + 4, top + 16, reachable(this.selected) ? GREY5 : 0xFFFF6464, false);
        // LAUNCH (mirrored wedge, green when reachable)
        final boolean ok = reachable(this.selected);
        graphics.setColor(ok ? 0.0F : 1.0F, ok ? 1.0F : 0.0F, 0.0F, 1.0F);
        blitMirrored(graphics, rhs - 74, top, 74, 11, 0, 392, 148, 22);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        final String launch = Component.translatable("gui.galacticraftcore.message.launch").getString().toUpperCase(Locale.ROOT);
        graphics.drawString(this.font, launch, rhs - 40 - this.font.width(launch) / 2, top + 2, WHITE, false);
    }

    /** Left-right mirrored blit (reversed u range; a negative scale would get the quad culled). */
    private static void blitMirrored(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight) {
        graphics.blit(FRAME, x, y, width, height, u + uWidth, v, -uWidth, vHeight, 512, 512);
    }

    private boolean isOver(Body body, double mouseX, double mouseY, float partialTick) {
        final float[] pos = position(body, partialTick);
        final float half = body.size() / 2.0F + 2.0F;
        return Math.abs(mouseX - pos[0]) <= half && Math.abs(mouseY - pos[1]) <= half;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int lhs = lhs();
        final int rhs = this.width - lhs;
        if (this.selected != null && mouseX > rhs - 74 && mouseX < rhs && mouseY > lhs && mouseY < lhs + 11) {
            if (reachable(this.selected) && this.selected.dimension() != null) {
                PacketDistributor.sendToServer(new GCNetwork.ChooseDestination(this.selected.dimension().location(),
                        this.selected.station() == null ? -1 : this.selected.station().id()));
                return true;
            }
            return false;
        }
        // Moons first: they sit on top of their planet.
        if (this.renameBox != null) {
            if (this.renameBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            closeRename();
        }
        if (clickStationPanel(mouseX, mouseY)) {
            return true;
        }
        // Stations and moons first: they sit on top of their planet.
        for (int i = this.bodies.size() - 1; i >= 0; i--) {
            if (isOver(this.bodies.get(i), mouseX, mouseY, 0.0F)) {
                this.selected = this.bodies.get(i);
                closeRename();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ------------------------------------------------------------------ space stations

    private static final int PANEL_WIDTH = 120;

    private int panelX() {
        return this.width - lhs() - PANEL_WIDTH - 4;
    }

    private int panelY() {
        return lhs() + 18;
    }

    /** Earth with no station yet: the 1.12.2 requirements box and CREATE; an owned station: owner line and RENAME. */
    private void drawStationPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.selected == null) {
            return;
        }
        final int x = panelX();
        int y = panelY();
        if (this.selected == EARTH && this.canCreateStation) {
            final List<net.minecraft.util.FormattedCharSequence> title = this.font.split(
                    Component.translatable("gui.galacticraftcore.message.can_create_space_station"), PANEL_WIDTH - 8);
            final int titleHeight = title.size() * this.font.lineHeight + 5;
            final int height = titleHeight + StationRecipe.REQUIREMENTS.size() * 18 + 18;
            graphics.fill(x, y, x + PANEL_WIDTH, y + height, 0xC0000000);
            graphics.renderOutline(x, y, PANEL_WIDTH, height, GREY2);
            for (int i = 0; i < title.size(); i++) {
                graphics.drawString(this.font, title.get(i), x + 4, y + 4 + i * this.font.lineHeight, GREY5, false);
            }
            y += titleHeight;
            final net.minecraft.world.entity.player.Player player = this.minecraft.player;
            boolean affordable = true;
            for (StationRecipe.Requirement requirement : StationRecipe.REQUIREMENTS) {
                final int have = player == null ? 0 : requirement.countIn(player.getInventory());
                affordable &= have >= requirement.count();
                graphics.renderItem(requirement.icon().get(), x + 4, y);
                graphics.drawString(this.font, Math.min(have, requirement.count()) + " / " + requirement.count(), x + 24, y + 4,
                        have >= requirement.count() ? 0xFF00FF00 : 0xFFFF6464, false);
                y += 18;
            }
            affordable |= player != null && player.getAbilities().instabuild;
            drawWedgeButton(graphics, x + PANEL_WIDTH / 2 - 37, y + 2, Component.translatable("gui.galacticraftcore.message.create_ss"), affordable);
        } else if (this.selected.station() != null) {
            final GCNetwork.StationInfo station = this.selected.station();
            graphics.fill(x, y, x + PANEL_WIDTH, y + (station.owned() ? 34 : 16), 0xC0000000);
            graphics.renderOutline(x, y, PANEL_WIDTH, station.owned() ? 34 : 16, GREY2);
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.message.ss_owner").append(" " + station.owner()), x + 4, y + 4, GREY5, false);
            if (station.owned() && this.renameBox == null) {
                drawWedgeButton(graphics, x + PANEL_WIDTH / 2 - 37, y + 18, Component.translatable("gui.galacticraftcore.message.rename"), true);
            }
        }
    }

    private void drawWedgeButton(GuiGraphics graphics, int x, int y, Component text, boolean enabled) {
        graphics.setColor(enabled ? 0.0F : 1.0F, enabled ? 1.0F : 0.0F, 0.0F, 1.0F);
        graphics.blit(FRAME, x, y, 74, 11, 0, 392, 148, 22, 512, 512);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        final String label = text.getString().toUpperCase(Locale.ROOT);
        graphics.drawString(this.font, label, x + 37 - this.font.width(label) / 2, y + 2, WHITE, false);
    }

    private boolean clickStationPanel(double mouseX, double mouseY) {
        if (this.selected == null) {
            return false;
        }
        final int x = panelX();
        final int y = panelY();
        if (this.selected == EARTH && this.canCreateStation) {
            final int titleHeight = this.font.split(Component.translatable("gui.galacticraftcore.message.can_create_space_station"), PANEL_WIDTH - 8).size()
                    * this.font.lineHeight + 5;
            final int buttonY = y + titleHeight + StationRecipe.REQUIREMENTS.size() * 18 + 2;
            if (inside(mouseX, mouseY, x + PANEL_WIDTH / 2 - 37, buttonY, 74, 11)) {
                final net.minecraft.world.entity.player.Player player = this.minecraft.player;
                if (player != null && (player.getAbilities().instabuild || StationRecipe.matches(player))) {
                    PacketDistributor.sendToServer(new GCNetwork.CreateStation());
                }
                return true;
            }
        } else if (this.selected.station() != null && this.selected.station().owned() && this.renameBox == null
                && inside(mouseX, mouseY, x + PANEL_WIDTH / 2 - 37, y + 18, 74, 11)) {
            this.renameBox = new net.minecraft.client.gui.components.EditBox(this.font, x + 4, y + 18, PANEL_WIDTH - 8, 12,
                    Component.translatable("gui.galacticraftcore.message.rename"));
            this.renameBox.setMaxLength(32);
            this.renameBox.setValue(this.selected.station().name());
            this.renameBox.setFocused(true);
            setFocused(this.renameBox);
            return true;
        }
        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void closeRename() {
        if (this.renameBox != null) {
            setFocused(null);
            this.renameBox = null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.renameBox != null) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                if (this.selected != null && this.selected.station() != null && !this.renameBox.getValue().isBlank()) {
                    PacketDistributor.sendToServer(new GCNetwork.RenameStation(this.selected.station().id(), this.renameBox.getValue()));
                }
                closeRename();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                closeRename();
                return true;
            }
            return this.renameBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.renameBox != null) {
            return this.renameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
