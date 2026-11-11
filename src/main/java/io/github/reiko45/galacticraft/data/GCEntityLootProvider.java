package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Evolved mob drops (1.12.2 EntityEvolved*#dropFewItems / #addRandomDrop): the usual drops, plus a rare drop when a
 * player made the kill (2.5% + 2% per Looting level) picked from each mob's 1.12.2 list. Entries that 1.12.2 only gave
 * in "challenge mode" are left out (they drop nothing).
 */
public class GCEntityLootProvider extends EntityLootSubProvider {
    public GCEntityLootProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return GCEntities.ENTITY_TYPES.getEntries().stream().map(holder -> (EntityType<?>) holder.get());
    }

    @Override
    public void generate() {
        add(GCEntities.EVOLVED_ZOMBIE.get(), LootTable.lootTable()
                .withPool(common(Items.ROTTEN_FLESH, 0, 1))
                .withPool(rare(weighted(GCItems.DEHYDRATED_CARROT.get(), 3), weighted(GCItems.METEORIC_IRON_RAW.get(), 2),
                        weighted(GCItems.DEHYDRATED_POTATO.get(), 2), halfTank(2), weighted(GCItems.OXYGEN_MASK.get(), 1),
                        weighted(GCItems.OXYGEN_VENT.get(), 1), weighted(Items.CARROT, 2), nothing(3))));
        add(GCEntities.EVOLVED_SKELETON.get(), LootTable.lootTable()
                .withPool(common(Items.ARROW, 0, 2))
                .withPool(common(Items.BONE, 0, 2))
                .withPool(rare(weighted(io.github.reiko45.galacticraft.registry.GCBlocks.FLUID_PIPE.get(), 6), halfTank(1),
                        weighted(GCItems.TIN_CANISTER.get(), 2), nothing(1))));
        add(GCEntities.EVOLVED_SPIDER.get(), LootTable.lootTable()
                .withPool(common(Items.STRING, 0, 2))
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.SPIDER_EYE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(-1.0F, 1.0F)))
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries, UniformGenerator.between(0.0F, 1.0F))))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(rare(weighted(GCItems.CHEESE_CURD.get(), 3), weighted(Items.FERMENTED_SPIDER_EYE, 3), halfTank(2),
                        weighted(GCItems.OXYGEN_GEAR.get(), 1), weighted(GCItems.OXYGEN_CONCENTRATOR.get(), 1), nothing(1))));
        add(GCEntities.EVOLVED_CREEPER.get(), LootTable.lootTable()
                .withPool(common(Items.GUNPOWDER, 0, 2))
                .withPool(rare(weighted(Blocks.SAND, 4), halfTank(2), weighted(GCItems.OXYGEN_GEAR.get(), 1), weighted(Blocks.ICE, 2), nothing(1))));
        add(GCEntities.EVOLVED_ENDERMAN.get(), LootTable.lootTable()
                .withPool(common(Items.ENDER_PEARL, 0, 1))
                .withPool(rare(weighted(Items.ENDER_PEARL, 6), halfTank(1), weighted(Items.ENDER_EYE, 2),
                        weighted(GCItems.OXYGEN_CONCENTRATOR.get(), 1), weighted(GCItems.OXYGEN_MASK.get(), 1))));
        // 1.12.2 EntitySkeletonBoss#dropFewItems: a bow, (5 + looting) in 200 (its key and treasure come at the end of the death)
        add(GCEntities.SKELETON_BOSS.get(), LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.BOW))
                .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(this.registries, 0.025F, 0.005F))));
        // 1.12.2 EntitySlimeling#getDropItem: 0-2 slime balls (plus looting); EntitySludgeling drops nothing
        add(GCEntities.SLIMELING.get(), LootTable.lootTable().withPool(common(Items.SLIME_BALL, 0, 2)));
        add(GCEntities.SLUDGELING.get(), LootTable.lootTable());
        // 1.12.2 EntityCreeperBoss#dropFewItems: the same rare bow
        add(GCEntities.CREEPER_BOSS.get(), LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.BOW))
                .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(this.registries, 0.025F, 0.005F))));
    }

    private LootPool.Builder common(Item item, int min, int max) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries, UniformGenerator.between(0.0F, 1.0F))));
    }

    /** 1.12.2: wasRecentlyHit && rand < 0.025 + looting * 0.02, then one entry. */
    @SafeVarargs
    private LootPool.Builder rare(LootPoolEntryContainer.Builder<?>... entries) {
        final LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(this.registries, 0.025F, 0.02F));
        for (LootPoolEntryContainer.Builder<?> entry : entries) {
            pool.add(entry);
        }
        return pool;
    }

    private static LootPoolEntryContainer.Builder<?> weighted(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolEntryContainer.Builder<?> nothing(int weight) {
        return EmptyLootItem.emptyItem().setWeight(weight);
    }

    /** 1.12.2 dropped a Medium Oxygen Tank holding a random 0-50% of its 1,800; here always 900. */
    private static LootPoolEntryContainer.Builder<?> halfTank(int weight) {
        return LootItem.lootTableItem(GCItems.MEDIUM_OXYGEN_TANK.get()).setWeight(weight)
                .apply(SetComponentsFunction.setComponent(GCDataComponents.OXYGEN.get(), 900));
    }
}
