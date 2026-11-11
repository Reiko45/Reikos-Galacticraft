package io.github.reiko45.galacticraft.client.hud;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1.12.2 OverlayLaunchCountdown and OverlayLander. */
public final class VehicleHud {
    public static final ResourceLocation LAYER = GalacticraftCore.id("vehicle");

    private VehicleHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();
        if (minecraft.player.getVehicle() instanceof Tier1RocketEntity rocket && rocket.phase() == Tier1RocketEntity.Phase.IGNITED) {
            final int count = (int) Math.floor(rocket.countdown() / 2 / 10.0F);
            final String text = String.valueOf(count);
            final float scale = count <= 10 ? 4.0F : 2.0F;
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);
            final int x = (int) (width / (2.0F * scale)) - minecraft.font.width(text) / 2;
            final int y = count <= 10 ? height / 20 : height / 8;
            graphics.drawString(minecraft.font, text, x, y, 0xFFFF0000, false);
            graphics.pose().popPose();
        } else if (minecraft.player.getVehicle() instanceof LanderEntity lander) {
            final double motionY = lander.getDeltaMovement().y;
            graphics.pose().pushPose();
            graphics.pose().scale(2.0F, 2.0F, 1.0F);
            if (motionY < -2.0D) {
                final Component warning = Component.translatable("gui.galacticraftcore.warning");
                graphics.drawString(minecraft.font, warning, width / 4 - minecraft.font.width(warning) / 2, height / 8 - 20, 0xFFFF0000, false);
                final int alpha = (int) (200 * (Mth.sin(minecraft.player.tickCount) * 0.5F + 0.5F)) + 5;
                final Component press = Component.translatable("gui.galacticraftcore.lander.warning2")
                        .append(Component.keybind("key.jump")).append(Component.translatable("gui.galacticraftcore.lander.warning3"));
                graphics.drawString(minecraft.font, press, width / 4 - minecraft.font.width(press) / 2, height / 8,
                        alpha << 24 | alpha << 16 | alpha << 8 | alpha, false);
            }
            graphics.pose().popPose();
            if (motionY != 0.0D) {
                final Component velocity = Component.translatable("gui.galacticraftcore.lander.velocity")
                        .append(": " + Math.round(motionY * 1000) / 100.0D + " ").append(Component.translatable("gui.galacticraftcore.lander.velocityu"));
                final int red = Mth.clamp((int) Math.floor(Math.abs(motionY) * 51.0D), 0, 255);
                graphics.drawString(minecraft.font, velocity, width / 2 - minecraft.font.width(velocity) / 2, height / 3,
                        0xFF000000 | red << 16 | (255 - red) << 8, false);
            }
        }
    }
}
