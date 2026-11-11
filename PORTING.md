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
| `WorldProvider` + `ChunkProvider` + numeric dimension IDs | Datapack `dimension_type` + `dimension`; custom `ChunkGenerator` with a `MapCodec` | Moon, orbit, Mars done |
| Gravity via ASM (`TransformerHooks.getGravityForEntity`, `getItemGravity`, `getArrowGravity`) | `generic.gravity` attribute modifier (vanilla since 1.20.5) for living entities; two Mixins on `ItemEntity`/`AbstractArrow#getDefaultGravity` | done |
| `LivingFallEvent` distance scaling | Same event in NeoForge | done |
| `IRenderHandler` sky/cloud providers | `DimensionSpecialEffects` + NeoForge `renderSky`/`renderClouds` overrides (`MoonSkyRenderer`, `MarsSkyRenderer`) | Moon, orbit, Mars done |
| `MapGenBaseMeta` / `MapGenCavesMoon` | `ChunkGenerator#applyCarvers` (AIR step) running the 1.12.2 worm carver (`WormCaves`, shared with Mars) | done |
| `TileEntityAdvanced` networked fields (`@NetworkedField`) | Block entity update tag + `ClientboundBlockEntityDataPacket`, sent when needed | Fallen Meteor heat done |
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
| `WorldUtil.transferEntityToDimension` + `ITeleportType` | `ServerPlayer#teleportTo(ServerLevel, ...)` in `SpaceTravel`, per destination | Overworld, Moon, orbit, Mars done |
| `EntitySpaceshipBase`/`EntityTieredRocket`/`EntityLander` (`IControllableEntity`, key packets) | Plain `Entity` subclasses with `SynchedEntityData`; keys sent as payloads (`VehicleControls`); riders kept by cancelling `EntityMountEvent` | done |
| `KeyHandlerClient` key bindings | `RegisterKeyMappingsEvent` (Spaceship Inventory, default F as in 1.12.2) | done |

### MicdoodleCore ASM hooks → replacements

