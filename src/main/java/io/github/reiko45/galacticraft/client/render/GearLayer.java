package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.ClientPayloads;
import io.github.reiko45.galacticraft.player.GearData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.resources.ResourceLocation;

/**
 * Oxygen gear on players (1.12.2 LayerOxygenMask, LayerOxygenTanks, LayerOxygenGear): a glass helmet over the head,
 * two tanks on the back (green light, orange medium, red heavy/infinite) and the tubes running up to the mask.
 * Positions are the 1.12.2 values after its GL translations, in model units relative to the body.
 */
public class GearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation MASK = new ModelLayerLocation(GalacticraftCore.id("oxygen_mask"), "main");
    public static final ModelLayerLocation GEAR = new ModelLayerLocation(GalacticraftCore.id("oxygen_gear"), "main");
    private static final ResourceLocation MASK_TEXTURE = GalacticraftCore.id("textures/model/oxygen.png");
    private static final ResourceLocation GEAR_TEXTURE = GalacticraftCore.id("textures/model/player.png");

    private final ModelPart mask;
    private final ModelPart[] tanks;
    private final ModelPart tube;

    public GearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.mask = models.bakeLayer(MASK).getChild("mask");
        final ModelPart gear = models.bakeLayer(GEAR);
        this.tanks = new ModelPart[]{gear.getChild("tank_light"), gear.getChild("tank_medium"), gear.getChild("tank_heavy")};
        this.tube = gear.getChild("tube");
    }

    public static LayerDefinition createMask() {
        final MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("mask", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 16, 16), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createGear() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("tank_light", CubeListBuilder.create().texOffs(4, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3), PartPose.ZERO);
        root.addOrReplaceChild("tank_medium", CubeListBuilder.create().texOffs(16, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3), PartPose.ZERO);
        root.addOrReplaceChild("tank_heavy", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3), PartPose.ZERO);
        root.addOrReplaceChild("tube", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        final GearData gear = ClientPayloads.gear(player.getId());
        if (gear.equals(GearData.NONE)) {
            return;
        }
        final PlayerModel<AbstractClientPlayer> model = getParentModel();
        if (gear.mask()) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            poseStack.scale(0.525F, 0.525F, 0.525F);
            this.mask.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(MASK_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(GEAR_TEXTURE));
        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        drawTank(poseStack, consumer, light, gear.leftTank(), 2.8F);
        drawTank(poseStack, consumer, light, gear.rightTank(), -2.8F);
        if (gear.gear() && !model.crouching) {
            for (int i = 0; i < 7; i++) {
                final float step = i * 2 - 1;
                final float y = -0.52F * step + 3.2F;
                final float z = -0.026F * step * step + 3.2F;
                for (int side = 0; side < 2; side++) {
                    poseStack.pushPose();
                    poseStack.translate((side == 0 ? -2.8F : 2.8F) / 16.0F, y / 16.0F, z / 16.0F);
                    this.tube.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }
        poseStack.popPose();
    }

    private void drawTank(PoseStack poseStack, VertexConsumer consumer, int light, int tier, float x) {
        if (tier <= 0) {
            return;
        }
        poseStack.pushPose();
        // 1.12.2: rotation point (0, 0.5, 0.5) + translate (0.175, 0.2, 0.2); lower and closer when sneaking
        poseStack.translate(x / 16.0F, 3.7F / 16.0F, 3.7F / 16.0F);
        this.tanks[Math.min(tier, 3) - 1].render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
