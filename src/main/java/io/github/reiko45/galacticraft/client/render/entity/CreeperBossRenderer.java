package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.CreeperBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.12.2 RenderCreeperBoss: the model at four times size; spins faster and faster as it dies. */
public class CreeperBossRenderer extends MobRenderer<CreeperBossEntity, CreeperBossModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/creeper_boss.png");

    public CreeperBossRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperBossModel(context.bakeLayer(CreeperBossModel.LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(CreeperBossEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(CreeperBossEntity boss, PoseStack poseStack, float partialTick) {
        poseStack.scale(4.0F, 4.0F, 4.0F);
        final float now = boss.deathTime * boss.deathTime / 5.0F;
        final float before = (boss.deathTime - 1) * (boss.deathTime - 1) / 5.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(now + (now - before) * partialTick));
    }

    /** No vanilla tip-over while dying: the spin replaces it. */
    @Override
    protected void setupRotations(CreeperBossEntity boss, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yBodyRot));
    }

    @Override
    protected float getFlipDegrees(CreeperBossEntity boss) {
        return 0.0F;
    }
}
