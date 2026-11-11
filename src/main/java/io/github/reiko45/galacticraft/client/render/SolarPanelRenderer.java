package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * Draws a solar panel's pole and tilting 3x3 panel (1.12.2 TileEntitySolarPanelRenderer + ModelSolarPanel, same
 * boxes and transforms). The panel tilts east-west by the block entity's current angle; the base block is a normal model.
 */
public class SolarPanelRenderer implements BlockEntityRenderer<SolarPanelBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("solar_panel"), "main");
    private static final ResourceLocation BASIC = GalacticraftCore.id("textures/model/solar_panel_basic.png");
    private static final ResourceLocation ADVANCED = GalacticraftCore.id("textures/model/solar_panel_advanced.png");
    /** 1.12.2 rendered currentAngle - (celestialAngle - celestialAngle2), a constant offset of 0.78469056 * 360 degrees. */
    private static final float TILT_OFFSET = 0.78469056F * 360.0F;

    private final ModelPart panel;
    private final ModelPart pole;

    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {
        final ModelPart root = context.bakeLayer(LAYER);
        this.panel = root.getChild("panel");
        this.pole = root.getChild("pole");
    }

    /** ModelSolarPanel: 256x128 texture, all boxes mirrored. */
    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final CubeListBuilder panel = CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-23.0F, -0.5F, -23.0F, 46, 1, 46);
        for (float x : new float[]{-24.0F, -9.0F, 8.0F, 23.0F}) {
            panel.texOffs(0, 48).addBox(x, -1.111F, -23.0F, 1, 1, 46);
        }
        for (float z : new float[]{23.0F, -0.5F, -24.0F}) {
            panel.texOffs(94, 48).addBox(-24.0F, -1.1F, z, 48, 1, 1);
        }
        root.addOrReplaceChild("panel", panel, PartPose.ZERO);
        root.addOrReplaceChild("pole", CubeListBuilder.create().mirror().texOffs(94, 50).addBox(-1.5F, 0.0F, -1.5F, 3, 24, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void render(SolarPanelBlockEntity solar, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(solar.isAdvanced() ? ADVANCED : BASIC));
        final int light = solar.getLevel() != null ? LevelRenderer.getLightColor(solar.getLevel(), solar.getBlockPos().above(2)) : packedLight;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.0D, 0.5D);
        this.pole.render(poseStack, consumer, light, packedOverlay);
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(solar.currentAngle() + TILT_OFFSET));
        this.panel.render(poseStack, consumer, light, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolarPanelBlockEntity solar) {
        return solar.renderBounds();
    }
}
