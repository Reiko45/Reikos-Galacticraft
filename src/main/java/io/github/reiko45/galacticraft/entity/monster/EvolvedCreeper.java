package io.github.reiko45.galacticraft.entity.monster;

import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * Evolved Creeper (1.12.2 EntityEvolvedCreeper): 25 health, breathes in space, otherwise a creeper. It can be a baby
 * (hatched from a Creeper Egg): half size, half again as fast, and worth 2.5 times the experience.
 */
public class EvolvedCreeper extends Creeper {
    private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(EvolvedCreeper.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation BABY_SPEED_ID = GalacticraftCore.id("baby_speed_boost");
    private static final AttributeModifier BABY_SPEED = new AttributeModifier(BABY_SPEED_ID, 0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    public EvolvedCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes().add(Attributes.MAX_HEALTH, 25.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BABY, false);
    }

    @Override
    public boolean isBaby() {
        return this.entityData.get(BABY);
    }

    @Override
    public void setBaby(boolean baby) {
        this.entityData.set(BABY, baby);
        if (!level().isClientSide) {
            final AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(BABY_SPEED_ID);
                if (baby) {
                    speed.addTransientModifier(BABY_SPEED);
                }
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (BABY.equals(key)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected int getBaseExperienceReward() {
        if (isBaby()) {
            this.xpReward = (int) (this.xpReward * 2.5D);
        }
        return super.getBaseExperienceReward();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (isBaby()) {
            tag.putBoolean("IsBaby", true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("IsBaby")) {
            setBaby(true);
        }
    }
}
