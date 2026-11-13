/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.overlay.OverlaySensorGlasses and
 * TickHandlerClient). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.item.SensorGlassesItem;
import io.github.reiko45.galacticraft.network.SensorNetwork;
import io.github.reiko45.galacticraft.registry.GCModTags;
import io.github.reiko45.galacticraft.registry.GCParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of the Sensor Glasses (1.12.2 OverlaySensorGlasses and TickHandlerClient): while they are worn, every
 * second the blocks in the 9 x 9 x 9 cube around the player tagged galacticraftcore:sensor_valuable are noted, and the
 * nearest Oxygen Sealer within 96 blocks is asked where its air gets out; the screen gets a pulsing visor with a marker
 * pointing at each ore within 8 blocks, and the leak is shown as a stream of oxygen. K toggles "Advanced Mode", in which
 * the markers spread by the full distance rather than the level one.
 */
public final class SensorGlasses {
    public static final ResourceLocation LAYER = GalacticraftCore.id("sensor_glasses");
    public static final KeyMapping TOGGLE = new KeyMapping("key.galacticraftcore.sensor_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
            VehicleControls.CATEGORY);
    private static final ResourceLocation HUD = GalacticraftCore.id("textures/gui/sensor_glasses.png");
    private static final ResourceLocation INDICATOR = GalacticraftCore.id("textures/gui/sensor_indicator.png");

    private static final List<BlockPos> VALUABLE = new ArrayList<>();
    private static List<BlockPos> leakTrace = List.of();
    private static BlockPos leakSealer;
    private static boolean advanced;
    private static int zoom;
    private static int ticks;

    private SensorGlasses() {
    }

    public static boolean worn() {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player != null && SensorGlassesItem.isWorn(player);
    }

    public static boolean advanced() {
        return advanced;
    }

    public static List<BlockPos> valuable() {
        return VALUABLE;
    }

    public static List<BlockPos> leakTrace() {
        return leakTrace;
    }

    /** The server's answer (1.12.2 updateClient on the sealer); only kept for the sealer last asked about. */
    public static void setLeakTrace(BlockPos sealer, List<BlockPos> trace) {
        if (sealer.equals(leakSealer)) {
            leakTrace = List.copyOf(trace);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            VALUABLE.clear();
            leakTrace = List.of();
            leakSealer = null;
            return;
        }
        // 1.12.2 KeyHandlerClient: the toggle works with or without the glasses on
        while (TOGGLE.consumeClick()) {
            advanced = !advanced;
        }
        if (++ticks % 20 == 0) {
            VALUABLE.clear();
            if (SensorGlassesItem.isWorn(player)) {
                final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                final int px = Mth.floor(player.getX());
                final int py = Mth.floor(player.getY());
                final int pz = Mth.floor(player.getZ());
                for (int i = -4; i < 5; i++) {
                    for (int j = -4; j < 5; j++) {
                        for (int k = -4; k < 5; k++) {
                            pos.set(px + i, py + j, pz + k);
                            if (minecraft.level.getBlockState(pos).is(GCModTags.SENSOR_VALUABLE)) {
                                VALUABLE.add(pos.immutable());
                            }
                        }
                    }
                }
                final OxygenSealerBlockEntity sealer = OxygenSealerBlockEntity.nearest(minecraft.level, player.blockPosition());
                if (sealer == null) {
                    leakSealer = null;
                    leakTrace = List.of();
                } else {
                    if (!sealer.getBlockPos().equals(leakSealer)) {
                        leakTrace = List.of();
                    }
                    leakSealer = sealer.getBlockPos();
                    PacketDistributor.sendToServer(new SensorNetwork.RequestLeakTrace(leakSealer));
                }
            } else {
                leakSealer = null;
                leakTrace = List.of();
            }
        }
        if (!leakTrace.isEmpty()) {
            spawnLeakParticles(minecraft, leakTrace);
        }
    }

