package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.12.2 RenderSlimeling: turned to face forwards, sized by age and lifted onto its belly. */
public class SlimelingRenderer extends MobRenderer<SlimelingEntity, SlimelingModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/slimeling.png");
    /** 1.12.2 GuiSlimeling.renderingOnGui: no name tag over the slimeling drawn in its own panel. */
    public static boolean renderingOnGui;

    public SlimelingRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimelingModel(context.bakeLayer(SlimelingModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SlimelingEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SlimelingEntity slimeling, PoseStack poseStack, float partialTick) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        final float scale = slimeling.slimelingScale();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.10F, 0.0F);
    }

    @Override
    protected boolean shouldShowName(SlimelingEntity slimeling) {
        return !renderingOnGui && super.shouldShowName(slimeling);
    }
}
