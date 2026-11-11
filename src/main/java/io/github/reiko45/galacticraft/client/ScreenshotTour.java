package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.block.entity.OxygenCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDecompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenMachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.menu.ExtendedInventoryMenu;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.client.CameraType;
import io.github.reiko45.galacticraft.registry.GCFluids;
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
import java.util.function.BiConsumer;
import io.github.reiko45.galacticraft.compat.jei.GalacticraftJeiPlugin;
import net.neoforged.fml.ModList;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development aid, inert unless the client is started with {@code -Dgalacticraftcore.screenshotTour=true}
 * ({@code ./gradlew runScreenshotTour}): creates a flat creative world, builds a row of machines on the integrated
 * server, opens every machine GUI and saves screenshots to {@code screenshots/}, then shows the solar and oil
 * scene and quits. Used to check GUI
 * layouts and block models without a human at the keyboard.
 */
public final class ScreenshotTour {
    private static final String PROPERTY = "galacticraftcore.screenshotTour";
    /** Viewpoints of the solar / oil scene: offset x, y, z from its origin, yaw, pitch. */
    private static final double[][] VIEWS = {{1.5, 2.5, 8.5, 180.0, 20.0}, {4.5, 2.0, 3.5, 160.0, 35.0}, {-5.0, 5.0, 6.5, 180.0, 30.0}};

