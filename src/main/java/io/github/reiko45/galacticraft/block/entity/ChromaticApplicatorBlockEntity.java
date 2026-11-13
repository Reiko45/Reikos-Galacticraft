/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityPainter). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.LightingPanelBlock;
import io.github.reiko45.galacticraft.block.SpaceGlassBlock;
import io.github.reiko45.galacticraft.menu.ChromaticApplicatorMenu;
import io.github.reiko45.galacticraft.network.PaintNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.util.ColorUtil;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 1.12.2 TileEntityPainter: two slots, the colour source (0) and the thing to paint (1), and the colour being mixed
 * (white to start). Mix averages in the source's colour (a dye's, or a block's map colour); Reset goes back to white;
 * Apply paints the type of Space Glass or the Lighting Panels in slot 1. The paint isn't on the blocks: every player is
 * shown Space Glass in the colours of the nearest applicator within 96 blocks (kept when they move away), and Lighting
 * Panels in the colour they last applied.
 */
public class ChromaticApplicatorBlockEntity extends BlockEntity implements MenuProvider {
    private static final int RANGE = 96;
    private static final Map<Level, Set<ChromaticApplicatorBlockEntity>> LOADED = new WeakHashMap<>();

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The glass colours by type (vanilla, clear, strong), -1 unpainted. */
    private final int[] glassColor = {-1, -1, -1};
    private int guiColor = 0xFFFFFF;

    /** guiColor, low 16 bits and high 8, for the open screen. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? ChromaticApplicatorBlockEntity.this.guiColor & 0xFFFF : ChromaticApplicatorBlockEntity.this.guiColor >> 16 & 0xFF;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ChromaticApplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.CHROMATIC_APPLICATOR.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return this.inventory;
    }

    public int guiColor() {
        return this.guiColor;
    }

    public int glassColor(int type) {
        return this.glassColor[type];
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            LOADED.computeIfAbsent(this.level, l -> new LinkedHashSet<>()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            final Set<ChromaticApplicatorBlockEntity> loaded = LOADED.get(this.level);
            if (loaded != null) {
                loaded.remove(this);
            }
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        setRemoved();
        super.onChunkUnloaded();
    }

    /** 1.12.2 onServerTick: each player takes the glass colours of the nearest applicator within 96 blocks. */
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final Set<ChromaticApplicatorBlockEntity> loaded = LOADED.get(level);
        if (loaded == null || loaded.isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            ChromaticApplicatorBlockEntity nearest = null;
            double best = RANGE * RANGE;
            for (ChromaticApplicatorBlockEntity applicator : loaded) {
                final double distance = applicator.getBlockPos().distSqr(player.blockPosition());
                if (distance < best && !applicator.isRemoved()) {
                    best = distance;
                    nearest = applicator;
                }
            }
            if (nearest != null) {
                PaintNetwork.setGlassColors(player, nearest.glassColor[0], nearest.glassColor[1], nearest.glassColor[2]);
            }
        }
    }

    /** 1.12.2 buttonPressed: 0 apply, 1 mix, 2 reset. */
    public void buttonPressed(int index, @Nullable Player player) {
        switch (index) {
            case 0 -> apply(this.inventory.getStackInSlot(1), player);
            case 1 -> mix(this.inventory.getStackInSlot(0));
            case 2 -> this.guiColor = 0xFFFFFF;
            default -> {
                return;
            }
        }
        setChanged();
    }

    /** 1.12.2 takeColorFromItem: a dye's colour, or a block's map colour, averaged into the colour being mixed. */
    private void mix(ItemStack stack) {
        int color = -1;
        final DyeColor dye = DyeColor.getColor(stack);
        if (dye != null) {
            color = dye.getFireworkColor();
        } else if (stack.getItem() instanceof BlockItem blockItem) {
            color = blockItem.getBlock().defaultBlockState().getMapColor(this.level, this.worldPosition).col;
        }
        if (color >= 0) {
            this.guiColor = ColorUtil.addColorsBasic(color, this.guiColor);
        }
    }

    /** 1.12.2 applyColorToItem: blocks get the colour a shade lighter. */
    private void apply(ItemStack stack, @Nullable Player player) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        final int color = ColorUtil.lighten(this.guiColor, 0.03F);
        final Block block = blockItem.getBlock();
        if (block instanceof SpaceGlassBlock glass) {
            this.glassColor[glass.type().ordinal()] = color;
        } else if (block instanceof LightingPanelBlock && player instanceof ServerPlayer serverPlayer) {
            GCPlayerData.get(serverPlayer).setPanelLightingColor(color);
            PaintNetwork.sendColors(serverPlayer);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", this.inventory.serializeNBT(registries));
        tag.putInt("G1", this.glassColor[0]);
        tag.putInt("G2", this.glassColor[1]);
        tag.putInt("G3", this.glassColor[2]);
        tag.putInt("guic", this.guiColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.deserializeNBT(registries, tag.getCompound("Items"));
        if (tag.contains("guic")) {
            this.glassColor[0] = tag.getInt("G1");
            this.glassColor[1] = tag.getInt("G2");
            this.glassColor[2] = tag.getInt("G3");
            this.guiColor = tag.getInt("guic");
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.galacticraftcore.chromatic_applicator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ChromaticApplicatorMenu(id, playerInventory, this, this.data);
    }
}
