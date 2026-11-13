/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.blocks.BlockSlimelingEgg). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.block.entity.SlimelingEggBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Slimeling Egg (1.12.2 BlockSlimelingEgg), found on the Martian surface in red, blue and yellow. Using it, or trying to
 * break it with anything but a pickaxe, cracks it: a little later (0-30 ticks, or 20-70 when used) a Slimeling hatches,
 * already tame and belonging to whoever cracked it. A pickaxe of diamond strength collects the egg instead, and a Desh
 * Pickaxe with Silk Touch comes away from it coated in slime: a Sticky Desh Pickaxe.
 */
public class SlimelingEggBlock extends BaseEntityBlock implements io.github.reiko45.galacticraft.item.ShiftDescribed {
    public static final MapCodec<SlimelingEggBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Color.CODEC.fieldOf("color").forGetter(SlimelingEggBlock::color), propertiesCodec()).apply(instance, SlimelingEggBlock::new));
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    /** 1.12.2 EnumEggColor, with the colour its slimeling hatches in (TileEntitySlimelingEgg#update). */
    public enum Color implements StringRepresentable {
        RED("red", 1.0F, 0.0F, 0.0F), BLUE("blue", 0.0F, 0.0F, 1.0F), YELLOW("yellow", 1.0F, 1.0F, 0.0F);

        public static final Codec<Color> CODEC = StringRepresentable.fromEnum(Color::values);
        private final String name;
        public final float red;
        public final float green;
        public final float blue;

        Color(String name, float red, float green, float blue) {
            this.name = name;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    private final Color color;

    public SlimelingEggBlock(Color color, Properties properties) {
        super(properties);
        this.color = color;
        registerDefaultState(this.stateDefinition.any().setValue(BROKEN, false));
    }

    public Color color() {
        return this.color;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BROKEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlimelingEggBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.SLIMELING_EGG.get(), SlimelingEggBlockEntity::serverTick);
    }

    /** 1.12.2 beginHatch: crack the egg (once) and start the countdown for this player. */
    public static boolean beginHatch(Level level, BlockPos pos, Player player, int time) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlimelingEggBlock) || state.getValue(BROKEN)) {
            return false;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BROKEN, true), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof SlimelingEggBlockEntity egg) {
                egg.startHatching(level.random.nextInt(30 + time) + time, player);
            }
        }
        return true;
    }

    /** 1.12.2 onBlockActivated. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return beginHatch(level, pos, player, 20) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    /** 1.12.2 removedByPlayer: only a pickaxe (or creative mode) takes the egg away; anything else just cracks it. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.getMainHandItem().getItem() instanceof PickaxeItem || player.getAbilities().instabuild) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        beginHatch(level, pos, player, 0);
        return false;
    }

    /** 1.12.2 harvestBlock: the egg itself (uncracked), and the Silk Touch Desh Pickaxe turns sticky. */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!(tool.getItem() instanceof PickaxeItem)) {
            return;
        }
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.025F);
        popResource(level, pos, new ItemStack(this));
        final ItemStack held = player.getMainHandItem();
        final var silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        if (held.is(GCItems.DESH_PICKAXE.get()) && EnchantmentHelper.getItemEnchantmentLevel(silkTouch, held) > 0) {
            player.setItemInHand(InteractionHand.MAIN_HAND, held.transmuteCopy(GCItems.STICKY_DESH_PICKAXE.get(), 1));
        }
    }

    /** 1.12.2 getShiftDescription: a text shared with others like it. */
    @Override
    public String shiftDescriptionKey(net.minecraft.world.item.ItemStack stack) {
        return "block.galacticraftcore.slimeling_egg.description";
    }
}
