package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.CheeseBlock;
import io.github.reiko45.galacticraft.block.GlowstoneTorchBlock;
import io.github.reiko45.galacticraft.block.GlowstoneWallTorchBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry. 1.12.2 packed most of these into metadata variants of a few blocks
 * ({@code basic_block_core}, {@code basic_block_moon}, {@code slab_gc_half}, {@code wall_gc});
 * since the 1.13 flattening every variant is its own block. Hardness/resistance values are taken
 * from the 1.12.2 overrides ({@code BlockBasic#getBlockHardness}, {@code BlockBasicMoon#getExplosionResistance}).
 */
public final class GCBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GalacticraftCore.MOD_ID);

    private static final List<DeferredBlock<?>> BLOCKS_WITH_ITEMS = new ArrayList<>();

    // --- Overworld ores and storage blocks (copper uses the vanilla blocks since 1.17) ---
    public static final DeferredBlock<Block> TIN_ORE = withItem("tin_ore", () -> new Block(ore(MapColor.STONE, 5.0F, 3.0F)));
    public static final DeferredBlock<Block> ALUMINUM_ORE = withItem("aluminum_ore", () -> new Block(ore(MapColor.STONE, 6.0F, 3.0F)));
    public static final DeferredBlock<Block> SILICON_ORE = withItem("silicon_ore", () -> new Block(ore(MapColor.STONE, 3.0F, 3.0F)));

    public static final DeferredBlock<Block> TIN_BLOCK = withItem("tin_block", () -> new Block(metal(MapColor.METAL, 1.0F, 6.0F)));
    public static final DeferredBlock<Block> ALUMINUM_BLOCK = withItem("aluminum_block", () -> new Block(metal(MapColor.METAL, 1.0F, 6.0F)));
    public static final DeferredBlock<Block> METEORIC_IRON_BLOCK = withItem("meteoric_iron_block", () -> new Block(metal(MapColor.COLOR_GRAY, 1.0F, 8.0F)));
    public static final DeferredBlock<Block> SILICON_BLOCK = withItem("silicon_block", () -> new Block(metal(MapColor.COLOR_BLACK, 1.0F, 6.0F)));

    // --- Tin decoration family ("deco_block_0" / "deco_block_1") ---
    public static final DeferredBlock<Block> TIN_DECORATION_BLOCK_1 = withItem("tin_decoration_block_1", () -> new Block(decoration()));
    public static final DeferredBlock<Block> TIN_DECORATION_BLOCK_2 = withItem("tin_decoration_block_2", () -> new Block(decoration()));
    public static final DeferredBlock<StairBlock> TIN_DECORATION_STAIRS_1 = stairs("tin_decoration_stairs_1", TIN_DECORATION_BLOCK_1);
    public static final DeferredBlock<StairBlock> TIN_DECORATION_STAIRS_2 = stairs("tin_decoration_stairs_2", TIN_DECORATION_BLOCK_2);
    public static final DeferredBlock<SlabBlock> TIN_DECORATION_SLAB_1 = slab("tin_decoration_slab_1", TIN_DECORATION_BLOCK_1);
    public static final DeferredBlock<SlabBlock> TIN_DECORATION_SLAB_2 = slab("tin_decoration_slab_2", TIN_DECORATION_BLOCK_2);
    public static final DeferredBlock<WallBlock> TIN_DECORATION_WALL_1 = wall("tin_decoration_wall_1", TIN_DECORATION_BLOCK_1);
    public static final DeferredBlock<WallBlock> TIN_DECORATION_WALL_2 = wall("tin_decoration_wall_2", TIN_DECORATION_BLOCK_2);

    // --- Moon ---
    public static final DeferredBlock<Block> MOON_TURF = withItem("moon_turf", () -> new Block(moonSoil(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<Block> MOON_DIRT = withItem("moon_dirt", () -> new Block(moonSoil(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> MOON_ROCK = withItem("moon_rock", () -> new Block(moonStone(1.5F, 6.0F)));
    public static final DeferredBlock<Block> MOON_DUNGEON_BRICK = withItem("moon_dungeon_brick", () -> new Block(moonStone(4.0F, 40.0F)));
    public static final DeferredBlock<Block> MOON_COPPER_ORE = withItem("moon_copper_ore", () -> new Block(moonStone(5.0F, 3.0F)));
    public static final DeferredBlock<Block> MOON_TIN_ORE = withItem("moon_tin_ore", () -> new Block(moonStone(5.0F, 3.0F)));
    public static final DeferredBlock<Block> MOON_CHEESE_ORE = withItem("moon_cheese_ore", () -> new Block(moonStone(3.0F, 3.0F)));
    public static final DeferredBlock<Block> MOON_SAPPHIRE_ORE = withItem("moon_sapphire_ore", () -> new Block(moonStone(5.0F, 3.0F)));

    public static final DeferredBlock<StairBlock> MOON_ROCK_STAIRS = stairs("moon_rock_stairs", MOON_ROCK);
    public static final DeferredBlock<StairBlock> MOON_DUNGEON_BRICK_STAIRS = stairs("moon_dungeon_brick_stairs", MOON_DUNGEON_BRICK);
    public static final DeferredBlock<SlabBlock> MOON_ROCK_SLAB = slab("moon_rock_slab", MOON_ROCK);
    public static final DeferredBlock<SlabBlock> MOON_DUNGEON_BRICK_SLAB = slab("moon_dungeon_brick_slab", MOON_DUNGEON_BRICK);
    public static final DeferredBlock<WallBlock> MOON_ROCK_WALL = wall("moon_rock_wall", MOON_ROCK);
    public static final DeferredBlock<WallBlock> MOON_DUNGEON_BRICK_WALL = wall("moon_dungeon_brick_wall", MOON_DUNGEON_BRICK);

    // --- Misc ---
    public static final DeferredBlock<CheeseBlock> CHEESE_BLOCK = withItem("cheese_block", () -> new CheeseBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5F).sound(SoundType.WOOL)
                    .forceSolidOn().pushReaction(PushReaction.DESTROY)));

    /** Hardness 40 like 1.12.2; drops raw meteoric iron. The hot/cooling tile-entity behaviour is a later phase. */
    public static final DeferredBlock<Block> FALLEN_METEOR = withItem("fallen_meteor", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(40.0F, 40.0F).sound(SoundType.STONE).noOcclusion()));

    public static final DeferredBlock<GlowstoneTorchBlock> GLOWSTONE_TORCH = BLOCKS.register("glowstone_torch",
            () -> new GlowstoneTorchBlock(torch()));
    public static final DeferredBlock<GlowstoneWallTorchBlock> GLOWSTONE_WALL_TORCH = BLOCKS.register("glowstone_wall_torch",
            () -> new GlowstoneWallTorchBlock(torch().lootFrom(GLOWSTONE_TORCH)));

    static {
        // Torches need a combined standing/wall item instead of a plain BlockItem.
        GCItems.ITEMS.register("glowstone_torch",
                () -> new StandingAndWallBlockItem(GLOWSTONE_TORCH.get(), GLOWSTONE_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));
    }

    private GCBlocks() {
    }

    /** Blocks that have a plain {@link net.minecraft.world.item.BlockItem}, in registration order. */
    public static List<DeferredBlock<?>> blocksWithItems() {
        return Collections.unmodifiableList(BLOCKS_WITH_ITEMS);
    }

    private static <B extends Block> DeferredBlock<B> withItem(String name, Supplier<B> factory) {
        final DeferredBlock<B> block = BLOCKS.register(name, factory);
        GCItems.ITEMS.registerSimpleBlockItem(block);
        BLOCKS_WITH_ITEMS.add(block);
        return block;
    }

    private static DeferredBlock<StairBlock> stairs(String name, DeferredBlock<Block> base) {
        return withItem(name, () -> new StairBlock(base.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(base.get())));
    }

    private static DeferredBlock<SlabBlock> slab(String name, DeferredBlock<Block> base) {
        return withItem(name, () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base.get())));
    }

    private static DeferredBlock<WallBlock> wall(String name, DeferredBlock<Block> base) {
        return withItem(name, () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(base.get()).forceSolidOn()));
    }

    private static BlockBehaviour.Properties ore(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.METAL);
    }

    /** 1.12.2 decoration blocks were "soft, like cauldrons": hardness 1, resistance 2. */
    private static BlockBehaviour.Properties decoration() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.0F, 2.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties moonSoil(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.5F, 0.5F).sound(SoundType.GRAVEL);
    }

    private static BlockBehaviour.Properties moonStone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
    }

    /** setLightLevel(0.85F) in 1.12.2 == light level 12. */
    private static BlockBehaviour.Properties torch() {
        return BlockBehaviour.Properties.of().noCollission().instabreak().lightLevel(state -> 12)
                .sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY);
    }
}
