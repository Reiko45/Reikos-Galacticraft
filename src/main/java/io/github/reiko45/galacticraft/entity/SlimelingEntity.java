/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.entities.EntitySlimeling, the slimeling
 * cases of PacketSimpleMars and EventHandlerMars#onLivingDeath). Copyright (c) 2023 Team Galacticraft. Licensed under
 * the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.menu.SlimelingMenu;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCSounds;
import io.github.reiko45.galacticraft.util.ColorUtil;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Slimeling (1.12.2 EntitySlimeling), a pet hatched from a Slimeling Egg on Mars. A wild one is tamed with slime balls
 * (one chance in three each). It grows from half to full size over 100,000 ticks (1.12.2 counted one every two ticks:
 * about 2.8 hours), gaining health with age (20 to 50 once tamed). Owners open its panel by using it, or feed it its
 * favourite food (one of ten items, which may change after each meal) to grow it, breed it, strengthen its attack or heal
 * it. With an Inventory Bag in its bag slot it carries 27 stacks. Its colour is that of its egg, or a mix of its parents'.
 */
public class SlimelingEntity extends TamableAnimal {
    public static final int MAX_AGE = 100000;
    public static final int ACTION_SIT = 0;
    public static final int ACTION_RENAME = 1;
    public static final int ACTION_GROW = 2;
    public static final int ACTION_BREED = 3;
    public static final int ACTION_STRENGTHEN = 4;
    public static final int ACTION_HEAL = 5;
    public static final int ACTION_OPEN_INVENTORY = 6;
    public static final int SCREEN_MAIN = 0;
    public static final int SCREEN_FEED = 1;
    public static final int MAX_NAME_LENGTH = 30;

    /** 1.12.2 setRandomFavFood. */
    public static final List<Item> FOODS = List.of(Items.GOLD_INGOT, Items.FLINT_AND_STEEL, Items.BAKED_POTATO, Items.STONE_SWORD,
            Items.GUNPOWDER, Items.WOODEN_HOE, Items.EMERALD, Items.COD, Items.REPEATER, Items.OAK_BOAT);

