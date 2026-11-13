/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.tile.TileEntityLaserTurret). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import com.google.common.collect.ComparisonChain;
import io.github.reiko45.galacticraft.block.ConfigurableMachineBlock;
import io.github.reiko45.galacticraft.entity.MeteorEntity;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.LaserTurretMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.race.SpaceRace;
import io.github.reiko45.galacticraft.race.SpaceRaces;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.12.2 TileEntityLaserTurret. Powered (over 1,000 gJ stored, enabled and without a redstone signal) it charges for 60
 * ticks, and once a second picks a target: any meteor within 90 blocks across (if it targets meteors), else the living
 * things within 15 blocks of its barrels, not more than 60 degrees below them, in sight, in the order its priorities
 * give. A full charge fires at the target: 1.5 damage (a meteor is destroyed), for 1,000 gJ.
 * <p>
 * By default (whitelist mode) it fires at everything except the players and entity types on its list; in blacklist mode
 * only at those. Unless told otherwise it never fires at its owner or the owner's Space Race team, and it never fires at
 * the owner's pets. Only the owner can change its settings.
 */
public class LaserTurretBlockEntity extends MachineBlockEntity {
    public static final float RANGE = 15.0F;
    public static final float METEOR_RANGE = 90.0F;
    public static final float SHOT_ENERGY = 1000.0F;
    public static final float DAMAGE = 1.5F;
    public static final int FULL_CHARGE = 60;
    /** 1.12.2 laser turret: the default tile storage (16,000 gJ, 25 in, 10 out). */
    public static final float CAPACITY = 16_000.0F;
    /** Most names a list keeps (1.12.2 had no limit). */
    public static final int MAX_LIST = 64;
    public static final int MAX_NAME = 64;

    private final List<String> players = new ArrayList<>();
    private final List<ResourceLocation> entities = new ArrayList<>();
    private boolean active;
    private int targetedEntity = -1;
    private int chargeLevel;
    private boolean blacklistMode;
    private boolean targetMeteors = true;
    private boolean alwaysIgnoreSpaceRace = true;
    private int priorityClosest = 1;
    private int priorityLowestHealth = 2;
    private int priorityHighestHealth = 3;
    private boolean disabled;
    private int disableCooldown;
    @Nullable
    private UUID ownerId;
    private String ownerName = "";
    private int ticks;

    // client only: the barrels' aim, eased towards the target each tick
    public float yaw;
    public float pitch;
    public float prevYaw;
    public float prevPitch;
    private float targetYaw;
    private float targetPitch;
    public int timeSinceShot = -1;

