/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockEnclosed, the BuildCraft pipe types).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.compat.buildcraft.BuildCraftCompat;
import io.github.reiko45.galacticraft.item.ShiftDescribed;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/**
 * Sealable BuildCraft pipes (1.12.2 BlockEnclosed, BC_ITEM_STONEPIPE to BC_POWER_GOLDPIPE): a BuildCraft Reborn pipe
 * run through a full tin decoration block. As in 1.12.2 (createTileEntity made BuildCraft's pipe tile, and
 * onBlockPlacedBy gave it the pipe item), the block entity is BuildCraft's own pipe holder with that pipe in it, so it
 * joins the pipes, machines and inventories beside it as the bare pipe would, while keeping the air in; it takes no
 * plugs, gates, wires or paint. Everything BuildCraft does for it goes through its pipe block's ordinary Minecraft
 * methods, with no BuildCraft class named. Without BuildCraft Reborn it is a plain tin block, kept out of the creative
 * tab and recipe viewers.
 */
public class EnclosedBuildCraftPipeBlock extends Block implements EntityBlock, PartialSealable, ShiftDescribed {
    public static final MapCodec<EnclosedBuildCraftPipeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("pipe").forGetter(EnclosedBuildCraftPipeBlock::pipe), propertiesCodec())
            .apply(instance, EnclosedBuildCraftPipeBlock::new));

    private final ResourceLocation pipe;

    public EnclosedBuildCraftPipeBlock(ResourceLocation pipe, Properties properties) {
        super(properties);
        this.pipe = pipe;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** The BuildCraft pipe item inside (1.12.2 EnumEnclosedBlockType#getBCPipeType). */
    public ResourceLocation pipe() {
        return pipe;
    }

    public ItemStack pipeStack() {
        return BuiltInRegistries.ITEM.getOptional(pipe).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        final BlockEntityType<?> type = BuildCraftCompat.pipeHolderType();
        return type == null ? null : type.create(pos, state);
    }

    /** BuildCraft's pipe block's ticker, which runs the pipe. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        final Block holder = BuildCraftCompat.pipeHolderBlock();
        return holder instanceof EntityBlock entityBlock ? entityBlock.getTicker(level, holder.defaultBlockState(), type) : null;
    }

    /** 1.12.2 onBlockPlacedBy: the tile is given the pipe, as BuildCraft's pipe block does for the pipe's own item. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        givePipe(level, pos, state, placer);
    }

    private void givePipe(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer) {
        final Block holder = BuildCraftCompat.pipeHolderBlock();
        if (holder != null && level.getBlockEntity(pos) != null) {
            holder.setPlacedBy(level, pos, state, placer, pipeStack());
        }
    }

    /** Put down some other way (a command, a structure), it gets its pipe on the next tick unless it came with one. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (BuildCraftCompat.loaded() && level.getBlockEntity(pos) != null && !BuildCraftCompat.hasPipe(level, pos)) {
            givePipe(level, pos, state, null);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        final Block holder = BuildCraftCompat.pipeHolderBlock();
        if (holder != null) {
            holder.defaultBlockState().handleNeighborChanged(level, pos, block, fromPos, movedByPiston);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        final Block holder = BuildCraftCompat.pipeHolderBlock();
        if (holder != null && !state.is(newState.getBlock())) {
            holder.defaultBlockState().onRemove(level, pos, newState, movedByPiston);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Itself (1.12.2 dropped the sealable block), and what BuildCraft's pipe drops besides its own item: what it carried. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        final Block holder = BuildCraftCompat.pipeHolderBlock();
        if (holder != null && params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) != null) {
            final List<ItemStack> carried = new ArrayList<>(holder.defaultBlockState().getDrops(params));
            final ItemStack pipeItem = pipeStack();
            for (int i = 0; i < carried.size(); i++) {
                if (ItemStack.isSameItem(carried.get(i), pipeItem)) {
                    carried.remove(i);
                    break;
                }
            }
            drops.addAll(carried);
        }
        return drops;
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    /** 1.12.2 getShiftDescription: a text shared with others like it. */
    @Override
    public String shiftDescriptionKey(ItemStack stack) {
        return "block.galacticraftcore.enclosed.description";
    }
}
