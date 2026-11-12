/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.player.GCPlayerHandler#checkThermalStatus
 * and #normaliseThermalLevel). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.player;

import io.github.reiko45.galacticraft.item.ThermalPaddingItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.world.Atmosphere;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * Body temperature on cold and hot worlds (1.12.2 GCPlayerHandler#checkThermalStatus). Each player has a thermal level
 * from -22 (frozen) to 22 (overheated). On a world with a thermal modifier (Mars -1, the asteroids -1.5) it drifts one
 * step towards that side every floor(200 / |modifier|) x (1 + pieces of padding worn) / |modifier| ticks: each piece of
 * Thermal Padding slows the drift. With all four pieces worn the level is also pulled back towards 0, and if that pull is
 * the faster of the two the player is "normalising" and safe. Otherwise, at the limit they take 1.5 thermal damage every
 * drift cooldown, and below -15 they are slowed (above 15, sick). Air from a sealer with an Ambient Thermal Controller
 * brings the level back. Elsewhere it returns to 0: 2 a tick on worlds with no modifier, 3 off Galacticraft worlds.
 */
public final class Thermal {
    public static final int LIMIT = 22;
    /** 1.12.2 DamageSourceGC.thermal damage at the limit. */
    public static final float DAMAGE = 1.5F;

    private Thermal() {
    }

    public static void check(ServerPlayer player, GCPlayerData data) {
        final boolean exempt = player.isCreative() || player.isSpectator();
        check(player, data, !exempt && CelestialPhysics.of(player.level().dimension()) != null, Atmosphere.thermalLevel(player.level().dimension()));
    }

    /**
     * One tick for a survival player on a world that is ({@code galacticraftWorld}) or isn't a Galacticraft world (or for
     * a creative or spectating one: false), with this thermal modifier.
     */
    public static void check(ServerPlayer player, GCPlayerData data, boolean galacticraftWorld, float worldModifier) {
        if (!galacticraftWorld) {
            data.thermalNormalising = true;
            normalise(data, 3);
            return;
        }
        final ItemStack helmet = data.gear(GearSlot.THERMAL_HELMET);
        final ItemStack chest = data.gear(GearSlot.THERMAL_CHESTPLATE);
        final ItemStack legs = data.gear(GearSlot.THERMAL_LEGGINGS);
        final ItemStack boots = data.gear(GearSlot.THERMAL_BOOTS);
        final boolean fullSet = !helmet.isEmpty() && !chest.isEmpty() && !legs.isEmpty() && !boots.isEmpty();
        float strength = 0.0F;
        if (fullSet) {
            strength = Math.abs((strength(helmet) + strength(chest) + strength(legs) + strength(boots)) / 4.0F);
        }
        float modifier = worldModifier;
        float absModifier = Math.abs(modifier);
        if (absModifier <= 0.0F) {
            data.thermalNormalising = true;
            normalise(data, 2);
            return;
        }
        final int cooldownBase = Math.abs(Mth.floor(200.0F / modifier));
        int normaliseCooldown = Mth.floor(150.0F / strength);
        int tickCooldown = Math.max(1, cooldownBase);
        final int tick = player.tickCount - 1;
        if (fullSet) {
            final float relativeFactor = Math.max(1.0F, absModifier / strength) / 1.5F;
            normaliseCooldown = Math.max(1, Mth.floor(normaliseCooldown / absModifier * relativeFactor));
            if (tick % normaliseCooldown == 0) {
                normalise(data, 1);
            }
            modifier /= Math.max(1.0F, strength / 2.0F);
            absModifier = Math.abs(modifier);
        }
        if (OxygenUtil.isInThermalAir(player)) {
            data.thermalNormalising = true;
            normalise(data, 1);
            return;
        }
        for (ItemStack piece : new ItemStack[]{helmet, chest, legs, boots}) {
            if (!piece.isEmpty()) {
                tickCooldown += cooldownBase;
            }
        }
        final int singleCooldown = Math.max(1, Mth.floor(tickCooldown / absModifier));
        if (tick % singleCooldown == 0) {
            data.thermalLevel = Mth.clamp(data.thermalLevel + (modifier < 0.0F ? -1 : 1), -LIMIT, LIMIT);
        }
        data.thermalNormalising = singleCooldown > normaliseCooldown && fullSet;
        if (!data.thermalNormalising) {
            if (tick % tickCooldown == 0 && Math.abs(data.thermalLevel) >= LIMIT) {
                player.hurt(GCDamageTypes.thermal(player.level()), DAMAGE);
            }
            if (data.thermalLevel < -15) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 2, true, true));
            }
            if (data.thermalLevel > 15) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5, 2, true, true));
            }
        }
    }

    private static float strength(ItemStack padding) {
        return padding.getItem() instanceof ThermalPaddingItem item ? item.strength() : 0.0F;
    }

    /** 1.12.2 normaliseThermalLevel: up to {@code step} back towards 0. */
    public static void normalise(GCPlayerData data, int step) {
        if (data.thermalLevel < 0) {
            data.thermalLevel += Math.min(step, -data.thermalLevel);
        } else if (data.thermalLevel > 0) {
            data.thermalLevel -= Math.min(step, data.thermalLevel);
        }
    }
}
