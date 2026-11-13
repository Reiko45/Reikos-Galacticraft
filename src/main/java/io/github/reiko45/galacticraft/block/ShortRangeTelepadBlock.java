/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockShortRangeTelepad).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TelepadFakeBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Short Range Telepad (1.12.2 BlockShortRangeTelepad): a 3 x 3 pad with a 3 x 3 canopy two blocks above it, all placed
 * at once (it needs that room). Power goes into the top of the canopy's middle. Breaking any part breaks it all.
 */
public class ShortRangeTelepadBlock extends BaseEntityBlock {
    public static final MapCodec<ShortRangeTelepadBlock> CODEC = simpleCodec(ShortRangeTelepadBlock::new);
    private static final VoxelShape SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 7.2D, 16.0D);

    public ShortRangeTelepadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** 1.12.2 getPositions: the 8 blocks around it and the 9 two above. */
    public static List<BlockPos> partPositions(BlockPos main) {
        final List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < 3; y += 2) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        positions.add(main.offset(x, y, z));
                    }
                }
            }
        }
        return positions;
    }

    public static boolean hasRoom(LevelReader level, BlockPos main) {
        if (main.getY() + 2 >= level.getMaxBuildHeight()) {
            return false;
        }
        for (BlockPos pos : partPositions(main)) {
            if (!level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!hasRoom(context.getLevel(), context.getClickedPos())) {
            if (context.getLevel().isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("gui.galacticraftcore.warning.no_room").withStyle(ChatFormatting.RED), false);
            }
            return null;
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        buildParts(level, pos);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ShortRangeTelepadBlockEntity telepad) {
            telepad.setOwner(player);
        }
    }

    /** 1.12.2 onCreate: the fake blocks, the top ones marked as such. */
    public static void buildParts(Level level, BlockPos main) {
        for (BlockPos part : partPositions(main)) {
            level.setBlock(part, GCBlocks.TELEPAD_FAKE.get().defaultBlockState().setValue(TelepadFakeBlock.TOP, part.getY() == main.getY() + 2), 3);
            if (level.getBlockEntity(part) instanceof TelepadFakeBlockEntity fake) {
                fake.setMainPos(main);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ShortRangeTelepadBlockEntity telepad) {
                telepad.unregister();
                telepad.dropContents(level, pos);
            }
            for (BlockPos part : partPositions(pos)) {
                if (level.getBlockState(part).is(GCBlocks.TELEPAD_FAKE.get())) {
                    level.destroyBlock(part, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ShortRangeTelepadBlockEntity telepad) {
            telepad.openMenu(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShortRangeTelepadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof MachineBlockEntity machine) {
                MachineBlockEntity.serverTick(lvl, pos, st, machine);
            }
        };
    }

    /**
     * 1.12.2 randomDisplayTick: blue motes rising off the pad (coloured as for receiving) and falling from the canopy's
     * edges (coloured as for sending).
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                level.addParticle(GCParticles.TELEPAD_UP.get(), pos.getX() + 0.2D + random.nextDouble() * 0.6D, pos.getY() + 0.1D,
                        pos.getZ() + 0.2D + random.nextDouble() * 0.6D, 0.0D, 1.4D, 0.0D);
            }
            level.addParticle(GCParticles.TELEPAD_DOWN.get(), pos.getX() + random.nextDouble() * 0.2D, pos.getY() + 2.9D, pos.getZ() + random.nextDouble(),
                    0.0D, -2.95D, 0.0D);
            level.addParticle(GCParticles.TELEPAD_DOWN.get(), pos.getX() + 0.8D + random.nextDouble() * 0.2D, pos.getY() + 2.9D,
                    pos.getZ() + random.nextDouble(), 0.0D, -2.95D, 0.0D);
            level.addParticle(GCParticles.TELEPAD_DOWN.get(), pos.getX() + random.nextDouble(), pos.getY() + 2.9D, pos.getZ() + 0.2D + random.nextDouble() * 0.2D,
                    0.0D, -2.95D, 0.0D);
            level.addParticle(GCParticles.TELEPAD_DOWN.get(), pos.getX() + random.nextDouble(), pos.getY() + 2.9D, pos.getZ() + 0.8D + random.nextDouble() * 0.2D,
                    0.0D, -2.95D, 0.0D);
        }
    }
}
