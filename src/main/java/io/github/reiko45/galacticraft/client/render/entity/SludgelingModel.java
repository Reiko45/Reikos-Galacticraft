package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SludgelingEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** 1.12.2 ModelSludgeling: a 2x1x3 body and four one-pixel tail segments that wiggle side to side. */
public class SludgelingModel extends HierarchicalModel<SludgelingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("sludgeling"), "main");
    private final ModelPart root;
    private final ModelPart[] tail = new ModelPart[4];

    public SludgelingModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i] = root.getChild("tail" + (i + 1));
        }
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartPose pivot = PartPose.offset(0.0F, 23.5F, -2.0F);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(4, 0).mirror().addBox(-1.0F, -0.5F, -1.5F, 2, 1, 3), pivot);
        root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.3F, 1.5F, 1, 1, 1), pivot);
        root.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.1F, 2.5F, 1, 1, 1), pivot);
        root.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, 0.1F, 3.5F, 1, 1, 1), pivot);
        root.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, 0.3F, 4.5F, 1, 1, 1), pivot);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(SludgelingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // 1.12.2: segment i swings with phase i * 0.15 pi; the first by 3 * 0.025 pi, the others (a typo there) by 2 * 0.025 pi
        for (int i = 0; i < this.tail.length; i++) {
            final int amplitude = i == 0 ? 3 : 2;
            this.tail[i].yRot = Mth.cos(ageInTicks * 0.3F + i * 0.15F * Mth.PI) * Mth.PI * 0.025F * amplitude;
        }
    }
}
