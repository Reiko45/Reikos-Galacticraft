/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.model.ModelBipedGC, api.item.IHoldableItem
 * and the isSneaking overrides of core.entities.player.GCEntityClientPlayerMP / GCEntityOtherPlayerMP). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import io.github.reiko45.galacticraft.client.ClientPayloads;
import io.github.reiko45.galacticraft.client.VehicleCamera;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.item.HoldableItem;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * 1.12.2 ModelBipedGC, run after the vanilla pose of every player model: limbs swing slowly while floating in a
 * Galacticraft world, both arms go up under an open parachute and while carrying a {@link HoldableItem} (rockets, the
 * Moon Buggy and the Astro Miner, carried in a crouch, and the flag, carried standing), and a player near a rocket that
 * someone else is flying waves at it.
 */
public final class PlayerPoses {
    private static final float PI = (float) Math.PI;
    /** Players drawn crouching this frame, to take back down the drop afterwards. */
    private static final Set<Player> CROUCHED = Collections.newSetFromMap(new WeakHashMap<>());

    private PlayerPoses() {
    }

    /**
     * 1.12.2 isSneaking while rendering: standing on the ground, outside orbit and not in a rocket or lander, a player
     * with a crouch-carried item in their main hand is drawn crouching.
     */
    public static boolean drawnCrouching(Player player) {
        return player.onGround() && !player.isCrouching() && player.getPose() == Pose.STANDING
                && player.getMainHandItem().getItem() instanceof HoldableItem holdable && holdable.crouches(player)
                && !VehicleCamera.zooms(player.getVehicle())
                && !player.level().dimension().equals(GCDimensions.ORBIT);
    }

    public static void pre(RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        if (drawnCrouching(player)) {
            event.getRenderer().getModel().crouching = true;
            // PlayerRenderer#getRenderOffset: a crouching player is drawn 2/16 lower
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0.0F, -0.125F * player.getScale(), 0.0F);
            CROUCHED.add(player);
        }
    }

    public static void post(RenderPlayerEvent.Post event) {
        if (CROUCHED.remove(event.getEntity())) {
            event.getPoseStack().popPose();
        }
    }

    /** 1.12.2 ModelBipedGC#setRotationAngles, called at the end of HumanoidModel#setupAnim for players. */
    public static void setupAnim(HumanoidModel<?> model, Player player, float limbSwing, float swingAmount, float ageInTicks) {
        if (!player.onGround() && CelestialPhysics.of(player.level().dimension()) != null && !player.isPassenger()
                && !player.isFallFlying() && !(player.getMainHandItem().getItem() instanceof HoldableItem)) {
            // Take out the walking swing and put in one a little over a sixth as fast, legs swinging twice as fast as arms
            final float slow = Mth.cos(limbSwing * 0.1162F);
            final float rightMod = model.rightArmPose == HumanoidModel.ArmPose.ITEM ? 1.0F : 2.0F;
            model.rightArm.xRot -= Mth.cos(limbSwing * 0.6662F + PI) * rightMod * swingAmount * 0.5F;
            model.leftArm.xRot -= Mth.cos(limbSwing * 0.6662F) * 2.0F * swingAmount * 0.5F;
            model.rightArm.xRot += -slow * 4.0F * swingAmount * 0.5F;
            model.leftArm.xRot += slow * 4.0F * swingAmount * 0.5F;
            model.leftLeg.xRot -= Mth.cos(limbSwing * 0.6662F + PI) * 1.4F * swingAmount;
            model.leftLeg.xRot += Mth.cos(limbSwing * 0.2324F + PI) * 1.4F * swingAmount;
            model.rightLeg.xRot -= Mth.cos(limbSwing * 0.6662F) * 1.4F * swingAmount;
            model.rightLeg.xRot += Mth.cos(limbSwing * 0.2324F) * 1.4F * swingAmount;
        }

        if (ClientPayloads.gear(player.getId()).parachuteOpen()) {
            model.leftArm.xRot += PI;
            model.leftArm.zRot += PI / 10.0F;
            model.rightArm.xRot += PI;
            model.rightArm.zRot -= PI / 10.0F;
        }

        // 1.12.2 looked at both hands, the off hand last
        HoldableItem holdable = null;
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof HoldableItem item) {
                holdable = item;
            }
        }
        if (holdable != null && !VehicleCamera.zooms(player.getVehicle())) {
            if (holdable.holdsLeftHandUp(player)) {
                model.leftArm.xRot = PI + 0.3F;
                model.leftArm.yRot = 0.0F;
                model.leftArm.zRot = PI / 10.0F;
            }
            if (holdable.holdsRightHandUp(player)) {
                model.rightArm.xRot = PI + 0.3F;
                model.rightArm.yRot = 0.0F;
                model.rightArm.zRot = -PI / 10.0F;
            }
        }

        if (!player.level().getEntitiesOfClass(RocketEntity.class, player.getBoundingBox().inflate(15.0D), rocket -> rocket.wavedAtBy(player))
                .isEmpty()) {
            model.rightArm.zRot -= PI / 8.0F + Mth.sin(ageInTicks * 0.9F) * 0.2F;
            model.rightArm.xRot = PI;
        }
    }
}
