package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SkeletonBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.12.2 RenderEvolvedSkeletonBoss: drawn 1.2x as large, with a bow in each hand; spins faster and faster as it dies. */
public class SkeletonBossRenderer extends MobRenderer<SkeletonBossEntity, SkeletonBossModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/skeletonboss.png");

    public SkeletonBossRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonBossModel(context.bakeLayer(SkeletonBossModel.LAYER)), 0.9F);
        addLayer(new Bows(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonBossEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SkeletonBossEntity boss, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2F, 1.2F, 1.2F);
        final float now = boss.deathTime * boss.deathTime / 5.0F;
        final float before = (boss.deathTime - 1) * (boss.deathTime - 1) / 5.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(now + (now - before) * partialTick));
    }

    /** No vanilla tip-over while dying: the spin replaces it. */
    @Override
    protected void setupRotations(SkeletonBossEntity boss, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yBodyRot));
    }

    @Override
    protected float getFlipDegrees(SkeletonBossEntity boss) {
        return 0.0F;
    }

    /** 1.12.2 LayerHeldItemEvolvedSkeletonBoss: two bows, hidden while it grabs and throws. */
    static class Bows extends RenderLayer<SkeletonBossEntity, SkeletonBossModel> {
        private static final ItemStack BOW = new ItemStack(Items.BOW);
        private final ItemInHandRenderer items;

        Bows(RenderLayerParent<SkeletonBossEntity, SkeletonBossModel> parent, ItemInHandRenderer items) {
            super(parent);
            this.items = items;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, SkeletonBossEntity boss, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (boss.throwTicks() > 0) {
                return;
            }
            for (boolean right : new boolean[] {false, true}) {
                poseStack.pushPose();
                getParentModel().arm(right).translateAndRotate(poseStack);
                if (right) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.translate(0.05F, 0.125F, -1.525F);
                } else {
                    poseStack.translate(0.025F, 1.525F, -0.125F);
                }
                this.items.renderItem(boss, BOW, right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                        false, poseStack, buffers, light);
                poseStack.popPose();
            }
        }
    }
}
