/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockIceAsteroids). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Dense Ice (1.12.2 BlockIceAsteroids): slippery ice that drops only with Silk Touch. Broken or melted (by block light
 * over 13) it leaves water, except on Galacticraft worlds and in the Nether, where it just goes.
 */
public class DenseIceBlock extends IceBlock {
    public DenseIceBlock(Properties properties) {
        super(properties);
    }

    private static boolean vanishes(Level level) {
        return level.dimensionType().ultraWarm() || CelestialPhysics.of(level.dimension()) != null;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_ICE_MELTING) && vanishes(level)) {
            // like vanilla ice, minus the water
            player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            level.removeBlock(pos, false);
            return;
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 13 - state.getLightBlock(level, pos)) {
            melt(state, level, pos);
        }
    }

    @Override
    protected void melt(BlockState state, Level level, BlockPos pos) {
        if (vanishes(level)) {
            level.removeBlock(pos, false);
        } else {
            super.melt(state, level, pos);
        }
    }
}
