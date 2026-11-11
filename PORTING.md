# Porting Galacticraft 1.12.2 → NeoForge 1.21.1

This is the plan and the running log for the port. The source is **Galacticraft Legacy** by Team Galacticraft
(`TeamGalacticraft/Galacticraft-Legacy`, branch `master-1.12`, commit `acec442`), which is MIT-licensed.

> **Why Legacy and not `micdoodle8/Galacticraft`?** The original micdoodle8 repository uses the custom
> "Galacticraft License". Its section 28(e) explicitly prohibits ports to other Minecraft/Forge versions
> without written permission. Team Galacticraft's Legacy repository is the maintained 1.12.2 version. It was
> relicensed under MIT (every source file carries `Copyright (c) 2023 Team Galacticraft, Licensed under the MIT
> license`). Port from Legacy only, and keep `LICENSE-GALACTICRAFT-LEGACY` in the repository and the jar.

## Size of the job

Legacy is about 1,190 Java files (~211k lines) plus about 2,200 resource files:

| Area | 1.12.2 contents |
|---|---|
| Core | ~134k lines: 73 tile entities (incl. planets), ~56 GUIs, 43 containers, 16 core entities, 78 `PacketSimple` message types |
| Planets | ~62k lines: Mars, Asteroids and Venus (each with its own blocks, machines, mobs and boss) |
| API | ~14.5k lines: galaxy registry, power/oxygen interfaces, rocket prefabs |
| MicdoodleCore | ASM coremod with about 25 bytecode hooks into vanilla (see below) |
| Assets | ~776 PNG, 53 OBJ models, 29 sounds, 25 languages, 262 JSON recipes, 28 advancements |

Treat it as a rewrite onto modern APIs that keeps Galacticraft's gameplay, numbers and assets. A mechanical
translation won't work: almost every vanilla and Forge API it touched changed between 1.12 and 1.21.

## How 1.12.2 concepts map to NeoForge 1.21.1

