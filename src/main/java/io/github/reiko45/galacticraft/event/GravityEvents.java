package io.github.reiko45.galacticraft.event;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Low gravity on other celestial bodies.
 *
 * <p>1.12.2 patched {@code EntityLivingBase#travel} with ASM ({@code TransformerHooks#getGravityForEntity}).
 * Since 1.20.5 vanilla has a {@code generic.gravity} attribute, so a transient attribute modifier does the
 * same job without a coremod, and it is synced to the client for movement prediction.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class GravityEvents {
    public static final ResourceLocation GRAVITY_MODIFIER_ID = GalacticraftCore.id("celestial_gravity");

    private GravityEvents() {
    }

    /** Fires whenever an entity is added to a level, including after a dimension change or respawn. */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            applyGravity(living, CelestialPhysics.of(event.getLevel().dimension()));
        }
    }

    /** Same rule as 1.12.2 {@code EventHandlerGC#onEntityFall}: scale the fall distance. */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        final CelestialPhysics physics = CelestialPhysics.of(event.getEntity().level().dimension());
        if (physics != null) {
            event.setDistance(event.getDistance() * physics.fallDamageModifier());
        }
    }

    public static void applyGravity(LivingEntity entity, CelestialPhysics physics) {
        final AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (physics == null || physics.gravityReduction() == 0.0D) {
            gravity.removeModifier(GRAVITY_MODIFIER_ID);
        } else {
            gravity.addOrUpdateTransientModifier(new AttributeModifier(GRAVITY_MODIFIER_ID,
                    -physics.gravityReduction(), AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
