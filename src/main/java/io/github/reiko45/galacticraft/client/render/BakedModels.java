package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Draws standalone baked models (the 1.12.2 OBJ entity models) like 1.12.2 ClientUtil#drawBakedModel. */
public final class BakedModels {
    private static final float[] FULL_BRIGHTNESS = {1.0F, 1.0F, 1.0F, 1.0F};
    private static final Direction[] SIDES = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final RandomSource RANDOM = RandomSource.create();

    private BakedModels() {
    }

    /** A model registered with ModelEvent.RegisterAdditional from models/{@code path}.json. */
    public static ModelResourceLocation location(String path) {
        return ModelResourceLocation.standalone(GalacticraftCore.id(path));
    }

    /** Every quad of the model in one colour (1.12.2 ClientUtil#drawBakedModelColored; give it a white texture). */
    public static void drawTinted(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, float red, float green, float blue) {
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        final int[] lights = {light, light, light, light};
        for (Direction side : SIDES) {
            RANDOM.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, RANDOM, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, FULL_BRIGHTNESS, red, green, blue, 1.0F, lights, OverlayTexture.NO_OVERLAY, false);
            }
        }
    }

    /** Every quad of the model at the current pose, keeping the quads' own (material) colour. */
    public static void draw(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light) {
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        final int[] lights = {light, light, light, light};
        for (Direction side : SIDES) {
            RANDOM.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, RANDOM, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, FULL_BRIGHTNESS, 1.0F, 1.0F, 1.0F, 1.0F, lights, OverlayTexture.NO_OVERLAY, true);
            }
        }
    }
}
