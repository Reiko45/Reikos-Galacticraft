package io.github.reiko45.galacticraft.player;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.MeteorShowers;
import io.github.reiko45.galacticraft.footprint.Footprints;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCModTags;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side player and mob rules (1.12.2 GCPlayerHandler#onPlayerUpdate and EventHandlerGC#entityLivingEvent):
 * breathing from tanks, suffocation, torch swapping and keeping clients informed about gear.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class GCPlayerHandler {
    /** 1.12.2 config defaults: Suffocation Cooldown 100 ticks, Suffocation Damage 2. */
    public static final int SUFFOCATION_COOLDOWN = 100;
    public static final int SUFFOCATION_DAMAGE = 2;

    private GCPlayerHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final GCPlayerData data = GCPlayerData.get(player);
        if (data.damageCounter() > 0) {
            data.setDamageCounter(data.damageCounter() - 1);
        }
        swapTorches(player);
        spaceRacePopup(player, data);
        SpaceTravel.tick(player, data);
        MeteorShowers.tick(player);
        io.github.reiko45.galacticraft.entity.SmallAsteroidEntity.spawnNear(player);
        Footprints.updateFeet(player, data);
        checkOxygen(player, data, needsOxygen(player, data));
        Thermal.check(player, data);
        sendState(player, data);
        if (data.takeGearChanged()) {
            broadcastGear(player, data);
        }
        data.setLastOxygenSetupValid(data.oxygenSetupValid());
    }

    /**
     * Airless worlds, and the Overworld above y 450 (1.12.2 OXYGENHEIGHTLIMIT), except in creative/spectator, in a rocket
     * or lander, or while choosing a destination.
     */
    private static boolean needsOxygen(ServerPlayer player, GCPlayerData data) {
        if (player.isCreative() || player.isSpectator() || data.inSpace
                || player.getVehicle() instanceof RocketEntity || player.getVehicle() instanceof LanderEntity) {
            return false;
        }
        return CelestialPhysics.isAirless(player.level()) || player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD && player.getY() > 450.0D;
    }

    /**
     * 1.12.2 checkOxygen: when the player needs oxygen (an airless world, outside creative/spectator) they breathe 1 unit
     * from a tank every 9 ticks unless in breathable air; without a working mask + gear + tank setup or breathable air
     * they take 2 suffocation damage every 100 ticks.
     */
    public static void checkOxygen(ServerPlayer player, GCPlayerData data, boolean needsOxygen) {
        if (!needsOxygen) {
            if (player.isCreative()) {
                data.setAirRemaining(90);
                data.setAirRemaining2(90);
            } else if ((player.tickCount - 1) % 20 == 0 && data.airRemaining() < 90) {
                data.setAirRemaining(data.airRemaining() + 1);
                data.setAirRemaining2(data.airRemaining2() + 1);
            }
            data.setOxygenSetupValid(true);
            return;
        }
        final ItemStack tank1 = data.gear(GearSlot.LEFT_TANK);
        final ItemStack tank2 = data.gear(GearSlot.RIGHT_TANK);
        data.setAirRemaining(OxygenUtil.tankOxygen(tank1));
        data.setAirRemaining2(OxygenUtil.tankOxygen(tank2));
        final boolean inAir = OxygenUtil.isInBreathableAir(player);
        if (OxygenUtil.drainSpacing(tank1, tank2) > 0) {
            if ((player.tickCount - 1) % 9 == 0 && !inAir) {
                if (data.airRemaining() > 0) {
                    drain(data, GearSlot.LEFT_TANK, tank1);
                    data.setAirRemaining(data.airRemaining() - (tank1.getItem() instanceof OxygenTankItem ? 1 : 0));
                } else if (data.airRemaining2() > 0) {
                    drain(data, GearSlot.RIGHT_TANK, tank2);
                    data.setAirRemaining2(data.airRemaining2() - (tank2.getItem() instanceof OxygenTankItem ? 1 : 0));
                }
            }
        }
        final boolean airEmpty = data.airRemaining() <= 0 && data.airRemaining2() <= 0;
        if (player.onClimbable()) {
            data.setOxygenSetupValid(data.lastOxygenSetupValid());
        } else {
            data.setOxygenSetupValid(OxygenUtil.hasValidOxygenSetup(data) && !airEmpty || inAir);
        }
        if (!player.isAlive()) {
            return;
        }
        if (!data.oxygenSetupValid()) {
            if (data.damageCounter() == 0) {
                data.setDamageCounter(SUFFOCATION_COOLDOWN);
                player.hurt(GCDamageTypes.suffocation(player.level()), SUFFOCATION_DAMAGE * (2 + data.incrementalDamage()) / 2.0F);
            }
        } else {
            data.setIncrementalDamage(0);
        }
    }

    private static void drain(GCPlayerData data, GearSlot slot, ItemStack tank) {
        if (tank.getItem() instanceof OxygenTankItem) {
            final ItemStack drained = tank.copy();
            OxygenTankItem.setOxygen(drained, OxygenTankItem.oxygen(tank) - 1);
            data.inventory().setStackInSlot(slot.index(), drained);
        }
    }

    /** 1.12.2 checkCurrentItem: held vanilla torches become space torches on airless worlds, and back elsewhere. */
    private static void swapTorches(ServerPlayer player) {
        final boolean airless = CelestialPhysics.isAirless(player.level());
        final Item from = airless ? Items.TORCH : GCBlocks.UNLIT_TORCH_ITEM.get();
        final Item to = airless ? GCBlocks.UNLIT_TORCH_ITEM.get() : Items.TORCH;
        for (InteractionHand hand : InteractionHand.values()) {
            final ItemStack held = player.getItemInHand(hand);
            if (held.is(from)) {
                player.setItemInHand(hand, held.transmuteCopy(to, held.getCount()));
            }
        }
    }

    /** HUD values (scaled to 0..90 per tank, 1.12.2 sendAirRemainingPacket) and setup validity, sent when they change. */
    private static void sendState(ServerPlayer player, GCPlayerData data) {
        final int air1 = scaled(data.airRemaining(), data.gear(GearSlot.LEFT_TANK));
        final int air2 = scaled(data.airRemaining2(), data.gear(GearSlot.RIGHT_TANK));
        final GCNetwork.OxygenState state = new GCNetwork.OxygenState(air1, air2, data.oxygenSetupValid(), data.thermalLevel, data.thermalNormalising);
        if (!state.equals(data.lastSentState) || player.tickCount % 100 == 0) {
            data.lastSentState = state;
            GCNetwork.sendToPlayer(player, state);
        }
    }

    private static int scaled(int air, ItemStack tank) {
        final int capacity = OxygenUtil.tankCapacity(tank);
        return capacity <= 0 ? 0 : Mth.floor(air * 90.0F / capacity);
    }

    public static void broadcastGear(ServerPlayer player, GCPlayerData data) {
        GCNetwork.sendToTrackingAndSelf(player, new GCNetwork.GearSync(player.getId(), GearData.of(data)));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher) {
            GCNetwork.sendToPlayer(watcher, new GCNetwork.GearSync(target.getId(), GearData.of(GCPlayerData.get(target))));
        }
    }

    /**
     * 1.12.2 WorldProviderMars#canRespawnHere only said yes while a bed was being used, so beds on Mars could be slept
     * in without exploding but never became a spawn point (respawning there without oxygen gear would be fatal).
     */
    @SubscribeEvent
    public static void onSetSpawn(net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent event) {
        if (!event.isForced() && event.getNewSpawn() != null
                && (event.getSpawnLevel().equals(io.github.reiko45.galacticraft.world.GCDimensions.MARS)
                || event.getSpawnLevel().equals(io.github.reiko45.galacticraft.world.GCDimensions.ASTEROIDS))) {
            event.setCanceled(true);
        }
    }

    /** 1.12.2 WorldProviderAsteroids#isDaytime was always true, so beds there always said it wasn't night. */
    @SubscribeEvent
    public static void onCanSleep(net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent event) {
        if (event.getProblem() == null && event.getLevel().dimension().equals(io.github.reiko45.galacticraft.world.GCDimensions.ASTEROIDS)
                && !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof io.github.reiko45.galacticraft.block.CryogenicChamberBlock)) {
            event.setProblem(net.minecraft.world.entity.player.Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        GCPlayerData.get(event.getEntity()).markGearChanged();
        if (event.getEntity() instanceof ServerPlayer player) {
            io.github.reiko45.galacticraft.race.SpaceRaces.get(player.server).onLogin(player);
        }
    }

    /** 1.12.2 SpaceRaceManager#tick, from TickHandlerServer. */
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        io.github.reiko45.galacticraft.race.SpaceRaces.get(event.getServer()).tick(event.getServer());
    }

    /**
     * 1.12.2 GCPlayerHandler#onPlayerUpdate: with enableSpaceRaceManagerPopup on, a player without a named team sees the
     * Space Race Manager once, a second after first logging in.
     */
    private static void spaceRacePopup(ServerPlayer player, GCPlayerData data) {
        if (player.tickCount >= 25 && !data.openedSpaceRaceManager && io.github.reiko45.galacticraft.GCConfig.ENABLE_SPACE_RACE_MANAGER_POPUP.get()) {
            final io.github.reiko45.galacticraft.race.SpaceRace race = io.github.reiko45.galacticraft.race.SpaceRaces.get(player.server).of(player.getUUID());
            if (race == null || io.github.reiko45.galacticraft.race.SpaceRace.DEFAULT_NAME.equals(race.name())) {
                io.github.reiko45.galacticraft.race.RaceNetwork.openManager(player);
            }
            data.openedSpaceRaceManager = true;
        }
    }

    /** No getting out of a launched rocket or a flying lander (1.12.2 IIgnoreShift). */
    @SubscribeEvent
    public static void onMount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof net.minecraft.world.entity.player.Player
                && (event.getEntityBeingMounted() instanceof RocketEntity rocket && rocket.keepsRider()
                || event.getEntityBeingMounted() instanceof LanderEntity lander && lander.keepsRider())
                && !event.getEntityBeingMounted().isRemoved()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        GCPlayerData.get(event.getEntity()).markGearChanged();
    }

    /** Respawn / End exit: keep the gear unless it was dropped on death. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        final Player original = event.getOriginal();
        final boolean keep = !event.isWasDeath() || original.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        GCPlayerData.get(event.getEntity()).copyFrom(GCPlayerData.get(original), keep);
    }

    /** The Galacticraft inventory drops on death with the rest (1.12.2 GCPlayerHandler#onPlayerDeath). */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        final GCPlayerData data = GCPlayerData.get(player);
        for (int i = 0; i < GearSlot.COUNT; i++) {
            final ItemStack stack = data.inventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                final ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3D, player.getZ(), stack.copy());
                item.setDefaultPickUpDelay();
                event.getDrops().add(item);
                data.inventory().setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 1.12.2 EventHandlerGC#onEntityDamaged: without oxygen nothing keeps burning. A burning entity on an airless world
     * outside breathable air is put out (in a puff of smoke) the first time the flames hurt it.
     */
    @SubscribeEvent
    public static void onFireDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        final LivingEntity entity = event.getEntity();
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) || !(entity.level() instanceof net.minecraft.server.level.ServerLevel level)
                || !CelestialPhysics.isAirless(level) || OxygenUtil.isInBreathableAir(entity)) {
            return;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(),
                50, 0.0D, 0.05D, 0.0D, 0.001D);
        entity.clearFire();
    }

    /** 1.12.2 EventHandlerGC#entityLivingEvent: mobs on airless worlds take 1 damage every 100 ticks outside breathable air. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide || entity instanceof Player
                || entity instanceof ArmorStand || entity.tickCount % SUFFOCATION_COOLDOWN != 0) {
            return;
        }
        if (CelestialPhysics.isAirless(entity.level()) && !entity.getType().is(GCModTags.BREATHES_IN_SPACE) && !OxygenUtil.isInBreathableAir(entity)) {
            entity.hurt(GCDamageTypes.suffocation(entity.level()), Math.max(SUFFOCATION_DAMAGE / 2, 1));
        }
    }
}
