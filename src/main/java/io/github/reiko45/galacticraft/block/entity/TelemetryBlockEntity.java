/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityTelemetry). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.entity.Telemetry;
import io.github.reiko45.galacticraft.item.FrequencyModuleItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.network.DisplayNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2 TileEntityTelemetry: every other tick, sends the linked entity's name and five numbers to everyone within 320
 * blocks. Living things report hurt time, health %, a heart rate that rises with speed and with being hurt (400 to 2000,
 * shown divided by 10), and players also food and oxygen; horses, villagers, wolves, sheep and zombies send what their
 * picture needs; rockets and the Astro Miner fill the numbers in themselves. The link is kept as the entity's UUID, so
 * a respawned or returning player is found again.
 */
public class TelemetryBlockEntity extends BlockEntity {
    private static final Map<Level, Set<TelemetryBlockEntity>> LOADED = new WeakHashMap<>();
    private static final Map<ServerPlayer, ItemStack> LAST_WORN = new WeakHashMap<>();

    @Nullable
    private UUID linked;
    private int pulseRate = 400;
    private int lastHurtTime;
    private int ticks;

    // client
    private String clientName = "";
    private int[] clientData = {-1};
    private String clientUuid = "";

    public TelemetryBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.TELEMETRY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            LOADED.computeIfAbsent(this.level, l -> new LinkedHashSet<>()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            final Set<TelemetryBlockEntity> loaded = LOADED.get(this.level);
            if (loaded != null) {
                loaded.remove(this);
            }
        }
        super.setRemoved();
    }

    /** 1.12.2 getNearest: the closest loaded unit within 32 blocks of {@code pos}, on the client. */
    @Nullable
    public static TelemetryBlockEntity nearest(Level level, BlockPos pos) {
        TelemetryBlockEntity nearest = null;
        int best = 1025;
        for (TelemetryBlockEntity telemetry : LOADED.getOrDefault(level, Set.of())) {
            final int distance = (int) telemetry.getBlockPos().distSqr(pos);
            if (distance < best && !telemetry.isRemoved()) {
                best = distance;
                nearest = telemetry;
            }
        }
        return nearest;
    }

    public void addTrackedEntity(@Nullable UUID uuid) {
        this.pulseRate = 400;
        this.lastHurtTime = 0;
        this.linked = uuid;
        setChanged();
    }

    public void removeTrackedEntity() {
        addTrackedEntity(null);
    }

    @Nullable
    public UUID linked() {
        return this.linked;
    }

    @Nullable
    private Entity linkedEntity() {
        if (this.linked == null || !(this.level instanceof ServerLevel server)) {
            return null;
        }
        final Entity entity = server.getEntity(this.linked);
        return entity != null ? entity : server.getServer().getPlayerList().getPlayer(this.linked) instanceof ServerPlayer player
                && player.level() == server ? player : null;
    }

    public void serverTick() {
        if (++this.ticks % 2 == 0 && this.level instanceof ServerLevel server) {
            final DisplayNetwork.TelemetryData data = snapshot();
            for (ServerPlayer player : server.players()) {
                if (player.distanceToSqr(this.worldPosition.getCenter()) < 320.0D * 320.0D) {
                    io.github.reiko45.galacticraft.network.GCNetwork.sendToPlayer(player, data);
                }
            }
        }
    }

    /** What the unit sends now (1.12.2 update's packet), moving the heart rate on a step. */
    public DisplayNetwork.TelemetryData snapshot() {
        String name = "";
        final int[] data = {-1, -1, -1, -1, -1};
        String uuid = "";
        final Entity entity = linkedEntity();
        if (entity != null && entity.isAlive()) {
            name = entity instanceof ServerPlayer player ? "$" + player.getGameProfile().getName() : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            final double motionX = entity.getDeltaMovement().x;
            final double motionY = entity instanceof LivingEntity ? entity.getDeltaMovement().y + 0.078D : entity.getDeltaMovement().y;
            final double motionZ = entity.getDeltaMovement().z;
            data[2] = (int) (Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ)) * 2000.0F);
            if (entity instanceof Telemetry telemetry) {
                telemetry.transmitData(data);
            } else if (entity instanceof LivingEntity living) {
                data[0] = living.hurtTime;
                this.pulseRate--;
                if (living.hurtTime > this.lastHurtTime) {
                    this.pulseRate += 100;
                }
                this.lastHurtTime = living.hurtTime;
                if (living.getVehicle() != null) {
                    data[2] /= 4; // a quieter pulse while riding
                } else if (data[2] > 1) {
                    this.pulseRate += 2;
                }
                this.pulseRate += Math.max(data[2] - this.pulseRate, 0) / 4;
                this.pulseRate = Mth.clamp(this.pulseRate, 400, 2000);
                data[2] = this.pulseRate / 10;
                data[1] = (int) (living.getHealth() * 100 / living.getMaxHealth());
                if (living instanceof ServerPlayer player) {
                    data[3] = player.getFoodData().getFoodLevel() * 5;
                    final GCPlayerData stats = GCPlayerData.get(player);
                    data[4] = stats.airRemaining() * 4096 + stats.airRemaining2();
                    uuid = player.getUUID().toString();
                } else if (living instanceof Horse horse) {
                    data[4] = horse.getVariant().getId() & 255 | horse.getMarkings().getId() << 8 & 65280; // its saved "Variant"
                } else if (living instanceof Villager villager) {
                    data[3] = BuiltInRegistries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
                    data[4] = villager.getAge();
                } else if (living instanceof Wolf wolf) {
                    data[3] = wolf.getCollarColor().getId();
                    data[4] = wolf.isInterested() ? 1 : 0;
                } else if (living instanceof Sheep sheep) {
                    data[3] = sheep.getColor().getId();
                    data[4] = sheep.isSheared() ? 1 : 0;
                } else if (living instanceof Zombie zombie) {
                    data[4] = zombie.isBaby() ? 1 : 0;
                }
            }
        }
        return new DisplayNetwork.TelemetryData(this.worldPosition, name, data[0], data[1], data[2], data[3], data[4], uuid);
    }

    /** Client (1.12.2 receiveUpdate). */
    public void receive(DisplayNetwork.TelemetryData payload) {
        this.clientName = payload.name();
        this.clientData = new int[] {payload.data0(), payload.data1(), payload.data2(), payload.data3(), payload.data4()};
        this.clientUuid = payload.uuid();
    }

    /** "" for no link, "$name" for a player, else the entity type's id. */
    public String clientName() {
        return this.clientName;
    }

    public int[] clientData() {
        return this.clientData;
    }

    public String clientUuid() {
        return this.clientUuid;
    }

    /**
     * 1.12.2 frequencyModulePlayer, run every tick: putting on a frequency module last used on a Telemetry Unit links the
     * wearer to it; taking it off unlinks them.
     */
    public static void frequencyModuleTick(ServerPlayer player) {
        final ItemStack worn = GCPlayerData.get(player).gear(GearSlot.FREQUENCY_MODULE);
        final ItemStack last = LAST_WORN.getOrDefault(player, ItemStack.EMPTY);
        if (ItemStack.matches(worn, last)) {
            return;
        }
        LAST_WORN.put(player, worn.copy());
        if (worn.isEmpty()) {
            link(last, player, true);
        } else if (last.isEmpty()) {
            link(worn, player, false);
        }
    }

    private static void link(ItemStack module, ServerPlayer player, boolean remove) {
        if (!FrequencyModuleItem.hasTelemetry(module) || player.getServer() == null) {
            return;
        }
        final var key = FrequencyModuleItem.telemetryLevel(module);
        final ServerLevel level = key == null ? null : player.getServer().getLevel(key);
        final BlockPos pos = FrequencyModuleItem.telemetryPos(module);
        if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TelemetryBlockEntity telemetry) {
            if (!remove) {
                telemetry.addTrackedEntity(player.getUUID());
            } else if (player.getUUID().equals(telemetry.linked)) {
                telemetry.removeTrackedEntity();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.linked != null) {
            tag.putUUID("entityUUID", this.linked);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linked = tag.hasUUID("entityUUID") ? tag.getUUID("entityUUID") : null;
    }
}
