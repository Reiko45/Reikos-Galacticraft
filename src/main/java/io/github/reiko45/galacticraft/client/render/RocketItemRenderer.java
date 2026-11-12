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

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
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
}
