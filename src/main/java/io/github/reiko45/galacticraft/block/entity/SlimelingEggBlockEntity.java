/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntitySlimelingEgg). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.SlimelingEggBlock;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A cracked Slimeling Egg counting down to hatching (1.12.2 TileEntitySlimelingEgg). */
public class SlimelingEggBlockEntity extends BlockEntity {
    private int timeToHatch = -1;
    @Nullable
    private UUID owner;
    private String ownerName = "";

    public SlimelingEggBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.SLIMELING_EGG.get(), pos, state);
    }

    public void startHatching(int ticks, Player player) {
        this.timeToHatch = ticks;
        this.owner = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public int timeToHatch() {
        return this.timeToHatch;
    }

    /** 1.12.2 update: the slimeling hatches a block up, tame, with 20 health, and the egg is gone. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SlimelingEggBlockEntity egg) {
        if (egg.timeToHatch > 0) {
            egg.timeToHatch--;
            egg.setChanged();
            return;
        }
        if (egg.timeToHatch != 0 || egg.owner == null || !(state.getBlock() instanceof SlimelingEggBlock block)) {
            return;
        }
        final SlimelingEggBlock.Color color = block.color();
        final SlimelingEntity slimeling = SlimelingEntity.hatch(level, color.red, color.green, color.blue);
        if (slimeling != null) {
            slimeling.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            slimeling.setOwnerUUID(egg.owner);
            slimeling.setTame(true, true);
            slimeling.setOwnerName(egg.ownerName);
            slimeling.setHealth(20.0F);
            level.addFreshEntity(slimeling);
        }
        level.removeBlock(pos, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TimeToHatch", this.timeToHatch);
        if (this.owner != null) {
            tag.putUUID("OwnerUUID", this.owner);
        }
        tag.putString("OwnerUsername", this.ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.timeToHatch = tag.contains("TimeToHatch") ? tag.getInt("TimeToHatch") : -1;
        this.owner = tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null;
        this.ownerName = tag.getString("OwnerUsername");
    }
}
