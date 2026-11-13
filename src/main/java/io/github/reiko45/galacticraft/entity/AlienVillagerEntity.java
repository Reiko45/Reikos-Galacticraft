/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.EntityAlienVillager). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Alien Villager (1.12.2 EntityAlienVillager): the big-brained people of the Moon villages. They breathe without air,
 * never despawn, can't be leashed, and keep away from zombies. Talking to one needs a Frequency Module in the
 * Galacticraft inventory; then it trades its fixed list for Lunar Sapphires: oxygen gear, a part-filled tank, fuel and
 * a battery, a parachute, a wafer, swapping the Moon Buggy and Tier 2 Rocket schematics, compressing copper and tin,
 * canned carrots for an empty canister, and sapphires for jungle saplings, the one thing they can't grow. Each trade
 * can be made 7 times; after the first trade (and sometimes later ones) it restocks: 2 seconds after trading stops,
 * every sold-out trade gets 2-12 more uses, and the villager regenerates for 10 seconds. Struck by lightning it turns
 * into a witch.
 */
public class AlienVillagerEntity extends AbstractVillager {
    /** 1.12.2 MerchantRecipe's default maxTradeUses. */
    public static final int MAX_USES = 7;
    /** Ticks a player is ignored between "needs a Frequency Module" messages (1.12.2 chat cooldown). */
    private static final int CHAT_COOLDOWN = 20;

    private int timeUntilReset;
    private boolean needsRestock;

