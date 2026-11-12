/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.entity.RenderAstroMiner).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.AstroMinerEntity;
import io.github.reiko45.galacticraft.world.noise.NoiseModule;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderAstroMiner: the astro_miner_full OBJ at 0.0495 scale, wobbling on its hover pad (which pulses, with a
 * soft glow) while it works, two blue scanning fans sweeping ahead of it, and every block it is cutting lit up pink,
 * fading after. Its lasers unfold as it leaves the dock and fold away when it gets back. Docked it shows the dimmed
 * astro_miner_full_off model.
 */
public class AstroMinerRenderer extends EntityRenderer<AstroMinerEntity> {
    public static final ModelResourceLocation MAIN = BakedModels.location("entity/astro_miner/main");
    public static final ModelResourceLocation HOVER_PAD = BakedModels.location("entity/astro_miner/hover_pad");
    public static final ModelResourceLocation GLOW = BakedModels.location("entity/astro_miner/glow");
    public static final ModelResourceLocation LASER_FRONT = BakedModels.location("entity/astro_miner/laser_front");
    public static final ModelResourceLocation LASER_CENTER = BakedModels.location("entity/astro_miner/laser_center");
    public static final ModelResourceLocation LASER_GUARD = BakedModels.location("entity/astro_miner/laser_guard");
    public static final ModelResourceLocation INACTIVE = BakedModels.location("entity/astro_miner/inactive");
    private static final ResourceLocation SCAN = GalacticraftCore.id("textures/misc/gradient.png");
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float RETRACTION_SPEED = 0.02F;

    private final NoiseModule wobbleX;
    private final NoiseModule wobbleY;
    private final NoiseModule wobbleZ;
    private final NoiseModule wobbleXX;
    private final NoiseModule wobbleYY;
    private final NoiseModule wobbleZZ;
    private float lastPartialTick;

