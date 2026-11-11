package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.GCDimensions;
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
 * Earth and Moon on their orbits, drawn with the 1.12.2 body icons inside the grey 1.12.2 frame. Click a body to select
 * it, then LAUNCH. Bodies above the rocket's tier are shown in red and can't be chosen. The 1.12.2 zoomable map,
 * space-race and space-station panels come with later phases. Escape does not close it: the player is in space.
 */
public class CelestialSelectionScreen extends Screen {
    private static final ResourceLocation FRAME = GalacticraftCore.id("textures/gui/celestialselection.png");
    private static final int GREY0 = 0xFF282828;
    private static final int GREY1 = 0xFF505050;
    private static final int GREY2 = 0xFF646464;
    private static final int GREY5 = 0xFF969696;
    private static final int WHITE = 0xFFFFFFFF;

    /** A body on the map; {@code parent} null orbits the Sun, {@code orbit} in fractions of the Earth orbit. */
    private record Body(String name, @Nullable ResourceKey<Level> dimension, @Nullable Body parent, float orbit,
                        float period, int size, String nameKey) {
        ResourceLocation icon() {
            return GalacticraftCore.id("textures/gui/celestialbodies/" + this.name + ".png");
        }
    }

    private static final Body SUN = new Body("sun", null, null, 0.0F, 1.0F, 24, "star.galacticraftcore.sol");
    private static final Body EARTH = new Body("earth", Level.OVERWORLD, null, 1.0F, 1.0F, 12, "planet.galacticraftcore.overworld");
    private static final Body MOON = new Body("moon", GCDimensions.MOON, EARTH, 0.18F, 1.0F / 13.0F, 8, "moon.galacticraftcore.moon");
    private static final List<Body> BODIES = List.of(SUN, EARTH, MOON);

    private final int tier;
    private final long[] stars = new long[200];
    @Nullable
    private Body selected;
    private int ticks;

    public CelestialSelectionScreen(int tier) {
        super(Component.translatable("gui.galacticraftcore.message.catalog"));
        this.tier = tier;
        final RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < this.stars.length; i++) {
            this.stars[i] = random.nextLong();
        }
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
        final float angle = (this.ticks + partialTick) / 3600.0F / body.period() * Mth.TWO_PI;
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

    /** Select the body of {@code dimension}, as clicking it would (used by the screenshot tour). */
    public void select(ResourceKey<Level> dimension) {
        for (Body body : BODIES) {
            if (dimension.equals(body.dimension())) {
                this.selected = body;
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
        for (Body body : BODIES) {
            final float[] pos = position(body, partialTick);
            final int size = body == this.selected ? body.size() * 3 / 2 : body.size();
            graphics.blit(body.icon(), Math.round(pos[0] - size / 2.0F), Math.round(pos[1] - size / 2.0F), size, size, 0, 0, 16, 16, 16, 16);
            if (body == this.selected || isOver(body, mouseX, mouseY, partialTick)) {
                drawCursor(graphics, Math.round(pos[0]), Math.round(pos[1]), size / 2 + 3, body == this.selected ? 0xFF00A0FF : GREY5);
            }
        }
        drawBorder(graphics);
        drawButtons(graphics, mouseX, mouseY);
    }

    /** 1.12.2 drawCircles: 90-segment orbits around the Sun and around Earth. */
    private void drawOrbits(GuiGraphics graphics, float partialTick) {
        for (Body body : BODIES) {
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
        final String name = Component.translatable(this.selected.nameKey()).getString().toUpperCase(Locale.ROOT);
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
                PacketDistributor.sendToServer(new GCNetwork.ChooseDestination(this.selected.dimension().location()));
                return true;
            }
            return false;
        }
        // Moons first: they sit on top of their planet.
        for (int i = BODIES.size() - 1; i >= 0; i--) {
            if (isOver(BODIES.get(i), mouseX, mouseY, 0.0F)) {
                this.selected = BODIES.get(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
