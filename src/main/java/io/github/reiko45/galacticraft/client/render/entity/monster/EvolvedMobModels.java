package io.github.reiko45.galacticraft.client.render.entity.monster;

import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Meshes of the evolved mobs (1.12.2 ModelEvolvedZombie/Skeleton/Creeper/Spider/Enderman), generated from the 1.12.2
 * model code. The body parts use vanilla part names (so vanilla models animate them) at the 1.12.2 texture offsets, and
 * the oxygen mask, tanks and tubes hang off the head and body like the 1.12.2 models moved them each frame.
 */
public final class EvolvedMobModels {
    public static final ModelLayerLocation ZOMBIE = layer("evolved_zombie");
    public static final ModelLayerLocation SKELETON = layer("evolved_skeleton");
    public static final ModelLayerLocation CREEPER = layer("evolved_creeper");
    public static final ModelLayerLocation SPIDER = layer("evolved_spider");
    public static final ModelLayerLocation ENDERMAN = layer("evolved_enderman");

    private EvolvedMobModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(GalacticraftCore.id(name), "main");
    }

    /** 1.12.2 ModelEvolvedZombie: zombie */
    public static LayerDefinition zombie() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        final PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("leftoxygentank", CubeListBuilder.create().texOffs(56, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(2.0F, 2.0F, 3.8F));
        body.addOrReplaceChild("rightoxygentank", CubeListBuilder.create().texOffs(56, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(-2.0F, 2.0F, 3.8F));
        body.addOrReplaceChild("tuberight2", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, 6.8F));
        body.addOrReplaceChild("tubeleft1", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 3.0F, 5.8F));
        body.addOrReplaceChild("tuberight3", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 1.0F, 6.8F));
        body.addOrReplaceChild("tuberight4", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 0.0F, 6.8F));
        body.addOrReplaceChild("tuberight5", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft6", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -2.0F, 5.8F));
        body.addOrReplaceChild("tuberight7", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -3.0F, 4.8F));
        body.addOrReplaceChild("tuberight1", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 3.0F, 5.8F));
        body.addOrReplaceChild("tubeleft2", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 2.0F, 6.8F));
        body.addOrReplaceChild("tubeleft3", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft4", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 0.0F, 6.8F));
        body.addOrReplaceChild("tubeleft5", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft7", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -3.0F, 4.8F));
        body.addOrReplaceChild("tuberight6", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -2.0F, 5.8F));
        body.addOrReplaceChild("tubeleft8", CubeListBuilder.create().texOffs(56, 30).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -5.0F, 0.0F));
        head.addOrReplaceChild("oxygenmask", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** 1.12.2 ModelEvolvedSkeleton: skeleton */
    public static LayerDefinition skeleton() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        final PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("leftoxygentank", CubeListBuilder.create().texOffs(48, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(2.0F, 2.0F, 3.8F));
        body.addOrReplaceChild("rightoxygentank", CubeListBuilder.create().texOffs(48, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(-2.0F, 2.0F, 3.8F));
        body.addOrReplaceChild("tuberight2", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, 6.8F));
        body.addOrReplaceChild("tubeleft1", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 3.0F, 5.8F));
        body.addOrReplaceChild("tuberight3", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 1.0F, 6.8F));
        body.addOrReplaceChild("tuberight4", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 0.0F, 6.8F));
        body.addOrReplaceChild("tuberight5", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft6", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -2.0F, 5.8F));
        body.addOrReplaceChild("tuberight7", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -3.0F, 4.8F));
        body.addOrReplaceChild("tuberight1", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 3.0F, 5.8F));
        body.addOrReplaceChild("tubeleft2", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 2.0F, 6.8F));
        body.addOrReplaceChild("tubeleft3", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft4", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 0.0F, 6.8F));
        body.addOrReplaceChild("tubeleft5", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft7", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -3.0F, 4.8F));
        body.addOrReplaceChild("tuberight6", CubeListBuilder.create().texOffs(48, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -2.0F, 5.8F));
        head.addOrReplaceChild("oxygenmask", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** 1.12.2 ModelEvolvedCreeper: creeper */
    public static LayerDefinition creeper() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        final PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(-2.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(2.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(-2.0F, 18.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(2.0F, 18.0F, -4.0F));
        body.addOrReplaceChild("leftoxygentank", CubeListBuilder.create().texOffs(40, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(2.0F, 1.0F, 3.8F));
        body.addOrReplaceChild("rightoxygentank", CubeListBuilder.create().texOffs(40, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(-2.0F, 1.0F, 3.8F));
        body.addOrReplaceChild("tuberight2", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft1", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 2.0F, 5.8F));
        body.addOrReplaceChild("tuberight3", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 0.0F, 6.8F));
        body.addOrReplaceChild("tuberight4", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -1.0F, 6.8F));
        body.addOrReplaceChild("tuberight5", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -2.0F, 6.8F));
        body.addOrReplaceChild("tubeleft6", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -3.0F, 5.8F));
        body.addOrReplaceChild("tuberight7", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -4.0F, 4.8F));
        body.addOrReplaceChild("tuberight1", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, 5.8F));
        body.addOrReplaceChild("tubeleft2", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft3", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, 0.0F, 6.8F));
        body.addOrReplaceChild("tubeleft4", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -1.0F, 6.8F));
        body.addOrReplaceChild("tubeleft5", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -2.0F, 6.8F));
        body.addOrReplaceChild("tubeleft7", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.0F, -4.0F, 4.8F));
        body.addOrReplaceChild("tuberight6", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -3.0F, 5.8F));
        body.addOrReplaceChild("tubeleft8", CubeListBuilder.create().texOffs(40, 30).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -6.0F, 0.0F));
        head.addOrReplaceChild("oxygenmask", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** 1.12.2 ModelEvolvedSpider: spider */
    public static LayerDefinition spider() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 15.0F, -3.0F));
        final PartDefinition body = root.addOrReplaceChild("body0", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 12).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F), PartPose.offset(0.0F, 15.0F, 9.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(-4.0F, 15.0F, 2.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(4.0F, 15.0F, 2.0F));
        root.addOrReplaceChild("right_middle_hind_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(-4.0F, 15.0F, 1.0F));
        root.addOrReplaceChild("left_middle_hind_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(4.0F, 15.0F, 1.0F));
        root.addOrReplaceChild("right_middle_front_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(-4.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("left_middle_front_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(4.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(-4.0F, 15.0F, -1.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offset(4.0F, 15.0F, -1.0F));
        head.addOrReplaceChild("oxygenmask", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-5.0F, -5.0F, -9.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tank1", CubeListBuilder.create().texOffs(40, 34).mirror().addBox(1.0F, -3.0F, 0.0F, 3.0F, 3.0F, 7.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tank2", CubeListBuilder.create().texOffs(40, 34).mirror().addBox(-4.0F, -3.0F, 0.0F, 3.0F, 3.0F, 7.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube1", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, 0.0F, -6.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube2", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -1.0F, -5.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube3", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -1.0F, -4.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube4", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -2.0F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube5", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -3.0F, -2.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube6", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -4.0F, -2.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube7", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -5.0F, -1.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube8", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube9", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(2.0F, -4.0F, 0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube10", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, 0.0F, -6.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube11", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -1.0F, -5.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube12", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -1.0F, -4.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube13", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -2.0F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube15", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -4.0F, -2.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube14", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -3.0F, -2.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube16", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -5.0F, -1.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube17", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        body.addOrReplaceChild("tube18", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-3.0F, -4.0F, 0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, 4.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** 1.12.2 ModelEvolvedEnderman: enderman */
    public static LayerDefinition enderman() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, -13.0F, 0.0F));
        final PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -13.0F, 0.0F));
        final PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, -14.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F), PartPose.offset(-5.0F, -12.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F), PartPose.offset(5.0F, -12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F), PartPose.offset(-2.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F), PartPose.offset(2.0F, -5.0F, 0.0F));
        head.addOrReplaceChild("oxygenmask", CubeListBuilder.create().texOffs(0, 44).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tank1", CubeListBuilder.create().texOffs(52, 54).addBox(0.0F, 0.0F, 0.0F, 3.0F, 7.0F, 3.0F), PartPose.offset(0.0F, 3.0F, 2.0F));
        body.addOrReplaceChild("tank2", CubeListBuilder.create().texOffs(52, 54).addBox(0.0F, 0.0F, 0.0F, 3.0F, 7.0F, 3.0F), PartPose.offset(-3.0F, 3.0F, 2.0F));
        body.addOrReplaceChild("tube1", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 3.5F, 5.0F));
        body.addOrReplaceChild("tube2", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 3.0F, 6.0F));
        body.addOrReplaceChild("tube3", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, 7.0F));
        body.addOrReplaceChild("tube4", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 1.0F, 7.0F));
        body.addOrReplaceChild("tube5", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, 0.0F, 7.0F));
        body.addOrReplaceChild("tube6", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -1.0F, 6.0F));
        body.addOrReplaceChild("tube7", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.0F, -2.0F, 5.0F));
        body.addOrReplaceChild("tube8", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, 3.5F, 5.0F));
        body.addOrReplaceChild("tube9", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, 3.0F, 6.0F));
        body.addOrReplaceChild("tube10", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, 2.0F, 7.0F));
        body.addOrReplaceChild("tube11", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, 1.0F, 7.0F));
        body.addOrReplaceChild("tube12", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, 0.0F, 7.0F));
        body.addOrReplaceChild("tube13", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, -1.0F, 6.0F));
        body.addOrReplaceChild("tube14", CubeListBuilder.create().texOffs(44, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.0F, -2.0F, 5.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

}
