package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.LandingBalloonsEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderLandingBalloons: the landing_balloon OBJ sphere (models/entity/landing_balloons.json) at half scale,
 * tumbling with the entity, and above y 500 a gray parachute (ModelBalloonParachute) holding it.
 */
public class LandingBalloonsRenderer extends EntityRenderer<LandingBalloonsEntity> {
    public static final ModelResourceLocation MODEL = BakedModels.location("entity/landing_balloons");
    public static final ModelLayerLocation CHUTE = new ModelLayerLocation(GalacticraftCore.id("landing_balloons"), "chute");
    private static final ResourceLocation CHUTE_TEXTURE = GalacticraftCore.id("textures/model/parachute/gray.png");

    private final ModelPart chute;

    public LandingBalloonsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.chute = context.bakeLayer(CHUTE);
        this.shadowRadius = 1.2F;
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    /** The Parachest's canopy and strings on the 512x256 texture grid ModelBalloonParachute used. */
    public static LayerDefinition createChute() {
        return LayerDefinition.create(ParachestRenderer.chuteMesh(), 512, 256);
    }

    @Override
    public void render(LandingBalloonsEntity balloons, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.8F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, balloons.xRotO, balloons.getXRot())));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), MODEL, light);
        poseStack.popPose();
        if (balloons.getY() >= 500.0D) {
            poseStack.pushPose();
            poseStack.translate(-1.25F, -0.93F, -0.3F);
            poseStack.scale(2.5F, 3.0F, 2.5F);
            this.chute.render(poseStack, buffers.getBuffer(RenderType.entityCutout(CHUTE_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        super.render(balloons, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LandingBalloonsEntity balloons) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