    /** 1.12.2 spawnLeakParticles: oxygen swinging from each block of the way out to the one two steps further back. */
    private static void spawnLeakParticles(Minecraft minecraft, List<BlockPos> trace) {
        final RandomSource random = minecraft.level.random;
        for (int i = trace.size() - 1; i >= 0; i--) {
            if (i == 1) {
                continue;
            }
            final BlockPos current = trace.get(i);
            int next = i - 2;
            if (i > 2 && random.nextInt(3) == 0) {
                next--;
            }
            final BlockPos target = i > 1 ? trace.get(next) : current.below(2);
            final Vec3 start = Vec3.atCenterOf(current).add(random.nextDouble() / 2 - 0.25D, random.nextDouble() / 2 - 0.25D, random.nextDouble() / 2 - 0.25D);
            minecraft.level.addParticle(GCParticles.OXYGEN.get(), start.x, start.y, start.z, target.getX() - current.getX(),
                    target.getY() - current.getY(), target.getZ() - current.getZ());
        }
    }

    /** 1.12.2 renderSensorGlassesMain and renderSensorGlassesValueableBlocks (the helmet overlay, first person only). */
    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !minecraft.options.getCameraType().isFirstPerson() || !SensorGlassesItem.isWorn(player)) {
            return;
        }
        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();
        // the visor, twice as wide as the screen is high, breathing in and out with each frame drawn
        zoom++;
        final float f = Mth.sin(zoom / 80.0F) * 0.1F + 0.1F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        quad(graphics, HUD, width / 2 - height - f * 80, -f * 40, width / 2 + height + f * 80, height + f * 40);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        if (VALUABLE.isEmpty()) {
            return;
        }
        graphics.drawString(minecraft.font, Component.translatable("gui.galacticraftcore.sensor.advanced").append(": ")
                .append(Component.translatable(advanced ? "gui.galacticraftcore.sensor.advancedon" : "gui.galacticraftcore.sensor.advancedoff")),
                width / 2 - 50, 4, 0x03b88f, false);
        final float partial = delta.getGameTimeDeltaPartialTick(true);
        final double x = Mth.lerp(partial, player.xo, player.getX());
        final double y = Mth.lerp(partial, player.yo, player.getY());
        final double z = Mth.lerp(partial, player.zo, player.getZ());
        final float yaw = player.getViewYRot(partial);
        RenderSystem.enableBlend(); // drawing the text turned it off
        RenderSystem.defaultBlendFunc();
        for (BlockPos pos : VALUABLE) {
            final double dx = x - pos.getX() - 0.5D;
            final double dy = y - pos.getY() - 0.5D;
            final double dz = z - pos.getZ() - 0.5D;
            final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.5D;
            if (distance >= 4.0D) {
                continue;
            }
            final double level = Math.sqrt(dx * dx + dz * dz) * 0.5D;
            final double angle = Math.toRadians(-Math.toDegrees(Math.atan2(dx, dz)) - yaw + 180.0F);
            final double length = (advanced ? distance : level) * 16.0D;
            final float cx = (float) (width / 2 + length * Math.sin(angle));
            final float cy = (float) (height / 2 - length * Math.cos(angle));
            RenderSystem.setShaderColor(0.0F, 1.0F, 198.0F / 255.0F, (float) Math.min(1.0D, Math.max(0.2D, (distance - 1.0D) * 0.1D)));
            quad(graphics, INDICATOR, cx - 4, cy - 4, cx + 4, cy + 4);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void quad(GuiGraphics graphics, ResourceLocation texture, float x0, float y0, float x1, float y1) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        final Matrix4f matrix = graphics.pose().last().pose();
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, x0, y1, 0.0F).setUv(0.0F, 1.0F);
        builder.addVertex(matrix, x1, y1, 0.0F).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, x1, y0, 0.0F).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, x0, y0, 0.0F).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
}
