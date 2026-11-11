package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.entity.DungeonBossEntity;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 BlockBossSpawner / BlockBossSpawnerMars: invisible and unbreakable, it holds the boss room's
 * {@link DungeonSpawnerBlockEntity}, which summons this block's boss.
 */
public class BossSpawnerBlock extends BaseEntityBlock {
    /**
     * Which boss a spawner summons, with the sound it makes doing so and the monsters it keeps out of the fight (1.12.2
     * TileEntityDungeonSpawner&lt;EntitySkeletonBoss&gt; and TileEntityDungeonSpawnerMars).
     */
    public enum Boss implements StringRepresentable {
        SKELETON("skeleton", GCEntities.SKELETON_BOSS, GCSounds.BOSS_LAUGH, 10.0F, 1.0F, () -> Set.of(
                GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.EVOLVED_SKELETON.get(), GCEntities.EVOLVED_SPIDER.get(),
                GCEntities.EVOLVED_CREEPER.get(), GCEntities.EVOLVED_ENDERMAN.get(),
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER, EntityType.ENDERMAN)),
        CREEPER("creeper", GCEntities.CREEPER_BOSS, GCSounds.SCARY_SCAPE, 9.0F, 1.4F, () -> Set.of(
                GCEntities.EVOLVED_SKELETON.get(), GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.EVOLVED_SPIDER.get()));

        public static final Codec<Boss> CODEC = StringRepresentable.fromEnum(Boss::values);

        private final String name;
        private final Supplier<? extends EntityType<? extends DungeonBossEntity>> type;
        private final Supplier<SoundEvent> sound;
        private final float volume;
        private final float pitch;
        private final Supplier<Set<EntityType<?>>> disabled;

        Boss(String name, Supplier<? extends EntityType<? extends DungeonBossEntity>> type, Supplier<SoundEvent> sound, float volume, float pitch,
             Supplier<Set<EntityType<?>>> disabled) {
            this.name = name;
            this.type = type;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.disabled = disabled;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public EntityType<? extends DungeonBossEntity> type() {
            return this.type.get();
        }

        public SoundEvent sound() {
            return this.sound.get();
        }

        public float volume() {
            return this.volume;
        }

        public float pitch() {
            return this.pitch;
        }

        /** 1.12.2 getDisabledCreatures: removed from the room while the boss is there. */
        public Set<EntityType<?>> disabled() {
            return this.disabled.get();
        }
    }

    public static final MapCodec<BossSpawnerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Boss.CODEC.fieldOf("boss").forGetter(BossSpawnerBlock::boss), propertiesCodec()).apply(instance, BossSpawnerBlock::new));

    private final Boss boss;

    public BossSpawnerBlock(Boss boss, Properties properties) {
        super(properties);
        this.boss = boss;
    }

    public Boss boss() {
        return this.boss;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.DUNGEON_SPAWNER.get(), DungeonSpawnerBlockEntity::serverTick);
    }
}
