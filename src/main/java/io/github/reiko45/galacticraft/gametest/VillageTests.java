package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.data.GCWorldgenProvider;
import io.github.reiko45.galacticraft.entity.AlienVillagerEntity;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCModTags;
import io.github.reiko45.galacticraft.world.village.MoonVillage;
import io.github.reiko45.galacticraft.world.village.MoonVillagePiece;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 5c: Moon villages and the Alien Villagers. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageTests {
    private static final String AIR = "air_room";

    private VillageTests() {
    }

    private static List<MoonVillagePiece> layout(long seed) {
        final StructurePiecesBuilder builder = new StructurePiecesBuilder();
        MoonVillage.generate(builder, RandomSource.create(seed), 2, 2);
        final List<MoonVillagePiece> pieces = new ArrayList<>();
        for (StructurePiece piece : builder.build().pieces()) {
            pieces.add((MoonVillagePiece) piece);
        }
        return pieces;
    }

    /**
     * StructureVillageStartMoon: a well, plank paths off it, and along them houses, fields and lamp posts that never
     * overlap, within 112 (plus a building) of the well; at most (int) (limit / 1.5) of each kind; the same for the same seed.
     */
    @GameTest(template = "empty")
    public static void villageLayoutFollows112(GameTestHelper helper) {
        int paths = 0;
        int buildings = 0;
        for (long seed = 1; seed <= 20; seed++) {
            final List<MoonVillagePiece> pieces = layout(seed);
            helper.assertTrue(pieces.get(0).kind() == MoonVillagePiece.Kind.WELL, "the well comes first");
            final int[] counts = new int[MoonVillagePiece.Kind.values().length];
            for (int i = 0; i < pieces.size(); i++) {
                final MoonVillagePiece piece = pieces.get(i);
                counts[piece.kind().ordinal()]++;
                final BoundingBox box = piece.getBoundingBox();
                helper.assertTrue(Math.abs(box.minX() - 2) <= 112 + 17 && Math.abs(box.minZ() - 2) <= 112 + 17, "near the well: " + box);
                for (int j = i + 1; j < pieces.size(); j++) {
                    helper.assertFalse(box.intersects(pieces.get(j).getBoundingBox()), "pieces " + i + " and " + j + " overlap (seed " + seed + ")");
                }
            }
            helper.assertTrue(counts[MoonVillagePiece.Kind.HOUSE.ordinal()] <= 2 && counts[MoonVillagePiece.Kind.WOOD_HUT.ordinal()] <= 3
                    && counts[MoonVillagePiece.Kind.FIELD.ordinal()] <= 3 && counts[MoonVillagePiece.Kind.CROP_FIELD.ordinal()] <= 3,
                    "within the limits (seed " + seed + ")");
            paths += counts[MoonVillagePiece.Kind.PATH.ordinal()];
            buildings += pieces.size() - 1 - counts[MoonVillagePiece.Kind.PATH.ordinal()];
        }
        helper.assertTrue(paths > 20 && buildings > 20, "villages have paths (" + paths + ") and buildings (" + buildings + ")");
        final List<MoonVillagePiece> once = layout(42L);
        final List<MoonVillagePiece> again = layout(42L);
        helper.assertValueEqual(once.size(), again.size(), "same seed, same village");
        for (int i = 0; i < once.size(); i++) {
            helper.assertTrue(once.get(i).getBoundingBox().equals(again.get(i).getBoundingBox()) && once.get(i).kind() == again.get(i).kind(), "piece " + i);
        }
        helper.succeed();
    }

    /** One village per 32x32 chunks, at least 8 apart, on the Moon only. */
    @GameTest(template = "empty")
    public static void villagesAreSpreadOut(GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE).get(GCWorldgenProvider.MOON_VILLAGE) instanceof MoonVillage, "registered");
        final StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(GCWorldgenProvider.MOON_VILLAGES);
        helper.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement spread && spread.spacing() == 32
                && spread.separation() == 8, "32 / 8 random spread");
        helper.succeed();
    }

    /**
     * A house built on flat ground: tin decoration floor and walls, a glass skylight, glowstone torches, and four Alien
     * Villagers inside (built far from the other tests: it is 17 blocks wide and its walls go 8 blocks down).
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void houseIsBuiltWithVillagers(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos corner = helper.absolutePos(BlockPos.ZERO).offset(3000, 0, 3000).atY(100);
        for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(16, 0, 16))) {
            level.setBlockAndUpdate(pos, GCBlocks.MOON_TURF.get().defaultBlockState());
        }
        final MoonVillagePiece house = new MoonVillagePiece(MoonVillagePiece.Kind.HOUSE, 1,
                BoundingBox.orientBox(corner.getX(), 64, corner.getZ(), 0, 0, 0, 17, 9, 17, Direction.SOUTH), Direction.SOUTH, null);
        final BoundingBox everywhere = new BoundingBox(corner.getX() - 4, level.getMinBuildHeight(), corner.getZ() - 4,
                corner.getX() + 20, level.getMaxBuildHeight(), corner.getZ() + 20);
        house.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(5L), everywhere,
                new ChunkPos(corner), corner);
        final BoundingBox box = house.getBoundingBox();
        helper.assertValueEqual(box.minY(), 101, "settled on the ground (floor at the old surface + 1)");
        helper.assertTrue(level.getBlockState(new BlockPos(box.minX() + 8, box.minY(), box.minZ() + 8)).is(GCBlocks.TIN_DECORATION_BLOCK_1.get()), "floor");
        helper.assertTrue(level.getBlockState(new BlockPos(box.minX() + 1, box.minY() - 8, box.minZ() + 6)).is(GCBlocks.TIN_DECORATION_BLOCK_2.get()),
                "walls 8 blocks into the ground");
        helper.assertTrue(level.getBlockState(new BlockPos(box.minX() + 8, box.minY() + 9, box.minZ() + 8)).is(Blocks.GLASS), "skylight");
        helper.assertTrue(level.getBlockState(new BlockPos(box.minX() + 8, box.minY() + 4, box.minZ() + 2)).is(GCBlocks.GLOWSTONE_WALL_TORCH.get()), "torch");
        helper.assertTrue(level.getBlockState(new BlockPos(box.minX() + 1, box.minY() + 3, box.minZ() + 8)).isAir(), "doorway");
        final List<AlienVillagerEntity> villagers = level.getEntitiesOfClass(AlienVillagerEntity.class, AABB.of(box).inflate(2.0D));
        helper.assertValueEqual(villagers.size(), 4, "four villagers");
        house.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(5L), everywhere,
                new ChunkPos(corner), corner);
        helper.assertValueEqual(level.getEntitiesOfClass(AlienVillagerEntity.class, AABB.of(box).inflate(2.0D)).size(), 4, "no more on a second build");
        villagers.forEach(villager -> villager.discard());
        helper.succeed();
    }

    /** EntityAlienVillager's trade list, priced in Lunar Sapphires, 7 uses each. */
    @GameTest(template = "empty")
    public static void alienVillagerTrades(GameTestHelper helper) {
        final MerchantOffers offers = AlienVillagerEntity.tradeList(RandomSource.create(7L));
        helper.assertValueEqual(offers.size(), 13, "13 trades");
        for (MerchantOffer offer : offers) {
            helper.assertValueEqual(offer.getMaxUses(), AlienVillagerEntity.MAX_USES, "7 uses");
        }
        final MerchantOffer mask = offers.get(0);
        helper.assertTrue(mask.getResult().is(GCItems.OXYGEN_MASK.get()) && mask.getItemCostA().item().value() == GCItems.LUNAR_SAPPHIRE.get()
                && mask.getItemCostA().count() >= 1 && mask.getItemCostA().count() <= 2, "a mask for 1-2 sapphires");
        helper.assertValueEqual(OxygenTankItem.oxygen(offers.get(1).getResult()), 665, "a tank with 665 oxygen");
        helper.assertValueEqual(LiquidCanisterItem.contents(offers.get(3).getResult()).getAmount(), 684, "684 mB of fuel");
        final ItemStack battery = offers.get(5).getResult();
        helper.assertTrue(Math.abs(((ElectricItem) battery.getItem()).stored(battery) / ((ElectricItem) battery.getItem()).capacity() - 0.42F) < 0.001F,
                "a battery at 42%");
        final MerchantOffer carrots = offers.get(6);
        helper.assertTrue(carrots.getItemCostA().item().value() == GCItems.OIL_CANISTER.get() && carrots.getItemCostB().isPresent()
                && carrots.getResult().is(GCItems.DEHYDRATED_CARROT.get()), "canned carrots for a canister and a sapphire");
        helper.assertTrue(offers.get(8).getItemCostA().item().value() == GCItems.BUGGY_SCHEMATIC.get()
                && offers.get(8).getResult().is(GCItems.TIER_2_ROCKET_SCHEMATIC.get()), "schematic swap");
        final MerchantOffer saplings = offers.get(12);
        helper.assertTrue(saplings.getItemCostA().item().value() == Items.JUNGLE_SAPLING && saplings.getItemCostA().count() >= 11
                && saplings.getItemCostA().count() <= 39 && saplings.getResult().is(GCItems.LUNAR_SAPPHIRE.get()), "a sapphire for jungle saplings");
        helper.assertTrue(GCEntities.ALIEN_VILLAGER.get().is(GCModTags.BREATHES_IN_SPACE), "breathes on the Moon");
        helper.succeed();
    }

    /** Talking to them takes a Frequency Module in the Galacticraft inventory. */
    @GameTest(template = AIR)
    public static void tradingNeedsFrequencyModule(GameTestHelper helper) {
        final AlienVillagerEntity villager = helper.spawn(GCEntities.ALIEN_VILLAGER.get(), new BlockPos(6, 1, 6));
        @SuppressWarnings("removal")
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(villager.getTradingPlayer() == null, "no module, no trade");
        GCPlayerData.get(player).setGear(GearSlot.FREQUENCY_MODULE, new ItemStack(GCItems.FREQUENCY_MODULE.get()));
        villager.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(villager.getTradingPlayer() == player, "trading");
        villager.setTradingPlayer(null);
        player.closeContainer();
        helper.succeed();
    }

    /** useRecipe + updateAITasks: a first sale schedules a restock; 40 ticks later sold-out trades get more uses and it regenerates. */
    @GameTest(template = AIR, timeoutTicks = 120)
    public static void alienVillagerRestocks(GameTestHelper helper) {
        final AlienVillagerEntity villager = helper.spawn(GCEntities.ALIEN_VILLAGER.get(), new BlockPos(6, 1, 6));
        final MerchantOffer offer = villager.getOffers().get(0);
        for (int i = 0; i < AlienVillagerEntity.MAX_USES; i++) {
            villager.notifyTrade(offer);
        }
        helper.assertTrue(villager.getOffers().get(0).isOutOfStock(), "sold out");
        helper.runAfterDelay(50, () -> {
            final MerchantOffer restocked = villager.getOffers().get(0);
            helper.assertTrue(!restocked.isOutOfStock() && restocked.getMaxUses() >= AlienVillagerEntity.MAX_USES + 2
                    && restocked.getMaxUses() <= AlienVillagerEntity.MAX_USES + 12, "2-12 more uses: " + restocked.getMaxUses());
            helper.assertTrue(villager.hasEffect(MobEffects.REGENERATION), "regenerating");
            helper.succeed();
        });
    }
}
