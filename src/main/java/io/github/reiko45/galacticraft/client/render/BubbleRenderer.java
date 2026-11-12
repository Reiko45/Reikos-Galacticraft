package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.block.entity.BubbleProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * A bubble around a block entity (1.12.2 TileEntityBubbleProviderRenderer): a translucent sphere of the bubble's radius
 * and colour (the Oxygen Distributor's blue, the Terraformer's green), seen from inside and outside.
 */
public class BubbleRenderer<T extends BlockEntity & BubbleProvider> implements BlockEntityRenderer<T> {
    private static final int STACKS = 24;
    private static final int SLICES = 32;

    public BubbleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T provider, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final float radius = provider.bubbleSize();
        if (!provider.bubbleVisible() || radius <= 0.0F) {
            return;
        }
        final float[] color = provider.bubbleColor();
        // Translucent, unculled position-colour quads, so the bubble shows from inside and outside
        final VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        final PoseStack.Pose pose = poseStack.last();
        final int r = (int) (color[0] * 255);
        final int g = (int) (color[1] * 255);
        final int b = (int) (color[2] * 255);
        final int a = 48;
        for (int stack = 0; stack < STACKS; stack++) {
            final float phi0 = Mth.PI * stack / STACKS;
            final float phi1 = Mth.PI * (stack + 1) / STACKS;
            for (int slice = 0; slice < SLICES; slice++) {
                final float theta0 = Mth.TWO_PI * slice / SLICES;
                final float theta1 = Mth.TWO_PI * (slice + 1) / SLICES;
                vertex(consumer, pose, radius, phi0, theta0, r, g, b, a);
                vertex(consumer, pose, radius, phi1, theta0, r, g, b, a);
                vertex(consumer, pose, radius, phi1, theta1, r, g, b, a);
                vertex(consumer, pose, radius, phi0, theta1, r, g, b, a);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float radius, float phi, float theta, int r, int g, int b, int a) {
        final float x = 0.5F + radius * Mth.sin(phi) * Mth.cos(theta);
        final float y = 0.5F + radius * Mth.cos(phi);
        final float z = 0.5F + radius * Mth.sin(phi) * Mth.sin(theta);
        consumer.addVertex(pose, x, y, z).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen(T provider) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(T provider) {
        return provider.renderBounds();
    }
}
