package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The 1.12.2 settings that changed recipes, ore generation and drops, read where 1.21 data asks: a recipe condition
 * (quick mode's wire and battery counts), a placement filter (the ore toggles) and a loot condition (quick mode's extra
 * silicon). All three are {@code {"type": "galacticraftcore:config", "option": "<name>", "value": true}}: they hold when
 * the common setting of that name has that value ({@code value} defaults to true). Common settings are read at start-up,
 * so they are in place before any world loads its recipes or generates a chunk.
 */
public final class ConfigChecks {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, GalacticraftCore.MOD_ID);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, GalacticraftCore.MOD_ID);
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, GalacticraftCore.MOD_ID);

    public static final Supplier<MapCodec<RecipeCondition>> RECIPE_CONDITION = CONDITIONS.register("config", () -> RecipeCondition.CODEC);
    public static final Supplier<PlacementModifierType<Filter>> FILTER = PLACEMENT_MODIFIERS.register("config", () -> () -> Filter.CODEC);
    public static final Supplier<LootItemConditionType> LOOT_CONDITION = LOOT_CONDITIONS.register("config",
            () -> new LootItemConditionType(LootCondition.CODEC));

    private ConfigChecks() {
    }

    /** Whether the common setting {@code option} is {@code value}; a name that isn't a setting never matches. */
    public static boolean holds(String option, boolean value) {
        return GCConfig.commonFlag(option).map(set -> set == value).orElse(false);
    }

    private static <T> MapCodec<T> settingCodec(java.util.function.BiFunction<String, Boolean, T> make,
                                         java.util.function.Function<T, String> option, java.util.function.Function<T, Boolean> value) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.validate(name -> GCConfig.commonFlagNames().contains(name)
                        ? com.mojang.serialization.DataResult.success(name)
                        : com.mojang.serialization.DataResult.error(() -> "no Galacticraft on/off setting called " + name)).fieldOf("option").forGetter(option),
                Codec.BOOL.optionalFieldOf("value", true).forGetter(value)
        ).apply(i, make::apply));
    }

    public record RecipeCondition(String option, boolean value) implements ICondition {
        public static final MapCodec<RecipeCondition> CODEC = settingCodec(RecipeCondition::new, RecipeCondition::option, RecipeCondition::value);

        @Override
        public boolean test(IContext context) {
            return holds(this.option, this.value);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class Filter extends PlacementFilter {
        public static final MapCodec<Filter> CODEC = settingCodec(Filter::new, f -> f.option, f -> f.value);
        private final String option;
        private final boolean value;

        public Filter(String option, boolean value) {
            this.option = option;
            this.value = value;
        }

        /** Places only while the on/off setting {@code option} is {@code value}. */
        public static Filter when(String option, boolean value) {
            return new Filter(option, value);
        }

        @Override
        protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
            return holds(this.option, this.value);
        }

        @Override
        public PlacementModifierType<?> type() {
            return FILTER.get();
        }
    }

    public record LootCondition(String option, boolean value) implements LootItemCondition {
        public static final MapCodec<LootCondition> CODEC = settingCodec(LootCondition::new, LootCondition::option, LootCondition::value);

        @Override
        public LootItemConditionType getType() {
            return LOOT_CONDITION.get();
        }

        @Override
        public boolean test(LootContext context) {
            return holds(this.option, this.value);
        }
    }
}
