package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.FluidTankBlock;
import io.github.reiko45.galacticraft.block.entity.FluidPipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 1.12.2 TileEntityFluidPipeRenderer: the pull nozzles of a pipe in pull mode (one per adjacent non-pipe fluid block,
 * pushed 1/16 outwards against a Fluid Tank), and the fluid visible through the glass. Liquids fill the pipe from
 * the bottom up to the network's fill level (and run thinner down vertical pipes); lighter-than-air fluids fill it
 * completely and fade with the level.
 */
public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {
    public static final ModelResourceLocation PULL_MODEL = ModelResourceLocation.standalone(GalacticraftCore.id("block/fluid_pipe_pull"));

    /** 1.12.2: the liquid is a 0.18-wide channel inside the 3/16-wide glass. */
    private static final float SIZE = 0.09F;
    private static final float MIN = 0.5F - SIZE;
    private static final float MAX = 0.5F + SIZE;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidPipeBlockEntity pipe, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final BlockState state = pipe.getBlockState();
        if (!(state.getBlock() instanceof FluidPipeBlock)) {
            return;
        }
        if (state.getValue(FluidPipeBlock.PULL)) {
            renderNozzles(pipe, state, poseStack, buffers, light, overlay);
        }
        if (!pipe.displayFluid().isEmpty() && pipe.displayLevel() > 0) {
            renderFluid(pipe, state, poseStack, buffers, light, overlay);
        }
    }

    private static void renderNozzles(FluidPipeBlockEntity pipe, BlockState state, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final Level level = pipe.getLevel();
        if (level == null) {
            return;
        }
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(PULL_MODEL);
        final VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        for (Direction dir : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.CONNECTIONS.get(dir))) {
                continue;
            }
            final BlockState neighbor = level.getBlockState(pipe.getBlockPos().relative(dir));
            if (neighbor.getBlock() instanceof FluidPipeBlock) {
                continue;
            }
            poseStack.pushPose();
            if (neighbor.getBlock() instanceof FluidTankBlock && dir.getAxis().isHorizontal()) {
                poseStack.translate(dir.getStepX() / 16.0F, 0.0F, dir.getStepZ() / 16.0F);
            }
            // The model is the north nozzle; turn it to face dir about the block centre.
            poseStack.translate(0.5F, 0.5F, 0.5F);
            switch (dir) {
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                default -> {
                }
            }
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), consumer, state, model,
                    1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
            poseStack.popPose();
        }
    }

    private static void renderFluid(FluidPipeBlockEntity pipe, BlockState state, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final FluidStack fluid = pipe.displayFluid();
        final IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        final boolean gas = fluid.getFluidType().isLighterThanAir();
        final float scale = Math.min(1.0F, pipe.displayLevel() / (float) FluidPipeBlockEntity.DISPLAY_STEPS);
        // 1.12.2: liquids rise from the bottom of the pipe and thin out in vertical runs; gases fill it and fade.
        final float level = gas ? 1.0F : scale;
        int color = extensions.getTintColor(fluid);
        if (gas) {
            color = (color & 0x00FFFFFF) | ((int) (((color >>> 24) & 0xFF) * scale) << 24);
        }
        final float top = MIN + (MAX - MIN) * level;
        final float halfWidth = SIZE * level;
        final float lo = 0.5F - halfWidth;
        final float hi = 0.5F + halfWidth;
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        final PoseStack.Pose pose = poseStack.last();

        box(consumer, pose, sprite, color, light, overlay, MIN, MIN, MIN, MAX, top, MAX);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.CONNECTIONS.get(dir))) {
                continue;
            }
            switch (dir) {
                case NORTH -> box(consumer, pose, sprite, color, light, overlay, MIN, MIN, 0.0F, MAX, top, MIN);
                case SOUTH -> box(consumer, pose, sprite, color, light, overlay, MIN, MIN, MAX, MAX, top, 1.0F);
                case WEST -> box(consumer, pose, sprite, color, light, overlay, 0.0F, MIN, MIN, MIN, top, MAX);
                case EAST -> box(consumer, pose, sprite, color, light, overlay, MAX, MIN, MIN, 1.0F, top, MAX);
                case DOWN -> box(consumer, pose, sprite, color, light, overlay, lo, 0.0F, lo, hi, MIN, hi);
                case UP -> box(consumer, pose, sprite, color, light, overlay, lo, MAX, lo, hi, 1.0F, hi);
            }
        }
    }

    /** An axis-aligned box of the fluid's still texture, UVs taken from the block-space coordinates. */
    private static void box(VertexConsumer c, PoseStack.Pose pose, TextureAtlasSprite s, int color, int light, int overlay,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        // down / up
        quad(c, pose, s, color, light, overlay, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, z1, x0, z0, x1, z0, x1, z1);
        quad(c, pose, s, color, light, overlay, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, z0, x0, z1, x1, z1, x1, z0);
        // north / south
        quad(c, pose, s, color, light, overlay, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, x0, y0, x0, y1, x1, y1);
        quad(c, pose, s, color, light, overlay, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, x1, y0, x1, y1, x0, y1);
        // west / east
        quad(c, pose, s, color, light, overlay, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0, y0, z1, y0, z1, y1, z0, y1);
        quad(c, pose, s, color, light, overlay, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, z1, y0, z0, y0, z0, y1, z1, y1);
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose, TextureAtlasSprite s, int color, int light, int overlay,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float au, float av, float bu, float bv, float cu, float cv, float du, float dv) {
        vertex(c, pose, s, color, light, overlay, nx, ny, nz, ax, ay, az, au, av);
        vertex(c, pose, s, color, light, overlay, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(c, pose, s, color, light, overlay, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(c, pose, s, color, light, overlay, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, TextureAtlasSprite s, int color, int light, int overlay,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        // Block-space coordinate (0..1) -> sprite UV; v grows downwards, so flip the vertical coordinate.
        c.addVertex(pose, x, y, z).setColor(color).setUv(s.getU(u), s.getV(1.0F - v)).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