| Hook (1.12.2) | Plan in 1.21.1 |
|---|---|
| `EntityLivingBase.travel` gravity, `EntityItem`/`EntityArrow` gravity | `generic.gravity` attribute (living) and Mixins on `getDefaultGravity` (`galacticraftcore.mixins.json`) — done |
| Replace `EntityPlayerMP`/`EntityPlayerSP` with GC subclasses | Not needed: player state lives in data attachments, logic in tick events |
| Fog/sky colour, lightmap, rain strength hooks | `DimensionSpecialEffects`, `ViewportEvent.ComputeFogColor`, biome effects |
| `ModelBiped` rotation (oxygen gear on player) | `RenderLivingEvent` / render layers for worn gear |
| Footprints on the Moon (`ParticleManager` / `EntityRenderer` hook) | Server-side tracker + payloads, decals drawn in `RenderLevelStageEvent` (`FootprintRenderer`) — done |
| Camera orientation in rockets | `ViewportEvent.ComputeCameraAngles` |
| `Chunk.populate` (block other mods' worldgen on GC planets) | Biome tags / biome modifiers (other mods target `#c:is_overworld` etc.) |
| `World.getRainStrength` | Biome `has_precipitation: false` |
| IC2/AE2/Optifine/SyncMod patches | Dropped (mods no longer exist on 1.21.1) |

### Known design risks

* **Space stations need dimensions created at runtime.** NeoForge 1.21.1 has no public API for adding levels
  while the server is running. Options: (a) a fixed pool of pre-declared station dimensions, (b) one shared
  "orbit" dimension where each station is a far-apart region (simplest, keeps saves small), or (c) a
  dynamic-dimension library. Done in Phase 6 with (b): station n lives at x = n x 4096 in `galacticraftcore:overworld_orbit`.
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
- [ ] Deferred: the full galaxy map (zoom, catalogue panels, space stations). (The Moon Buggy, its fueling pad and
      the schematic pages came in Phase 5b.)

### Phase 5a – The Moon as a place ✅
- [x] Moon caves (1.12.2 MapGenCavesMoon worm carver, through moon blocks only)
- [x] Moon sky (1.12.2 SkyProviderMoon): stars by day and night, the orbital sun, Earth fixed overhead, no clouds
- [x] Footprints on moon turf: alternate feet every 0.35 blocks, fade over 3,200 ticks, gone when the turf is dug up
- [x] Meteor showers (1 in 3,750 per tick per player for a small one, a third of that for a large one), meteor
      entity and model, impact explosion, hot Fallen Meteor block entity (cools over 5,000 ticks, glows orange, burns
      and pushes whatever touches it, falls like sand); `meteorSpawnMod` and `meteorBlockDamage` server config
- [x] Low gravity for dropped items (0.0046) and arrows (0.005) on the Moon

### Phase 5b – Things to fight and find ✅
- [x] Evolved Zombie, Skeleton, Spider, Creeper and Enderman (1.12.2 models with oxygen gear, converted by
      `tools/convert_java_model.py`; 1.12.2 health, speed and damage; common drops plus the rare player-kill drops),
      spawning on the Moon (weights 8/8/8/8/10), with spawn eggs
- [x] Moon dungeon (1.12.2 MapGenDungeon: one per 704x704 block region, a crater down to y 25, 10-25 corridors and
      rooms with spawners, cobwebs and loot chests, then the boss room, a low corridor and the treasure room)
- [x] Skeleton Boss (150 health × `dungeonBossHealthMod`, two bows, grabs and throws its target, boss bar, leaves
      when you leave its room, 200-tick death), its laugh/death/hurt sounds; it fills the locked Tier 1 Treasure Chest
      with `dungeon_tier_1` loot plus a Moon Buggy or Tier 2 Rocket schematic and drops the Tier 1 Dungeon Key
- [x] Schematics: per-player unlocked NASA Workbench pages (kept through death), Back/Next page buttons, the
      "Add New Schematic" page with its slot and Unlock Schematic button
- [x] Moon Buggy: workbench page (11 plates, a seat, 4 wheels, 0-3 storage boxes), item (0-3 boxes, fuel kept),
      entity drawn from the 1.12.2 OBJ (steering and spinning wheels, wobbling radar dish), W/S/A/D driving with the
      1.12.2 speeds and climbing, fuel use, 18 slots per storage box behind the inventory key
- [x] Buggy Fueling Pad: joins 3x3 like the launch pad (never with it); an attached Fuel Loader fills a parked buggy
- [x] Fire needs oxygen: burning entities on the Moon outside breathable air are put out when the flames first hurt
      them (1.12.2 EventHandlerGC#onEntityDamaged, missed in Phase 3)
- [ ] Deferred to Phase 5c: Moon villages and Alien Villagers (1.12.2 MapGenVillageMoon, its eleven building pieces
      and EntityAlienVillager trading for lunar sapphires). The Tier 2 Rocket schematic drops there; its page
      came with the Tier 2 Rocket in Phase 7a.

### Phase 5c – Moon villages
- [ ] Moon village structure and pieces, Alien Villager with its trades

### Phase 6 – Space stations ✅
- [x] Earth orbit dimension (`galacticraftcore:overworld_orbit`, 1.12.2 WorldProviderOverworldOrbit): empty space, the
      1.12.2 orbit sky (close stars, the sun and the Moon, the Earth below, smaller the higher you are), gravity 0.005,
      fall distance x0.4, no meteors, no air; solar panels x2 (`spaceStationEnergyScalar`) and lit for 5/6 of the day;
      rockets leave at y 750 and burn half as much fuel (the Moon's 0.7 fuel factor is now ported too); anything that
      drops below y 30 falls back to Earth (players under their parachute, above where they last launched from)
- [x] Space stations: built from the destination screen (Earth selected, from a rocket) for 32 tin, 16 aluminum,
      1 advanced wafer and 24 iron; one per player; the 1.12.2 starter station (tin hull, glass, the unbreakable Space
      Station Arrival Block); stations circle Earth on the screen and LAUNCH flies there (no lander: a para-chest with
      the cargo drifts down into the station's bay); owners rename them there; `disableSpaceStationCreation` config
- [x] Ownership and access: `/ssinvite <player>|+all|-all`, `/ssuninvite <player>`, `/ssnewowner <station#> <player>`
      (operators), and `/gchouston` to abandon a mission and go home (typed twice)
- [x] Zero-g freefall (1.12.2 FreefallHandler): a player touching nothing keeps drifting, Forward thrusts gently,
      Jump/Sneak move up and down, 0.7 blocks/tick at most; floating items and mobs keep their momentum; servers don't
      kick floating players for flying
- [x] Spin Thruster (1.12.2 BlockSpinThruster + SpinManager): fixed to the station, reversed with the wrench, off with
      redstone; the station's shape, centre of mass and moment of inertia come from a flood fill from its arrival
      block; unbalanced thrusters spin it up (balanced ones stop it); the sky turns and anything floating is carried
      round the centre, with the 1.12.2 centrifugal push against the walls
- [x] Circuit Fabricator zero-g bonus in orbit (5 basic wafers, 15 solar wafers, other wafers doubled)
- [ ] Deferred to Phase 6b: Space Race teams (team GUI, the flag designer, invites, shared stats)

### Phase 7a – Mars ✅
Ported from the Legacy repository's planets module (`micdoodle8.mods.galacticraft.planets.mars`); its assets come in
through the porter's `planets:` prefix (`assets/galacticraftplanets`).
- [x] Mars blocks (1.12.2 BlockBasicMars, Material.ROCK, hardness 2.2): copper, tin, iron and desh ores, Mars Fine
      Regolith (surface), Mars Regolith, Mars Stone (breaks to Mars Cobblestone, which smelts back), Mars Cobblestone,
      Mars Dungeon Brick (hardness 4, resistance 40), Block of Desh (resistance 60); cobblestone and dungeon brick stairs,
      slabs and walls; ore and stone harvest levels as in 1.12.2 (desh ore and dungeon bricks need diamond)
- [x] Cavernous Vines (1.12.2 BlockCavernousVine): grow down from a solid ceiling, the lowest vine glowing at 15 and
      each above it 4 less, entangle and poison mobs (never fatally), climbable, collected only with shears
- [x] Bacterial Sludge fluid, block and bucket (density 800, viscosity 1500, glowing)
- [x] Desh: Unrefined Desh (from desh ore, Fortune as in 1.12.2: 4/33/62% for a second), Desh Ingot, Desh Stick,
      Compressed Desh, Heavy-Duty Plate (compressor), Block of Desh; Desh pickaxe/axe/shovel/hoe/sword (harvest level 4 =
      netherite's tier, 1024 uses) and the Sticky Desh Pickaxe; Desh armour (4/7/9/4, toughness 3, its three 1.12.2
      texture sheets); smelting, compressing and crafting recipes from RecipeManagerMars and the planets JSON recipes
- [x] The Mars dimension (`galacticraftcore:mars`): ChunkProviderSpace terrain with ChunkProviderMars's settings
      (plains, hills, valleys and mountains around y 93, a few large craters), fine regolith over regolith over Mars
      stone, MapGenCaveMars worm caves, MapGenCavernMars caverns (80 blocks across at y 17-33, vines from the ceiling,
      sludge on the floor), BiomeDecoratorMars ores, regolith pockets and ice; the evolved mobs spawn there
- [x] Mars physics (WorldProviderMars): gravity 0.058, fall distance x0.38, arrows 0.015, meteors 10, rockets burn
      0.9x fuel, solar panels x0.512, no air; beds can be slept in but don't set a spawn point (1.12.2 canRespawnHere)
- [x] The Mars sky (SkyProviderMars, WorldProviderMars): a butterscotch sky and orange fog that only dim to a quarter at
      night, stars at night, a smaller sun in a pale pink haze, the Earth as a dot, no clouds
- [x] Tier 2 Rocket (EntityTier2Rocket): 1,500 mB, climbs twice as fast as the Tier 1 and burns 1 mB every 2 ticks;
      the OBJ model; 0-3 chests of cargo; reaches Mars (tier 2) as well as the Moon and orbit. The NASA Workbench's Tier 2
      page (nose cone, 10 heavy-duty plates, 2 boosters, 4 fins, engine, chests) is unlocked by the Tier 2 Rocket
      Schematic from Moon dungeons
- [x] Landing Balloons (EntityLandingBalloons, TeleportTypeMars): arrival on Mars from y 900, falling a fiftieth of the
      height each tick, under a parachute above y 500, then 14 bounces; afterwards they open as a chest with the cargo
- [x] Mars on the destination screen, locked for Tier 1 rockets

### Phase 7b – Mars mobs and dungeon
- [ ] Slimelings (eggs, taming, the Slimeling inventory and its cargo item; making the Sticky Desh Pickaxe from an egg)
- [ ] Sludgelings (spawned from bacterial sludge), Creeper Eggs
- [ ] The Mars dungeon (MapGenDungeonMars, RoomBossMars/RoomTreasureMars), the Creeper Boss, the Tier 2 Treasure Chest
      and Tier 2 Dungeon Key, the dungeon bricks' sludge drips

### Phase 7c – Mars machines and the cargo rocket
- [ ] Terraformer, Cryogenic Chamber, Launch Controller, Gas Liquefier, Methane Synthesizer, Water Electrolyzer
- [ ] Cargo Rocket and its schematic, Fluid Manipulator, Fragmented Carbon

### Phase 7d+ – More planets
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
* Moon day length is vanilla's. 1.12.2 used an 8× longer day, which data-driven dimension types can't express. Like
  every non-Overworld world, the Moon also shares the Overworld's clock.
* Added for modern parity: blasting, smoker/campfire and stonecutter recipes, `c:` tags, and the Heavy Duty set
  in the enchantability tags.
* Both tin stair and slab variants keep their duplicate 1.12.2 English names ("Tin Decoration Stairs").
* Energy is moved only through Galacticraft wires and direct contact, like 1.12.2; foreign FE cables can
  connect to any machine face that takes or gives power.
* Coal Generator heat now survives a world reload (1.12.2 saved and loaded it under different NBT keys).
* Compressor recipes for steel/bronze ingots load only when some mod puts items in `c:ingots/steel` or
  `c:ingots/bronze` (the 1.12.2 OreDictionary check). Hard/quick/adventure mode recipe variants are not ported.
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
* The destination screen is a simplified galaxy map: the Sun, Earth, the Moon and Mars on their orbits with the 1.12.2 icons,
  frame and LAUNCH button. Zooming, the planet catalogue panels and the space station list come with later phases.
* The NASA Workbench recipes (Tier 1 Rocket, Moon Buggy) are hard-coded as in 1.12.2 (`RecipeManagerGC`), so they are
  not JSON recipes and don't show in JEI yet. Page ids are names (`rocket_t1`, `moon_buggy`, ...) instead of the
  configurable 1.12.2 numbers, and pages are opened by the server (1.12.2 let the client ask for any page id). A
  schematic whose page doesn't exist yet (Cargo Rocket, Tier 3 Rocket, ...) is not used up by Unlock Schematic.
* Rocket types 0-3 (18/36/54 cargo slots) are a `rocket_storage` data component on one item instead of item damage; the
  creative "pre-fuelled" rocket is a rocket item with a full `rocket_fuel` component.
* The Para-chest's colour is a block state (16 colours) instead of tile-entity data, so its model needs no tile renderer.
* Rocket and lander exhaust uses vanilla flame and smoke particles instead of 1.12.2's custom flame particles.
* The Fuel Loader takes power on its left face and fuel on its right; the sneak + wrench side configuration of 1.12.2
  is not ported for it yet. Fuel Loaders take any fluid in the `c:fuel` tag.
* The Buggy Fueling Pad is its own block (`buggy_fueling_pad`, 1.12.2 `landing_pad:1`); launch pads and fueling pads
  next to each other never join into one pad.
* The Moon Buggy is steered with the normal movement keys (1.12.2 had separate key bindings defaulting to the same
  keys) and its storage uses the Spaceship Inventory key. Like 1.12.2 the driver's client moves it; it now rolls to
  a stop on the ground when out of fuel (1.12.2 coasted). The 1.12.2 radar dish had no texture (drawn white); it uses
  white concrete. The item's hand and GUI transforms approximate the 1.12.2 matrices.
* Evolved Zombies don't call reinforcements (vanilla would spawn plain zombies), don't drown into anything, and Evolved
  Skeletons don't freeze into strays. The Evolved Witch (Mars) and 1.12.2 "challenge mode" drops are not ported.
* The Skeleton Boss's treasure goes into the treasure chest its dungeon was built with (1.12.2 searched for the
  nearest Tier 1 chest within 1,000 blocks; that search is kept, within 32 blocks, as a fallback). The boss respawns
  15 minutes after a kill when a player enters again. Its rare bow drop is a plain bow (1.12.2 enchanted it when the
  kill used Looting).
* All of Earth's space stations share one orbit dimension: station n is at x = n x 4096 (2,048 blocks of room each way)
  instead of in its own dimension at 0, 64, 0. Station data lives in one `galacticraftcore_space_stations` file.
  Stations are only built around Earth so far (1.12.2 had one station type per planet).
* The Space Station Arrival Block is a single block (1.12.2 stacked two invisible blocks on top of it); arriving players
  stand beside it on the room floor.
* The spin maths (mass centre, moment of inertia) is done relative to the arrival block in doubles: 1.12.2's float
  sums of absolute coordinates fail at the distances stations sit at here. In freefall, the reach towards the wall a
  player faces now points the way they look (1.12.2's east/west reach pointed backwards).
* Zero-g "jetpack" boosting (1.12.2 turned a jump while drifting into a push along the look direction) is not ported.
* `/gchouston` asks for confirmation for 250 ticks after the first use, like 1.12.2, and clears the rocket cargo kept
  in transit.
* Dungeons need "Generate Structures" like any structure; `/place structure galacticraftcore:moon_dungeon` places one.
  Without terrain adaptation the dungeon digs its own crater, as in 1.12.2.
* Footprints are tracked only on the server and sent to players who can see them (1.12.2 also drew your own prints
  on your own client). They are still not saved. Prints sit on top of the turf; 1.12.2's server put other players'
  prints one block lower.
* The Moon sky leaves out 1.12.2's live picture of the Overworld (`S_REQUEST_OVERWORLD_IMAGE`) and always shows the
  Earth icon. The "more stars" client option is not ported.
* Meteors leave a smoke trail of vanilla large smoke particles (1.12.2 had its own "distance smoke"); meteors spawn
  within the simulation distance rather than the view distance so that they actually move.
* The Fallen Meteor's mining speed bonus for better pickaxes (1.12.2 divided by 30 instead of 100) is not ported.
* Mars blocks have their own ids (`mars_fine_regolith`, `desh_ore`, ...) under the `galacticraftcore` namespace instead of
  the planets mod's `galacticraftplanets:mars` metadata block; recipes from the planets JSON files are converted too.
* Mars Fine Regolith and Mars Regolith keep 1.12.2's Material.ROCK: they need a shovel to drop, and sound like stone.
* Desh tools are repaired with Desh Ingots like the armour (1.12.2 gave the tools no repair item).
* The Sticky Desh Pickaxe exists, but making it (from a Slimeling Egg) and its use (Creeper Eggs) come with Phase 7b.
* Mars's day is vanilla's length (1.12.2: 24,660 ticks). Mars's sky colour hooks `ClientLevel#getSkyColor` (a client
  mixin) so the sky dome and vanilla's fog blending both get the 1.12.2 colours; 1.12.2's black square behind the sun
  was drawn with additive blending and hid nothing, so it is left out.
* MapGenCavernMars carved the same ellipsoid on each of its ~50 steps and kept only the last step's vines and sludge;
  the port carves and decorates once (same caverns). Sludge pools are ticked once after generation so they settle.
* ChunkProviderSpace re-tuned two noise generators between terrain passes; they are split into fixed-frequency copies
  with the same seeds. Craters only dug down from y 127, so in taller columns they are hidden pockets, as in 1.12.2.
* The Tier 2 Rocket's flames are vanilla flame, soul-fire flame and smoke particles. Its item is drawn with the OBJ
  model at a fixed angle (1.12.2 slowly turned the icon in the inventory).
* Landing balloons are simulated on the server (1.12.2 let the rider's client move them).
* Cavernous Vines keep a `hanging` count (vines from here down, up to 5) in their block state, which sets their glow:
  1.12.2 counted the strand on every light query, but a 1.21 light engine thread that reads other blocks can deadlock
  chunk generation. Generated strands are recounted once all carvers have run.

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
