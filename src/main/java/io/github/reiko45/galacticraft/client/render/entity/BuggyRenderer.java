package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.BuggyEntity;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 1.12.2 RenderBuggy: the buggy OBJ, one baked model per part (models/entity/buggy/*.json show one group each), at
 * 0.41 scale. The front wheels steer, all four spin, the radar dish wobbles, and the storage boxes show the buggy's
 * storage.
 */
public class BuggyRenderer extends EntityRenderer<BuggyEntity> {
    private static final String[] PARTS = {"MainBody", "RadarDish_Dish", "Wheel_Left", "Wheel_Right", "Wheel_Left_Cover", "Wheel_Right_Cover",
            "CargoLeft", "CargoMid", "CargoRight"};
    private static final float[] FULL_BRIGHTNESS = {1.0F, 1.0F, 1.0F, 1.0F};
    private final RandomSource random = RandomSource.create();

    public BuggyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(GalacticraftCore.id("entity/buggy/" + name.toLowerCase(Locale.ROOT)));
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (String name : PARTS) {
            event.register(part(name));
        }
    }

    @Override
    public void render(BuggyEntity buggy, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-buggy.getViewXRot(partialTick)));
        poseStack.scale(0.41F, 0.41F, 0.41F);
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        final float steer = buggy.wheelRotationZ;
        final float spin = buggy.wheelRotationX;

        // Front wheels: steer, then spin
        final float dY = 0.976F;
        float dX = 1.25F;
        float dZ = -2.727F;
        for (int side = 1; side >= -1; side -= 2) {
            poseStack.pushPose();
            poseStack.translate(side * dX, dY, dZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(steer));
            draw(poseStack, consumer, side > 0 ? "Wheel_Right_Cover" : "Wheel_Left_Cover", light);
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            draw(poseStack, consumer, side > 0 ? "Wheel_Right" : "Wheel_Left", light);
            poseStack.popPose();
        }
        // Back wheels: steer the other way, spin
        dX = 1.9F;
        dZ = -dZ;
        for (int side = 1; side >= -1; side -= 2) {
            poseStack.pushPose();
            poseStack.translate(side * dX, dY, dZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(-steer));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            draw(poseStack, consumer, side > 0 ? "Wheel_Right" : "Wheel_Left", light);
            poseStack.popPose();
        }
        draw(poseStack, consumer, "MainBody", light);

        poseStack.pushPose();
        poseStack.translate(-1.178F, 4.1F, -2.397F);
        final int ticks = buggy.tickCount + buggy.getId() * 10000;
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(ticks * 0.05D) * 50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.cos(ticks * 0.1D) * 50.0F));
        draw(poseStack, consumer, "RadarDish_Dish", light);
        poseStack.popPose();

        final int storage = buggy.storage();
        if (storage > 0) {
            draw(poseStack, consumer, "CargoLeft", light);
        }
        if (storage > 1) {
            draw(poseStack, consumer, "CargoMid", light);
        }
        if (storage > 2) {
            draw(poseStack, consumer, "CargoRight", light);
        }
        poseStack.popPose();
        super.render(buggy, entityYaw, partialTick, poseStack, buffers, light);
    }

    /** 1.12.2 ClientUtil#drawBakedModel, keeping the quads' own (material) colour. */
    private void draw(PoseStack poseStack, VertexConsumer consumer, String name, int light) {
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(part(name));
        final int[] lights = {light, light, light, light};
        for (Direction side : new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            this.random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, this.random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, FULL_BRIGHTNESS, 1.0F, 1.0F, 1.0F, 1.0F, lights, OverlayTexture.NO_OVERLAY, true);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BuggyEntity buggy) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
