/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.blocks.BlockMachineMars, type
 * launch_controller). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The Launch Controller block: whoever places it owns it (1.12.2 onBlockPlacedBy set the owner's name). */
public class LaunchControllerBlock extends MachineBlock {
    public LaunchControllerBlock(Properties properties) {
        super(properties, LaunchControllerBlockEntity::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller) {
            controller.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller) {
            serverPlayer.openMenu(controller, buf -> {
                buf.writeBlockPos(pos);
                buf.writeUtf(controller.ownerName());
                buf.writeBoolean(controller.isOwner(serverPlayer));
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