| 1.12.2 (Forge 14.23) | NeoForge 1.21.1 | Status |
|---|---|---|
| `@Mod` + `CommonProxy`/`ClientProxy` + FML lifecycle events | `@Mod` constructor with `IEventBus`, `@Mod(dist = CLIENT)` class, `@EventBusSubscriber` | done |
| Metadata sub-blocks/items (`basic_block_core:6`) | One registry entry per variant (`tin_ore`); mapping in `tools/legacy_mapping.json` | done for Phase 1 |
| `GameRegistry.register` / `RegistryEvent` | `DeferredRegister` (`GCBlocks`, `GCItems`, ...) | done |
| `setHarvestLevel(tool, level)` | `mineable/*` and `needs_*_tool` block tags (datagen) | done |
| OreDictionary | `c:` convention tags (`compressedSteel` → `c:plates/steel`) | done |
| JSON recipes with `forge:ore_dict`/metadata | Converted by `tools/port_legacy_assets.py` | 60 of 166 converted; the rest need unported items |
| Code-registered smelting (`RecipeManagerGC`) | Datagen (`GCRecipeProvider`) | done |
| Machine recipes (compressor, circuit fabricator, refinery...) | Custom `RecipeType` + `RecipeSerializer`, JSON recipes (`galacticraftcore:compressing_shaped/_shapeless`, `circuit_fabricating`) | compressor + fabricator done |
| `EnumHelper.addToolMaterial/ArmorMaterial` | `SimpleTier`; `ArmorMaterial` registry entry | done |
| `IWorldGenerator` ore generation | Configured/placed features + NeoForge biome modifier | done |
| `WorldProvider` + `ChunkProvider` + numeric dimension IDs | Datapack `dimension_type` + `dimension`; custom `ChunkGenerator` with a `MapCodec` | Moon done |
| Gravity via ASM (`TransformerHooks.getGravityForEntity`) | `generic.gravity` attribute modifier (vanilla since 1.20.5) | done (living entities) |
| `LivingFallEvent` distance scaling | Same event in NeoForge | done |
| `IRenderHandler` sky/cloud providers | `DimensionSpecialEffects` (+ `renderSky` override) | Moon: black sky only; Earth/stars renderer pending |
| `TileEntity` + `ITickable` | `BlockEntity` + `BlockEntityType` + `BlockEntityTicker` (`MachineBlockEntity`) | done |
| `IGuiHandler`, `Container`, `GuiContainer` | `MenuType`, `AbstractContainerMenu`, `AbstractContainerScreen`, `RegisterMenuScreensEvent` (`MachineMenu`/`MachineScreen`, 1.12.2 GUI sheets reused) | done |
| `ISidedInventory` | `IItemHandler` capability per side (`SidedItemHandler`, rules evaluated per insert) | done |
| Forge capabilities (energy, fluids, items) | NeoForge 1.21 capabilities: `BlockCapability`, `RegisterCapabilitiesEvent` (`GCCapabilities`) | done |
| `GCPlayerStats` capability (~54 fields) | Data attachment `galacticraftcore:player_data` (`GCPlayerData`), synced with payloads | oxygen/gear fields done |
| Item NBT (battery charge, oxygen in tanks, fuel in canisters, rocket inventory) | Data components (`galacticraftcore:energy`, `galacticraftcore:fluid`, `galacticraftcore:oxygen`, `galacticraftcore:rocket_storage`, `galacticraftcore:rocket_fuel`) | done |
| `FMLEmbeddedChannel`, `PacketSimple` (78 enum types) | `CustomPacketPayload` + `StreamCodec`, one record per message, `RegisterPayloadHandlersEvent` (`GCNetwork`) | oxygen, gear, inventory tab, rocket/lander controls, destination done |
| Forge `Configuration` .cfg + config GUI | `ModConfigSpec` (COMMON/SERVER/CLIENT; SERVER configs sync automatically) | client config started |
| TESRs (18), entity renderers (~33), Forge OBJ loader | `BlockEntityRenderer`, `EntityRenderer` with `LayerDefinition`s converted from the 1.12.2 `ModelRenderer` code (`tools/convert_java_model.py`), NeoForge OBJ loader (`neoforge:obj`), `BlockEntityWithoutLevelRenderer` for item models | solar panel, fluid tank/pipe, rocket, lander, para-chest, NASA Workbench done |
| Forge fluids (oil, fuel, oxygen, ...) | `FluidType` + `BaseFlowingFluid`, `FluidStack`, `IFluidHandler`, `IClientFluidTypeExtensions`; oxygen is a block-less `GasFluid` | oil, fuel, oxygen done |
| Commands (12) | Brigadier via `RegisterCommandsEvent` | as needed |
| Advancements (28) + `GCTriggers` | 1.21 advancement JSON (datagen) + `CriterionTrigger` registry | Phase 8 |
| JEI 4.x plugin | JEI 19.x (1.21.1), optional (`compat/jei`) | Compressor, Circuit Fabricator, Refinery done |
| gJ energy with RF/IC2/BC/Mekanism conversion | Float gJ internally (`GCEnergyStorage`, `galacticraftcore:energy` capability) + FE (`IEnergyStorage`) on every machine at 1 FE = 1.6 gJ | done |
| `EnergyNetwork` / `NetworkFinder`, conductor tiers | `EnergyNetworkManager`: flood-filled wire networks, rebuilt on change, ticked per level; tier = min(source, weakest wire) | done |
| Forge `Configuration` energy display options | `ModConfigSpec` client config (`energyDisplayUnit`: gJ or FE) | done |
| `BlockMulti` + `TileEntityFake` (solar panels, later launch pads/dishes) | Invisible `SolarPartBlock` parts pointing at the main block entity; breaking any part breaks the whole | solar done |
| `IMachineSides` (sneak + wrench face config) | `INPUT`/`SIDES` block state properties, models per configuration, capabilities invalidated on change | done |
| `FluidNetwork` / `TileEntityFluidPipe` | `FluidNetworkManager`: flood-filled pipe networks per level, colour-separated, buffer shared out per pipe | done |
| `EventHandlerGC#generateOil` (populate event) | `OilPocketFeature` + placed feature + biome modifier (`LAKES` step) | done |
| `InventoryExtended` + `TabRegistry` inventory tabs | 11-slot `ItemStackHandler` in the player attachment, `ExtendedInventoryMenu`, tab buttons added in `ScreenEvent.Init` | done |
| `ThreadFindSeal` (own thread) | `SealFinder` on the server thread, one check per server tick as in 1.12.2 | done |
| `DamageSourceGC.oxygenSuffocation`, `.spaceshipCrash` | Data-driven damage types `galacticraftcore:oxygen_suffocation`, `galacticraftcore:spaceship_crash` + damage type tags | done |
| Overlays drawn in `TickHandlerClient` | `RegisterGuiLayersEvent` GUI layers (`OxygenHud`) | oxygen done |
| `LayerOxygenMask/Gear/Tanks/Parachute` on `RenderPlayer` | `RenderLayer` added in `EntityRenderersEvent.AddLayers` (`GearLayer`) | mask, tanks, tubes, parachute done |
| `WorldUtil.transferEntityToDimension` + `ITeleportType` | `ServerPlayer#teleportTo(ServerLevel, ...)` in `SpaceTravel`, per destination | Overworld, Moon done |
| `EntitySpaceshipBase`/`EntityTieredRocket`/`EntityLander` (`IControllableEntity`, key packets) | Plain `Entity` subclasses with `SynchedEntityData`; keys sent as payloads (`VehicleControls`); riders kept by cancelling `EntityMountEvent` | done |
| `KeyHandlerClient` key bindings | `RegisterKeyMappingsEvent` (Spaceship Inventory, default F as in 1.12.2) | done |