    public AstroMinerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.0F;
        final Random random = new Random();
        this.wobbleX = NoiseModule.gradient(random.nextLong(), 2, 1.0F, 0.025F, 1.0F, 1.0F, 0.5F);
        this.wobbleY = NoiseModule.gradient(random.nextLong(), 2, 1.0F, 0.025F, 1.0F, 1.0F, 0.6F);
        this.wobbleZ = NoiseModule.gradient(random.nextLong(), 2, 1.0F, 0.025F, 1.0F, 1.0F, 0.1F);
        this.wobbleXX = NoiseModule.gradient(random.nextLong(), 2, 1.0F, 0.8F, 1.0F, 1.0F, 0.1F);
        this.wobbleYY = NoiseModule.gradient(random.nextLong(), 2, 1.0F, 0.8F, 1.0F, 1.0F, 0.15F);
        this.wobbleZZ = NoiseModule.gradient(random.nextLong(), 2, 1.0F, 0.8F, 1.0F, 1.0F, 0.04F);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : new ModelResourceLocation[]{MAIN, HOVER_PAD, GLOW, LASER_FRONT, LASER_CENTER, LASER_GUARD, INACTIVE}) {
            event.register(model);
        }
    }

    @Override
    public void render(AstroMinerEntity miner, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        final int state = miner.syncedState();
        io.github.reiko45.galacticraft.client.sound.AstroMinerSound.ensure(miner);
        final boolean active = state > AstroMinerEntity.AISTATE_ATBASE;
        final float time = miner.tickCount + partialTick;
        final float pulse = (Mth.sin(time / 4.0F) + 1.0F) / 4.0F + 0.5F;
        final float wx = active ? this.wobbleX.get(time, 0.0F) + this.wobbleXX.get(time, 0.0F) : 0.0F;
        final float wy = active ? this.wobbleY.get(time, 0.0F) + this.wobbleYY.get(time, 0.0F) : 0.0F;
        final float wz = active ? this.wobbleZ.get(time, 0.0F) + this.wobbleZZ.get(time, 0.0F) : 0.0F;
        float partTime = partialTick - this.lastPartialTick;
        this.lastPartialTick = partialTick;
        while (partTime < 0.0F) {
            partTime += 1.0F;
        }
        final float pitch = Mth.lerp(partialTick, miner.xRotO, miner.getXRot());
        final float yaw = Mth.lerp(partialTick, miner.yRotO, miner.getYRot());
        final VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.4F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        if (pitch != 0.0F) {
            poseStack.translate(-0.65F, -0.65F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch / 4.0F));
            poseStack.translate(0.65F, 0.65F, 0.0F);
        }
        poseStack.translate(0.0F, -0.42F, 0.28F);
        poseStack.scale(0.0495F, 0.0495F, 0.0495F);
        poseStack.translate(wx, wy, wz);
        if (active) {
            BakedModels.draw(poseStack, solid, MAIN, light);
            renderLasers(poseStack, solid, miner.retraction, light);
            BakedModels.drawTinted(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)), HOVER_PAD,
                    LightTexture.FULL_BRIGHT, pulse, pulse, pulse);
            BakedModels.drawTinted(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS)), GLOW,
                    LightTexture.FULL_BRIGHT, pulse * 0.6F, pulse * 0.6F, pulse * 0.6F);
            if (state < AstroMinerEntity.AISTATE_DOCKING) {
                renderScan(miner, poseStack, buffers);
            }
            poseStack.popPose();
            renderCutting(miner, poseStack, buffers);
            miner.retraction = Math.max(0.0F, miner.retraction - RETRACTION_SPEED * partTime);
        } else {
            BakedModels.draw(poseStack, solid, INACTIVE, light);
            renderLasers(poseStack, solid, miner.retraction, light);
            poseStack.popPose();
            miner.retraction = Math.min(1.0F, miner.retraction + RETRACTION_SPEED * partTime);
        }
        super.render(miner, entityYaw, partialTick, poseStack, buffers, light);
    }

    /** 1.12.2 renderLaserModel: the main laser slides out and the two guards swing aside as the lasers unfold. */
    private static void renderLasers(PoseStack poseStack, VertexConsumer consumer, float retraction, int light) {
        final float laser = Math.min(1.0F, retraction / 0.8F);
        final float guard = Math.max(0.0F, (retraction - 0.6F) / 0.4F * 1.875F);
        poseStack.pushPose();
        float z = laser * 5.0F;
        float y = z;
        if (y > 0.938F) {
            y = 0.938F;
            z = (z - y) * 2.5F + y;
        }
        poseStack.translate(0.0F, y, z);
        BakedModels.draw(poseStack, consumer, LASER_FRONT, light);
        if (y == 0.938F) {
            poseStack.translate(0.0F, 0.0F, -z + 0.938F);
        }
        BakedModels.draw(poseStack, consumer, LASER_CENTER, light);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(guard, 0.0F, 0.0F);
        BakedModels.draw(poseStack, consumer, LASER_GUARD, light);
        poseStack.translate(-2.0F * guard + 8.75F, 0.0F, 0.0F);
        BakedModels.draw(poseStack, consumer, LASER_GUARD, light);
        poseStack.popPose();
    }

    /** 1.12.2: two faint blue fans from the miner's sides, swinging with its progress through each block. */
    private static void renderScan(AstroMinerEntity miner, PoseStack poseStack, MultiBufferSource buffers) {
        float partBlock = switch (miner.facing) {
            case DOWN -> (float) (miner.getY() % 2.0D);
            case UP -> 1.0F - (float) (miner.getY() % 2.0D);
            case NORTH -> (float) (miner.getZ() % 2.0D);
            case SOUTH -> 1.0F - (float) (miner.getZ() % 2.0D);
            case WEST -> (float) (miner.getX() % 2.0D);
            case EAST -> 1.0F - (float) (miner.getX() % 2.0D);
        };
        partBlock /= 0.06F;
        final float progress = Mth.cos(partBlock * 0.012F * 6.283F) * 0.747F;
        final float angle = 0.69866F - progress * progress;
        final float endX = 38.77F * Mth.sin(angle);
        final float endY = 32.0F;
        final float endZ = 38.77F * Mth.cos(angle) + 20.0F;
        final VertexConsumer fan = buffers.getBuffer(RenderType.entityTranslucentEmissive(SCAN));
        final PoseStack.Pose pose = poseStack.last();
        for (int side = -1; side <= 1; side += 2) {
            final float x = 15.6F * side;
            final float ex = x + endX * side;
            scanVertex(fan, pose, x, -0.6F, -20.0F, 0.0F, 0.0F);
            scanVertex(fan, pose, ex, endY - 0.6F, -endZ, 1.0F, 0.0F);
            scanVertex(fan, pose, ex, -0.6F - endY, -endZ, 1.0F, 1.0F);
            scanVertex(fan, pose, x, -0.7F, -20.0F, 0.0F, 1.0F);
        }
    }

    private static void scanVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(0.0F, 0.6F, 1.0F, 0.2F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /**
     * 1.12.2 doLaser / doAfterGlow: a pink haze over each block being cut (the last 3 ticks), fading out over the next
     * 16 ticks.
     */
    private static void renderCutting(AstroMinerEntity miner, PoseStack poseStack, MultiBufferSource buffers) {
        int remove = 0;
        int afterglow = 0;
        for (Integer cut : new ArrayList<>(miner.laserTimes)) {
            if (cut < miner.tickCount - 19) {
                remove++;
            } else if (cut < miner.tickCount - 3) {
                afterglow++;
            }
        }
        for (int i = 0; i < remove && !miner.laserBlocks.isEmpty(); i++) {
            miner.laserBlocks.removeFirst();
            miner.laserTimes.removeFirst();
        }
        final VertexConsumer haze = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        int index = 0;
        for (BlockPos block : new ArrayList<>(miner.laserBlocks)) {
            float alpha = 0.2F;
            if (index < afterglow) {
                final int fade = Math.max(0, miner.tickCount - miner.laserTimes.get(index) - 8);
                alpha = 0.016667F * (12 - fade);
            }
            if (alpha > 0.0F) {
                poseStack.pushPose();
                poseStack.translate(block.getX() - miner.getX(), block.getY() - miner.getY(), block.getZ() - miner.getZ());
                box(haze, poseStack.last(), alpha);
                poseStack.popPose();
            }
            index++;
        }
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose pose, float alpha) {
        final float a = -0.01F;
        final float b = 1.01F;
        final float[][][] faces = {
                {{a, b, a}, {b, b, a}, {b, b, b}, {a, b, b}}, {{a, a, a}, {a, a, b}, {b, a, b}, {b, a, a}},
                {{a, a, a}, {a, b, a}, {a, b, b}, {a, a, b}}, {{b, a, a}, {b, a, b}, {b, b, b}, {b, b, a}},
                {{a, a, a}, {b, a, a}, {b, b, a}, {a, b, a}}, {{b, a, b}, {a, a, b}, {a, b, b}, {b, b, b}}};
        for (float[][] face : faces) {
            for (float[] corner : face) {
                consumer.addVertex(pose, corner[0], corner[1], corner[2]).setColor(1.0F, 0.7F, 0.7F, alpha).setUv(0.5F, 0.5F)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AstroMinerEntity miner) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
