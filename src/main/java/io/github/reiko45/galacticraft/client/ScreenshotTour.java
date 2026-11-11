package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development aid, inert unless the client is started with {@code -Dgalacticraftcore.screenshotTour=true}
 * ({@code ./gradlew runScreenshotTour}): creates a flat creative world, builds a row of machines on the integrated
 * server, opens every machine GUI and saves screenshots to {@code screenshots/}, then quits. Used to check GUI
 * layouts and block models without a human at the keyboard.
 */
public final class ScreenshotTour {
    private static final String PROPERTY = "galacticraftcore.screenshotTour";

    private final List<BlockPos> machines = new ArrayList<>();
    private boolean worldRequested;
    private int ticks;
    private int step;

    private ScreenshotTour() {
    }

    public static void registerIfEnabled() {
        if (Boolean.getBoolean(PROPERTY)) {
            final ScreenshotTour tour = new ScreenshotTour();
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tour.tick());
            GalacticraftCore.LOGGER.info("Screenshot tour enabled");
        }
    }

    private void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!this.worldRequested && minecraft.screen instanceof TitleScreen) {
            this.worldRequested = true;
            final String name = "galacticraft_tour_" + System.currentTimeMillis();
            minecraft.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1234L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    minecraft.screen);
            return;
        }
        final MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null) {
            return;
        }
        this.ticks++;
        if (this.ticks == 40) {
            server.execute(() -> build(server));
        } else if (this.ticks == 160) {
            shoot(minecraft, "galacticraft_machines");
        } else if (this.ticks > 160 && (this.ticks - 160) % 30 == 0) {
            if (this.step < this.machines.size()) {
                final BlockPos pos = this.machines.get(this.step);
                server.execute(() -> {
                    final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player.level().getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                        player.openMenu(machine, pos);
                    }
                });
                this.step++;
            } else {
                minecraft.stop();
            }
        } else if (this.ticks > 160 && (this.ticks - 160) % 30 == 20 && minecraft.screen != null) {
            final String name = minecraft.screen.getTitle().getString().toLowerCase().replace(' ', '_');
            shoot(minecraft, "gui_" + name);
            server.execute(() -> server.getPlayerList().getPlayers().get(0).closeContainer());
        }
    }

    private static void shoot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.getMainRenderTarget(),
                message -> GalacticraftCore.LOGGER.info("Screenshot tour: {}", message.getString()));
    }

    private void build(MinecraftServer server) {
        final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        final ServerLevel level = player.serverLevel();
        player.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000);
        final BlockPos base = player.blockPosition().offset(0, 0, -5);
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 6.5, 180.0F, 15.0F);

        // Front row, all facing the camera (south).
        final Block[] row = {GCBlocks.COAL_GENERATOR.get(), GCBlocks.ENERGY_STORAGE_MODULE.get(), GCBlocks.COMPRESSOR.get(),
                GCBlocks.ELECTRIC_COMPRESSOR.get(), GCBlocks.ELECTRIC_FURNACE.get(), GCBlocks.CIRCUIT_FABRICATOR.get(),
                GCBlocks.ENERGY_STORAGE_CLUSTER.get(), GCBlocks.ADVANCED_COMPRESSOR.get(), GCBlocks.ELECTRIC_ARC_FURNACE.get()};
        for (int i = 0; i < row.length; i++) {
            final BlockPos pos = base.offset(i - row.length / 2, 0, 0);
            level.setBlockAndUpdate(pos, row[i].defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            this.machines.add(pos);
        }
        // A powered setup on the row above: Storage Module (output faces west) -> wires -> Electric Furnace.
        final BlockPos module = base.offset(2, 2, 0);
        level.setBlockAndUpdate(module, GCBlocks.ENERGY_STORAGE_MODULE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        for (int dx = 1; dx <= 3; dx++) {
            level.setBlockAndUpdate(module.west(dx), GCBlocks.ALUMINUM_WIRE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(module.west(3).above(), GCBlocks.HEAVY_ALUMINUM_WIRE.get().defaultBlockState());
        final BlockPos furnace = module.west(4);
        level.setBlockAndUpdate(furnace, GCBlocks.ELECTRIC_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(base.offset(-6, -1, 0), Blocks.STONE.defaultBlockState());

        if (level.getBlockEntity(module) instanceof EnergyStorageBlockEntity storage) {
            storage.energy().setStored(400_000.0F);
        }
        if (level.getBlockEntity(furnace) instanceof ElectricFurnaceBlockEntity powered) {
            powered.inventory().setStackInSlot(1, new ItemStack(Items.RAW_IRON, 32));
        }
        // Give the GUIs something to show.
        fill(level, this.machines.get(0), be -> {
            if (be instanceof CoalGeneratorBlockEntity generator) {
                generator.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 16));
            }
        });
        fill(level, this.machines.get(1), be -> be.energy().setStored(250_000.0F));
        fill(level, this.machines.get(2), be -> {
            if (be instanceof CompressorBlockEntity compressor) {
                compressor.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 8));
                compressor.inventory().setStackInSlot(CompressorBlockEntity.GRID_START, new ItemStack(Items.IRON_INGOT, 8));
                compressor.inventory().setStackInSlot(CompressorBlockEntity.GRID_START + 1, new ItemStack(Items.IRON_INGOT, 8));
            }
        });
        fill(level, this.machines.get(3), be -> {
            if (be instanceof ElectricCompressorBlockEntity compressor) {
                compressor.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
                compressor.inventory().setStackInSlot(ElectricCompressorBlockEntity.GRID_START, new ItemStack(GCItems.TIN_INGOT.get(), 16));
                compressor.inventory().setStackInSlot(ElectricCompressorBlockEntity.GRID_START + 1, new ItemStack(GCItems.TIN_INGOT.get(), 16));
            }
        });
        fill(level, this.machines.get(5), be -> {
            if (be instanceof CircuitFabricatorBlockEntity fabricator) {
                fabricator.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
                fabricator.inventory().setStackInSlot(1, new ItemStack(Items.DIAMOND, 4));
                fabricator.inventory().setStackInSlot(2, new ItemStack(GCItems.RAW_SILICON.get(), 4));
                fabricator.inventory().setStackInSlot(3, new ItemStack(GCItems.RAW_SILICON.get(), 4));
                fabricator.inventory().setStackInSlot(4, new ItemStack(Items.REDSTONE, 4));
                fabricator.inventory().setStackInSlot(5, new ItemStack(Items.LAPIS_LAZULI, 4));
            }
        });
        this.machines.add(furnace);
    }

    private static void fill(ServerLevel level, BlockPos pos, java.util.function.Consumer<MachineBlockEntity> filler) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            filler.accept(machine);
        }
    }
}
