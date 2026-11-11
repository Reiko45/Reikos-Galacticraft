package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.client.render.entity.Tier1RocketRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The Tier 1 Rocket item drawn with the rocket's own model (1.12.2 ItemRendererTier1Rocket did the same). */
public class RocketItemRenderer extends BlockEntityWithoutLevelRenderer {
    private ModelPart model;

    public RocketItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
        this.model = null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (this.model == null) {
            this.model = Minecraft.getInstance().getEntityModels().bakeLayer(Tier1RocketRenderer.LAYER);
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        final float scale = context == ItemDisplayContext.GUI ? 0.3F : 0.35F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.34F, 0.0F);
        Tier1RocketRenderer.renderModel(this.model, poseStack, buffers.getBuffer(RenderType.entityCutout(Tier1RocketRenderer.TEXTURE)), light);
        poseStack.popPose();
    }
}