    private final List<BlockPos> machines = new ArrayList<>();
    private BlockPos oilScene = BlockPos.ZERO;
    private BlockPos oxygenScene = BlockPos.ZERO;
    /** Steps after the machine GUIs, one every 30 ticks. */
    private final List<BiConsumer<Minecraft, MinecraftServer>> extras = new ArrayList<>();
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
                        // Stand next to it: menus close at once when the player is out of reach.
                        player.teleportTo(player.serverLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 3.5, 180.0F, 15.0F);
                        if (machine instanceof RefineryBlockEntity) {
                            machine.energy().setStored(machine.energy().getCapacityGJ()); // it has been refining all along
                        }
                        player.openMenu(machine, pos);
                    }
                });
                this.step++;
            } else if (this.step - this.machines.size() < this.extras.size()) {
                this.extras.get(this.step - this.machines.size()).accept(minecraft, server);
                this.step++;
            } else {
                minecraft.stop();
            }
        } else if (this.ticks > 160 && (this.ticks - 160) % 30 == 20 && minecraft.screen != null && this.step <= this.machines.size()) {
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
        // Hover where the views are taken instead of dropping to the ground after each teleport.
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
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
        buildOilScene(level, base.offset(0, 0, -9));
        buildOxygenScene(level, base.offset(0, 0, -20));

        // The solar / oil scene from a few viewpoints: teleport, then shoot on the next step.
        for (int i = 0; i < VIEWS.length; i++) {
            final double[] v = VIEWS[i];
            final String name = "galacticraft_oil_and_solar_" + i;
            this.extras.add((minecraft, srv) -> srv.execute(() -> {
                final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
                p.teleportTo(p.serverLevel(), this.oilScene.getX() + v[0], this.oilScene.getY() + v[1], this.oilScene.getZ() + v[2],
                        (float) v[3], (float) v[4]);
            }));
            this.extras.add((minecraft, srv) -> shoot(minecraft, name));
        }
        // Oxygen machines from the front
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            p.teleportTo(p.serverLevel(), this.oxygenScene.getX() + 2.5, this.oxygenScene.getY() + 2.5, this.oxygenScene.getZ() + 9.5, 180.0F, 15.0F);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "galacticraft_oxygen"));
        // The Galacticraft inventory tab, wearing a full oxygen setup
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            final GCPlayerData data = GCPlayerData.get(p);
            data.setGear(GearSlot.MASK, new ItemStack(GCItems.OXYGEN_MASK.get()));
            data.setGear(GearSlot.GEAR, new ItemStack(GCItems.OXYGEN_GEAR.get()));
            data.setGear(GearSlot.LEFT_TANK, GCItems.HEAVY_OXYGEN_TANK.get().full());
            data.setGear(GearSlot.RIGHT_TANK, GCItems.MEDIUM_OXYGEN_TANK.get().full());
            data.setGear(GearSlot.PARACHUTE, new ItemStack(GCItems.PARACHUTES.get(13).get()));
            data.setGear(GearSlot.FREQUENCY_MODULE, new ItemStack(GCItems.FREQUENCY_MODULE.get()));
            ExtendedInventoryMenu.open(p);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_galacticraft_inventory"));
        this.extras.add((minecraft, srv) -> {
            minecraft.player.closeContainer();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "player_gear_front"));
        this.extras.add((minecraft, srv) -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "player_gear_back"));
        // On the Moon: a sealed glass room (torch inside burns, the one outside doesn't), the HUD, then the warning
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> buildMoonScene(srv));
        });
        for (int i = 0; i < 9; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // let the sealer run its first check (126 ticks after placing)
        }
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_oxygen_hud"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            GCPlayerData.get(p).setGear(GearSlot.MASK, ItemStack.EMPTY);
        }));
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_oxygen_warning"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> srv.getPlayerList().getPlayers().get(0).setGameMode(GameType.CREATIVE)));
        // JEI recipe categories, when JEI is installed.
        if (ModList.get().isLoaded("jei")) {
            final String[] categories = {"compressing", "circuit_fabricating", "refining"};
            for (int i = 0; i < categories.length; i++) {
                final int category = i;
                this.extras.add((minecraft, srv) -> {
                    if (!GalacticraftJeiPlugin.showCategory(category)) {
                        GalacticraftCore.LOGGER.warn("Screenshot tour: JEI runtime not available");
                    }
                });
                this.extras.add((minecraft, srv) -> {
                    shoot(minecraft, "jei_" + categories[category]);
                    minecraft.setScreen(null);
                });
            }
        }
    }

    /**
     * Basic and advanced solar panels; an oil tank stack -> pull pipe -> pipe -> Refinery (facing the camera, oil in on
     * its west face, fuel out east) -> pull pipe -> dyed pipe -> fuel tank; and a source block of each liquid.
     */
    private void buildOilScene(ServerLevel level, BlockPos origin) {
        this.oilScene = origin;
        for (int x = -9; x <= 12; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 4; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (Block solar : new Block[]{GCBlocks.BASIC_SOLAR_PANEL.get(), GCBlocks.ADVANCED_SOLAR_PANEL.get()}) {
            final BlockPos pos = origin.offset(solar == GCBlocks.BASIC_SOLAR_PANEL.get() ? -7 : -3, 0, 0);
            final BlockState state = solar.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
            level.setBlockAndUpdate(pos, state);
            solar.setPlacedBy(level, pos, state, null, new ItemStack(solar));
            this.machines.add(pos);
        }

        final BlockPos refinery = origin.offset(4, 0, 0);
        level.setBlockAndUpdate(refinery, GCBlocks.REFINERY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(refinery) instanceof RefineryBlockEntity machine) {
            machine.energy().setStored(16_000.0F);
            machine.oilTank().fill(new FluidStack(GCFluids.OIL.get(), 6_000), IFluidHandler.FluidAction.EXECUTE);
            machine.fuelTank().fill(new FluidStack(GCFluids.FUEL.get(), 3_000), IFluidHandler.FluidAction.EXECUTE);
            machine.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
            machine.inventory().setStackInSlot(1, LiquidCanisterItem.full(false));
            machine.inventory().setStackInSlot(2, new ItemStack(GCItems.OIL_CANISTER.get()));
        }
        this.machines.add(refinery);

        // Oil supply: two stacked tanks, then a pull pipe and a normal pipe into the refinery's oil face.
        final BlockPos oilTank = origin.offset(1, 0, 0);
        level.setBlockAndUpdate(oilTank, GCBlocks.FLUID_TANK.get().defaultBlockState());
        level.setBlockAndUpdate(oilTank.above(), GCBlocks.FLUID_TANK.get().defaultBlockState());
        if (level.getBlockEntity(oilTank) instanceof FluidTankBlockEntity tank) {
            tank.stackHandler().fill(new FluidStack(GCFluids.OIL.get(), 24_000), IFluidHandler.FluidAction.EXECUTE);
        }
        // Fuel out: pull pipe, an orange pipe, and a tank.
        final BlockPos fuelTank = origin.offset(7, 0, 0);
        level.setBlockAndUpdate(fuelTank, GCBlocks.FLUID_TANK.get().defaultBlockState());
        final BlockState pipe = GCBlocks.FLUID_PIPE.get().defaultBlockState();
        pipe(level, origin.offset(2, 0, 0), pipe.setValue(FluidPipeBlock.PULL, true));
        pipe(level, origin.offset(3, 0, 0), pipe);
        pipe(level, origin.offset(5, 0, 0), pipe.setValue(FluidPipeBlock.PULL, true));
        pipe(level, origin.offset(6, 0, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.ORANGE));
        // A loose pipe run on top, to show the colours and vertical joints.
        pipe(level, origin.offset(3, 1, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.LIME));
        pipe(level, origin.offset(3, 2, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.LIME));
        pipe(level, origin.offset(4, 2, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.LIGHT_BLUE));

        level.setBlockAndUpdate(origin.offset(10, -1, 0), GCFluids.OIL_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(10, -1, 2), GCFluids.FUEL_BLOCK.get().defaultBlockState());
    }

    /**
     * Collector (with leaves), storage module, compressor, decompressor, detector and a running distributor with its
     * bubble, all facing the camera; their GUIs are added to the tour.
     */
    private void buildOxygenScene(ServerLevel level, BlockPos origin) {
        this.oxygenScene = origin;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-9, 0, -4), origin.offset(12, 6, 4))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        final BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, 0, -2), origin.offset(-5, 1, -1))) {
            level.setBlockAndUpdate(pos, leaves);
        }
        final Block[] row = {GCBlocks.OXYGEN_COLLECTOR.get(), GCBlocks.OXYGEN_STORAGE_MODULE.get(), GCBlocks.OXYGEN_COMPRESSOR.get(),
                GCBlocks.OXYGEN_DECOMPRESSOR.get(), GCBlocks.OXYGEN_SEALER.get(), GCBlocks.OXYGEN_DISTRIBUTOR.get()};
        for (int i = 0; i < row.length; i++) {
            final BlockPos pos = origin.offset(-6 + i * 2, 0, 0);
            level.setBlockAndUpdate(pos, row[i].defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (level.getBlockEntity(pos) instanceof OxygenMachineBlockEntity machine) {
                machine.energy().setStored(machine.energy().getCapacityGJ());
                machine.setOxygenStored(machine.oxygenTank().getCapacity() / 2);
                if (machine instanceof OxygenCompressorBlockEntity) {
                    final ItemStack tank = new ItemStack(GCItems.MEDIUM_OXYGEN_TANK.get());
                    OxygenTankItem.setOxygen(tank, 700);
                    machine.inventory().setStackInSlot(0, tank);
                } else if (machine instanceof OxygenDecompressorBlockEntity) {
                    machine.inventory().setStackInSlot(0, GCItems.LIGHT_OXYGEN_TANK.get().full());
                } else if (machine instanceof OxygenSealerBlockEntity) {
                    machine.inventory().setStackInSlot(1, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
                    machine.inventory().setStackInSlot(2, new ItemStack(GCItems.AMBIENT_THERMAL_CONTROLLER.get()));
                } else if (machine instanceof OxygenDistributorBlockEntity distributor) {
                    distributor.setBubbleSize(3.0F);
                    machine.inventory().setStackInSlot(1, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
                }
            }
            this.machines.add(pos);
        }
        level.setBlockAndUpdate(origin.offset(6, 0, 0), GCBlocks.OXYGEN_DETECTOR.get().defaultBlockState());
    }

    /** Teleport to the Moon (survival, wearing gear) beside a sealed glass room and an unsealed torch. */
    private static void buildMoonScene(MinecraftServer server) {
        final ServerLevel moon = server.getLevel(GCDimensions.MOON);
        final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        if (moon == null) {
            GalacticraftCore.LOGGER.warn("Screenshot tour: no Moon dimension");
            return;
        }
        moon.getChunk(0, 0);
        final int y = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 2, 2);
        final BlockPos origin = new BlockPos(0, y, 0);
        moon.setDayTime(6000);
        // A 5x4x5 glass box, hollow, on a floor of moon rock
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, 0, -6), origin.offset(6, 6, 6))) {
            moon.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -6), origin.offset(6, -1, 6))) {
            moon.setBlockAndUpdate(pos, GCBlocks.MOON_ROCK.get().defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, 0, -4), origin.offset(4, 4, 0))) {
            final boolean wall = pos.getX() == origin.getX() || pos.getX() == origin.getX() + 4 || pos.getZ() == origin.getZ() - 4
                    || pos.getZ() == origin.getZ() || pos.getY() == origin.getY() + 4;
            moon.setBlockAndUpdate(pos, wall ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        final BlockPos sealer = origin.offset(2, 0, -2);
        moon.setBlockAndUpdate(sealer, GCBlocks.OXYGEN_SEALER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (moon.getBlockEntity(sealer) instanceof OxygenSealerBlockEntity machine) {
            machine.energy().setStored(machine.energy().getCapacityGJ());
            machine.inventory().setStackInSlot(1, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
        }
        moon.setBlockAndUpdate(origin.offset(1, 0, -3), GCBlocks.UNLIT_TORCH.get().defaultBlockState());
        moon.setBlockAndUpdate(origin.offset(-2, 0, -2), GCBlocks.UNLIT_TORCH.get().defaultBlockState());
        player.setGameMode(GameType.SURVIVAL);
        final GCPlayerData data = GCPlayerData.get(player);
        data.setGear(GearSlot.MASK, new ItemStack(GCItems.OXYGEN_MASK.get()));
        player.teleportTo(moon, origin.getX() + 1.0, origin.getY(), origin.getZ() + 5.5, 200.0F, 20.0F);
    }

    private static void pipe(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, ((FluidPipeBlock) state.getBlock()).withConnections(level, pos, state));
    }

    private static void fill(ServerLevel level, BlockPos pos, java.util.function.Consumer<MachineBlockEntity> filler) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            filler.accept(machine);
        }
    }
}