    public AlienVillagerEntity(EntityType<? extends AlienVillagerEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
        setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.6D, 0.6D));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6D));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    // ------------------------------------------------------------------ trading

    private static ItemStack lunarSapphires(int count) {
        return new ItemStack(GCItems.LUNAR_SAPPHIRE.get(), count);
    }

    /** 1.12.2 PriceInfo#getPrice: min to max inclusive. */
    private static int price(RandomSource random, int min, int max) {
        return min >= max ? min : min + random.nextInt(max - min + 1);
    }

    /** 1.12.2 ListItemForEmeralds: the item for some sapphires. */
    private static MerchantOffer sell(ItemStack item, int price) {
        return new MerchantOffer(new ItemCost(GCItems.LUNAR_SAPPHIRE.get(), price), item, MAX_USES, 0, 0.0F);
    }

    /** 1.12.2 ItemAndEmeraldToItem: an item and some sapphires for another item. */
    private static MerchantOffer exchange(ItemLike given, int count, int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(given, count), Optional.of(new ItemCost(GCItems.LUNAR_SAPPHIRE.get(), price)), result, MAX_USES, 0, 0.0F);
    }

    /** 1.12.2 DEFAULT_TRADE_LIST_MAP, in its order. */
    public static MerchantOffers tradeList(RandomSource random) {
        final MerchantOffers offers = new MerchantOffers();
        offers.add(sell(new ItemStack(GCItems.OXYGEN_MASK.get()), price(random, 1, 2)));
        // 1.12.2 tank damage 235 of 900: 665 oxygen left
        final ItemStack tank = new ItemStack(GCItems.LIGHT_OXYGEN_TANK.get());
        OxygenTankItem.setOxygen(tank, 900 - 235);
        offers.add(sell(tank, price(random, 3, 4)));
        offers.add(sell(new ItemStack(GCItems.OXYGEN_GEAR.get()), price(random, 3, 4)));
        // 1.12.2 canister damage 317 of 1,001: 684 mB of fuel
        offers.add(sell(LiquidCanisterItem.filled(GCItems.FUEL_CANISTER.get(), new FluidStack(GCFluids.FUEL.get(), 1001 - 317)), price(random, 3, 4)));
        offers.add(sell(new ItemStack(GCItems.PARACHUTES.get(0).get()), price(random, 1, 2)));
        // 1.12.2 battery damage 58 of 100: 42% charged
        final ItemStack battery = new ItemStack(GCItems.BATTERY.get());
        ((ElectricItem) battery.getItem()).setStored(battery, ((ElectricItem) battery.getItem()).capacity() * 0.42F);
        offers.add(sell(battery, price(random, 2, 4)));
        // "carrots = also yields a tin!"
        offers.add(exchange(GCItems.OIL_CANISTER.get(), 1, price(random, 1, 1), new ItemStack(GCItems.DEHYDRATED_CARROT.get())));
        offers.add(sell(new ItemStack(GCItems.BASIC_WAFER.get()), price(random, 3, 4)));
        offers.add(exchange(GCItems.BUGGY_SCHEMATIC.get(), 1, price(random, 3, 5), new ItemStack(GCItems.TIER_2_ROCKET_SCHEMATIC.get())));
        offers.add(exchange(GCItems.TIER_2_ROCKET_SCHEMATIC.get(), 1, price(random, 3, 5), new ItemStack(GCItems.BUGGY_SCHEMATIC.get())));
        offers.add(exchange(Items.COPPER_INGOT, 2, price(random, 1, 1), new ItemStack(GCItems.COMPRESSED_COPPER.get())));
        offers.add(exchange(GCItems.TIN_INGOT.get(), 2, price(random, 1, 1), new ItemStack(GCItems.COMPRESSED_TIN.get())));
        // 1.12.2 EmeraldForItems: "the one thing Alien Villagers don't have and can't get is jungle trees"
        offers.add(new MerchantOffer(new ItemCost(Items.JUNGLE_SAPLING, price(random, 11, 39)), lunarSapphires(1), MAX_USES, 0, 0.0F));
        return offers;
    }

    @Override
    protected void updateTrades() {
        getOffers().addAll(tradeList(this.random));
    }

    /** 1.12.2 useRecipe: 3-6 experience (8-11 when it decides to restock: the first sale of a trade, or one in five). */
    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        int xp = 3 + this.random.nextInt(4);
        if (offer.getUses() == 1 || this.random.nextInt(5) == 0) {
            this.timeUntilReset = 40;
            this.needsRestock = true;
            xp += 5;
        }
        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new ExperienceOrb(this.level(), getX(), getY() + 0.5D, getZ(), xp));
        }
    }

    /** 1.12.2 updateAITasks: two seconds after trading stops, sold-out trades get 2-12 more uses, and it regenerates. */
    @Override
    protected void customServerAiStep() {
        if (!isTrading() && this.timeUntilReset > 0) {
            --this.timeUntilReset;
            if (this.timeUntilReset <= 0) {
                if (this.needsRestock) {
                    restock();
                    this.needsRestock = false;
                }
                addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            }
        }
        super.customServerAiStep();
    }

    private void restock() {
        final MerchantOffers offers = getOffers();
        for (int i = 0; i < offers.size(); i++) {
            final MerchantOffer offer = offers.get(i);
            if (offer.isOutOfStock()) {
                final int more = this.random.nextInt(6) + this.random.nextInt(6) + 2;
                offers.set(i, new MerchantOffer(offer.getItemCostA(), offer.getItemCostB(), offer.getResult(), offer.getUses(), offer.getMaxUses() + more,
                        offer.getXp(), offer.getPriceMultiplier(), offer.getDemand()));
            }
        }
    }

    /** 1.12.2 processInteract: trading needs a Frequency Module to talk to them. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        final ItemStack held = player.getItemInHand(hand);
        if (!held.is(GCItems.ALIEN_VILLAGER_SPAWN_EGG.get()) && isAlive() && !isTrading() && !isBaby() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide && (this.offers == null || !getOffers().isEmpty())) {
                final GCPlayerData data = GCPlayerData.get(player);
                if (!data.gear(GearSlot.FREQUENCY_MODULE).isEmpty()) {
                    setTradingPlayer(player);
                    openTradingScreen(player, getDisplayName(), 1);
                } else if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount - data.lastVillagerChat >= CHAT_COOLDOWN) {
                    serverPlayer.sendSystemMessage(Component.translatable("gui.galacticraftcore.village.warning.no_freq_mod"));
                    data.lastVillagerChat = serverPlayer.tickCount;
                }
            }
            player.awardStat(Stats.TALKED_TO_VILLAGER);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    // ------------------------------------------------------------------ the rest of 1.12.2 EntityAlienVillager

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        final AlienVillagerEntity child = GCEntities.ALIEN_VILLAGER.get().create(level);
        if (child != null) {
            child.finalizeSpawn(level, level.getCurrentDifficultyAt(child.blockPosition()), MobSpawnType.BREEDING, null);
        }
        return child;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** 1.12.2: into an Evolved Witch. */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        if (isRemoved()) {
            return;
        }
        final Witch witch = io.github.reiko45.galacticraft.registry.GCEntities.EVOLVED_WITCH.get().create(level);
        if (witch == null) {
            return;
        }
        witch.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        witch.finalizeSpawn(level, level.getCurrentDifficultyAt(witch.blockPosition()), MobSpawnType.CONVERSION, null);
        witch.setNoAi(isNoAi());
        if (hasCustomName()) {
            witch.setCustomName(getCustomName());
            witch.setCustomNameVisible(isCustomNameVisible());
        }
        witch.setPersistenceRequired();
        level.addFreshEntityWithPassengers(witch);
        discard();
    }

    /** 1.12.2 canVillagerPickupItem: bread, potatoes, carrots, wheat and seeds, into its 8-slot inventory. */
    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return (stack.is(Items.BREAD) || stack.is(Items.POTATO) || stack.is(Items.CARROT) || stack.is(Items.WHEAT) || stack.is(Items.WHEAT_SEEDS))
                && getInventory().canAddItem(stack);
    }

    @Override
    protected void pickUpItem(ItemEntity item) {
        InventoryCarrier.pickUpItem(this, this, item);
    }

    /** 1.12.2 getSoundPitch: a low voice. */
    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.5F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean accepted) {
        return accepted ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TimeUntilReset", this.timeUntilReset);
        tag.putBoolean("NeedsRestock", this.needsRestock);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.timeUntilReset = tag.getInt("TimeUntilReset");
        this.needsRestock = tag.getBoolean("NeedsRestock");
    }
}
