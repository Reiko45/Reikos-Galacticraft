package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.Tier2RocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderTier2Rocket: the rocket_t2 OBJ (models/entity/tier_2_rocket.json) at 0.8 scale, rocking when hit like
 * the Tier 1. Also draws the rocket item.
 */
public class Tier2RocketRenderer extends EntityRenderer<Tier2RocketEntity> {
    public static final ModelResourceLocation MODEL = BakedModels.location("entity/tier_2_rocket");
    /** 1.12.2 EntityTier2Rocket#getRenderOffsetY. */
    private static final float RENDER_OFFSET_Y = -0.1F;

    public Tier2RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.0F;
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    /** The model as 1.12.2 drew it after its own transforms: upside down, flipped back by scale(-1, -1, 1), at 0.8. */
    public static void renderModel(PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), MODEL, light);
        poseStack.popPose();
    }

    @Override
    public void render(Tier2RocketEntity rocket, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        final float pitch = Mth.lerp(partialTick, rocket.xRotO, rocket.getXRot()) + 180.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, RENDER_OFFSET_Y, 0.0F);
        final float roll = rocket.roll() - partialTick;
        if (roll > 0.0F) {
            final float angle = Mth.sin(roll) * roll * 0.3F * partialTick;
            poseStack.mulPose(Axis.XP.rotationDegrees(angle));
            poseStack.mulPose(new org.joml.Quaternionf().rotationAxis(angle * Mth.DEG_TO_RAD, 0.70710677F, 0.0F, 0.70710677F));
        }
        renderModel(poseStack, buffers, light);
        poseStack.popPose();
        super.render(rocket, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Tier2RocketEntity rocket) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
