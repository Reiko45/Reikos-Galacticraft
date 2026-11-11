package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.dungeon.Dungeon;
import io.github.reiko45.galacticraft.world.dungeon.DungeonPiece;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structure and structure piece types (1.12.2 MapGenDungeon / the dungeon StructureComponents). */
public final class GCStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, GalacticraftCore.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, GalacticraftCore.MOD_ID);

    /** The id dates from the Moon; Mars dungeons use the same type with their own config. */
    public static final Supplier<StructureType<Dungeon>> DUNGEON = STRUCTURE_TYPES.register("moon_dungeon", () -> () -> Dungeon.CODEC);
    public static final Supplier<StructurePieceType> DUNGEON_PIECE = STRUCTURE_PIECES.register("moon_dungeon_piece",
            () -> DungeonPiece::new);

    private GCStructures() {
    }
}
