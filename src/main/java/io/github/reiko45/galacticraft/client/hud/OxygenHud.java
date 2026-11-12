package io.github.reiko45.galacticraft.client.hud;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.ClientPayloads;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Oxygen HUD on airless worlds (1.12.2 OverlayOxygenTanks and OverlayOxygenWarning): two tank gauges and the
 * thermometer in the top right corner, and a flashing warning while the oxygen setup does not work.
 */
public final class OxygenHud {
    public static final ResourceLocation TANKS_LAYER = GalacticraftCore.id("oxygen_tanks");
    public static final ResourceLocation WARNING_LAYER = GalacticraftCore.id("oxygen_warning");
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/gui.png");

    private OxygenHud() {
    }

    private static boolean applies(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && CelestialPhysics.isAirless(minecraft.level)
                && !minecraft.player.isCreative() && !minecraft.player.isSpectator() && !minecraft.options.hideGui;
    }

    /** 1.12.2 renderOxygenTankIndicator with the default right/top placement. */
    public static void renderTanks(GuiGraphics graphics, DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!applies(minecraft) || minecraft.getDebugOverlay().showDebugScreen()
                || minecraft.screen != null && !(minecraft.screen instanceof ChatScreen)) {
            return;
        }
        final int width = graphics.guiWidth();
        final int left = width - 49;
        final int right = width - 29;
        final int top = 10;
        // 1.12.2 passes 90 - air (the empty part, in half pixels), or 90 when the tank is empty
        drawTank(graphics, left, top, ClientPayloads.air1() <= 0 ? 90 : 90 - ClientPayloads.air1());
        drawTank(graphics, right, top, ClientPayloads.air2() <= 0 ? 90 : 90 - ClientPayloads.air2());
        final int thermal = ClientPayloads.thermalLevel();
        drawThermal(minecraft, graphics, width - 59, top, thermal + 22, Math.abs(thermal) >= 10 && !ClientPayloads.thermalNormalising());
    }

    /**
     * 1.12.2 renderOxygenTankIndicator's thermometer left of the tanks: a 9 x 47 bar (drawn upside down from the
     * texture) with a 2-pixel marker at the thermal level + 22, coloured from blue (cold) to red (hot), and while the
     * level is 10 or more from 0 and the padding isn't bringing it back, "Invalid Thermal Protection!" (and 1.12.2's
     * red-tinted arrow, from a nearly empty part of the texture).
     */
    private static void drawThermal(Minecraft minecraft, GuiGraphics graphics, int x, int top, int heat, boolean invalid) {
        final int bottom = top + 47;
        // the texture's v runs bottom to top here (a flipped pose would turn the quad's back to the screen)
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TEXTURE);
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        final org.joml.Matrix4f matrix = graphics.pose().last().pose();
        final com.mojang.blaze3d.vertex.BufferBuilder builder = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
        final float u0 = 66.0F / 256.0F;
        final float u1 = 75.0F / 256.0F;
        final float vBottom = 47.0F / 256.0F;
        final float vTop = 94.0F / 256.0F;
        builder.addVertex(matrix, x, top, 0.0F).setUv(u0, vTop);
        builder.addVertex(matrix, x, bottom, 0.0F).setUv(u0, vBottom);
        builder.addVertex(matrix, x + 9, bottom, 0.0F).setUv(u1, vBottom);
        builder.addVertex(matrix, x + 9, top, 0.0F).setUv(u1, vTop);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder.buildOrThrow());
        final int scaled = Mth.clamp(heat, 1, 45);
        final int max = Math.min(scaled + 2, 45);
        final int min = Math.max(max - 2, 0);
        // one texture row, the colour for this heat, stretched over the marker
        graphics.blit(TEXTURE, x + 1, bottom - max, 7, max - min, 76.0F, 48 + 45 - scaled, 7, 1, 256, 256);
        if (invalid) {
            graphics.setColor(1.0F, 0.0F, 0.0F, 1.0F);
            graphics.blit(TEXTURE, x - 5, bottom - max - 3, 4, max - min + 6, 84.0F, 47.0F, 5, 9, 256, 256);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            final Component warning = Component.translatable("gui.galacticraftcore.invalid_thermal");
            graphics.drawString(minecraft.font, warning, x - 8 - minecraft.font.width(warning), bottom - scaled - minecraft.font.lineHeight / 2 - 1,
                    FastColor.ARGB32.color(255, 255, 10, 10), false);
        }
    }

    private static void drawTank(GuiGraphics graphics, int x, int y, int empty) {
        graphics.blit(TEXTURE, x, y, 85, 0, 19, 47);
        final int h = Mth.clamp(empty / 2, 0, 45);
        if (h > 0) {
            graphics.blit(TEXTURE, x + 1, y + 1, 105, 1, 17, h);
            graphics.blit(TEXTURE, x, y + h, 66, 1, 18, 1);
        }
    }

    /** 1.12.2 renderOxygenWarningOverlay: "WARNING!" and a pulsing "Oxygen Setup is Invalid!", at double size. */
    public static void renderWarning(GuiGraphics graphics, DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!applies(minecraft) || ClientPayloads.oxygenValid() || minecraft.screen != null) {
            return;
        }
        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();
        final Component warning = Component.translatable("gui.galacticraftcore.warning");
        final Component invalid = Component.translatable("gui.galacticraftcore.oxygen_invalid");
        final float ticks = minecraft.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        final int alpha = (int) (200 * (Math.sin(ticks / 1.5F) * 0.5F + 0.5F)) + 5;
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(minecraft.font, warning, width / 4 - minecraft.font.width(warning) / 2, height / 8 - 20,
                FastColor.ARGB32.color(255, 255, 0, 0), false);
        graphics.drawString(minecraft.font, invalid, width / 4 - minecraft.font.width(invalid) / 2, height / 8,
                FastColor.ARGB32.color(alpha, 255, 0, 0), false);
        graphics.pose().popPose();
    }
}
