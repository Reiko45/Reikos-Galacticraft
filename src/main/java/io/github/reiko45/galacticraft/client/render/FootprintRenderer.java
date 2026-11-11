package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.footprint.Footprint;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 1.12.2 FootprintRenderer: the footprints of the current world, drawn flat on the ground with
 * {@code textures/misc/footprint.png}, darkening and fading as they age.
 */
public final class FootprintRenderer {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/misc/footprint.png");
    private static final Map<Long, List<Footprint>> PRINTS = new HashMap<>();
    private static Level forLevel;

    private FootprintRenderer() {
    }

    public static void add(long chunk, Footprint print) {
        checkLevel();
        PRINTS.computeIfAbsent(chunk, key -> new ArrayList<>()).add(print);
    }

    public static void set(long chunk, List<Footprint> prints) {
        checkLevel();
        PRINTS.put(chunk, new ArrayList<>(prints));
    }

    /** Footprints belong to one world: forget them when the player changes world. */
    private static void checkLevel() {
        final Level level = Minecraft.getInstance().level;
        if (level != forLevel) {
            PRINTS.clear();
            forLevel = level;
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        checkLevel();
        PRINTS.values().removeIf(prints -> {
            prints.removeIf(print -> print.grow(1));
            return prints.isEmpty();
        });
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || PRINTS.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft.level;
        if (level == null || level != forLevel) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        // The model-view matrix already holds the camera rotation during this pass; positions are camera-relative.
        final PoseStack poseStack = new PoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        final Matrix4f pose = poseStack.last().pose();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final double scale = 0.5D;
        for (List<Footprint> prints : PRINTS.values()) {
            for (Footprint print : prints) {
                if (camera.distanceToSqr(print.x(), print.y(), print.z()) > 64.0D * 64.0D) {
                    continue;
                }
                pos.set(print.x(), print.y() - 0.5D, print.z());
                if (!level.getBlockState(pos).is(GCBlocks.MOON_TURF.get())) {
                    continue; // the turf was dug up
                }
                final int light = LevelRenderer.getLightColor(level, pos.move(0, 1, 0));
                final float fade = 1.0F - print.age() / (float) Footprint.MAX_AGE;
                final float x = (float) (print.x() - camera.x);
                final float y = (float) (print.y() - camera.y) + 0.001F;
                final float z = (float) (print.z() - camera.z);
                final float[][] uv = {{1, 1}, {1, 0}, {0, 0}, {0, 1}};
                for (int i = 0; i < 4; i++) {
                    final double angle = Math.toRadians(45 + i * 90 - print.rotation());
                    consumer.addVertex(pose, x + (float) (Math.sin(angle) * scale), y, z + (float) (Math.cos(angle) * scale))
                            .setColor(fade, fade, fade, fade).setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
                }
            }
        }
        buffers.endBatch(RenderType.entityTranslucent(TEXTURE));
    }
}
