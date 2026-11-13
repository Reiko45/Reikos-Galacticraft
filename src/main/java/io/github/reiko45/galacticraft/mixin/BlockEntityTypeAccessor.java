package io.github.reiko45.galacticraft.mixin;

import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the sealable BuildCraft pipes join BuildCraft Reborn's pipe holder block entity type (see
 * compat.buildcraft.BuildCraftCompat#addPipeHolders): NeoForge's BlockEntityTypeAddBlocksEvent only takes blocks of
 * the type's own block class.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {
    @Accessor("validBlocks")
    @Mutable
    void galacticraftcore$setValidBlocks(Set<Block> blocks);
}
