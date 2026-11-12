/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityTerraformer and the
 * isTerraformable rules of BlockBasicMars and BlockBasicMoon). Copyright (c) 2023 Team Galacticraft. Licensed under the
 * MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.menu.TerraformerMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Terraformer (1.12.2 TileEntityTerraformer), 30 gJ/t: grows a green bubble 15 blocks across, and once it is full size
 * (with power, water and bone meal) turns the Mars fine regolith (and Moon turf) inside it into grass, now and then a
 * pool of water, and plants saplings (or other plants) on the grass. One block every 15 ticks, one plant every 50; each
 * grass block uses 1 mB of water, each plant or pool 50; every 4th use eats a bone meal, every 4th grass block a seed,
 * every plant its sapling. Slots: water, battery, 4 bone meal, 4 saplings, 4 seeds. Water comes in through any face but
 * the power port on the left.
 */
public class TerraformerBlockEntity extends MachineBlockEntity implements BubbleProvider {
    public static final float MAX_SIZE = 15.0F;
    public static final int WATER_CAPACITY = 2000;
    public static final float ENERGY_PER_TICK = 30.0F;

    private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final List<BlockPos> terraformable = new ArrayList<>();
    private final List<BlockPos> grass = new ArrayList<>();
    private final List<BlockPos> grownTrees = new ArrayList<>();
    private final int[] useCount = new int[2];
    private int saplingIndex = 6;
    private boolean treesDisabled;
    private boolean grassDisabled;
    private boolean bubbleVisible = true;
    private float bubbleSize;
    private float lastSyncedSize;
    private boolean active;
    private boolean lastActive;
    private boolean hasEnoughEnergy;
    private int ticks;