    public LaserTurretBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.LASER_TURRET.get(), pos, state, 1, CAPACITY, 25.0F, 10.0F);
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> (this.disabled ? 1 : 0) | (powered() ? 2 : 0));
        data.addShort(() -> this.disableCooldown);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ElectricItem;
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        final BlockState state = getBlockState();
        final RelativeSide input = state.hasProperty(ConfigurableMachineBlock.INPUT)
                ? state.getValue(ConfigurableMachineBlock.INPUT).side() : RelativeSide.LEFT;
        return side == input ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    /** 1.12.2 getSlotsForFace: the battery slot from every side. */
    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0};
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return true;
    }

    // ------------------------------------------------------------------ server

    private boolean powered() {
        return this.level != null && this.level.hasNeighborSignal(this.worldPosition);
    }

    /** 1.12.2: over 1,000 gJ, enabled and no redstone signal. */
    public boolean canFire() {
        return this.energy.getStoredGJ() > SHOT_ENERGY && !this.disabled && !powered();
    }

    @Override
    protected void tick() {
        this.ticks++;
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        dischargeBattery(0);
        // 1.12.2 slowDischarge: a machine that doesn't use energy as it runs loses 5 gJ every 10 ticks
        if (this.ticks % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
        final boolean ready = canFire();
        if (ready) {
            if (this.chargeLevel < FULL_CHARGE) {
                this.chargeLevel++;
                if (this.chargeLevel == 1) {
                    sync();
                }
            }
        } else if (this.chargeLevel != 0) {
            this.chargeLevel = 0;
            sync();
        }
        if (this.ticks % 20 == 0) {
            final Entity target = ready ? findTarget() : null;
            final boolean nowActive = target != null && this.chargeLevel > 0;
            final int id = nowActive ? target.getId() : -1;
            if (nowActive != this.active || id != this.targetedEntity) {
                this.active = nowActive;
                this.targetedEntity = id;
                sync();
            }
        }
        if (this.chargeLevel >= FULL_CHARGE && this.targetedEntity != -1) {
            final Entity target = this.level.getEntity(this.targetedEntity);
            if (target != null) {
                if (target instanceof LivingEntity living) {
                    living.hurt(new DamageSource(this.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(GCDamageTypes.LASER_TURRET)), DAMAGE);
                } else if (target instanceof MeteorEntity) {
                    target.discard();
                }
                this.level.playSound(null, this.worldPosition.above(), GCSounds.LASER_SHOOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                this.energy.setStored(this.energy.getStoredGJ() - SHOT_ENERGY);
                this.chargeLevel = 0;
                sync();
            }
        } else if (this.chargeLevel == 22) {
            this.level.playSound(null, this.worldPosition.above(), GCSounds.LASER_CHARGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Where the barrels are: 1.78 blocks above the base block's middle. */
    public Vec3 barrels() {
        return new Vec3(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.78D, this.worldPosition.getZ() + 0.5D);
    }

    /** 1.12.2 updateTarget. */
    @Nullable
    public Entity findTarget() {
        final Vec3 barrels = barrels();
        if (this.targetMeteors) {
            // 1.12.2: the first meteor in range is taken at once, before any living thing and without a sight check
            final AABB column = new AABB(barrels.x - METEOR_RANGE, this.level.getMinBuildHeight(), barrels.z - METEOR_RANGE,
                    barrels.x + METEOR_RANGE, this.level.getMaxBuildHeight(), barrels.z + METEOR_RANGE);
            for (MeteorEntity meteor : this.level.getEntitiesOfClass(MeteorEntity.class, column, Entity::isAlive)) {
                final Vec3 to = meteor.position().add(0.0D, meteor.getEyeHeight(), 0.0D).subtract(barrels);
                if (shouldTarget(meteor) && to.horizontalDistanceSqr() < METEOR_RANGE * METEOR_RANGE && notTooSteep(to)) {
                    return meteor;
                }
            }
        }
        final List<Candidate> list = new ArrayList<>();
        for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, new AABB(barrels, barrels).inflate(RANGE),
                entity -> entity.isAlive() && !entity.isSpectator())) {
            final Vec3 to = living.getEyePosition().subtract(barrels);
            if (to.lengthSqr() < RANGE * RANGE && notTooSteep(to) && shouldTarget(living)) {
                list.add(new Candidate(living, to.length()));
            }
        }
        list.sort(priorityOrder());
        for (Candidate candidate : list) {
            final Vec3 end = candidate.entity.getEyePosition();
            // 1.12.2: the sight line starts a block out from the barrels, towards the target
            final Vec3 start = barrels.add(end.subtract(barrels).normalize());
            final HitResult hit = this.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return candidate.entity;
            }
        }
        return null;
    }

    private static boolean notTooSteep(Vec3 to) {
        return Math.asin(to.normalize().y) > -Math.PI / 3.0D;
    }

    private record Candidate(LivingEntity entity, double distance) {
    }

    /** 1.12.2's sort: by whichever of closest, lowest and highest health has the lowest priority number, then the next. */
    private Comparator<Candidate> priorityOrder() {
        final int closest = this.priorityClosest;
        final int lowest = this.priorityLowestHealth;
        final int highest = this.priorityHighestHealth;
        return (a, b) -> {
            if (closest < highest && closest < lowest) {
                return lowest < highest
                        ? ComparisonChain.start().compare(a.distance, b.distance).compare(a.entity.getHealth(), b.entity.getHealth()).result()
                        : ComparisonChain.start().compare(a.distance, b.distance).compare(b.entity.getHealth(), a.entity.getHealth()).result();
            } else if (highest < lowest && highest < closest) {
                return ComparisonChain.start().compare(b.entity.getHealth(), a.entity.getHealth()).compare(a.distance, b.distance).result();
            } else if (lowest < highest && lowest < closest) {
                return ComparisonChain.start().compare(a.entity.getHealth(), b.entity.getHealth()).compare(a.distance, b.distance).result();
            }
            return 0;
        };
    }

    /** 1.12.2 updateTarget's list check. */
    public boolean shouldTarget(Entity entity) {
        boolean target = !this.blacklistMode;
        if (entity instanceof Player player) {
            if (this.alwaysIgnoreSpaceRace && (player.getUUID().equals(this.ownerId) || onOwnersTeam(player.getUUID()))) {
                return false;
            }
            for (String name : this.players) {
                if (name.equalsIgnoreCase(player.getGameProfile().getName())) {
                    target = !target;
                }
            }
        } else if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            if (pet.getOwnerUUID().equals(this.ownerId) || this.alwaysIgnoreSpaceRace && onOwnersTeam(pet.getOwnerUUID())) {
                return false;
            }
        } else {
            final ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            for (ResourceLocation listed : this.entities) {
                if (listed.equals(type)) {
                    target = !target;
                }
            }
        }
        return target;
    }

    private boolean onOwnersTeam(UUID player) {
        if (this.ownerId == null || this.level == null || this.level.getServer() == null) {
            return false;
        }
        final SpaceRace race = SpaceRaces.get(this.level.getServer()).of(this.ownerId);
        return race != null && race.has(player);
    }

    // ------------------------------------------------------------------ client

    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, LaserTurretBlockEntity turret) {
        turret.clientTick();
    }

    /** 1.12.2 update, client side: the charge counts on, and the barrels ease round to the target. */
    private void clientTick() {
        if (this.chargeLevel > 0 && this.chargeLevel < FULL_CHARGE) {
            this.chargeLevel++;
        }
        if (this.chargeLevel < 5) {
            this.timeSinceShot = 0;
        }
        this.timeSinceShot++;
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;
        if (this.active && this.targetedEntity != -1) {
            final Entity entity = this.level.getEntity(this.targetedEntity);
            if (entity != null && entity.isAlive()) {
                final Vec3 to = entity.position().add(0.0D, entity.getEyeHeight(), 0.0D).subtract(barrels()).normalize();
                this.targetPitch = Mth.clamp((float) Math.toDegrees(Math.asin(to.y)), -60.0F, 90.0F);
                this.targetYaw = (float) Math.toDegrees(Math.atan2(to.x, to.z)) + 90.0F;
                while (this.targetYaw > 360.0F) {
                    this.targetYaw -= 360.0F;
                }
                while (this.targetYaw < 0.0F) {
                    this.targetYaw += 360.0F;
                }
            }
        } else {
            this.targetPitch = -45.0F;
        }
        if (this.targetYaw > 270.0F && this.yaw < 90.0F) {
            this.yaw += (this.targetYaw - (this.yaw + 360.0F)) / 3.0F;
        } else if (this.targetYaw < 90.0F && this.yaw > 270.0F) {
            this.yaw += (this.targetYaw + 360.0F - this.yaw) / 3.0F;
        } else {
            this.yaw += (this.targetYaw - this.yaw) / 10.0F;
        }
        this.pitch += (this.targetPitch - this.pitch) / 3.0F;
        while (this.yaw > 360.0F) {
            this.yaw -= 360.0F;
            this.prevYaw -= 360.0F;
        }
        while (this.yaw < 0.0F) {
            this.yaw += 360.0F;
            this.prevYaw += 360.0F;
        }
        this.pitch = Mth.clamp(this.pitch, -60.0F, 90.0F);
    }

    // ------------------------------------------------------------------ settings

    public boolean isOwner(Player player) {
        return this.ownerId == null || this.ownerId.equals(player.getUUID());
    }

    @Nullable
    public UUID ownerId() {
        return this.ownerId;
    }

    public String ownerName() {
        return this.ownerName;
    }

    /** 1.12.2 onBlockPlacedBy: the placer owns it and goes on its list. */
    public void setOwner(Player player) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        addPlayer(this.ownerName);
    }

    public boolean active() {
        return this.active;
    }

    public int targetedEntity() {
        return this.targetedEntity;
    }

    public int chargeLevel() {
        return this.chargeLevel;
    }

    public boolean blacklistMode() {
        return this.blacklistMode;
    }

    public boolean targetMeteors() {
        return this.targetMeteors;
    }

    public boolean alwaysIgnoreSpaceRace() {
        return this.alwaysIgnoreSpaceRace;
    }

    public int priorityClosest() {
        return this.priorityClosest;
    }

    public int priorityLowestHealth() {
        return this.priorityLowestHealth;
    }

    public int priorityHighestHealth() {
        return this.priorityHighestHealth;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public List<String> players() {
        return this.players;
    }

    public List<ResourceLocation> entities() {
        return this.entities;
    }

    public void setBlacklistMode(boolean blacklist) {
        this.blacklistMode = blacklist;
        changed();
    }

    public void setTargetMeteors(boolean meteors) {
        this.targetMeteors = meteors;
        changed();
    }

    public void setAlwaysIgnoreSpaceRace(boolean ignore) {
        this.alwaysIgnoreSpaceRace = ignore;
        changed();
    }

    public void setPriorities(int closest, int lowestHealth, int highestHealth) {
        this.priorityClosest = closest;
        this.priorityLowestHealth = lowestHealth;
        this.priorityHighestHealth = highestHealth;
        changed();
    }

    /** 1.12.2 setDisabled 0: 10 tick cooldown between presses. */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 10;
            setChanged();
        }
    }

    public void addPlayer(String name) {
        if (this.players.size() < MAX_LIST) {
            this.players.add(name);
            changed();
        }
    }

    public void addEntity(ResourceLocation type) {
        if (this.entities.size() < MAX_LIST) {
            this.entities.add(type);
            changed();
        }
    }

    public void removePlayer(String name) {
        if (this.players.removeIf(name::equals)) {
            changed();
        }
    }

    public void removeEntity(ResourceLocation type) {
        if (this.entities.removeIf(type::equals)) {
            changed();
        }
    }

    /** 1.12.2 GuiLaserTurret#getStatus, then getGUIstatus. */
    public static MachineStatus status(boolean disabled, boolean redstone, float energy) {
        if (disabled) {
            return MachineStatus.DISABLED;
        }
        if (energy < SHOT_ENERGY) {
            return MachineStatus.MISSING_POWER;
        }
        if (redstone) {
            return MachineStatus.OFF;
        }
        return MachineStatus.ACTIVE;
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this, buf -> {
            buf.writeBlockPos(this.worldPosition);
            buf.writeBlockPos(this.worldPosition);
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new LaserTurretMenu(id, playerInventory, this);
    }

    // ------------------------------------------------------------------ saving and syncing

    private void changed() {
        setChanged();
        sync();
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void saveSettings(CompoundTag tag) {
        final ListTag players = new ListTag();
        for (String player : this.players) {
            final CompoundTag entry = new CompoundTag();
            entry.putString("PlayerName", player);
            players.add(entry);
        }
        tag.put("PlayerList", players);
        final ListTag entities = new ListTag();
        for (ResourceLocation entity : this.entities) {
            final CompoundTag entry = new CompoundTag();
            entry.putString("EntityRes", entity.toString());
            entities.add(entry);
        }
        tag.put("EntitiesList", entities);
        tag.putBoolean("active", this.active);
        tag.putInt("targettedEntity", this.targetedEntity);
        tag.putInt("chargeLevel", this.chargeLevel);
        tag.putBoolean("blacklistMode", this.blacklistMode);
        tag.putBoolean("targetMeteors", this.targetMeteors);
        tag.putBoolean("alwaysIgnoreSpaceRace", this.alwaysIgnoreSpaceRace);
        tag.putInt("priorityClosest", this.priorityClosest);
        tag.putInt("priorityLowestHealth", this.priorityLowestHealth);
        tag.putInt("priorityHighestHealth", this.priorityHighestHealth);
        tag.putString("ownerName", this.ownerName);
        if (this.ownerId != null) {
            tag.putUUID("ownerUUID", this.ownerId);
        }
    }

    private void loadSettings(CompoundTag tag) {
        this.players.clear();
        for (Tag entry : tag.getList("PlayerList", Tag.TAG_COMPOUND)) {
            this.players.add(((CompoundTag) entry).getString("PlayerName"));
        }
        this.entities.clear();
        for (Tag entry : tag.getList("EntitiesList", Tag.TAG_COMPOUND)) {
            final ResourceLocation type = ResourceLocation.tryParse(((CompoundTag) entry).getString("EntityRes"));
            if (type != null) {
                this.entities.add(type);
            }
        }
        this.active = tag.getBoolean("active");
        this.targetedEntity = tag.contains("targettedEntity") ? tag.getInt("targettedEntity") : -1;
        this.chargeLevel = tag.getInt("chargeLevel");
        this.blacklistMode = tag.getBoolean("blacklistMode");
        this.targetMeteors = !tag.contains("targetMeteors") || tag.getBoolean("targetMeteors");
        this.alwaysIgnoreSpaceRace = !tag.contains("alwaysIgnoreSpaceRace") || tag.getBoolean("alwaysIgnoreSpaceRace");
        if (tag.contains("priorityClosest")) {
            this.priorityClosest = tag.getInt("priorityClosest");
            this.priorityLowestHealth = tag.getInt("priorityLowestHealth");
            this.priorityHighestHealth = tag.getInt("priorityHighestHealth");
        }
        this.ownerName = tag.getString("ownerName");
        this.ownerId = tag.hasUUID("ownerUUID") ? tag.getUUID("ownerUUID") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveSettings(tag);
        tag.putBoolean("Disabled", this.disabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadSettings(tag);
        this.disabled = tag.getBoolean("Disabled");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        saveSettings(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadSettings(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        loadSettings(packet.getTag());
    }

    /** 1.12.2 getRenderBoundingBox: the block and the one above. */
    public AABB renderBox() {
        return new AABB(this.worldPosition).expandTowards(0.0D, 1.0D, 0.0D);
    }
}
