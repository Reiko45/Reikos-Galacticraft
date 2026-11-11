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
 * Oxygen HUD on airless worlds (1.12.2 OverlayOxygenTanks and OverlayOxygenWarning): two tank gauges in the top right
 * corner and a flashing warning while the oxygen setup does not work.
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