    public TerraformerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.TERRAFORMER.get(), pos, state, 14, 16_000.0F, 2.5F * ENERGY_PER_TICK, 0.0F);
    }

    // ------------------------------------------------------------------ what it terraforms

    /** 1.12.2 ITerraformableBlock#isTerraformable: Mars fine regolith under a non-full block, Moon turf under air. */
    public static boolean isTerraformable(Level level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(GCBlocks.MARS_FINE_REGOLITH.get())) {
            final BlockState above = level.getBlockState(pos.above());
            return !above.isCollisionShapeFullBlock(level, pos.above());
        }
        if (state.is(GCBlocks.MOON_TURF.get())) {
            return level.getBlockState(pos.above()).isAir();
        }
        return false;
    }

    private static boolean isTerraformableBlock(BlockState state) {
        return state.is(GCBlocks.MARS_FINE_REGOLITH.get()) || state.is(GCBlocks.MOON_TURF.get());
    }

    // ------------------------------------------------------------------ slots

    /** 1.12.2 ContainerTerraformer#isOnSaplingList: anything planted (BlockBush). */
    public static boolean isPlant(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BushBlock;
    }

    private int firstFilled(int start, int end) {
        for (int i = start; i < end; i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public ItemStack firstBonemeal() {
        final int slot = firstFilled(2, 6);
        return slot < 0 ? ItemStack.EMPTY : this.inventory.getStackInSlot(slot);
    }

    public ItemStack firstSeeds() {
        final int slot = firstFilled(10, 14);
        return slot < 0 ? ItemStack.EMPTY : this.inventory.getStackInSlot(slot);
    }

    /** 1.12.2 getFirstSaplingStack: a random filled sapling slot (remembered for when it is planted). */
    public ItemStack randomSapling() {
        int filled = 0;
        for (int i = 6; i < 10; i++) {
            filled += this.inventory.getStackInSlot(i).isEmpty() ? 0 : 1;
        }
        if (filled == 0 || this.level == null) {
            return ItemStack.EMPTY;
        }
        int pick = this.level.random.nextInt(filled);
        for (int i = 6; i < 10; i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty() && pick-- == 0) {
                this.saplingIndex = i;
                return this.inventory.getStackInSlot(i);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == 0) {
            return FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(FluidTags.WATER)).isPresent() || stack.is(Items.BUCKET);
        }
        if (slot == 1) {
            return stack.getItem() instanceof ElectricItem;
        }
        if (slot < 6) {
            return stack.is(Items.BONE_MEAL);
        }
        if (slot < 10) {
            return isPlant(stack);
        }
        return slot < 14 && stack.is(Items.WHEAT_SEEDS);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void tick() {
        this.ticks++;
        dischargeBattery(1);
        final ItemStack container = this.inventory.getStackInSlot(0);
        if (!container.isEmpty() && FluidUtil.getFluidContained(container).filter(fluid -> fluid.is(FluidTags.WATER)).isPresent()) {
            final FluidActionResult result = FluidUtil.tryEmptyContainer(container, this.water, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.inventory.setStackInSlot(0, result.getResult());
            }
        }
        final boolean switchedOff = this.level.hasNeighborSignal(this.worldPosition);
        this.hasEnoughEnergy = !switchedOff && this.energy.getStoredGJ() >= ENERGY_PER_TICK;
        final boolean working = !this.grassDisabled || !this.treesDisabled;
        if (working && this.hasEnoughEnergy) {
            this.energy.consume(ENERGY_PER_TICK);
        } else if (this.ticks % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
        this.active = this.bubbleSize == MAX_SIZE && this.hasEnoughEnergy && !firstBonemeal().isEmpty() && !this.water.isEmpty();
        if (this.active != this.lastActive || this.ticks % 60 == 0) {
            findBlocks();
        }
        if (!this.terraformable.isEmpty() && this.ticks % 15 == 0) {
            terraformOne();
        }
        if (!this.treesDisabled && !this.grass.isEmpty() && this.ticks % 50 == 0) {
            plantOne();
        }
        if (this.hasEnoughEnergy && working) {
            this.bubbleSize = Mth.clamp(this.bubbleSize + 0.1F, 0.0F, MAX_SIZE);
        } else {
            this.bubbleSize = Mth.clamp(this.bubbleSize - 0.1F, 0.0F, MAX_SIZE);
        }
        if (Math.abs(this.bubbleSize - this.lastSyncedSize) >= 1.0F || this.bubbleSize != this.lastSyncedSize
                && (this.bubbleSize == 0.0F || this.bubbleSize == MAX_SIZE)) {
            this.lastSyncedSize = this.bubbleSize;
            sync();
        }
        this.lastActive = this.active;
    }

    /** Rebuild the lists of regolith to turn into grass and grass to plant on, inside the bubble. */
    private void findBlocks() {
        this.terraformable.clear();
        this.grass.clear();
        if (!this.active) {
            return;
        }
        final int size = Mth.ceil(this.bubbleSize);
        final double sizeSq = (double) this.bubbleSize * this.bubbleSize;
        final boolean doGrass = !this.grassDisabled && !firstSeeds().isEmpty();
        final boolean doTrees = !this.treesDisabled && !randomSapling().isEmpty();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = this.worldPosition.getX() - size; x < this.worldPosition.getX() + size; x++) {
            for (int y = this.worldPosition.getY() - size; y < this.worldPosition.getY() + size; y++) {
                for (int z = this.worldPosition.getZ() - size; z < this.worldPosition.getZ() + size; z++) {
                    pos.set(x, y, z);
                    final BlockState state = this.level.getBlockState(pos);
                    final double dx = this.worldPosition.getX() + 0.5D - x;
                    final double dy = this.worldPosition.getY() + 0.5D - y;
                    final double dz = this.worldPosition.getZ() + 0.5D - z;
                    if (state.isAir() || dx * dx + dy * dy + dz * dz >= sizeSq) {
                        continue;
                    }
                    if (doGrass && isTerraformable(this.level, pos)) {
                        this.terraformable.add(pos.immutable());
                    } else if (doTrees && state.is(Blocks.GRASS_BLOCK) && this.level.getBlockState(pos.above()).isAir()) {
                        this.grass.add(pos.immutable());
                    }
                }
            }
        }
    }

    /** One random block: grass, or (1 in 40, if walled in on all four sides) a pool of water. */
    private void terraformOne() {
        final int index = this.level.random.nextInt(this.terraformable.size());
        final BlockPos pos = this.terraformable.get(index);
        if (isTerraformableBlock(this.level.getBlockState(pos))) {
            final boolean pool = this.level.random.nextInt(40) == 0 && fullCube(pos.west()) && fullCube(pos.east()) && fullCube(pos.north())
                    && fullCube(pos.south());
            if (pool) {
                this.level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                use(2);
            } else {
                this.level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                this.useCount[0]++;
                this.water.drain(1, IFluidHandler.FluidAction.EXECUTE);
                use(1);
            }
        }
        this.terraformable.remove(index);
    }

    private boolean fullCube(BlockPos pos) {
        return this.level.getBlockState(pos).isCollisionShapeFullBlock(this.level, pos);
    }

    /** One random grass block: a sapling (or other plant) on it, at least 3 blocks from earlier trees. */
    private void plantOne() {
        final int index = this.level.random.nextInt(this.grass.size());
        final BlockPos grassPos = this.grass.get(index);
        if (this.level.getBlockState(grassPos).is(Blocks.GRASS_BLOCK)) {
            final BlockPos plantPos = grassPos.above();
            boolean crowded = false;
            for (BlockPos tree : this.grownTrees) {
                crowded |= tree.distSqr(plantPos) < 9.0D;
            }
            final ItemStack sapling = randomSapling();
            if (!crowded && sapling.getItem() instanceof BlockItem item) {
                final Block block = item.getBlock();
                this.level.setBlockAndUpdate(plantPos, block.defaultBlockState());
                if (block instanceof SaplingBlock saplingBlock && this.level instanceof ServerLevel serverLevel) {
                    if (this.level.getMaxLocalRawBrightness(plantPos) >= 9) {
                        // 1.12.2 BlockSapling#grow: the next stage (the tree itself grows later, as saplings do)
                        saplingBlock.advanceTree(serverLevel, plantPos, this.level.getBlockState(plantPos), this.level.random);
                        this.grownTrees.add(plantPos);
                    }
                } else if (block instanceof BushBlock && this.level instanceof ServerLevel serverLevel && this.level.getMaxLocalRawBrightness(plantPos) >= 5) {
                    for (int i = 0; i < 12; i++) {
                        final BlockState planted = this.level.getBlockState(plantPos);
                        if (!planted.is(block)) {
                            this.grownTrees.add(plantPos);
                            break;
                        }
                        if (planted.isRandomlyTicking()) {
                            planted.randomTick(serverLevel, plantPos, this.level.random);
                        }
                    }
                }
                this.useCount[1]++;
                this.water.drain(50, IFluidHandler.FluidAction.EXECUTE);
                use(0);
            }
        }
        this.grass.remove(index);
    }

    /** 1.12.2 checkUsage: every 4th use a bone meal; a planted sapling; every 4th grass block a seed; a pool 50 water. */
    private void use(int type) {
        if ((this.useCount[0] + this.useCount[1]) % 4 == 0) {
            final ItemStack bonemeal = firstBonemeal();
            if (!bonemeal.isEmpty()) {
                bonemeal.shrink(1);
            }
        }
        switch (type) {
            case 0 -> this.inventory.getStackInSlot(this.saplingIndex).shrink(1);
            case 1 -> {
                if (this.useCount[0] % 4 == 0) {
                    final ItemStack seeds = firstSeeds();
                    if (!seeds.isEmpty()) {
                        seeds.shrink(1);
                    }
                }
            }
            default -> this.water.drain(50, IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    // ------------------------------------------------------------------ state for the GUI and renderer

    public FluidTank waterTank() {
        return this.water;
    }

    public boolean treesDisabled() {
        return this.treesDisabled;
    }

    public boolean grassDisabled() {
        return this.grassDisabled;
    }

    public void toggleTrees() {
        this.treesDisabled = !this.treesDisabled;
        setChanged();
    }

    public void toggleGrass() {
        this.grassDisabled = !this.grassDisabled;
        setChanged();
    }

    public void toggleBubbleVisible() {
        this.bubbleVisible = !this.bubbleVisible;
        setChanged();
        sync();
    }

    public int terraformableCount() {
        return this.terraformable.size();
    }

    public int grassCount() {
        return this.grass.size();
    }

    public boolean isActive() {
        return this.active;
    }

    /** For tests: start at full size. */
    public void setBubbleSize(float size) {
        this.bubbleSize = Mth.clamp(size, 0.0F, MAX_SIZE);
    }

    @Override
    public float bubbleSize() {
        return this.bubbleSize;
    }

    @Override
    public boolean bubbleVisible() {
        return this.bubbleVisible;
    }

    /** 1.12.2 getColor: (0.125, 0.5, 0.125). */
    @Override
    public float[] bubbleColor() {
        return new float[]{0.125F, 0.5F, 0.125F};
    }

    @Override
    public AABB renderBounds() {
        return new AABB(this.worldPosition).inflate(MAX_SIZE);
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Synced: water, trees off, grass off, bubble visible, bubble size x10, blocks to terraform, grass to plant on, redstone off. */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.water.getFluidAmount());
        data.addShort(() -> this.treesDisabled ? 1 : 0);
        data.addShort(() -> this.grassDisabled ? 1 : 0);
        data.addShort(() -> this.bubbleVisible ? 1 : 0);
        data.addShort(() -> Math.round(this.bubbleSize * 10.0F));
        data.addShort(() -> this.terraformable.size());
        data.addShort(() -> this.grass.size());
        data.addShort(() -> this.level != null && this.level.hasNeighborSignal(this.worldPosition) ? 1 : 0);
    }

    /** 1.12.2: power through the left face. */
    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return isItemValid(slot, stack) && !(slot == 0 && stack.is(Items.BUCKET));
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        return slot == 0 ? FluidUtil.getFluidContained(stack).isEmpty()
                : slot == 1 && stack.getItem() instanceof ElectricItem electric && electric.stored(stack) <= 0.0F;
    }

    /** 1.12.2 canFill: water through any face but the power port. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side != null && RelativeSide.of(facing(), side) == RelativeSide.LEFT) {
            return null;
        }
        return new GasMachineBlockEntity.DirectionalTank(this.water, true, false);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TerraformerMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Water", this.water.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("TreesDisabled", this.treesDisabled);
        tag.putBoolean("GrassDisabled", this.grassDisabled);
        tag.putBoolean("BubbleVisible", this.bubbleVisible);
        tag.putFloat("BubbleSize", this.bubbleSize);
        tag.putIntArray("UseCount", this.useCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.water.readFromNBT(registries, tag.getCompound("Water"));
        this.treesDisabled = tag.getBoolean("TreesDisabled");
        this.grassDisabled = tag.getBoolean("GrassDisabled");
        this.bubbleVisible = !tag.contains("BubbleVisible") || tag.getBoolean("BubbleVisible");
        this.bubbleSize = tag.getFloat("BubbleSize");
        this.lastSyncedSize = this.bubbleSize;
        final int[] uses = tag.getIntArray("UseCount");
        if (uses.length == 2) {
            this.useCount[0] = uses[0];
            this.useCount[1] = uses[1];
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putFloat("BubbleSize", this.bubbleSize);
        tag.putBoolean("BubbleVisible", this.bubbleVisible);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.bubbleSize = tag.getFloat("BubbleSize");
        this.bubbleVisible = tag.getBoolean("BubbleVisible");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
