/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection, the body
 * data registered in core.GalacticraftCore and the planets modules, and planets.asteroids.event
 * .AsteroidsEventHandlerClient's belt). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.station.StationRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * The galaxy map (1.12.2 GuiCelestialSelection). The Sol system lies on a tilted blue grid with its orbits; scroll to
 * zoom, drag or use the movement keys to pan. Click a body (or its line in the catalogue on the left) to select it, again
 * to zoom in on it and its moons and space stations; BACK or Escape unselects. The right-hand panel shows the station
 * list around a planet, and over Earth what a station costs and CREATE; an owner renames their station from the top
 * bar. LAUNCH (or Enter) flies there when the rocket's tier allows it.
 *
 * <p>Opened after a rocket reaches space, it can't be closed. Opened with the Galaxy Map key ({@link #MAP_KEY}, M), it is
 * only a map: nothing can be launched to or built, stations are not shown, and EXIT or Escape closes it.
 *
 * <p>1.12.2 counted its animations in rendered frames (adding each frame's partial tick); here they run on real time at
 * the rate that gave at 60 frames a second, 1.5 of its units a game tick.
 */
public class CelestialSelectionScreen extends Screen {
    public static final KeyMapping MAP_KEY = new KeyMapping("key.galacticraftcore.galaxy_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
            io.github.reiko45.galacticraft.client.VehicleControls.CATEGORY);

    private static final ResourceLocation MAIN0 = GalacticraftCore.id("textures/gui/celestialselection.png");
    private static final ResourceLocation MAIN1 = GalacticraftCore.id("textures/gui/celestialselection1.png");
    private static final int MAX_STATION_NAME_LENGTH = 32;
    private static final float UNITS_PER_TICK = 1.5F;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY5 = 0xFF969696;
    private static final int GREY4 = 0xFF8C8C8C;
    private static final int GREY3 = 0xFF787878;
    private static final int GREY2 = 0xFF646464;
    private static final int GREY1 = 0xFF505050;
    private static final int GREY0 = 0xFF282828;
    private static final int GREEN = 0xFF00FF00;
    private static final int RED = 0xFFFF0000;
    private static final int RED3 = 0xFFFF6464;
    private static final int CYAN = 0xFF96C8FF;

    public enum Kind {
        STAR, PLANET, MOON, SATELLITE
    }

    private enum Selection {
        UNSELECTED, SELECTED, ZOOMED;

        Selection next() {
            return values()[Math.min(ordinal() + 1, values().length - 1)];
        }
    }

    /**
     * A body on the map with its 1.12.2 numbers: {@code distance} from its parent (1.12.2 unScaledDistance; Earth's orbit
     * is 1), {@code orbitTime} relative to Earth's year (a moon's to its own), the starting {@code phase}, and the tier
     * needed (0: unknown, shown as "?"; -1: none, for the Sun). Bodies without a world can't be reached.
     */
    public record Body(String name, Kind kind, @Nullable Body parent, float distance, float orbitTime, float phase, int tier,
                       @Nullable ResourceKey<Level> dimension, ResourceLocation icon, Component label) {
        boolean child() {
            return this.kind == Kind.MOON || this.kind == Kind.SATELLITE;
        }

        /** 1.12.2 CelestialBody#isReachable: it has a world. */
        boolean hasWorld() {
            return this.dimension != null;
        }
    }

    private static Body body(String name, Kind kind, @Nullable Body parent, float distance, float orbitTime, float phase, int tier,
                             @Nullable ResourceKey<Level> dimension, String icon, String key) {
        return new Body(name, kind, parent, distance, orbitTime, phase, tier, dimension,
                GalacticraftCore.id("textures/gui/celestialbodies/" + icon + ".png"), Component.translatable(key));
    }

    public static final Body SOL = body("sol", Kind.STAR, null, 1.0F, 1.0F, 0.0F, -1, null, "sun", "star.galacticraftcore.sol");
    /** GalacticraftCore#postInit's dummy planets: on the map, but with no world to go to. */
    public static final Body MERCURY = body("mercury", Kind.PLANET, null, 0.5F, 0.24096386F, 1.45F, 0, null, "mercury", "planet.galacticraftcore.mercury");
    public static final Body VENUS = body("venus", Kind.PLANET, null, 0.75F, 0.6152793F, 2.0F, 3, GCDimensions.VENUS, "venus", "planet.galacticraftcore.venus");
    public static final Body EARTH = body("overworld", Kind.PLANET, null, 1.0F, 1.0F, 0.0F, 1, Level.OVERWORLD, "earth", "planet.galacticraftcore.overworld");
    public static final Body MARS = body("mars", Kind.PLANET, null, 1.25F, 1.8811610F, 0.1667F, 2, GCDimensions.MARS, "mars", "planet.galacticraftcore.mars");
    /** AsteroidsModule: a random starting point in 1.12.2; a fixed one here. */
    public static final Body ASTEROIDS = body("asteroids", Kind.PLANET, null, 1.375F, 45.0F, 4.0F, 3, GCDimensions.ASTEROIDS, "asteroid",
            "planet.galacticraftcore.asteroids");
    public static final Body JUPITER = body("jupiter", Kind.PLANET, null, 1.5F, 11.861994F, Mth.PI, 0, null, "jupiter", "planet.galacticraftcore.jupiter");
    public static final Body SATURN = body("saturn", Kind.PLANET, null, 1.75F, 29.463308F, 5.45F, 0, null, "saturn", "planet.galacticraftcore.saturn");
    public static final Body URANUS = body("uranus", Kind.PLANET, null, 2.0F, 84.06353F, 1.38F, 0, null, "uranus", "planet.galacticraftcore.uranus");
    public static final Body NEPTUNE = body("neptune", Kind.PLANET, null, 2.25F, 164.84118F, 1.0F, 0, null, "neptune", "planet.galacticraftcore.neptune");
    public static final Body MOON = body("moon", Kind.MOON, EARTH, 13.0F, 100.0F, 0.0F, 1, GCDimensions.MOON, "moon", "moon.galacticraftcore.moon");
    /** The stations around Earth, one body on the map; the list on the right picks which. */
    public static final Body STATION = body("spacestation.overworld", Kind.SATELLITE, EARTH, 9.0F, 20.0F, 0.0F, 1, GCDimensions.ORBIT,
            "space_station", "satellite.galacticraftcore.overworld_station");

    /** In 1.12.2 CelestialBody#compareTo order (nearest first). */
    private static final List<Body> PLANETS = List.of(MERCURY, VENUS, EARTH, MARS, ASTEROIDS, JUPITER, SATURN, URANUS, NEPTUNE);
    private static final List<Body> BODIES = List.of(SOL, MERCURY, VENUS, EARTH, MARS, ASTEROIDS, JUPITER, SATURN, URANUS, NEPTUNE, MOON, STATION);

    private final boolean mapMode;
    private int tier;
    private boolean canCreateStations;
    private List<GCNetwork.StationInfo> stations = List.of();

    private float zoom;
    private float planetZoom;
    private boolean doneZooming;
    private float preSelectZoom;
    private Vector2f preSelectPosition = new Vector2f();
    private float ticksSinceSelection;
    private float ticksSinceUnselection = -1.0F;
    private float ticksSinceMenuOpen;
    private float ticksTotal;
    private float animateGrandchildren;
    private Vector2f position = new Vector2f();
    private final Vector2f translation = new Vector2f();
    private final Map<Body, float[]> planetPos = new HashMap<>();
    @Nullable
    private Body selectedBody;
    @Nullable
    private Body lastSelectedBody;
    /** The planet whose moons the catalogue lists, or null for the Sol system's planets. */
    @Nullable
    private Body selectedParent;
    private int canCreateOffset = 24;
    private Selection selectionState = Selection.UNSELECTED;
    private int zoomTooltipPos;
    private int selectedStation = -1;
    private int stationListOffset;
    private boolean renaming;
    @Nullable
    private String renamingString;
    private boolean dragging;
    private float zoomNow;
    private long lastFrame = -1L;
    private int clientTicks;

    /** After a rocket reaches space: the destinations the rocket's tier allows, the visitable stations. */
    public CelestialSelectionScreen(GCNetwork.OpenCelestialSelection contents) {
        super(Component.translatable("gui.galacticraftcore.message.catalog"));
        this.mapMode = false;
        update(contents);
    }

    /** The Galaxy Map key's map (1.12.2 mapMode). */
    public CelestialSelectionScreen() {
        super(Component.translatable("gui.galacticraftcore.message.catalog"));
        this.mapMode = true;
    }

    public boolean mapMode() {
        return this.mapMode;
    }

    /** New contents from the server (a station was built or renamed): the selection stays where it can. */
    public void update(GCNetwork.OpenCelestialSelection contents) {
        this.tier = contents.tier();
        this.canCreateStations = contents.canCreateStation();
        this.stations = List.copyOf(contents.stations());
        if (station(this.selectedStation) == null) {
            this.selectedStation = this.selectedBody == STATION && this.stations.size() == 1 ? this.stations.get(0).id() : -1;
        }
        this.stationListOffset = Math.max(0, Math.min(this.stationListOffset, this.stations.size() - 1));
    }

    @Nullable
    private GCNetwork.StationInfo station(int id) {
        for (GCNetwork.StationInfo station : this.stations) {
            if (station.id() == id) {
                return station;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ the bodies

    /** 1.12.2 possibleBodies: where this rocket can go. Nowhere on the map key's map. */
    private boolean possible(Body body) {
        if (this.mapMode || body.dimension() == null) {
            return false;
        }
        if (body.kind() == Kind.SATELLITE) {
            return !this.stations.isEmpty() && this.tier >= body.tier();
        }
        final SpaceTravel.Destination destination = SpaceTravel.destination(body.dimension());
        return destination != null && destination.tier() <= this.tier && SpaceTravel.reachable(body.dimension());
    }

    private static List<Body> children(@Nullable Body of) {
        if (of == null) {
            return PLANETS;
        }
        return of.kind() == Kind.PLANET ? BODIES.stream().filter(b -> b.kind() == Kind.MOON && b.parent() == of).toList() : List.of();
    }

    private static List<Body> siblings(@Nullable Body of) {
        if (of == null) {
            return List.of();
        }
        if (of.kind() == Kind.PLANET) {
            return PLANETS;
        }
        if (of.child()) {
            final List<Body> list = new ArrayList<>(BODIES.stream().filter(b -> b.child() && b.parent() == of.parent()).toList());
            list.sort((a, b) -> Float.compare(a.distance(), b.distance()));
            return list;
        }
        return List.of();
    }

    private static float scale(Body body) {
        return 3.0F * body.distance() * (body.kind() == Kind.PLANET ? 25.0F : 1.0F / 5.0F);
    }

    private Vector3f bodyPosition(@Nullable Body body) {
        if (body == null || body.kind() == Kind.STAR) {
            return new Vector3f();
        }
        final float timeScale = body.kind() == Kind.PLANET ? 200.0F : 2.0F;
        final float distance = scale(body);
        final float angle = this.ticksTotal / (timeScale * body.orbitTime()) + body.phase();
        final Vector3f pos = new Vector3f(Mth.sin(angle) * distance, Mth.cos(angle) * distance, 0.0F);
        return pos.add(bodyPosition(body.kind() == Kind.PLANET ? SOL : body.parent()));
    }

    private int widthOf(Body body) {
        final boolean zoomed = body == this.selectedBody && this.selectionState == Selection.SELECTED;
        return body.kind() == Kind.STAR ? (zoomed ? 12 : 8) : zoomed ? 6 : 4;
    }

    /** 1.12.2 getAlpha: moons and stations show only around a selected planet; the rest fade as one is zoomed in on. */
    private float alpha(Body body) {
        if (body.child()) {
            final boolean selected = body == this.selectedBody || body.parent() == this.selectedBody && this.selectionState != Selection.SELECTED;
            final boolean ready = this.lastSelectedBody != null || this.ticksSinceSelection > 35.0F;
            final boolean sibling = siblings(this.selectedBody).contains(body);
            final boolean isPossible = body.kind() != Kind.SATELLITE || possible(body);
            if (!selected && !sibling || !isPossible) {
                return 0.0F;
            }
            if (isZoomed() && (!selected || !ready) && !sibling) {
                return Mth.clamp((this.ticksSinceSelection - 30.0F) / 15.0F, 0.0F, 1.0F);
            }
            return 1.0F;
        }
        final boolean isSelected = this.selectedBody == body;
        final boolean childSelected = this.selectedBody != null && this.selectedBody.child();
        final boolean ownChildSelected = childSelected && this.selectedBody.parent() == body;
        if (!isSelected && !ownChildSelected && (isZoomed() || childSelected)) {
            return this.lastSelectedBody != null || childSelected ? 0.0F : 1.0F - Math.min(this.ticksSinceSelection / 25.0F, 1.0F);
        }
        return 1.0F;
    }

    private boolean isZoomed() {
        return this.selectionState == Selection.ZOOMED;
    }

    private boolean isSelected() {
        return this.selectionState != Selection.UNSELECTED;
    }

    private String parentName() {
        if (this.selectedBody != null && this.selectedBody.child() && this.selectedBody.parent() != null) {
            return this.selectedBody.parent().label().getString();
        }
        return Component.translatable("solarsystem.galacticraftcore.sol").getString();
    }

    private String grandparentName() {
        return Component.translatable(this.selectedParent != null ? "solarsystem.galacticraftcore.sol" : "galaxy.galacticraftcore.milky_way").getString();
    }

    // ------------------------------------------------------------------ zoom and camera

    private float zoomAdvanced() {
        if (this.ticksTotal < 30.0F) {
            return Mth.lerp((float) Math.pow(Mth.clamp(this.ticksTotal / 30.0F, 0.0F, 1.0F), 0.5D), -0.75F, 0.0F);
        }
        if (this.selectedBody == null || !isZoomed()) {
            if (!this.doneZooming) {
                final float unselectScale = Mth.lerp(Mth.clamp(this.ticksSinceUnselection / 100.0F, 0.0F, 1.0F), this.zoom, this.preSelectZoom);
                if (unselectScale <= this.preSelectZoom + 0.05F) {
                    this.zoom = this.preSelectZoom;
                    this.ticksSinceUnselection = -1.0F;
                    this.doneZooming = true;
                }
                return unselectScale;
            }
            return this.zoom;
        }
        if (!this.doneZooming) {
            final float f = Mth.lerp(Mth.clamp((this.ticksSinceSelection - 20.0F) / 40.0F, 0.0F, 1.0F), this.zoom, 12.0F);
            if (f >= 11.95F) {
                this.doneZooming = true;
            }
            return f;
        }
        return 12.0F + this.planetZoom;
    }

    private Vector2f translationAdvanced() {
        if (this.selectedBody == null) {
            if (this.ticksSinceUnselection > 0.0F) {
                final float f0 = Mth.clamp(this.ticksSinceUnselection / 100.0F, 0.0F, 1.0F);
                if (f0 >= 0.999999F) {
                    this.ticksSinceUnselection = 0.0F;
                }
                return lerp(this.position, this.preSelectPosition, f0);
            }
            return new Vector2f(this.position).add(this.translation);
        }
        if (!isZoomed()) {
            if (this.selectedBody.child()) {
                final Vector3f parent = bodyPosition(this.selectedBody.parent());
                return new Vector2f(parent.x, parent.y);
            }
            return new Vector2f(this.position).add(this.translation);
        }
        Vector2f from = this.position;
        if (this.lastSelectedBody != null) {
            final Vector3f last = bodyPosition(this.lastSelectedBody);
            from = new Vector2f(last.x, last.y);
        }
        final Vector3f to = bodyPosition(this.selectedBody);
        return lerp(from, new Vector2f(to.x, to.y), Mth.clamp((this.ticksSinceSelection - 18.0F) / 7.5F, 0.0F, 1.0F));
    }

    private static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y));
    }

    /** 1.12.2 setIsometric: the map plane tilted back 55 degrees and turned 45, zoomed, the camera's point centred. */
    private Matrix4f isometric() {
        this.zoomNow = zoomAdvanced();
        this.zoom = this.zoomNow;
        this.position = translationAdvanced();
        this.translation.zero();
        return new Matrix4f().translate(this.width / 2.0F, this.height / 2, 0.0F).rotateX((float) Math.toRadians(55.0D))
                .rotateZ((float) Math.toRadians(-45.0D)).scale(1.1F + this.zoomNow).translate(-this.position.x, -this.position.y, 0.0F);
    }

    private static float[] project(Matrix4f world, float x, float y) {
        final Vector3f v = world.transformPosition(new Vector3f(x, y, 0.0F));
        return new float[] {v.x, v.y};
    }

    // ------------------------------------------------------------------ ticking and input

    @Override
    public void tick() {
        this.clientTicks++;
        if (!this.renaming && (this.selectedBody == null || !isZoomed()) && this.minecraft != null) {
            // 1.12.2 updateScreen: the movement keys pan (2 a tick, applied to each frame until the next: ~6 at 60 fps)
            final long window = this.minecraft.getWindow().getWindow();
            if (keyDown(window, this.minecraft.options.keyLeft)) {
                this.translation.add(-6.0F, -6.0F);
            }
            if (keyDown(window, this.minecraft.options.keyRight)) {
                this.translation.add(6.0F, 6.0F);
            }
            if (keyDown(window, this.minecraft.options.keyUp)) {
                this.translation.add(6.0F, -6.0F);
            }
            if (keyDown(window, this.minecraft.options.keyDown)) {
                this.translation.add(-6.0F, 6.0F);
            }
        }
    }

    private static boolean keyDown(long window, KeyMapping key) {
        final InputConstants.Key bound = key.getKey();
        return bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(window, bound.getValue());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.mapMode;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 1.12.2: 120 wheel units a notch, over 500 (or 250 with a body selected)
        final float wheel = (float) scrollY * 120.0F / (this.selectedBody == null ? 500.0F : 250.0F);
        if (wheel != 0.0F) {
            if (this.selectedBody == null || !isZoomed()) {
                this.zoom = Mth.clamp(this.zoom + wheel * (this.zoom + 2.0F) / 10.0F, -1.0F, 3.0F);
            } else {
                this.planetZoom = Mth.clamp(this.planetZoom + wheel, -4.9F, 5.0F);
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging && button == 0) {
            float multiplier = -Math.abs(this.zoom);
            if (this.zoom == -1.0F) {
                multiplier = -1.5F;
            }
            if (this.zoom >= -0.25F && this.zoom <= 0.15F) {
                multiplier = -0.2F;
            }
            if (this.zoom >= 0.15F) {
                multiplier = -0.15F;
            }
            // 1.12.2 added each drag step to every frame drawn until the next tick, about three at 60 fps
            final float factor = multiplier * (GCConfig.client(GCConfig.INVERT_MAP_SCROLL) ? -1.0F : 1.0F)
                    * GCConfig.client(GCConfig.MAP_SCROLL_SENSITIVITY).floatValue() * 0.2F * 3.0F;
            this.translation.add((float) (dragX - dragY) * factor, (float) (dragY + dragX) * factor);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.renaming) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.renaming = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (this.renamingString != null && !this.renamingString.isEmpty()) {
                    this.renamingString = this.renamingString.substring(0, this.renamingString.length() - 1);
                }
            } else if (Screen.isPaste(keyCode)) {
                final String paste = this.minecraft == null ? "" : this.minecraft.keyboardHandler.getClipboard();
                final String filtered = net.minecraft.util.StringUtil.filterText(paste);
                this.renamingString = (this.renamingString == null ? "" : this.renamingString) + filtered;
                this.renamingString = this.renamingString.substring(0, Math.min(this.renamingString.length(), MAX_STATION_NAME_LENGTH));
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.selectedBody != null) {
            unselect();
            if (!this.mapMode) {
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            teleportToSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.renaming && net.minecraft.util.StringUtil.isAllowedChatCharacter(codePoint)) {
            final String name = (this.renamingString == null ? "" : this.renamingString) + codePoint;
            this.renamingString = name.substring(0, Math.min(name.length(), MAX_STATION_NAME_LENGTH));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void unselect() {
        this.selectionState = Selection.UNSELECTED;
        this.ticksSinceUnselection = 0.0F;
        this.lastSelectedBody = this.selectedBody;
        this.selectedBody = null;
        this.doneZooming = false;
        this.selectedStation = -1;
        this.animateGrandchildren = 0.0F;
    }

    private void teleportToSelected() {
        if (this.selectedBody == null || !this.selectedBody.hasWorld() || !possible(this.selectedBody) || this.minecraft == null) {
            return;
        }
        if (this.selectedBody.kind() == Kind.SATELLITE && station(this.selectedStation) == null) {
            return;
        }
        PacketDistributor.sendToServer(new GCNetwork.ChooseDestination(this.selectedBody.dimension().location(),
                this.selectedBody.kind() == Kind.SATELLITE ? this.selectedStation : -1));
        // 1.12.2 went on to GuiTeleporting; the server asks again if the choice didn't go through
        this.minecraft.setScreen(null);
    }

    /** Select {@code dimension}'s body as one click would, its animations finished (the screenshot tour). */
    public void select(ResourceKey<Level> dimension) {
        for (Body body : BODIES) {
            if (dimension.equals(body.dimension())) {
                select(body);
                return;
            }
        }
    }

    /**
     * Select {@code body} as one click on the overview would, its animations finished (the screenshot tour): the camera
     * back where it was before anything was selected, as BACK would leave it.
     */
    public void select(Body body) {
        this.zoom = this.preSelectZoom;
        this.position = new Vector2f(this.preSelectPosition);
        this.planetZoom = 0.0F;
        this.lastSelectedBody = null;
        this.ticksSinceUnselection = -1.0F;
        this.selectedBody = body;
        this.selectedParent = body.child() ? body.parent() : null;
        this.selectionState = body.child() ? Selection.ZOOMED : Selection.SELECTED;
        if (body.kind() == Kind.SATELLITE && !this.stations.isEmpty()) {
            this.selectedStation = this.stations.get(0).id();
        }
        settle();
    }

    /** Zoom in on {@code dimension}'s body, as a second click would, its animations finished (the screenshot tour). */
    public void zoomIn(ResourceKey<Level> dimension) {
        select(dimension);
        this.selectionState = Selection.ZOOMED;
        settle();
    }

    private void settle() {
        this.ticksTotal = Math.max(this.ticksTotal, 100.0F);
        this.ticksSinceSelection = 100.0F;
        this.ticksSinceMenuOpen = 100.0F;
        this.doneZooming = false;
        this.zoomTooltipPos = 38;
        this.animateGrandchildren = 14.0F * children(this.selectedBody).size();
    }

    // ------------------------------------------------------------------ clicks

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int x = (int) mouseX;
        final int y = (int) mouseY;
        boolean handled = false;
        final int lhs = lhs();
        final int rhs = this.width - lhs;
        final int top = lhs;
        if (this.selectedBody != null && x > lhs && x < lhs + 88 && y > top && y < top + 13) {
            unselect();
            return true;
        }
        if (!this.mapMode && x >= rhs - 95 && x < rhs && y > top + 181 + this.canCreateOffset && y < top + 194 + this.canCreateOffset
                && this.selectedBody != null && canCreateStation(this.selectedBody) && this.minecraft != null && this.minecraft.player != null) {
            final Player player = this.minecraft.player;
            if (StationRecipe.matches(player) || player.getAbilities().instabuild) {
                PacketDistributor.sendToServer(new GCNetwork.CreateStation());
                // zoom in on Earth to show the new station
                if (!isZoomed()) {
                    this.selectionState = Selection.ZOOMED;
                    this.preSelectZoom = this.zoom;
                    this.preSelectPosition = new Vector2f(this.position);
                    this.ticksSinceSelection = 0.0F;
                    this.doneZooming = false;
                }
                return true;
            }
            handled = true;
        }
        if (this.mapMode && x > rhs - 88 && x < rhs && y > top && y < top + 13) {
            onClose();
            return true;
        }
        if (this.selectedBody != null && !this.mapMode && x > rhs - 88 && x < rhs && y > top && y < top + 13) {
            teleportToSelected();
            handled = true;
        }
        if (this.selectedBody != null && this.selectedBody.kind() == Kind.SATELLITE) {
            handled |= clickStations(x, y, rhs, top);
        }

        final int xPos = lhs + 2;
        int yPos = top + 10;
        final boolean planetZoomedMoon = isZoomed() && this.selectedParent != null;
        // the yellow button: the planet whose moons are listed
        if (x >= xPos && x <= xPos + 93 && y >= yPos && y <= yPos + 12 && this.selectedParent != null) {
            selectParent(planetZoomedMoon);
            handled = true;
        }
        yPos += 22;
        if (x >= xPos && x <= xPos + 93 && y >= yPos && y <= yPos + 12) {
            if (planetZoomedMoon) {
                selectParent(true);
            }
            handled = true;
        }
        if (!handled) {
            final List<Body> children = children(isZoomed() && this.selectedParent == null ? this.selectedBody : this.selectedParent);
            yPos = top + 50;
            for (Body child : children) {
                handled = testClicked(child, child == this.selectedBody ? 5 : 0, yPos, x, y, false);
                yPos += 14;
                if (!handled && !isZoomed() && child == this.selectedBody) {
                    final List<Body> grandchildren = children(child);
                    int gOffset = 0;
                    for (Body grandchild : grandchildren) {
                        if (gOffset + 14 > this.animateGrandchildren) {
                            break;
                        }
                        handled = testClicked(grandchild, 10, yPos, x, y, true);
                        yPos += 14;
                        gOffset += 14;
                        if (handled) {
                            break;
                        }
                    }
                    yPos += (int) this.animateGrandchildren - gOffset;
                }
                if (handled) {
                    break;
                }
            }
        }
        if (!handled) {
            handled = clickBodies(mouseX, mouseY);
        }
        if (!handled) {
            if (this.selectedBody != null) {
                unselect();
                this.planetZoom = 0.0F;
            }
            this.dragging = true;
        }
        if (this.selectedBody != null && this.selectedBody.child()) {
            this.selectedParent = this.selectedBody.parent();
        } else if (this.selectedBody == null || this.selectedBody.kind() == Kind.PLANET) {
            this.selectedParent = null;
        }
        return true;
    }

    private void selectParent(boolean planetZoomedMoon) {
        if (this.selectedBody == null) {
            this.preSelectZoom = this.zoom;
            this.preSelectPosition = new Vector2f(this.position);
        }
        final Selection old = this.selectionState;
        final Body parent = this.selectedParent;
        if (isSelected()) {
            unselect();
        }
        if (old == Selection.ZOOMED) {
            this.selectionState = Selection.SELECTED;
        }
        this.selectedBody = parent;
        this.ticksSinceSelection = 0.0F;
        this.selectionState = this.selectionState.next();
        if (isZoomed() && !planetZoomedMoon) {
            this.ticksSinceMenuOpen = 0.0F;
        }
    }

    private boolean testClicked(Body body, int xOffset, int yPos, int x, int y, boolean grandchild) {
        final int xPos = lhs() + 2 + xOffset;
        if (x >= xPos && x <= xPos + 93 && y >= yPos && y <= yPos + 12 && (this.selectedBody != body || !isZoomed())) {
            if (this.selectedBody == null) {
                this.preSelectZoom = this.zoom;
                this.preSelectPosition = new Vector2f(this.position);
            }
            if (this.selectionState == Selection.ZOOMED) {
                this.selectionState = Selection.SELECTED;
            }
            this.doneZooming = false;
            this.planetZoom = 0.0F;
            if (body != this.selectedBody) {
                this.lastSelectedBody = this.selectedBody;
                this.selectionState = Selection.SELECTED;
            } else {
                this.selectionState = this.selectionState.next(); // clicked again: zoom in
            }
            this.selectedBody = body;
            this.ticksSinceSelection = 0.0F;
            if (grandchild) {
                this.selectionState = Selection.ZOOMED;
            }
            if (isZoomed()) {
                this.ticksSinceMenuOpen = 0.0F;
            }
            this.animateGrandchildren = 0.0F;
            return true;
        }
        return false;
    }

    /** Bodies on the map, the ones drawn on top (moons and stations) first. */
    private boolean clickBodies(double mouseX, double mouseY) {
        for (int i = BODIES.size() - 1; i >= 0; i--) {
            final Body clicked = BODIES.get(i);
            final float[] pos = this.planetPos.get(clicked);
            if (pos == null || this.selectedBody == null && clicked.child()) {
                continue;
            }
            final float size = pos[2];
            if (mouseX >= pos[0] - size && mouseX <= pos[0] + size && mouseY >= pos[1] - size && mouseY <= pos[1] + size
                    && (this.selectedBody != clicked || !isZoomed())) {
                if (isSelected() && this.selectedBody != clicked) {
                    if (this.selectedBody == null || !this.selectedBody.child() || this.selectedBody.parent() != clicked) {
                        unselect();
                    } else if (isZoomed()) {
                        this.selectionState = Selection.SELECTED;
                    }
                }
                if (clicked != this.selectedBody) {
                    final boolean childOfIt = this.selectedBody != null && this.selectedBody.child() && this.selectedBody.parent() == clicked;
                    this.lastSelectedBody = this.selectedBody;
                    this.animateGrandchildren = 0.0F;
                    if (!childOfIt) {
                        // only zoom out if the new body isn't the planet of the selected moon
                        this.selectionState = Selection.UNSELECTED;
                    }
                } else {
                    this.doneZooming = false;
                    this.planetZoom = 0.0F;
                }
                this.selectedBody = clicked;
                this.ticksSinceSelection = 0.0F;
                this.selectionState = this.selectionState.next();
                if (clicked.child()) {
                    this.selectionState = Selection.ZOOMED;
                }
                if (isZoomed()) {
                    this.ticksSinceMenuOpen = 0.0F;
                }
                if (clicked.kind() == Kind.SATELLITE && this.stations.size() == 1) {
                    this.selectedStation = this.stations.get(0).id();
                }
                return true;
            }
        }
        return false;
    }

    /** The station list, its arrows, the rename bar and the rename dialog. */
    private boolean clickStations(int x, int y, int rhs, int top) {
        if (this.renaming) {
            if (x >= this.width / 2 - 90 && x <= this.width / 2 + 90 && y >= this.height / 2 - 38 && y <= this.height / 2 + 38) {
                if (x >= this.width / 2 - 90 + 17 && x <= this.width / 2 - 90 + 17 + 72 && y >= this.height / 2 + 21 && y <= this.height / 2 + 33) {
                    final GCNetwork.StationInfo station = station(this.selectedStation);
                    if (station != null && station.owned() && this.renamingString != null && !this.renamingString.isBlank()) {
                        PacketDistributor.sendToServer(new GCNetwork.RenameStation(station.id(), this.renamingString));
                    }
                    this.renaming = false;
                }
                if (x >= this.width / 2 && x <= this.width / 2 + 72 && y >= this.height / 2 + 21 && y <= this.height / 2 + 33) {
                    this.renaming = false;
                }
                return true;
            }
            return false;
        }
        boolean handled = false;
        if (x >= this.width / 2 - 47 && x <= this.width / 2 + 47 && y >= top && y <= top + 11) {
            final GCNetwork.StationInfo station = station(this.selectedStation);
            if (station != null && station.owned()) {
                this.renaming = true;
                this.renamingString = null;
                handled = true;
            }
        }
        final int max = Math.min(this.height / 2 / 14, this.stations.size());
        if (x >= rhs - 85 && x <= rhs - 85 + 61 && y >= top + 45 && y <= top + 49) {
            if (this.stationListOffset > 0) {
                this.stationListOffset--;
            }
            handled = true;
        }
        if (x >= rhs - 85 && x <= rhs - 85 + 61 && y >= top + 49 + max * 14 && y <= top + 53 + max * 14) {
            if (max + this.stationListOffset < this.stations.size()) {
                this.stationListOffset++;
            }
            handled = true;
        }
        for (int i = 0; i < max && i + this.stationListOffset < this.stations.size(); i++) {
            final GCNetwork.StationInfo station = this.stations.get(i + this.stationListOffset);
            final int xPos = rhs - 95 + (station.id() == this.selectedStation ? -5 : 0);
            final int yPos = top + 50 + i * 14;
            if (x >= xPos && x <= xPos + 93 && y >= yPos && y <= yPos + 12) {
                this.selectedStation = station.id();
                handled = true;
            }
        }
        return handled;
    }

    /** 1.12.2 canCreateSpaceStation: around Earth only, from a rocket that can reach it, once per player. */
    private boolean canCreateStation(Body at) {
        return !this.mapMode && this.canCreateStations && at == EARTH && possible(at)
                && !GCConfig.server(GCConfig.DISABLE_SPACE_STATION_CREATION);
    }

    // ------------------------------------------------------------------ drawing

    private int border() {
        return this.width / 65;
    }

    private int edge() {
        return border() / 4;
    }

    private int lhs() {
        return border() + edge();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        advanceTime();
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // the map plane's projected quads face either way, and nothing on it is depth-sorted
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        final Matrix4f world = isometric();
        drawGrid(graphics, world, 7000.0F, this.height / 3 / 3.5F);
        drawCircles(graphics, world);
        drawBodies(graphics, world);
        drawSelectionCursor(graphics, world);
        RenderSystem.enableDepthTest();
        drawButtons(graphics, mouseX, mouseY);
        drawBorder(graphics);
        graphics.flush();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void advanceTime() {
        final long now = Util.getNanos();
        final float dt = this.lastFrame < 0L ? 0.0F : Math.min(5.0F, (now - this.lastFrame) / 50_000_000.0F * UNITS_PER_TICK);
        this.lastFrame = now;
        this.ticksSinceMenuOpen += dt;
        this.ticksTotal += dt;
        if (this.selectedBody != null) {
            this.ticksSinceSelection += dt;
        }
        if (this.selectedBody == null && this.ticksSinceUnselection >= 0.0F) {
            this.ticksSinceUnselection += dt;
        }
        // 1.12.2 grew the moons' list 2 pixels a frame (0.5 of its units at 60 fps)
        this.animateGrandchildren += dt * 4.0F;
    }

    private float pixel() {
        return this.minecraft == null ? 1.0F : 1.0F / (float) this.minecraft.getWindow().getGuiScale();
    }

    /** 1.12.2 drawGrid: blue lines across the whole map plane, a third of the screen height / 3.5 apart. */
    private void drawGrid(GuiGraphics graphics, Matrix4f world, float gridSize, float gridScale) {
        final BufferBuilder buffer = colourBuffer();
        final Matrix4f pose = graphics.pose().last().pose();
        final float half = 0.5F * pixel();
        gridSize += gridScale / 2.0F;
        for (float v = -gridSize; v <= gridSize; v += gridScale) {
            line(buffer, pose, project(world, v, -gridSize), project(world, v, gridSize), half, 0.0F, 0.2F, 0.5F, 0.55F);
            line(buffer, pose, project(world, -gridSize, v), project(world, gridSize, v), half, 0.0F, 0.2F, 0.5F, 0.55F);
        }
        drawColour(buffer);
    }

    /** 1.12.2 drawCircles: each visible body's orbit in two alternating blues, three pixels wide; the belt instead. */
    private void drawCircles(GuiGraphics graphics, Matrix4f world) {
        final BufferBuilder buffer = colourBuffer();
        final Matrix4f pose = graphics.pose().last().pose();
        final float half = 1.5F * pixel();
        int count = 0;
        for (Body body : BODIES) {
            final float alpha = alpha(body);
            if (alpha <= 0.0F) {
                continue;
            }
            if (body == ASTEROIDS) {
                drawBelt(buffer, pose, world, alpha, 0.5F * pixel());
                continue;
            }
            final Vector3f centre = body.child() ? bodyPosition(body.parent()) : new Vector3f();
            final float[] colour = count % 2 == 0 ? new float[] {0.0F, 0.6F, 1.0F} : new float[] {0.3F, 0.8F, 1.0F};
            circle(buffer, pose, world, centre, scale(body), half, colour[0] / 1.4F, colour[1] / 1.4F, colour[2] / 1.4F, alpha / 1.4F);
            count++;
        }
        drawColour(buffer);
    }

    /** AsteroidsEventHandlerClient#onRingRender: a red band between 72 and 78 times the belt's distance. */
    private static void drawBelt(BufferBuilder buffer, Matrix4f pose, Matrix4f world, float alpha, float half) {
        final float inner = 72.0F * ASTEROIDS.distance();
        final float outer = 78.0F * ASTEROIDS.distance();
        circle(buffer, pose, world, new Vector3f(), outer, half, 0.7F, 0.0F, 0.0F, alpha / 2.0F);
        circle(buffer, pose, world, new Vector3f(), inner, half, 0.7F, 0.0F, 0.0F, alpha / 2.0F);
        for (int i = 0; i < 90; i++) {
            final float a0 = i * Mth.TWO_PI / 90.0F;
            final float a1 = (i + 1) * Mth.TWO_PI / 90.0F;
            final float[] p0 = project(world, Mth.cos(a0) * outer, Mth.sin(a0) * outer);
            final float[] p1 = project(world, Mth.cos(a0) * inner, Mth.sin(a0) * inner);
            final float[] p2 = project(world, Mth.cos(a1) * inner, Mth.sin(a1) * inner);
            final float[] p3 = project(world, Mth.cos(a1) * outer, Mth.sin(a1) * outer);
            quad(buffer, pose, p0, p1, p2, p3, 0.7F, 0.0F, 0.0F, alpha / 10.0F);
        }
    }

    private static void circle(BufferBuilder buffer, Matrix4f pose, Matrix4f world, Vector3f centre, float radius, float half,
                               float r, float g, float b, float a) {
        float[] previous = project(world, centre.x + radius, centre.y);
        for (int i = 1; i <= 90; i++) {
            final float angle = i * Mth.TWO_PI / 90.0F;
            final float[] next = project(world, centre.x + Mth.cos(angle) * radius, centre.y + Mth.sin(angle) * radius);
            line(buffer, pose, previous, next, half, r, g, b, a);
            previous = next;
        }
    }

    private static BufferBuilder colourBuffer() {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void drawColour(BufferBuilder buffer) {
        final MeshData mesh = buffer.build();
        if (mesh != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(mesh);
        }
    }

    /** A line as a thin quad (core OpenGL has no wide lines). */
    private static void line(BufferBuilder buffer, Matrix4f pose, float[] from, float[] to, float half, float r, float g, float b, float a) {
        final float dx = to[0] - from[0];
        final float dy = to[1] - from[1];
        final float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        final float nx = -dy / length * half;
        final float ny = dx / length * half;
        quad(buffer, pose, new float[] {from[0] - nx, from[1] - ny}, new float[] {from[0] + nx, from[1] + ny},
                new float[] {to[0] + nx, to[1] + ny}, new float[] {to[0] - nx, to[1] - ny}, r, g, b, a);
    }

    private static void quad(BufferBuilder buffer, Matrix4f pose, float[] p0, float[] p1, float[] p2, float[] p3, float r, float g, float b, float a) {
        buffer.addVertex(pose, p0[0], p0[1], 0.0F).setColor(r, g, b, a);
        buffer.addVertex(pose, p1[0], p1[1], 0.0F).setColor(r, g, b, a);
        buffer.addVertex(pose, p2[0], p2[1], 0.0F).setColor(r, g, b, a);
        buffer.addVertex(pose, p3[0], p3[1], 0.0F).setColor(r, g, b, a);
    }

    /** 1.12.2 drawCelestialBodies: each icon facing the screen, moons and stations a quarter size; the belt turns. */
    private void drawBodies(GuiGraphics graphics, Matrix4f world) {
        this.planetPos.clear();
        final float k = 1.1F + this.zoomNow;
        for (Body body : BODIES) {
            final float alpha = alpha(body);
            if (alpha <= 0.0F) {
                continue;
            }
            final Vector3f at = bodyPosition(body);
            final float[] pos = project(world, at.x, at.y);
            final float s = k * (body.child() ? 0.25F : 1.0F);
            final int size = widthOf(body);
            graphics.pose().pushPose();
            graphics.pose().translate(pos[0], pos[1], 0.0F);
            graphics.pose().scale(s, s, 1.0F);
            if (body == ASTEROIDS) {
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(this.clientTicks / 10.0F % 360.0F));
            }
            texture(graphics, body.icon(), -size / 2, -size / 2, size, size, 0, 0, 16, 16, false, false, 16.0F, 1.0F, 1.0F, 1.0F, alpha);
            graphics.pose().popPose();
            final float clickSize = 2.0F * s * (body.kind() == Kind.STAR ? 2.0F : 1.0F) * (body == this.selectedBody ? 1.5F : 1.0F);
            this.planetPos.put(body, new float[] {pos[0], pos[1], clickSize});
        }
    }

    /** 1.12.2 drawSelectionCursor: a pulsing yellow frame on a selected body, a blue one closing in on a zoomed one. */
    private void drawSelectionCursor(GuiGraphics graphics, Matrix4f world) {
        if (this.selectedBody == null || this.selectionState == Selection.UNSELECTED) {
            return;
        }
        final Vector3f at = bodyPosition(this.selectedBody);
        final float[] pos = project(world, at.x, at.y);
        final float k = 1.1F + this.zoomNow;
        final float half;
        final float[] colour;
        if (this.selectionState == Selection.SELECTED) {
            final float pulse = this.zoomNow < 4.9F ? Mth.sin(this.ticksSinceSelection / 2.0F) * 0.5F + 0.5F : 1.0F;
            half = (float) Math.floor(widthOf(this.selectedBody) / 2.0D * (this.selectedBody.child() ? 9.0D : 30.0D)) / 15.0F * k;
            colour = new float[] {1.0F, 1.0F, 0.0F, pulse};
        } else {
            final float div = this.zoom + 1.0F - this.planetZoom;
            final float scale = Math.max(0.3F, 1.5F / (this.ticksSinceSelection / 5.0F)) * 2.0F / div;
            final float pulse = this.zoomNow < 4.9F ? Mth.sin(this.ticksSinceSelection) * 0.5F + 0.5F : 1.0F;
            half = widthOf(this.selectedBody) * 13 * scale * k;
            colour = new float[] {0.4F, 0.8F, 1.0F, pulse};
        }
        texture(graphics, MAIN0, pos[0] - half, pos[1] - half, half * 2.0F, half * 2.0F, 266, 29, 100, 100, false, false, 512.0F,
                colour[0], colour[1], colour[2], colour[3]);
    }

    /** 1.12.2 drawTexturedModalRect: a tinted, optionally mirrored part of a texture. */
    private static void texture(GuiGraphics graphics, ResourceLocation texture, float x, float y, float width, float height, float u, float v,
                                float uWidth, float vHeight, boolean invertX, boolean invertY, float textureSize, float r, float g, float b, float a) {
        final float height0 = invertY ? 0 : vHeight;
        final float height1 = invertY ? vHeight : 0;
        final float width0 = invertX ? uWidth : 0;
        final float width1 = invertX ? 0 : uWidth;
        final float scale = 1.0F / textureSize;
        r = Mth.clamp(r, 0.0F, 1.0F);
        g = Mth.clamp(g, 0.0F, 1.0F);
        b = Mth.clamp(b, 0.0F, 1.0F);
        graphics.flush(); // text and fills queued before this are drawn under it, as 1.12.2 drew them in order
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        final Matrix4f pose = graphics.pose().last().pose();
        final BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(pose, x, y + height, 0.0F).setUv((u + width0) * scale, (v + height0) * scale).setColor(r, g, b, a);
        buffer.addVertex(pose, x + width, y + height, 0.0F).setUv((u + width1) * scale, (v + height0) * scale).setColor(r, g, b, a);
        buffer.addVertex(pose, x + width, y, 0.0F).setUv((u + width1) * scale, (v + height1) * scale).setColor(r, g, b, a);
        buffer.addVertex(pose, x, y, 0.0F).setUv((u + width0) * scale, (v + height1) * scale).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void texture(GuiGraphics graphics, ResourceLocation texture, float x, float y, float width, float height, float u, float v,
                                float uWidth, float vHeight, boolean invertX, boolean invertY, float r, float g, float b, float a) {
        texture(graphics, texture, x, y, width, height, u, v, uWidth, vHeight, invertX, invertY, 512.0F, r, g, b, a);
    }

    private static final float[] BLUE = {0.0F, 0.6F, 1.0F, 1.0F};

    private static void texture(GuiGraphics graphics, ResourceLocation texture, float x, float y, float width, float height, float u, float v,
                                float uWidth, float vHeight, boolean invertX, boolean invertY, float[] colour) {
        texture(graphics, texture, x, y, width, height, u, v, uWidth, vHeight, invertX, invertY, colour[0], colour[1], colour[2], colour[3]);
    }

    private void text(GuiGraphics graphics, String text, int x, int y, int colour) {
        graphics.drawString(this.font, text, x, y, colour, false);
    }

    /** 1.12.2 drawSplitString: lines wrapped to {@code width}, each centred on {@code x}; the number of lines. */
    private int splitString(GuiGraphics graphics, String text, int x, int y, int width, int colour, boolean simulate) {
        final List<FormattedCharSequence> lines = this.font.split(Component.literal(text), width);
        if (!simulate) {
            for (FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, x - this.font.width(line) / 2, y, colour, false);
                y += this.font.lineHeight;
            }
        }
        return lines.size();
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

    private static String upper(String key) {
        return Component.translatable(key).getString().toUpperCase(Locale.ROOT);
    }

    /** 1.12.2 drawButtons (its "preview" view; the "profile" one was never shown). */
    private void drawButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean handledSliderPos = false;
        final int lhs = lhs();
        final int rhs = this.width - lhs;
        final int top = lhs;
        // Catalog
        texture(graphics, MAIN0, lhs, top, 74, 11, 0, 392, 148, 22, false, false, BLUE);
        String str = upper("gui.galacticraftcore.message.catalog");
        text(graphics, str, lhs + 40 - this.font.width(str) / 2, top + 1, WHITE);
        final int scale = (int) Math.min(95.0F, this.ticksSinceMenuOpen * 12.0F);
        final boolean planetZoomedNotMoon = isZoomed() && this.selectedParent == null && this.selectedBody != null;
        // Parent frame
        texture(graphics, MAIN0, lhs - 95 + scale, top + 12, 95, 41, 0, 436, 95, 41, false, false, BLUE);
        str = planetZoomedNotMoon ? this.selectedBody.label().getString() : parentName();
        text(graphics, str, lhs + 9 - 95 + scale, top + 34, WHITE);
        // Grandparent frame
        texture(graphics, MAIN0, lhs + 2 - 95 + scale, top + 14, 93, 17, 95, 436, 93, 17, false, false, 1.0F, 1.0F, 0.0F, 1.0F);
        str = planetZoomedNotMoon ? parentName() : grandparentName();
        text(graphics, str, lhs + 7 - 95 + scale, top + 16, GREY3);
        drawChildren(graphics, children(planetZoomedNotMoon ? this.selectedBody : this.selectedParent), 0, 0, true);

        if (this.mapMode) {
            texture(graphics, MAIN0, rhs - 74, top, 74, 11, 0, 392, 148, 22, true, false, 1.0F, 0.0F, 0.0F, 1.0F);
            str = upper("gui.galacticraftcore.message.exit");
            text(graphics, str, rhs - 40 - this.font.width(str) / 2, top + 1, WHITE);
        }

        if (this.selectedBody != null) {
            final Body selected = this.selectedBody;
            final boolean satellite = selected.kind() == Kind.SATELLITE;
            if (satellite) {
                drawStationList(graphics, rhs, top);
            } else {
                texture(graphics, MAIN1, rhs - 96, top, 96, 139, 63, 0, 96, 139, false, false, BLUE);
            }
            if (canCreateStation(selected) && !satellite) {
                drawCreateStation(graphics, mouseX, mouseY, rhs, top);
            }
            // The catalogue fades under the selected body's
            texture(graphics, MAIN0, lhs, top, 74, 11, 0, 392, 148, 22, false, false, 1.0F, 1.0F, 1.0F,
                    0.3F - Math.min(0.3F, this.ticksSinceSelection / 50.0F));
            str = upper("gui.galacticraftcore.message.catalog");
            text(graphics, str, lhs + 40 - this.font.width(str) / 2, top + 1, WHITE);
            // Top bar: the body's name, or RENAME (green for the owner) over a station
            final GCNetwork.StationInfo station = station(this.selectedStation);
            final float[] barColour = !satellite ? BLUE : station != null && station.owned() ? new float[] {0.0F, 1.0F, 0.0F, 1.0F}
                    : new float[] {1.0F, 0.0F, 0.0F, 1.0F};
            texture(graphics, MAIN0, this.width / 2 - 47, top, 94, 11, 0, 414, 188, 22, false, false, barColour);
            if (selected.tier() >= 0 && !satellite) {
                final boolean canReach = selected.hasWorld() && possible(selected);
                final float[] tierColour = canReach ? new float[] {0.0F, 1.0F, 0.0F, 1.0F} : new float[] {1.0F, 0.0F, 0.0F, 1.0F};
                texture(graphics, MAIN0, this.width / 2 - 30, top + 11, 30, 11, 0, 414, 60, 22, false, false, tierColour);
                texture(graphics, MAIN0, this.width / 2, top + 11, 30, 11, 128, 414, 60, 22, false, false, tierColour);
                str = Component.translatable("gui.galacticraftcore.message.tier", selected.tier() == 0 ? "?" : String.valueOf(selected.tier())).getString();
                text(graphics, str, this.width / 2 - this.font.width(str) / 2, top + 13, canReach ? GREY4 : RED3);
            }
            str = satellite ? upper("gui.galacticraftcore.message.rename") : selected.label().getString();
            text(graphics, str, this.width / 2 - this.font.width(str) / 2, top + 2, WHITE);
            // the BACK wedge over the catalogue
            texture(graphics, MAIN0, lhs + 4, top, 83, 12, 0, 477, 83, 12, false, false, BLUE);
            if (!this.mapMode) {
                final boolean ok = selected.hasWorld() && possible(selected) && (!satellite || station != null);
                texture(graphics, MAIN0, rhs - 74, top, 74, 11, 0, 392, 148, 22, true, false,
                        ok ? new float[] {0.0F, 1.0F, 0.0F, 1.0F} : new float[] {1.0F, 0.0F, 0.0F, 1.0F});
                str = upper("gui.galacticraftcore.message.launch");
                text(graphics, str, rhs - 40 - this.font.width(str) / 2, top + 2, WHITE);
            }
            if (this.selectionState == Selection.SELECTED && !satellite) {
                handledSliderPos = true;
                drawZoomHint(graphics, selected, rhs);
            }
            if (satellite && this.renaming) {
                drawRenameDialog(graphics);
            }
        }
        if (!handledSliderPos) {
            this.zoomTooltipPos = 0;
        }
    }

    /** "Click again to zoom", sliding up from the bottom right. */
    private void drawZoomHint(GuiGraphics graphics, Body selected, int rhs) {
        int sliderPos = this.zoomTooltipPos;
        if (this.zoomTooltipPos != 38) {
            sliderPos = Math.min((int) this.ticksSinceSelection * 2, 38);
            this.zoomTooltipPos = sliderPos;
        }
        final int bottom = this.height - border() - edge();
        texture(graphics, MAIN0, rhs - 182, bottom - sliderPos, 83, 38, 512 - 166, 512 - 76, 166, 76, true, false, BLUE);
        final boolean satellites = selected == EARTH;
        final boolean moons = !children(selected).isEmpty() && selected.kind() == Kind.PLANET;
        final int hint = satellites && moons ? 0 : moons ? 1 : satellites ? 2 : 3;
        final int[] offsets = {2, 6, 6, 11};
        splitString(graphics, Component.translatable("gui.galacticraftcore.message.click_again." + hint).getString(), rhs - 182 + 41,
                bottom + offsets[hint] - sliderPos, 79, GREY5, false);
    }

    /** 1.12.2 drawChildren: the catalogue's lines sliding in, green when there is a world to go to, red when not. */
    private int drawChildren(GuiGraphics graphics, List<Body> children, int xOffsetBase, int yOffsetPrior, boolean recursive) {
        xOffsetBase += lhs();
        final int yOffsetBase = lhs() + 50 + yOffsetPrior;
        int yOffset = 0;
        for (int i = 0; i < children.size(); i++) {
            final Body child = children.get(i);
            final int xOffset = xOffsetBase + (child == this.selectedBody ? 5 : 0);
            final int scale = (int) Math.min(95.0F, Math.max(0.0F, this.ticksSinceMenuOpen * 25.0F - 95 * i));
            final float brightness = child == this.selectedBody ? 0.2F : 0.0F;
            final float fade = scale / 95.0F;
            if (child.hasWorld()) {
                texture(graphics, MAIN0, 3 + xOffset, yOffsetBase + yOffset + 1, 86, 10, 0, 489, 86, 10, false, false, 0.0F, 0.6F + brightness, 0.0F, fade);
            } else {
                texture(graphics, MAIN0, 3 + xOffset, yOffsetBase + yOffset + 1, 86, 10, 0, 489, 86, 10, false, false, 0.6F + brightness, 0.0F, 0.0F, fade);
            }
            texture(graphics, MAIN0, 2 + xOffset, yOffsetBase + yOffset, 93, 12, 95, 464, 93, 12, false, false, 3 * brightness,
                    0.6F + 2 * brightness, 1.0F, fade);
            if (scale > 0) {
                text(graphics, child.label().getString(), 7 + xOffset, yOffsetBase + yOffset + 2, 0xFFE0E0E0);
            }
            yOffset += 14;
            if (recursive && child == this.selectedBody) {
                final List<Body> grandchildren = children(child);
                if (!grandchildren.isEmpty()) {
                    final int full = 14 * grandchildren.size();
                    if (this.animateGrandchildren >= full) {
                        this.animateGrandchildren = full;
                        yOffset += drawChildren(graphics, grandchildren, 10, yOffset, false);
                    } else {
                        final int shown = (int) this.animateGrandchildren / 14;
                        if (shown > 0) {
                            drawChildren(graphics, grandchildren.subList(0, shown), 10, yOffset, false);
                        }
                        yOffset += (int) this.animateGrandchildren;
                    }
                }
            }
        }
        return yOffset;
    }

    /** The stations around the selected planet, the owner of the chosen one, and arrows to scroll the list. */
    private void drawStationList(GuiGraphics graphics, int rhs, int top) {
        final int max = Math.min(this.height / 2 / 14, this.stations.size());
        final GCNetwork.StationInfo chosen = station(this.selectedStation);
        texture(graphics, MAIN1, rhs - 95, top, 95, 53, chosen == null ? 95 : 0, 186, 95, 53, false, false, BLUE);
        final float[] grey = {0.65F, 0.65F, 0.65F, 1.0F};
        texture(graphics, MAIN1, rhs - 85, top + 45, 61, 4, 0, 239, 61, 4, false, false, this.stationListOffset <= 0 ? grey : BLUE);
        texture(graphics, MAIN1, rhs - 85, top + 49 + max * 14, 61, 4, 0, 239, 61, 4, false, true,
                max + this.stationListOffset >= this.stations.size() ? grey : BLUE);
        if (chosen == null) {
            splitString(graphics, Component.translatable("gui.galacticraftcore.message.select_ss").getString(), rhs - 47, top + 20, 91, WHITE, false);
        } else {
            text(graphics, Component.translatable("gui.galacticraftcore.message.ss_owner").getString(), rhs - 85, top + 18, WHITE);
            text(graphics, chosen.owner(), rhs - 47 - this.font.width(chosen.owner()) / 2, top + 30, WHITE);
        }
        for (int i = 0; i < max && i + this.stationListOffset < this.stations.size(); i++) {
            final GCNetwork.StationInfo station = this.stations.get(i + this.stationListOffset);
            final int xOffset = station.id() == this.selectedStation ? -5 : 0;
            texture(graphics, MAIN0, rhs - 95 + xOffset, top + 50 + i * 14, 93, 12, 95, 464, 93, 12, true, false, BLUE);
            String name = "";
            int point = 0;
            while (this.font.width(name) < 80 && point < station.name().length()) {
                name += station.name().charAt(point);
                point++;
            }
            if (this.font.width(name) >= 80) {
                name = name.substring(0, Math.max(0, name.length() - 3)) + "...";
            }
            text(graphics, name, rhs - 88 + xOffset, top + 52 + i * 14, WHITE);
        }
    }

    /** What a station costs (have / need for each), the flashing "can be created" and CREATE. */
    private void drawCreateStation(GuiGraphics graphics, int mouseX, int mouseY, int rhs, int top) {
        final Player player = this.minecraft == null ? null : this.minecraft.player;
        final boolean creative = player != null && player.getAbilities().instabuild;
        final String canCreate = Component.translatable("gui.galacticraftcore.message.can_create_space_station").getString();
        final int canCreateLength = Math.max(0, splitString(graphics, canCreate, 0, 0, 91, 0, true) - 2);
        this.canCreateOffset = canCreateLength * this.font.lineHeight;
        texture(graphics, MAIN1, rhs - 95, top + 134, 93, 4, 159, 102, 93, 4, false, false, BLUE);
        for (int barY = 0; barY < canCreateLength; ++barY) {
            texture(graphics, MAIN1, rhs - 95, top + 138 + barY * this.font.lineHeight, 93, this.font.lineHeight, 159, 106, 93,
                    this.font.lineHeight, false, false, BLUE);
        }
        texture(graphics, MAIN1, rhs - 95, top + 138 + this.canCreateOffset, 93, 43, 159, 106, 93, 43, false, false, BLUE);
        texture(graphics, MAIN1, rhs - 79, top + 129, 61, 4, 0, 170, 61, 4, false, false, BLUE);
        boolean valid = true;
        ItemStack hovered = ItemStack.EMPTY;
        final List<StationRecipe.Requirement> requirements = StationRecipe.REQUIREMENTS;
        for (int i = 0; i < requirements.size(); i++) {
            final StationRecipe.Requirement requirement = requirements.get(i);
            final int xPos = (int) (rhs - 95 + i * 93 / (double) requirements.size() + 5);
            final int yPos = top + 154 + this.canCreateOffset;
            final ItemStack icon = requirement.icon().get();
            graphics.renderItem(icon, xPos, yPos);
            graphics.renderItemDecorations(this.font, icon, xPos, yPos);
            if (mouseX >= xPos && mouseX <= xPos + 16 && mouseY >= yPos && mouseY <= yPos + 16) {
                hovered = icon;
            }
            final int have = player == null ? 0 : requirement.countIn(player.getInventory());
            final boolean enough = have >= requirement.count();
            valid &= enough;
            final String count = String.valueOf(requirement.count());
            text(graphics, count, xPos + 8 - this.font.width(count) / 2, top + 170 + this.canCreateOffset, enough || creative ? GREEN : RED);
        }
        final boolean ok = valid || creative;
        texture(graphics, MAIN1, rhs - 95, top + 182 + this.canCreateOffset, 93, 12, 0, 174, 93, 12, false, false,
                ok ? new float[] {0.0F, 1.0F, 0.1F, 1.0F} : new float[] {1.0F, 0.0F, 0.0F, 1.0F});
        final int pulse = (int) ((Math.sin(this.ticksSinceMenuOpen / 5.0D) * 0.5D + 0.5D) * 255.0D);
        splitString(graphics, canCreate, rhs - 48, top + 137, 91, 0xFF000000 | pulse << 16 | 0xFF00 | pulse, false);
        splitString(graphics, upper("gui.galacticraftcore.message.create_ss"), rhs - 48, top + 185 + this.canCreateOffset, 91, WHITE, false);
        if (!hovered.isEmpty()) {
            drawNameTooltip(graphics, hovered.getHoverName().getString(), mouseX, mouseY);
        }
    }

    /** 1.12.2's own tooltip for the station materials: the item's name in a purple frame above the cursor. */
    private void drawNameTooltip(GuiGraphics graphics, String name, int mouseX, int mouseY) {
        final int k = this.font.width(name);
        int x = mouseX - k / 2;
        int y = mouseY - 12;
        final int h = 8;
        if (x + k > this.width) {
            x -= x - this.width + k;
        }
        if (y + h + 6 > this.height) {
            y = this.height - h - 6;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        final int frame = 0xBE0099FF;
        graphics.fill(x - 3, y - 4, x + k + 3, y - 3, frame);
        graphics.fill(x - 3, y + h + 3, x + k + 3, y + h + 4, frame);
        graphics.fill(x - 3, y - 3, x + k + 3, y + h + 3, frame);
        graphics.fill(x - 4, y - 3, x - 3, y + h + 3, frame);
        graphics.fill(x + k + 3, y - 3, x + k + 4, y + h + 3, frame);
        final int edge = 0xAA0099FF;
        final int edgeDark = (edge & 0xFEFEFE) >> 1 | edge & 0xFF000000;
        graphics.fillGradient(x - 3, y - 2, x - 2, y + h + 2, edge, edgeDark);
        graphics.fillGradient(x + k + 2, y - 2, x + k + 3, y + h + 2, edge, edgeDark);
        graphics.fill(x - 3, y - 3, x + k + 3, y - 2, edge);
        graphics.fill(x - 3, y + h + 2, x + k + 3, y + h + 3, edgeDark);
        text(graphics, name, x, y, WHITE);
        graphics.pose().popPose();
    }

    /** "Assign Name": the owner's station name with a blinking cursor, Apply and Cancel. */
    private void drawRenameDialog(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        final int x = this.width / 2 - 90;
        final int y = this.height / 2 - 38;
        texture(graphics, MAIN1, x, y, 179, 67, 159, 0, 179, 67, false, false, BLUE);
        texture(graphics, MAIN1, x + 4, y + 2, 171, 10, 159, 92, 171, 10, false, false, BLUE);
        texture(graphics, MAIN1, x + 8, y + 18, 161, 13, 159, 67, 161, 13, false, false, BLUE);
        texture(graphics, MAIN1, x + 17, y + 59, 72, 12, 159, 80, 72, 12, true, false, BLUE);
        texture(graphics, MAIN1, this.width / 2, y + 59, 72, 12, 159, 80, 72, 12, false, false, BLUE);
        String str = Component.translatable("gui.galacticraftcore.message.assign_name").getString();
        text(graphics, str, this.width / 2 - this.font.width(str) / 2, this.height / 2 - 35, WHITE);
        str = Component.translatable("gui.galacticraftcore.message.apply").getString();
        text(graphics, str, this.width / 2 - this.font.width(str) / 2 - 36, this.height / 2 + 23, WHITE);
        str = Component.translatable("gui.galacticraftcore.message.cancel").getString();
        text(graphics, str, this.width / 2 + 36 - this.font.width(str) / 2, this.height / 2 + 23, WHITE);
        if (this.renamingString == null) {
            final GCNetwork.StationInfo station = station(this.selectedStation);
            this.renamingString = station == null ? "" : station.name();
        }
        final String shown = this.renamingString + ((int) (this.ticksSinceMenuOpen / 10.0F) % 2 == 0 ? "_" : "");
        text(graphics, shown, this.width / 2 - this.font.width(this.renamingString) / 2, this.height / 2 - 17, WHITE);
        graphics.pose().popPose();
    }

    // ------------------------------------------------------------------ the Galaxy Map key

    /** 1.12.2 KeyHandlerClient: the Galaxy Map key opens the map when no other screen is open. */
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        while (MAP_KEY.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new CelestialSelectionScreen());
            }
        }
    }
}
