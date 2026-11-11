package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The liquid inside a Fluid Tank (1.12.2 TileEntityFluidTankRenderer): side faces 0.399 out from the centre up to
 * 0.012 + amount / 16400, a top face unless the tank above holds liquid, a bottom face unless the tank below does.
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
    private static final float MIN = 0.5F - 0.399F;
    private static final float MAX = 0.5F + 0.399F;

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidTankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final FluidStack fluid = tank.tank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        final float level = fluid.getAmount() / 16400.0F;
        if (level <= 0.012F) {
            return;
        }
        final IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        final int color = extensions.getTintColor(fluid);
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        final PoseStack.Pose pose = poseStack.last();

        final float top = 0.012F + level;
        final float u0 = sprite.getU0();
        final float u1 = sprite.getU1();
        final float v0 = sprite.getV0();
        final float vSide = v0 + (sprite.getV1() - v0) * Math.min(level, 1.0F);

        // Sides (north, south, west, east)
        quad(consumer, pose, color, light, overlay, 0, 0, -1,
                MAX, 0, MIN, u0, vSide, MIN, 0, MIN, u1, vSide, MIN, top, MIN, u1, v0, MAX, top, MIN, u0, v0);
        quad(consumer, pose, color, light, overlay, 0, 0, 1,
                MIN, 0, MAX, u0, vSide, MAX, 0, MAX, u1, vSide, MAX, top, MAX, u1, v0, MIN, top, MAX, u0, v0);
        quad(consumer, pose, color, light, overlay, -1, 0, 0,
                MIN, 0, MIN, u0, vSide, MIN, 0, MAX, u1, vSide, MIN, top, MAX, u1, v0, MIN, top, MIN, u0, v0);
        quad(consumer, pose, color, light, overlay, 1, 0, 0,
                MAX, 0, MAX, u0, vSide, MAX, 0, MIN, u1, vSide, MAX, top, MIN, u1, v0, MAX, top, MAX, u0, v0);

        final FluidTankBlockEntity above = tank.above();
        if (above == null || above.tank().isEmpty()) {
            final float y = level + 0.002F;
            quad(consumer, pose, color, light, overlay, 0, 1, 0,
                    MIN, y, MIN, u0, v0, MIN, y, MAX, u0, sprite.getV1(), MAX, y, MAX, u1, sprite.getV1(), MAX, y, MIN, u1, v0);
        }
        final FluidTankBlockEntity below = tank.below();
        if (below == null || below.tank().isEmpty()) {
            final float y = 0.01F;
            quad(consumer, pose, color, light, overlay, 0, -1, 0,
                    MIN, y, MAX, u0, sprite.getV1(), MIN, y, MIN, u0, v0, MAX, y, MIN, u1, v0, MAX, y, MAX, u1, sprite.getV1());
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int color, int light, int overlay, float nx, float ny, float nz,
                             float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3, float x4, float y4, float z4, float u4, float v4) {
        vertex(consumer, pose, color, light, overlay, nx, ny, nz, x1, y1, z1, u1, v1);
        vertex(consumer, pose, color, light, overlay, nx, ny, nz, x2, y2, z2, u2, v2);
        vertex(consumer, pose, color, light, overlay, nx, ny, nz, x3, y3, z3, u3, v3);
        vertex(consumer, pose, color, light, overlay, nx, ny, nz, x4, y4, z4, u4, v4);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int color, int light, int overlay,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
