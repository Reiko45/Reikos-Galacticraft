package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;

/**
 * 1.12.2 RenderTier1Rocket + ModelRocketTier1 (boxes converted one to one by tools/convert_java_model.py, 256x256
 * textures/model/rocket_t1.png). Also draws the rocket item.
 */
public class Tier1RocketRenderer extends EntityRenderer<Tier1RocketEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("tier_1_rocket"), "main");
    public static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/rocket_t1.png");
    /** 1.12.2 EntityTier1Rocket#getRenderOffsetY. */
    private static final float RENDER_OFFSET_Y = 1.34F;

    private final ModelPart model;

    public Tier1RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = context.bakeLayer(LAYER);
        this.shadowRadius = 0.5F;
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("insideroof", CubeListBuilder.create().texOffs(0, 59).mirror().addBox(-9.0F, -45.0F, -9.0F, 18.0F, 1.0F, 18.0F),
                PartPose.offsetAndRotation(0.0F, 23.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rocketbase1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7.0F, -1.0F, -7.0F, 14.0F, 1.0F, 14.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rocketbase2", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(-6.0F, -2.0F, -6.0F, 12.0F, 1.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(248, 144).mirror().addBox(-1.0F, -76.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing4d", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(11.0F, -14.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing4c", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(13.0F, -12.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing4e", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(9.1F, -15.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing4b", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(15.0F, -9.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing4a", CubeListBuilder.create().texOffs(74, 0).mirror().addBox(17.0F, -14.0F, -1.0F, 1.0F, 15.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing1a", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-1.0F, -14.0F, -18.0F, 2.0F, 15.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing1b", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -9.0F, -17.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing1c", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -12.0F, -15.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing1e", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -15.0F, -11.1F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing1d", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -14.0F, -13.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing2e", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-11.1F, -15.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing2d", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-13.0F, -14.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing2c", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-15.0F, -12.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing2b", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-17.0F, -9.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing2a", CubeListBuilder.create().texOffs(74, 0).mirror().addBox(-18.0F, -14.0F, -1.0F, 1.0F, 15.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing3e", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -15.0F, 9.1F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing3d", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -14.0F, 11.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing3c", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -12.0F, 13.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing3b", CubeListBuilder.create().texOffs(66, 0).mirror().addBox(-1.0F, -9.0F, 15.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing3a", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-1.0F, -14.0F, 17.0F, 2.0F, 15.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top1", CubeListBuilder.create().texOffs(192, 60).mirror().addBox(-8.0F, -48.0F, -8.0F, 16.0F, 2.0F, 16.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top2", CubeListBuilder.create().texOffs(200, 78).mirror().addBox(-7.0F, -50.0F, -7.0F, 14.0F, 2.0F, 14.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top3", CubeListBuilder.create().texOffs(208, 94).mirror().addBox(-6.0F, -52.0F, -6.0F, 12.0F, 2.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top4", CubeListBuilder.create().texOffs(216, 108).mirror().addBox(-5.0F, -54.0F, -5.0F, 10.0F, 2.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top5", CubeListBuilder.create().texOffs(224, 120).mirror().addBox(-4.0F, -56.0F, -4.0F, 8.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top6", CubeListBuilder.create().texOffs(232, 130).mirror().addBox(-3.0F, -58.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("top7", CubeListBuilder.create().texOffs(240, 138).mirror().addBox(-2.0F, -60.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insidebottom", CubeListBuilder.create().texOffs(85, 18).mirror().addBox(-3.9F, -22.0F, -8.9F, 8.0F, 17.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insideleft", CubeListBuilder.create().texOffs(103, 0).mirror().addBox(3.9F, -46.0F, -8.9F, 5.0F, 41.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insidetop", CubeListBuilder.create().texOffs(85, 0).mirror().addBox(-3.9F, -46.0F, -8.9F, 8.0F, 17.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rocketbase3", CubeListBuilder.create().texOffs(0, 28).mirror().addBox(-5.0F, -4.0F, -5.0F, 10.0F, 2.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insideright", CubeListBuilder.create().texOffs(103, 42).mirror().addBox(-8.9F, -46.0F, -8.9F, 5.0F, 41.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insidesideleft", CubeListBuilder.create().texOffs(119, 57).mirror().addBox(8.1F, -46.0F, -7.9F, 1.0F, 41.0F, 17.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insidesideright", CubeListBuilder.create().texOffs(120, 0).mirror().addBox(-8.9F, -46.0F, -7.9F, 1.0F, 41.0F, 16.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insidesideback", CubeListBuilder.create().texOffs(120, 114).mirror().addBox(-8.9F, -46.0F, 8.1F, 17.0F, 41.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("insidefloor", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-9.0F, -4.0F, -9.0F, 18.0F, 1.0F, 18.0F),
                PartPose.offsetAndRotation(0.0F, 23.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** Draw the model at the origin as 1.12.2 did after its translate/rotate calls. */
    public static void renderModel(ModelPart model, PoseStack poseStack, VertexConsumer consumer, int light) {
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public void render(Tier1RocketEntity rocket, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        final float pitch = Mth.lerp(partialTick, rocket.xRotO, rocket.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, RENDER_OFFSET_Y, 0.0F);
        final float roll = rocket.roll() - partialTick;
        if (roll > 0.0F) {
            final float angle = Mth.sin(roll) * roll * 0.3F * partialTick;
            poseStack.mulPose(Axis.XP.rotationDegrees(angle));
            poseStack.mulPose(new org.joml.Quaternionf().rotationAxis(angle * Mth.DEG_TO_RAD, 0.70710677F, 0.0F, 0.70710677F));
        }
        renderModel(this.model, poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light);
        poseStack.popPose();
        super.render(rocket, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Tier1RocketEntity rocket) {
        return TEXTURE;
    }
}