    private static final EntityDataAccessor<Float> RED = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GREEN = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BLUE = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAVOURITE_FOOD = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ATTACK_DAMAGE = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.STRING);

    private final SlimelingInventory inventory = new SlimelingInventory(this);
    /** Meals eaten whose feed button hasn't been pressed yet (1.12.2 trusted the client here). */
    private int feedsPending;
    private long wrongPlayerMessageAt;

    public SlimelingEntity(EntityType<? extends SlimelingEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            switch (this.random.nextInt(3)) {
                case 0 -> setColor(1.0F, 0.0F, 0.0F);
                case 1 -> setColor(0.0F, 0.0F, 1.0F);
                default -> setColor(1.0F, 1.0F, 0.0F);
            }
            randomFavouriteFood();
        }
    }

    public static SlimelingEntity hatch(Level level, float red, float green, float blue) {
        final SlimelingEntity slimeling = GCEntities.SLIMELING.get().create(level);
        if (slimeling != null) {
            slimeling.setColor(red, green, blue);
        }
        return slimeling;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.30000001192092896D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.JUMP_STRENGTH, 0.48D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RED, 0.0F);
        builder.define(GREEN, 0.0F);
        builder.define(BLUE, 0.0F);
        builder.define(AGE, 0);
        builder.define(FAVOURITE_FOOD, 0);
        builder.define(ATTACK_DAMAGE, 0.05F);
        builder.define(KILLS, 0);
        builder.define(OWNER_NAME, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, SludgelingEntity.class, false, null));
    }

    // --- Synced state ---

    public float red() {
        return this.entityData.get(RED);
    }

    public float green() {
        return this.entityData.get(GREEN);
    }

    public float blue() {
        return this.entityData.get(BLUE);
    }

    public void setColor(float red, float green, float blue) {
        this.entityData.set(RED, red);
        this.entityData.set(GREEN, green);
        this.entityData.set(BLUE, blue);
    }

    /** 0 to {@link #MAX_AGE} (not the vanilla breeding age). */
    public int slimelingAge() {
        return this.entityData.get(AGE);
    }

    public void setSlimelingAge(int age) {
        this.entityData.set(AGE, Mth.clamp(age, 0, MAX_AGE));
    }

    /** 1.12.2 getScale: half size when hatched, full size when grown. */
    public float slimelingScale() {
        return slimelingAge() / (float) MAX_AGE * 0.5F + 0.5F;
    }

    public Item favouriteFood() {
        return FOODS.get(Mth.clamp(this.entityData.get(FAVOURITE_FOOD), 0, FOODS.size() - 1));
    }

    private void randomFavouriteFood() {
        this.entityData.set(FAVOURITE_FOOD, this.random.nextInt(FOODS.size()));
    }

    /** 0.05 when hatched, up to 1.0 by strengthening. */
    public float attackDamage() {
        return this.entityData.get(ATTACK_DAMAGE);
    }

    /** 1.12.2 getDamage: five times its attack damage once tamed, twice before. */
    public float damage() {
        return (isTame() ? 5 : 2) * attackDamage();
    }

    public int kills() {
        return this.entityData.get(KILLS);
    }

    public String ownerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        this.entityData.set(OWNER_NAME, name);
    }

    public SlimelingInventory inventory() {
        return this.inventory;
    }

    /** 1.12.2 isChild: under a third grown. */
    @Override
    public boolean isBaby() {
        return slimelingAge() / (float) MAX_AGE < 0.33F;
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(baby);
        if (baby) {
            setSlimelingAge(0);
        }
    }

    /** 1.12.2 setScaleForAge: its size (0.45 x 0.7 at scale 0.5) grows with it. */
    @Override
    public float getAgeScale() {
        return slimelingScale() * 2.0F;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (AGE.equals(key)) {
            refreshDimensions();
        }
    }

    /** 1.12.2 getMaxHealthSlimeling. */
    private double maxHealthForAge() {
        return isTame() ? 20.001D + 30.0D * slimelingAge() / MAX_AGE : 8.0D;
    }

    private void updateMaxHealth() {
        final var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(maxHealthForAge());
        }
    }

    @Override
    protected void applyTamingSideEffects() {
        updateMaxHealth();
    }

    /** 1.12.2 onLivingUpdate: one tick of age every two ticks; health follows age. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (this.tickCount % 2 == 0 && slimelingAge() < MAX_AGE) {
                setSlimelingAge(slimelingAge() + 1);
            }
            updateMaxHealth();
            if (ownerName().isEmpty() && getOwner() instanceof Player owner) {
                setOwnerName(owner.getGameProfile().getName());
            }
        }
    }

    // --- Taming, feeding and the panel ---

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (isTame()) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!stack.isEmpty() && stack.is(favouriteFood())) {
                if (isOwnedBy(player)) {
                    stack.shrink(1);
                    this.feedsPending++;
                    if (this.random.nextInt(3) == 0) {
                        randomFavouriteFood();
                    }
                    openScreen(player, SCREEN_FEED);
                } else if (level().getGameTime() - this.wrongPlayerMessageAt >= 100L) {
                    this.wrongPlayerMessageAt = level().getGameTime();
                    player.sendSystemMessage(Component.translatable("gui.galacticraftcore.slimeling.chat.wrong_player"));
                }
            } else {
                openScreen(player, SCREEN_MAIN);
            }
            return InteractionResult.CONSUME;
        }
        if (stack.is(Items.SLIME_BALL)) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            stack.consume(1, player);
            if (this.random.nextInt(3) == 0) {
                tame(player);
                this.navigation.stop();
                setTarget(null);
                setOrderedToSit(true);
                setHealth(20.0F);
                setOwnerName(player.getGameProfile().getName());
                level().broadcastEntityEvent(this, (byte) 7);
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    private void openScreen(Player player, int screen) {
        if (player instanceof ServerPlayer serverPlayer) {
            GCNetwork.sendToPlayer(serverPlayer, new GCNetwork.OpenSlimelingScreen(getId(), screen));
        }
    }

    /** 1.12.2 PacketSimpleMars S_UPDATE_SLIMELING_DATA: only the owner, from close by; feeding needs a meal first. */
    public void handleAction(ServerPlayer player, int action, String text) {
        if (!isOwnedBy(player) || player.distanceToSqr(this) > 64.0D || !isAlive()) {
            return;
        }
        switch (action) {
            case ACTION_SIT -> {
                setOrderedToSit(!isOrderedToSit());
                setJumping(false);
                this.navigation.stop();
                setTarget(null);
            }
            case ACTION_RENAME -> {
                final String name = text.length() > MAX_NAME_LENGTH ? text.substring(0, MAX_NAME_LENGTH) : text;
                setCustomName(name.isBlank() ? null : Component.literal(name));
            }
            case ACTION_OPEN_INVENTORY -> SlimelingMenu.open(player, this);
            case ACTION_GROW, ACTION_BREED, ACTION_STRENGTHEN, ACTION_HEAL -> {
                if (this.feedsPending > 0 && canFeed(action)) {
                    this.feedsPending--;
                    feed(player, action);
                }
            }
            default -> {
            }
        }
    }

    /** 1.12.2 GuiSlimelingFeed#drawScreen: which feed buttons are enabled. */
    public boolean canFeed(int action) {
        return switch (action) {
            case ACTION_GROW -> slimelingScale() < 1.0F;
            case ACTION_BREED -> !isInLove() && !isBaby();
            case ACTION_STRENGTHEN -> attackDamage() < 1.0F;
            case ACTION_HEAL -> getHealth() < Math.floor(getMaxHealth());
            default -> false;
        };
    }

    private void feed(ServerPlayer player, int action) {
        switch (action) {
            case ACTION_GROW -> setSlimelingAge(slimelingAge() + 5000);
            case ACTION_BREED -> setInLove(player);
            case ACTION_STRENGTHEN -> this.entityData.set(ATTACK_DAMAGE, Math.min(attackDamage() + 0.1F, 1.0F));
            case ACTION_HEAL -> setHealth(getHealth() + 5.0F);
            default -> {
            }
        }
    }

    // --- Combat ---

    /** 1.12.2 attackEntityFrom: being hurt makes it stand up; damage from anything but a player is roughly halved. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide) {
            setOrderedToSit(false);
        }
        if (source.getEntity() != null && !(source.getEntity() instanceof Player)) {
            amount = (amount + 1.0F) / 2.0F;
        }
        return super.hurt(source, amount);
    }

    /** 1.12.2 attackEntityAsMob: "slimeling" damage. */
    @Override
    public boolean doHurtTarget(Entity target) {
        return target.hurt(GCDamageTypes.slimeling(this), damage());
    }

    /** 1.12.2 EventHandlerMars#onLivingDeath: kills by its own attack count. */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity) {
        if (entity.getLastDamageSource() != null && entity.getLastDamageSource().is(GCDamageTypes.SLIMELING)) {
            this.entityData.set(KILLS, kills() + 1);
        }
        return super.killedEntity(level, entity);
    }

    /** 1.12.2 shouldAttackEntity: never creepers, ghasts, its owner's other slimelings, protected players or tame horses. */
    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast) {
            return false;
        }
        if (target instanceof SlimelingEntity slimeling && slimeling.isTame() && slimeling.getOwner() == owner) {
            return false;
        }
        if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false;
        }
        return !(target instanceof AbstractHorse horse && horse.isTamed());
    }

    // --- Breeding ---

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    /** 1.12.2 canMateWith: two tame slimelings, both in love, the other not sitting. */
    @Override
    public boolean canMate(Animal other) {
        return other != this && isTame() && other instanceof SlimelingEntity slimeling && slimeling.isTame() && !slimeling.isOrderedToSit()
                && isInLove() && slimeling.isInLove();
    }

    /** 1.12.2 spawnBabyAnimal: the parents' colours mixed by hue; tame and owned like this one. */
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (!(otherParent instanceof SlimelingEntity other)) {
            return null;
        }
        final Vec3 mixed = ColorUtil.addColorsRealistically(new Vec3(red(), green(), blue()), new Vec3(other.red(), other.green(), other.blue()));
        final SlimelingEntity baby = hatch(level, (float) Mth.clamp(mixed.x, 0.0D, 1.0D), (float) Mth.clamp(mixed.y, 0.0D, 1.0D),
                (float) Mth.clamp(mixed.z, 0.0D, 1.0D));
        if (baby != null && getOwnerUUID() != null) {
            baby.setOwnerUUID(getOwnerUUID());
            baby.setTame(true, true);
            baby.setOwnerName(ownerName());
        }
        return baby;
    }

    // --- Sounds and death ---

    /** 1.12.2: a slime block's step, a little higher. */
    @Override
    protected void playHurtSound(DamageSource source) {
        playSound(SoundEvents.SLIME_BLOCK_STEP, getSoundVolume(), 1.1F);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    /** 1.12.2 onDeath: the slimeling death sound, and the bag (with what was in it) falls out. */
    @Override
    public void die(DamageSource source) {
        if (!isRemoved() && !this.dead) {
            playSound(GCSounds.SLIME_DEATH.get(), getSoundVolume(), 0.8F);
            if (!level().isClientSide) {
                final ItemStack bag = this.inventory.removeItemNoUpdate(SlimelingInventory.BAG_SLOT);
                if (!bag.isEmpty()) {
                    spawnAtLocation(bag, 0.5F);
                }
            }
        }
        super.die(source);
    }

    // --- Saving ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        final HolderLookup.Provider registries = registryAccess();
        tag.put("SlimelingInventory", ContainerHelper.saveAllItems(new CompoundTag(), this.inventory.items(), registries));
        tag.putFloat("SlimeRed", red());
        tag.putFloat("SlimeGreen", green());
        tag.putFloat("SlimeBlue", blue());
        tag.putInt("SlimelingAge", slimelingAge());
        tag.putInt("FavFood", this.entityData.get(FAVOURITE_FOOD));
        tag.putFloat("SlimelingDamage", attackDamage());
        tag.putInt("SlimelingKills", kills());
        tag.putString("OwnerUsername", ownerName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        final NonNullList<ItemStack> items = this.inventory.items();
        ContainerHelper.loadAllItems(tag.getCompound("SlimelingInventory"), items, registryAccess());
        setColor(tag.getFloat("SlimeRed"), tag.getFloat("SlimeGreen"), tag.getFloat("SlimeBlue"));
        setSlimelingAge(tag.getInt("SlimelingAge"));
        this.entityData.set(FAVOURITE_FOOD, Mth.clamp(tag.getInt("FavFood"), 0, FOODS.size() - 1));
        if (tag.contains("SlimelingDamage")) {
            this.entityData.set(ATTACK_DAMAGE, tag.getFloat("SlimelingDamage"));
        }
        this.entityData.set(KILLS, tag.getInt("SlimelingKills"));
        setOwnerName(tag.getString("OwnerUsername"));
        updateMaxHealth();
    }
}