### MicdoodleCore ASM hooks → replacements

| Hook (1.12.2) | Plan in 1.21.1 |
|---|---|
| `EntityLivingBase.travel` gravity, `EntityItem`/`EntityArrow` gravity | `generic.gravity` attribute (living, done). Items and projectiles need a small mixin on `getDefaultGravity` (pending) |
| Replace `EntityPlayerMP`/`EntityPlayerSP` with GC subclasses | Not needed: player state lives in data attachments, logic in tick events |
| Fog/sky colour, lightmap, rain strength hooks | `DimensionSpecialEffects`, `ViewportEvent.ComputeFogColor`, biome effects |
| `ModelBiped` rotation (oxygen gear on player) | `RenderLivingEvent` / render layers for worn gear |
| Footprints on the Moon (`ParticleManager`) | Client tick + custom particle or decal renderer |
| Camera orientation in rockets | `ViewportEvent.ComputeCameraAngles` |
| `Chunk.populate` (block other mods' worldgen on GC planets) | Biome tags / biome modifiers (other mods target `#c:is_overworld` etc.) |
| `World.getRainStrength` | Biome `has_precipitation: false` |
| IC2/AE2/Optifine/SyncMod patches | Dropped (mods no longer exist on 1.21.1) |

### Known design risks

* **Space stations need dimensions created at runtime.** NeoForge 1.21.1 has no public API for adding levels
  while the server is running. Options: (a) a fixed pool of pre-declared station dimensions, (b) one shared
  "orbit" dimension where each station is a far-apart region (simplest, keeps saves small), or (c) a
  dynamic-dimension library. Recommendation: (b).
* **Oxygen sealing** (`ThreadFindSeal`) is a threaded flood fill over loaded chunks. Running it on a worker
  thread is not safe against 1.21 chunk access. Port it as an incremental, budgeted flood fill on the server thread.
* **Rendering**: 53 OBJ models (rockets, buggy, landers, machines). NeoForge's OBJ loader covers static models.
  Animated parts (rocket flames, buggy wheels) need code renderers.
* **GameTest mock players** never negotiate the mod's network channels; server-to-client payloads go through
  `GCNetwork#sendToPlayer`, which skips such connections.
* **GameTest server** ignores datapack dimensions, so the Moon tests in `PortSmokeTests` run the generator on
  in-memory chunks. Check the real dimension by hand (see the README).

## Phases

### Phase 1 – Foundation and the Moon (this commit) ✅
- [x] NeoForge 1.21.1 ModDevGradle build, Parchment mappings, GitHub Actions CI
- [x] Overworld: tin, aluminum and silicon ores with the 1.12.2 vein sizes, counts and heights; copper is vanilla
- [x] Storage blocks (tin, aluminum, meteoric iron, silicon) and the tin decoration family (blocks, stairs, slabs, walls)
- [x] Moon blocks: turf, dirt, rock, dungeon brick, copper/tin/cheese/sapphire ores, stairs/slabs/walls
- [x] Block of Cheese, Fallen Meteor (plain block for now), Glowstone Torch
- [x] Materials and parts: ingots, compressed plates, wafers, solar modules, rocket/oxygen/buggy parts, canisters
- [x] Food (1.12.2 nutrition values; canned food returns a tin canister) and Heavy Duty tools and armor
- [x] Moon dimension: ported `ChunkProviderMoon` terrain and craters, ores, low gravity (0.018), fall distance × 0.18
- [x] 60 crafting recipes converted, smelting/blasting/stonecutting, loot tables, tags, translations in 25 languages
- [x] GameTests + static asset check

### Phase 2a – Energy and machines ✅
- [x] gJ energy (float, 1.12.2 numbers unchanged) with FE interop on every machine face that takes or gives power
- [x] Aluminum Wire and Heavy Aluminum Wire forming networks; tier-2 power (Cluster over heavy wire) speeds machines up
- [x] Sources push straight into an adjacent machine too; per-tick output budget shared by wires, neighbours and batteries
- [x] Coal Generator (hull heat), Energy Storage Module and Cluster (charge level shown on the block)
- [x] Compressor (fuel), Electric Compressor, Advanced Compressor: compressed plates and Heavy-Duty Plates are craftable
- [x] Electric Furnace, Electric Arc Furnace (doubles ore smelting), Circuit Fabricator (wafers)
- [x] Battery (15,000 gJ; empty ones stack to 4), Standard Wrench (rotates machines)
- [x] 1.12.2 consumer rules: redstone turns machines off, idle machines lose 5 gJ / 10 ticks, battery power is tier 1
- [x] GUIs drawn from the 1.12.2 sheets at the 1.12.2 coordinates; status text and energy tooltips in 25 languages
- [x] Hopper/pipe automation per side (outputs from below, grid balancing for compressors, silicon balancing)

### Phase 2b – Solar, fluids and the rest of the power tier ✅
- [x] Basic/Advanced Solar Panel: pole + 3x3 panel of invisible parts (port of `BlockMulti`), panel renderer that
      flips at night (basic) or tracks the sun (advanced), sky checks every second, ×1.4 output on the Moon
- [x] Sneak + wrench reconfigures a machine's energy face (left/back/right) and a storage block's input/output pair,
      with matching block textures
- [x] Aluminum Wire Switch and Heavy Aluminum Wire Switch: a redstone signal cuts the network
- [x] Oil and fuel (flowing liquids, buckets, fuel explodes when burnt), Oil/Fuel Canisters (1,000 mB, 7 fill textures)
- [x] Refinery (oil in on the left, fuel out on the right, power on top; 1 mB per 2 ticks, per tick on tier-2 power)
- [x] Fluid Tank (16,000 mB, stacks act as one), Fluid Pipe (16 colours, pull mode via wrench, fluid visible inside)
- [x] Crude oil pockets in Overworld biomes (1.12.2 shape, depth and chances)
- [x] JEI categories for compressing, circuit fabricating and refining, with machine catalysts and GUI click areas

### Phase 3 – Oxygen and survival ✅
- [x] Player data attachment (the oxygen and gear parts of `GCPlayerStats`) and sync payloads
- [x] Galacticraft inventory tab: mask, gear, two tanks, parachute, frequency module, four thermal padding slots and a
      shield controller slot; gear is drawn on players (mask, tanks, tubes)
- [x] Oxygen Mask, Oxygen Gear, Light/Medium/Heavy Oxygen Tanks, Infinite Oxygen Supply, 16 parachutes; oxygen gas
- [x] Oxygen Collector, Compressor, Decompressor, Storage Module, Bubble Distributor (with bubble), Sealer (flood
      fill, shared between sealers, edge checks), Detector, with the 1.12.2 GUIs
- [x] Airless worlds: players breathe from tanks or suffocate, mobs suffocate, torches only burn in oxygen, breathable
      air, oxygen HUD and warning
- [ ] Later: parachute deployment (with landers), thermal padding and heat (Asteroids), frequency module dish and
      sensor glasses rendering, the Arc Lamp's bright air

### Phase 4 – Rockets and travel ✅
- [x] NASA Workbench (3x3x4 multiblock, 1.12.2 OBJ model through the NeoForge OBJ loader) with the Tier 1 Rocket page;
      Tier 1 Rocket item (0-3 chests, creative pre-fuelled variant) drawn with the rocket model
- [x] Rocket Launch Pad (nine join into a launch pad), Fuel Loader (12,000 mB, 2 mB per tick into a docked rocket)
- [x] Tier 1 rocket entity (1.12.2 ModelRocketTier1, converted by `tools/convert_java_model.py`): fuel, cargo,
      400 tick countdown with HUD, lift-off taking the launch pad along, crash when it falls back, hit to break
- [x] Destination screen (simplified 1.12.2 celestial selection: Sun, Earth, Moon), Overworld and Moon arrival
      (1.12.2 TeleportTypeOverworld/TeleportTypeMoon)
- [x] Space Lander (1.12.2 ModelLander, steering, braking, crash, entry HUD), Para-chest entity and block (16 parachute
      colours), parachute deployment (slow fall + canopy on the player)
- [ ] Deferred: Moon Buggy + Buggy Fueling Pad + buggy schematic (moon dungeon loot, Phase 5), the schematic page
      system (only the Tier 1 page exists), the full galaxy map (zoom, catalogue panels, space stations)

### Phase 5 – Finish the Moon
- [ ] Caves (`MapGenCavesMoon`), moon villages, dungeon + Skeleton Boss + Tier 1 treasure chest
- [ ] Evolved mobs and their spawns, meteor showers + hot Fallen Meteor block entity, sky renderer with Earth, footprints

### Phase 6 – Space stations
- [ ] Station creation/ownership, orbit dimension(s), Spin Thruster, docking, Space Race teams

### Phase 7 – Planets
- [ ] Mars (desh, Tier 2 and cargo rockets, slimelings, Creeper Boss, gas liquefier/methane synthesizer/electrolyzer, launch controller)
- [ ] Asteroids (Tier 3 rocket, Astro Miner, telepads, beam power, thermal gear)
- [ ] Venus (acid, geothermal generator, solar array, laser turret, Spider Queen)

### Phase 8 – Polish
- [ ] Advancements, sounds/music (check the provenance of the `*_jc.ogg` tracks first), manual, Jade/EMI integration

## Intentional differences from 1.12.2 (so far)

* Mod id `galacticraftcore` is kept so that asset paths and translation keys map 1:1. The display name marks
  the mod as an unofficial port, and it doesn't clash with Galacticraft 5's `galacticraft` id.
* Copper ore, copper ingot and copper block are vanilla's. Galacticraft's copper items map to them.
* Ores drop themselves, as in 1.12.2 (no raw ore items). Deepslate variants don't exist yet, so overworld ores
  only generate in stone (y 0–60).
* Moon day length is vanilla's. 1.12.2 used an 8× longer day, which data-driven dimension types can't express.
* Added for modern parity: blasting, smoker/campfire and stonecutter recipes, `c:` tags, and the Heavy Duty set
  in the enchantability tags.
* Both tin stair and slab variants keep their duplicate 1.12.2 English names ("Tin Decoration Stairs").
* Energy is moved only through Galacticraft wires and direct contact, like 1.12.2; foreign FE cables can
  connect to any machine face that takes or gives power.
* Coal Generator heat now survives a world reload (1.12.2 saved and loaded it under different NBT keys).
* Compressor recipes for steel/bronze ingots load only when some mod puts items in `c:ingots/steel` or
  `c:ingots/bronze` (the 1.12.2 OreDictionary check). Hard/quick/adventure mode recipe variants are not ported.
* The Circuit Fabricator's bonus output in zero-gravity dimensions waits for space stations.
* Fluid Pipe and Pull Fluid Pipe are one block with a `pull` state that the wrench toggles; there is no separate
  pull pipe item. Pipes only take fluid in; they can't be drained by other blocks, as in 1.12.2.
* The amount of liquid drawn inside a pipe is synced in 5% steps, and only when it changes.
* Oil generation uses the default 1.12.2 oil gen factor (1.8) and has no config, retrogen or extra-dimension list yet.
  Biome chance modifiers use tags: mountains and hills halve the chance, oceans and deserts multiply it by 1.8.
  Pockets are placed before structures, so mineshafts and strongholds cut through them instead of being flooded.
* A Fluid Tank drains from the bottom tank of a stack; the tanks above pour down into it on the next ticks.
* Oxygen is a fluid (mB of `galacticraftcore:oxygen` gas): tanks, machines and pipes trade it through the NeoForge
  fluid capability, so other mods' pipes and tanks can carry it. Oxygen Tanks keep their contents in a data component
  instead of item damage; crafted tanks are empty, like 1.12.2.
* The seal check runs on the server thread (1.12.2 used a thread), still one sealer per server tick. When a sealed
  space opens, the breathable air goes at the next check (1.12.2 timing); breathable air that no sealer vents into is
  cleared at the end of the tick.
* Vanilla torches held on an airless world turn into space torches (one block with a `lit` state instead of
  unlit_torch / unlit_torch_lit), and back on a world with air.
* Mobs that should breathe in space (1.12.2 IEntityBreathable) go in the `galacticraftcore:breathes_in_space` entity tag.
  Blocks that seal like glass go in `galacticraftcore:sealable` (glass panes by default, the 1.12.2 config default);
  full blocks that let air through go in `galacticraftcore:air_permeable` (gravel, wool, sponge).
* Suffocation uses the 1.12.2 defaults (2 damage every 100 ticks) without a config yet; hard-mode increasing damage
  is not ported.
* Machines report "running" when they used power for work that tick (1.12.2 looked at the progress counter, which
  made the Refinery's status flicker).
* The destination screen is a simplified galaxy map: the Sun, Earth and the Moon on their orbits with the 1.12.2 icons,
  frame and LAUNCH button. Zooming, the planet catalogue panels and the space station list come with later phases.
* The NASA Workbench opens straight on the Tier 1 Rocket page; there is no schematic paging until the Moon Buggy
  schematic exists. Its Tier 1 recipe is hard-coded as in 1.12.2 (`RecipeManagerGC`), so it is not a JSON recipe and
  doesn't show in JEI yet.
* Rocket types 0-3 (18/36/54 cargo slots) are a `rocket_storage` data component on one item instead of item damage; the
  creative "pre-fuelled" rocket is a rocket item with a full `rocket_fuel` component.
* The Para-chest's colour is a block state (16 colours) instead of tile-entity data, so its model needs no tile renderer.
* Rocket and lander exhaust uses vanilla flame and smoke particles instead of 1.12.2's custom flame particles.
* The Fuel Loader takes power on its left face and fuel on its right; the sneak + wrench side configuration of 1.12.2
  is not ported for it yet. Fuel Loaders take any fluid in the `c:fuel` tag.
* Buggy Fueling Pads (`landing_pad:1`) are not ported, so their recipe is skipped.

## Workflow for porting more content

1. Register the block/item in `GCBlocks`/`GCItems` (flatten metadata variants).
2. Add `textures`, `lang` and `recipes.items` entries to `tools/legacy_mapping.json` (`models` for 1.12.2 JSON
   models, `obj_models` for OBJ models and their `.mtl`).
3. `python3 tools/port_legacy_assets.py --legacy /path/to/Galacticraft-Legacy`. It copies textures, converts
   translations and re-converts recipes, and lists every recipe still blocked on unmapped items.
4. Add block states/models/loot/tags to the datagen providers, then run `./gradlew runData`.
5. `python3 tools/check_assets.py` and `./gradlew runGameTestServer`.
6. Entity and block-entity models written as 1.12.2 `ModelRenderer` code: `python3 tools/convert_java_model.py
   Model.java` prints the 1.21 `LayerDefinition` body (boxes, pivots, rotations, mirroring, texture offsets).
7. `xvfb-run -a ./gradlew runScreenshotTour` (or plain `./gradlew runScreenshotTour` with a display) builds scenes and
   saves screenshots of every GUI and model to `run/screenshots/` for a visual check.
