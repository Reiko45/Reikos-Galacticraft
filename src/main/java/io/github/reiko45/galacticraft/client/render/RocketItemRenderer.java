package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.client.render.entity.Tier1RocketRenderer;
import io.github.reiko45.galacticraft.client.render.entity.Tier2RocketRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Rocket items drawn with the rocket's own model (1.12.2 ItemRendererTier1Rocket and ItemModelRocketT2 did the same).
 * The Tier 2 model is taller, so it is drawn smaller.
 */
public class RocketItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final int tier;
    private ModelPart model;

    public RocketItemRenderer(int tier) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.tier = tier;
    }

    @Override
    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
        this.model = null;
    }

    /**
     * The model's length as drawn by its renderModel, nose up once turned by {@code flip} (the Tier 2 and 3 models are
     * drawn nose down, their entity renderers turning them over): from {@code base} to {@code nose}, in blocks.
     */
    private record Extent(float base, float nose, boolean flip) {
    }

    private Extent extent() {
        return switch (this.tier) {
            case 0 -> new Extent(0.0F, 2.73F, false); // cargo_rocket.obj, 0.02 to 6.82, at 0.4
            case 2 -> new Extent(-0.02F, 7.18F, true); // tier_2_rocket.obj, -0.03 to 8.98, at 0.8
            case 3 -> new Extent(-1.22F, 5.11F, true); // tier_3_rocket.obj, -1.53 to 6.39, at 0.8
            default -> new Extent(-1.5625F, 3.25F, false); // the Tier 1 model: 25 pixels below its origin to 52 above
        };
    }

    /** Length across the slot (a slot is 1; its diagonal 1.41), less for the Tier 1 Rocket's broad fins. */
    private float guiLength() {
        return this.tier == 1 ? 1.1F : 1.2F;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // 1.12.2 ItemModelRocket and its kin: the rocket laid across the slot at 45 degrees, turned on its axis
            final Extent extent = extent();
            final float scale = guiLength() / (extent.nose() - extent.base());
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-45.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0F, -(extent.base() + extent.nose()) / 2.0F, 0.0F);
            if (extent.flip()) {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            }
            drawModel(poseStack, buffers, light);
            poseStack.popPose();
            return;
        }
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (this.tier == 0) {
            // the Cargo Rocket (1.12.2 ItemModelCargoRocket)
            final float scale = context == ItemDisplayContext.GUI ? 0.5F : 0.6F;
            poseStack.scale(scale, scale, scale);
            io.github.reiko45.galacticraft.client.render.entity.CargoRocketRenderer.renderModel(poseStack, buffers, light);
        } else if (this.tier >= 3) {
            final float scale = context == ItemDisplayContext.GUI ? 0.16F : 0.2F;
            poseStack.scale(scale, scale, scale);
            io.github.reiko45.galacticraft.client.render.entity.Tier3RocketRenderer.renderModel(poseStack, buffers, light);
        } else if (this.tier >= 2) {
            final float scale = context == ItemDisplayContext.GUI ? 0.2F : 0.24F;
            poseStack.scale(scale, scale, scale);
            Tier2RocketRenderer.renderModel(poseStack, buffers, light);
        } else {
            if (this.model == null) {
                this.model = Minecraft.getInstance().getEntityModels().bakeLayer(Tier1RocketRenderer.LAYER);
            }
            final float scale = context == ItemDisplayContext.GUI ? 0.3F : 0.35F;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0F, 1.34F, 0.0F);
            Tier1RocketRenderer.renderModel(this.model, poseStack, buffers.getBuffer(RenderType.entityCutout(Tier1RocketRenderer.TEXTURE)), light);
        }
        poseStack.popPose();
    }

    /** The rocket's model as its entity renderer's renderModel draws it. */
    private void drawModel(PoseStack poseStack, MultiBufferSource buffers, int light) {
        switch (this.tier) {
            case 0 -> io.github.reiko45.galacticraft.client.render.entity.CargoRocketRenderer.renderModel(poseStack, buffers, light);
            case 2 -> Tier2RocketRenderer.renderModel(poseStack, buffers, light);
            case 3 -> io.github.reiko45.galacticraft.client.render.entity.Tier3RocketRenderer.renderModel(poseStack, buffers, light);
            default -> {
                if (this.model == null) {
                    this.model = Minecraft.getInstance().getEntityModels().bakeLayer(Tier1RocketRenderer.LAYER);
                }
                Tier1RocketRenderer.renderModel(this.model, poseStack, buffers.getBuffer(RenderType.entityCutout(Tier1RocketRenderer.TEXTURE)), light);
            }
        }
    }
}
