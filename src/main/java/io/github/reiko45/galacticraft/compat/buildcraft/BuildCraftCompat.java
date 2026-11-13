package io.github.reiko45.galacticraft.compat.buildcraft;

import io.github.reiko45.galacticraft.mixin.BlockEntityTypeAccessor;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import org.jetbrains.annotations.Nullable;

/**
 * BuildCraft Reborn support (1.12.2's BuildCraft compatibility: CompatibilityManager, EnergyConfigHandler's MJ, the
 * universal wrench, BlockEnclosed's BuildCraft pipes). The classes that use BuildCraft's API are only loaded when it is
 * installed.
 */
public final class BuildCraftCompat {
    public static final String MOD_ID = "buildcraftreborn";
    /** BuildCraft Reborn's pipe block and its block entity type, both by this name. */
    public static final ResourceLocation PIPE_HOLDER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "pipe_holder");

    private BuildCraftCompat() {
    }

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    public static void init(IEventBus modBus) {
        if (loaded()) {
            BuildCraftMj.init(modBus);
            modBus.addListener(BuildCraftCompat::addPipeHolders);
        }
    }

    /** BuildCraft Reborn's pipe block, or null without it. */
    @Nullable
    public static Block pipeHolderBlock() {
        return loaded() ? BuiltInRegistries.BLOCK.getOptional(PIPE_HOLDER).orElse(null) : null;
    }

    /** BuildCraft Reborn's pipe holder block entity type, or null without it. */
    @Nullable
    public static BlockEntityType<?> pipeHolderType() {
        return loaded() ? BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(PIPE_HOLDER).orElse(null) : null;
    }

    /** Is there a BuildCraft pipe at this place (a pipe holder holding a pipe)? */
    public static boolean hasPipe(Level level, BlockPos pos) {
        return loaded() && BuildCraftPipes.hasPipe(level, pos);
    }

    /**
     * The sealable BuildCraft pipes hold BuildCraft's pipe holder block entity, so they join its type before the
     * capabilities are given out (then BuildCraft's own pipe, item, fluid and MJ capabilities reach them). NeoForge's
     * {@link BlockEntityTypeAddBlocksEvent#modify} only takes blocks of BuildCraft's pipe block class; nothing of
     * BuildCraft's pipe holder or its renderer looks at the block it is in, so they are added beside it directly.
     */
    private static void addPipeHolders(BlockEntityTypeAddBlocksEvent event) {
        final BlockEntityType<?> type = pipeHolderType();
        if (type == null) {
            return;
        }
        final Set<Block> blocks = new HashSet<>(type.getValidBlocks());
        GCBlocks.SEALABLE_BUILDCRAFT_PIPES.forEach(block -> blocks.add(block.get()));
        ((BlockEntityTypeAccessor) type).galacticraftcore$setValidBlocks(blocks);
    }
}
