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
| JSON recipes with `forge:ore_dict`/metadata | Converted by `tools/port_legacy_assets.py` | all 238 converted (the alternate canister and Actually Additions recipes, and copper blocks, are skipped) |
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
| Forge `Configuration` .cfg + config GUI | `ModConfigSpec` (COMMON/SERVER/CLIENT; SERVER configs sync automatically), NeoForge's config screen in the mod list | done (see Settings) |
| TESRs (18), entity renderers (~33), Forge OBJ loader | `BlockEntityRenderer`, `EntityRenderer` with `LayerDefinition`s converted from the 1.12.2 `ModelRenderer` code (`tools/convert_java_model.py`), NeoForge OBJ loader (`neoforge:obj`), `BlockEntityWithoutLevelRenderer` for item models | solar panel, fluid tank/pipe, rocket, lander, para-chest, NASA Workbench done |
| Forge fluids (oil, fuel, oxygen, ...) | `FluidType` + `BaseFlowingFluid`, `FluidStack`, `IFluidHandler`, `IClientFluidTypeExtensions`; oxygen is a block-less `GasFluid` | oil, fuel, oxygen done |
| Commands (12) | Brigadier via `RegisterCommandsEvent` (`GCCommands`, `GCAdminCommands`) | 11 done; `/gckeeploaded` not needed |
| Advancements (28) + `GCTriggers` | 1.21 advancement JSON (datagen, `GCAdvancementProvider`) + `TRIGGER_TYPE` registry (`GCTriggers`, vanilla `PlayerTrigger`) | done |
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
- [x] Later, and done in later phases: parachute deployment (phase 4), thermal padding and heat (7d), the frequency
      module, Communications Dish and Sensor Glasses (7g), the Arc Lamp's bright air (7g)

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
- [x] The full galaxy map came in phase 9 part 4. (The Moon Buggy, its fueling pad and the schematic pages came in
      Phase 5b.)

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
      spawning on the Moon (weights 8/8/8/8/10), with spawn eggs; the Evolved Witch (an Alien Villager struck by
      lightning) followed in phase 9
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
- [x] Moon villages and Alien Villagers: see Phase 5c. The Tier 2 Rocket schematic drops there; its page came with the
      Tier 2 Rocket in Phase 7a.

### Phase 5c – Moon villages ✅
- [x] Moon villages (MapGenVillageMoon, StructureVillageStartMoon, StructureVillagePiecesMoon): the pre-1.14 vanilla
      village layout 1.12.2 copied, with its weights and limits (wood huts, houses, sapling fields and crop fields, a
      lamp post when nothing else fits), plank paths 7-21 blocks long and a range of 112 blocks from the well. The well,
      paths, lamp posts (StructureComponentVillageTorch), fields (Field, Field2) and the domed houses (House, WoodHut)
      are built block for block, each settling onto the average ground height of the first chunk that builds it
- [x] Alien Villager (EntityAlienVillager, ModelAlienVillager, RenderAlienVillager): four to a house; breathes without
      air, never despawns, keeps away from zombies, picks up food; trades only with a player wearing a Frequency Module,
      for Lunar Sapphires: oxygen gear, a part-filled tank, a fuel canister and battery, a parachute, a wafer, the Moon
      Buggy / Tier 2 Rocket schematic swap, compressed copper and tin, canned carrots, and a sapphire for jungle
      saplings; it restocks after trading and turns into a witch when struck by lightning; spawn egg

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
- [x] Space Race teams: done in Phase 6b

### Phase 6b – Space Race teams ✅
- [x] Teams (SpaceRaceManager, SpaceRace, WorldDataSpaceRaces): one per player, made the first time they open the Space
      Race Manager; a name (up to 25 characters), a 48x32 flag and a team colour, all set by the owner; saved with the
      world and sent to every player
- [x] Space Race Manager (GuiNewSpaceRace), from the box in the pause menu's corner (and, with
      `enableSpaceRaceManagerPopup`, once at first login): the flag and colour, renaming, Add Player(s) (players within
      25 blocks; invited names turn orange for a minute), Remove Player(s), and "View Server / Global Stats", which say
      "Coming Soon" as in 1.12.2; changes go to the server every 5 seconds and on closing
- [x] Flag designer: paintbrush and eraser (radius 1-11), a rectangle selection to paint within, a colour picker, a
      grid, red/green/blue sliders and the paint colour; the team colour page with its own sliders
- [x] Invitations and joining: the invited player gets the 1.12.2 chat message and joins with `/joinrace`
      (GuiJoinSpaceRace: flag, name, owner, size, Join); removed members get a team of their own
- [x] Shared schematics (SpaceRaceManager#teamUnlockSchematic): a page one member unlocks is unlocked for the team,
      for offline members at their next login
- [x] The team's clock (time with a member online) and the first visit to each world, kept as 1.12.2 kept them
- [x] Flag (ItemFlag, EntityFlag, RenderFlag, ModelFlag): hold right-click about a second, let go looking at a block;
      one flag per block; it flies its owner's team flag and ripples with the world's wind (Overworld 1, Mars 0.3,
      orbit 0.1, asteroids 0.05, the Moon still); right-click turns it, hits break it (creative at once), it falls over
      air; the OBJ item model with its green default flag; recipe from steel poles and canvas
- [x] The Tier 3 Rocket's nose cone in the viewer's team colour (white without a team)

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

### Phase 7b – Mars mobs and dungeon ✅
- [x] The Mars dungeon (MapGenDungeonMars with ChunkProviderMars's DungeonConfiguration): the Moon's generator, now
      driven by a per-planet `DungeonConfig` (Mars dungeon brick, y 30, corridors 7 high, rooms 7 high), one per 704x704
      area in Mars biomes, under its own crater. RoomBossMars is a 24x11x24 hall of brick with a two-high doorway,
      glowstone in the ceiling near the walls (1 in 4) and Creeper Eggs along the walls (1 in 6); RoomTreasureMars holds
      the locked Tier 2 Treasure Chest; chest rooms and the treasure chest roll 1.12.2's `dungeon_tier_2` loot
- [x] The Creeper Boss (EntityCreeperBoss): 200 health (times the boss health config), a yellow boss bar, three heads
      that drop to two at two thirds and one at one third of its health, TNT lobbed every 25 ticks at players within 20
      blocks. Only fireball damage hurts it, so its own TNT (EntityProjectileTNT: 6 + 3 per difficulty step to what it
      hits, an explosion of power 1) has to be punched or shot back. Its spawner (TileEntityDungeonSpawnerMars) plays
      the scary-scape sound and clears evolved skeletons, zombies and spiders from the room. Beaten, it fills the Tier 2
      Treasure Chest with loot and a Tier 3 Rocket or Cargo Rocket schematic (1.12.2's rules about schematics you already
      have, and the Astro Miner, included) and drops the Tier 2 Dungeon Key; a rare bow as before
- [x] Creeper Eggs (BlockCreeperEgg): fall like dragon eggs but never teleport; only the Sticky Desh Pickaxe breaks
      (in 5 ticks) and collects them; an explosion hatches a baby Evolved Creeper (half size, faster, 2.5x the XP)
- [x] Bacterial Sludge spawns Sludgelings (EntitySludgeling: 7 health, speed 1.0, arthropod, silverfish sounds, no drops)
      when anything but a flying player or a sludgeling wades in, up to three within five blocks; it drips through the
      floor below it (ParticleDrip) and bubbles like lava now and then. Mars dungeon bricks drip and make the single-drop
      sound
- [x] Slimeling Eggs (BlockSlimelingEgg, TileEntitySlimelingEgg) on the fine regolith of Mars (WorldGenEggs: two tries a
      chunk) in red, blue and yellow. Using one (or hitting it with anything but a pickaxe) cracks it, and a tame
      Slimeling of its colour hatches for that player 0-70 ticks later. A diamond-strength pickaxe collects the egg; a
      Desh Pickaxe with Silk Touch becomes a Sticky Desh Pickaxe (keeping its wear and enchantments)
- [x] Slimelings (EntitySlimeling): wild ones are tamed with slime balls (1 in 3); they sit, follow, fight for their
      owner (and hunt sludgelings while wild), grow from half to full size over 100,000 ticks with 20-50 health, and
      count their kills. Their panel (GuiSlimeling: colour, name, owner, kills, growth, attack, favourite food, Sit /
      Follow), the feeding screen after a meal of the favourite food (GuiSlimelingFeed: grow, breed, strengthen, heal)
      and the inventory (GuiSlimelingInventory) with the Slimeling Inventory Bag, which carries 27 stacks and spills them
      when taken out. Bred slimelings mix their parents' colours by hue (ColorUtil#addColorsRealistically)
- [x] Tier 2 Treasure Chest and Tier 2 Dungeon Key; the Tier 3 Rocket, Cargo Rocket and Astro Miner schematics (their
      workbench pages come with Phases 7c and 7d); spawn eggs for the Creeper Boss, Sludgeling and Slimeling

### Phase 7c – Mars machines and the cargo rocket ✅
- [x] Gases and liquids (1.12.2 FluidGC hydrogen, methane, atmospheric gases, carbon dioxide, nitrogen, argon, liquid
      oxygen / nitrogen / argon), held in tanks only; Mars air is CO2, argon and nitrogen, the Moon has none
      (WorldProviderMars / Moon `getCelestialBody().atmosphere`)
- [x] Canisters: the Methane, Liquid Oxygen and Liquid Nitrogen canisters (ItemCanisterMethane, ...LiquidOxygen,
      ...LiquidNitrogen) beside the Oil and Fuel canisters, with their 7 fill stages; Atmospheric Valve, Carbon
      Fragments, Fluid Manipulator
- [x] Water Electrolyzer (TileEntityElectrolyzer: 1 water makes 2 oxygen and 4 hydrogen, 120 gJ/t), Methane
      Synthesizer (TileEntityMethaneSynthesizer: hydrogen with Carbon Fragments, 40 uses each, or CO2 from Mars air
      through an Atmospheric Valve; 45 gJ/t) and Gas Liquefier (TileEntityGasLiquefier: methane to fuel, oxygen to
      liquid oxygen, nitrogen to liquid nitrogen, or the local air mix through a valve; 60 gJ/t), with their GUIs, side
      I/O (power from below, gases in on the left, out on the right / back) and Process / Stop
- [x] Terraformer (TileEntityTerraformer): with power, water, bone meal and seeds it grows a green bubble 15 blocks
      across and turns the Mars fine regolith and Moon turf inside it into grass (now and then a pool of water), and
      plants saplings (or any plant) on the grass, using up bone meal, seeds and saplings at the 1.12.2 rates; its GUI
      with Enable / Disable Trees and Grass and Show Bubble
- [x] Cryogenic Chamber (TileEntityCryogenicChamber, the cryo_chamber multiblock): a 3-high glass tube to sleep in by
      day or night, standing up; a full sleep heals 5 health and starts a 6,000 tick cooldown
      (EventHandlerMars#onPlayerWakeUp); it doesn't set the spawn point
- [x] Cargo Loader and Cargo Unloader (TileEntityCargoLoader / Unloader): beside a launch pad (or buggy fueling pad),
      one item every 15 ticks (9 on tier 2 power) into or out of the rocket or buggy on it, 45 gJ/t
- [x] Launch Controller (TileEntityLaunchController, GuiLaunchController / Advanced): owned by whoever placed it, with
      a frequency, a target frequency (hidden from others), Enable / Disable, and on its advanced page Remove Pad On
      Launch and Auto Launch When (cargo unloaded / cargo full / fully fuelled / instantly / 10 s / 30 s / 1 minute /
      redstone signal); it keeps its chunk loaded so rockets can find it
- [x] Cargo Rocket (EntityCargoRocket on EntityAutoRocket) with its NASA Workbench page (SchematicCargoRocket):
      18 / 36 / 54 slots and 2,000 mB, launched by the Launch button of its cargo GUI or by its controller's rule. It
      climbs (up to 5 / (1 + 0.1 per filled slot) blocks a tick), and on reaching space it comes down 800 blocks above
      the pad of the controller with the target frequency, on whichever world that is, and lands on it, slowing as it
      nears; 1 mB of fuel every 3 ticks, landing included; without fuel it falls and crashes

### Phase 7d – The asteroid belt ✅
- [x] Asteroid blocks (BlockBasicAsteroids): three asteroid rocks, asteroid aluminium and iron ores, ilmenite ore (an
      iron and a titanium shard, fortune-boosted, or the ore with Silk Touch), the dark decoration block and the
      titanium block; Dense Ice (BlockIceAsteroids: slippery, Silk Touch only, gone without water on Galacticraft worlds)
      and Bright Air (BlockBrightAir)
- [x] Titanium (ItemBasicAsteroids): ingots, shards, dust, compressed titanium, the Tier 3 heavy-duty plate, heavy
      rocket engine and fins, heavy nose cone, thermal cloth, beam core, Orion drive; titanium tools and armour
      (ItemPickaxeAsteroids..., ItemArmorAsteroids), with their smelting, compressor and crafting recipes
- [x] The asteroid belt (WorldProviderAsteroids, `galacticraftcore:asteroids`): ChunkProviderAsteroids's asteroids of
      5-25 blocks between y 48 and 208 in empty space, skewed by three noise fields and roughened by turbulence, with a
      core (rock, aluminium, ilmenite, iron, silicon, meteoric iron, diamond) and half the time a shell (rock or Dense
      Ice); one big one in ten is hollow, with grass, dirt, glowstone, trees, flowers and pools inside a Dense Ice shell;
      single rocks and ores float between them; Bright Air lights the faces turned towards the sun. Black sky
      (SkyProviderAsteroids: small stars, a smaller sun fixed on the horizon, the Earth a speck), no air, gravity 0.072
      below vanilla, falls a tenth as bad, arrows 0.002, solar 0.77; evolved mobs; tier 3 only
- [x] Tier 3 Rocket (EntityTier3Rocket, ItemTier3Rocket, RenderTier3Rocket with its blinking light) and its NASA
      Workbench page (SchematicTier3Rocket), unlocked with the Creeper Boss's schematic; 1,500 mB, up to 3.5 blocks a
      tick (5 in a vacuum)
- [x] Entry Pod (EntityEntryPod, TeleportTypeAsteroids): from y 310 onto the nearest asteroid not yet landed on, within
      160 blocks of the take-off point, through a shaft cut in the rock above it (or onto a small platform made for
      it); it opens as a chest with the rocket's cargo
- [x] Thermal Padding (ItemThermalPadding) in the four thermal gear slots, worn on the player with its pulsing glow
      (LayerThermalPadding), and body temperature (GCPlayerHandler#checkThermalStatus): Mars (-1) and the asteroids
      (-1.5) chill you down to -22, slowed by each piece of padding and held off by a full set or a sealer's Ambient
      Thermal Controller; at the limit it hurts, below -15 it slows you; the HUD thermometer and its warning

### Phase 7e – Asteroid machines ✅
- [x] Walkways (BlockWalkway, plain, with an Aluminium Wire or a Fluid Pipe through it) with their 1.12.2 model pieces:
      rails on the sides that meet no walkway or solid face
- [x] Space Wart (BlockSpaceWart, nether wart rules) and the two Strange Seeds (treasure of the abandoned bases)
- [x] Grapple (ItemGrapple, EntityGrapple): draw and let go to fire a hook on a string from your inventory; it reels
      you in once it holds, and the string comes back; 384 uses
- [x] Small asteroids (EntitySmallAsteroid) drifting past players in the belt, one in 50 ticks, 30-89 blocks off
- [x] Beam power (TileEntityBeamReflector, TileEntityBeamReceiver): a receiver on a machine's energy output beams it
      to the nearest node within 15 blocks, through reflectors (right-click cycles the target), into a receiver on an
      energy input (1,500 gJ a tick from other mods' machines); the turrets aim at their targets and the beams spin
- [x] Short Range Telepads (BlockShortRangeTelepad): a 3 x 3 pad under a canopy, an address and a target address (saved
      with the world, unique on the server), 256 blocks' range on the same world; whoever stands on it 150 ticks goes to
      the target pad for 2,500 gJ at each end; GUI, owner, enable / disable, particles
- [x] Astro Miner Base (BlockMinerBase / BlockMinerBaseFull): eight blocks in a cube join into the base, turned with the
      wrench; the dock GUI with its 72-slot store, battery, status and Recall; the miner charges from it and unloads
      into it
- [x] Astro Miner (EntityAstroMiner, ItemAstroMiner, RenderAstroMiner, SoundUpdaterMiner): docked from the item (at
      most `maximumAstroMiners` per player, not on space stations), it flies out to the nearest asteroids (straight
      ahead elsewhere), mines tunnels in its 1.12.2 pattern into its 227-slot hold, returns to unload and recharge, and
      keeps its chunk loaded; lasers, scanning glow and engine hum; its NASA Workbench page and schematic
- [x] Abandoned asteroid bases (MapGenAbandonedBase, BaseDeck, BaseRoom, BaseHangar, BasePlate): one per 576 x 576
      blocks, built into the nearest asteroid in one of three alien designs (humanoid, avian, tunneler): three decks of
      corridors and rooms, or two over a 26 x 42 hangar open to space; engineering rooms with NASA Workbenches and the
      Moon Buggy and Tier 2 Rocket schematics hung on the wall, power rooms of storage modules and wires, a store with a
      launch pad and a locked Cargo Loader holding the base's treasure (ghast tears or a Strange Seed, freed by loading
      it into a rocket), medical rooms with tanks of Bacterial Sludge and brewing stands of poison, crew quarters,
      control rooms, Cryogenic Chambers, and Space Wart growing in the empty rooms
- [x] Hanging schematics (EntityHangingSchematic): any schematic hangs on a wall like a 2 x 2 painting and drops again
- [x] Locked Cargo Loaders (TileEntityCargoLoader#locked), and the Dark Decoration Slab (BlockSlabGC asteroids_slab,
      missed in phase 7d)

### Phase 7f – Venus ✅
- [x] Venus blocks (BlockBasicVenus): soft, hard and volcanic rock and pumice (turned at random), two dungeon bricks,
      aluminium, copper, galena, quartz, silicon, tin and solar ores, the lead block; Scorched Venus Rock (smoking); the
      Vapor Spout over acid pits; Web String and the glowing Web Torch; the Crashed Probe with six slots of salvage loot
      and, if it really crashed, a Radioisotope Core
- [x] Sulphuric Acid (BlockSulphuricAcid): a fluid that burns whatever wades in it, with a bucket
- [x] Venus items: lead, solar dust, the solar array wafer and panel, the Radioisotope Core, the Atomic Battery (7 gJ/t,
      never runs down), the Shield Controller, Isothermal Fabric and Isothermal Padding (thermal strength 2), and the
      Volcanic Pickaxe that mines 3 x 3
- [x] Venus (WorldProviderVenus, ChunkProviderVenus, `galacticraftcore:venus`): the 1.12.2 density field over three
      biomes (flat, mountain, valley) laid out by the 1.12.2 biome layers; worm caves, sealed lava tubes, each biome's
      surface; lava lakes, acid vapor pools under vapor spouts, crashed probes and the seven ores
- [x] Venus's own weather (updateWeatherOverride): acid rain spells that hiss on the ground, distant lightning, the
      sulphur sky and fog; scorching heat (thermal level 5); armour corrodes a point a second unless a Shield Controller
      is worn (GCPlayerHandler#checkShield); beds don't set your spawn; Venus physics (gravity, solar ×2.37, fuel)
- [x] Travel by Tier 3 rocket in the Venus Entry Pod (EntityEntryPodVenus): a fiery plunge from y 900, a parachute from
      y 242, and the rocket's cargo in the landed pod
- [x] Venus dungeons (MapGenDungeonVenus, RoomBossVenus, RoomSpawnerVenus, RoomTreasureVenus) from the shared dungeon
      planner: corridors with lava behind iron bars, spawner rooms, a domed boss room hung with web strings, and the
      Tier 3 treasure chest opened by the Tier 3 key
- [x] The Spider Queen (EntitySpiderQueen): web shots that slow, a web cocoon at two thirds and one third of her health
      while up to six Juicers fight for her, the Tier 3 key and a Volcanic Pickaxe or Shield Controller; the Juicer
      (EntityJuicer), leaping between dungeon floors and ceilings; the Web Shot (EntityWebShot)
- [x] Geothermal Generator (TileEntityGeothermalGenerator): on a Vapor Spout with acid under it, 30-200 gJ/t on a sine
      wave, lit vents and acid exhaust while it works; GUI with the flickering flame and Enable / Disable
- [x] Solar Array Module and Controller (TileEntitySolarArrayController, SolarModuleNetwork): the controller gathers the
      modules joined side by side to its left, right and back, and makes 6.3 gJ/t for each one within 16 blocks that sees
      the sun, up to 1,000 gJ/t, more on space stations and high up on Venus; GUI with Enable / Disable

### Phase 7g – Core blocks still missing, and the Laser Turret ✅
- [x] Space Glass (clear, vanilla, strong; plain or tin frame): 1.12.2's connected models and shapes, sealing on its
      glass sides only (a `PartialSealable` block, which the seal finder and oxygen checks now ask)
- [x] Air Lock Frame, Controller and Seal: 1.12.2's frame protocol (a closed ring up to 24 blocks round, upright or
      flat), redstone, player-distance and inverted modes, the owner's settings screen and the seal sounds
- [x] Arc Lamp (a lit cone of bright air that scares mobs off, off with redstone), Concealed Redstone, Repeater and
      Detector, the sealable aluminium wires and fluid pipe, Grating and the five Lighting Panels (the wrench turns them)
- [x] Sensor Glasses: the visor, markers for ores within 4 blocks (K: Advanced Mode), mobs drawn glowing, footprints
      full bright, and the leak of the nearest unsealed Oxygen Sealer shown as a stream of oxygen
- [x] Magnetic Crafting Table: keeps its grid, remembers the last recipe crafted, lets hoppers fill it to that recipe
      and take the results; JEI moves recipes into it
- [x] Compact NASA Workbench (and back), Telemetry Unit and frequency module links, Display Screens (six pictures,
      joined up to 9 blocks a side) and the Communications Dish
- [x] The abandoned asteroid bases use the real blocks (no stand-ins left)
- [x] Deconstructor (TileEntityDeconstructor): takes things apart through their crafting recipes and the NASA
      Workbench pages, down to the metals and parts on 1.12.2's salvage list, each piece back three times in four
- [x] Chromatic Applicator (TileEntityPainter): mixes dyes (or a block's colour) and paints the three kinds of Space
      Glass and the Lighting Panels for the players near it
- [x] Hydraulic Platform: four join into a lift platform; jump to rise to the next platform above, sneak to sink
- [x] Emergency Post (lights up the air round it, keeps the evolved mobs off) and the Emergency Kit inside it, which
      puts on a mask, gear, tanks and a parachute and hands over the rest
- [x] Throwable Meteor Chunk (hot and cold, thrown or fired from a dispenser) and the Prelaunch Checklist book
- [x] Laser Turret (TileEntityLaserTurret, Venus): charges for 3 seconds and fires at the meteors and living things
      around it, sparing its owner, their team and pets; whitelist or blacklist of players and entity types, target
      priorities, line of sight; its GUI, list and priority screens; the turning turret, charge orb and beam. As in
      1.12.2 it has no recipe (creative menu only)

### Phase 8 – Polish ✅
- [x] The 28 advancements (`galacticraftcore:galacticraft/...`, same tree, icons and criteria) with 1.12.2's three
      Galacticraft criteria: launch a rocket, find the Moon's Skeleton Boss, build a space station
- [x] Sounds: the rocket engine from ignition to the edge of space, a parachute opening, the Electric and Advanced
      Compressors' clank; the space music in every Galacticraft world (see below for the `*_jc.ogg` tracks)
- [x] Optional Jade (energy in gJ, progress, status, owner) and EMI (Compressor, Circuit Fabricator and Refinery pages,
      workstations, Magnetic Crafting Table filling) plugins, beside the JEI one
- [x] The manual: nothing to port. 1.12.2 never loaded `manuals/gettingstarted.xml`, a placeholder whose pages hold
      another mod's tool text

### Phase 9 – The rest ✅
- [x] Evolved Witch (EntityEvolvedWitch): what an Alien Villager becomes when struck by lightning, in an oxygen mask with
      tanks (1.12.2 ModelEvolvedWitch), breathing anywhere, a witch's drops plus rare Galacticraft gear; spawn egg
- [x] The rest of 1.12.2's commands: `/gckit`, `/gcinv save|restore|drop|clear`, `/dimensiontp`,
      `/gcastrominer show|reset|set<n>`, `/gcenergyunits gJ|FE|MJ` (MJ with BuildCraft Reborn), `/gchelp`
      (`/gckeeploaded` has nothing to do in 1.21)
- [x] The 1.12.2 settings (see Settings below): quick and hard mode, suffocation, oil and ore generation, moon
      villages, rocket fuel, launch worlds, rocket explosions, landers, landing borders, respawning, station permissions,
      sealer edge checks, liquids in gratings, Launch Controller chunk loading, Venus lightning, and the client's stars,
      rocket particles, vehicle camera and oxygen gauge placement; a settings screen in the mod list
- [x] The full galaxy map (GuiCelestialSelection): the Sol system on a tilted grid with its orbits and the asteroid
      belt, Mercury, Jupiter, Saturn, Uranus and Neptune as places that can't be reached yet; zoom with the wheel, pan by
      dragging or with the movement keys; click (or pick from the sliding catalogue) to select, again to zoom in on a
      planet's moons and stations; the tier box, LAUNCH, BACK, the station list with its owner and RENAME dialog, and the
      station materials panel with CREATE. The Galaxy Map key (M) opens it as a map only, with EXIT. (Galacticraft
      Legacy's map has no Space Race panel.)

### Phase 10 – Last gaps ✅
- [x] "Press Shift for more info" (1.12.2 ItemBlockDesc / ItemDesc) on the 80 blocks and items 1.12.2 described, from
      its own texts in every language: the text wrapped to 150 pixels while the sneak key is held, otherwise the prompt
      after "Power required" for machines that draw power (the gJ a tick they use, times 20). One tooltip handler for
      all of them; the descriptions some blocks showed all the time before are behind Shift now, as in 1.12.2
- [x] Dungeon Locator (1.12.2 "dungeonfinder", creative only): a compass whose needle swings to the nearest dungeon
      (on the asteroids, the nearest abandoned base), its 32 frames split from 1.12.2's strip by the porter
- [x] Infinite Battery (creative only): gives a machine all it can take, as tier 3 power, and never runs down
- [x] NASA Workbench recipes in JEI and EMI: a page for each workbench page (Tier 1, 2 and 3 Rockets, Moon Buggy,
      Cargo Rocket, Astro Miner) drawn on a crop of its own GUI, every part slot showing what it takes, one recipe for
      each number of chests; the slot layouts are the menus' own. Also 1.12.2's Oxygen Compressor, Gas Liquefier and
      Methane Synthesizer pages
- [x] Zero-g jetpack boost (1.12.2 FreefallHandler): an upward kick while drifting with no sideways change, as another
      mod's jetpack gives, becomes a push the way the player faces (all of it when looking level, more of it taken
      out looking down), and jump adds no lift that tick; a kick with a sideways change is halved
- [x] Fuel Loader faces (1.12.2 IMachineSides): sneak + wrench steps its power and fuel inputs round the six left /
      back / right layouts, each with its 1.12.2 model
- [x] The Fallen Meteor's 1.12.2 mining rate: the harvestable 1/30 for anyone, a pickaxe at its full speed whatever its
      tier, twice that past diamond (titanium, netherite)
- [x] Player poses (1.12.2 ModelBipedGC, IHoldableItem): rockets, the Moon Buggy and the Astro Miner carried overhead in
      a crouch, the flag overhead standing; both arms up under an open parachute; slow swinging limbs while floating in
      a Galacticraft world; onlookers wave at a crewed rocket that is launching
- [x] The Overworld's own pictures (1.12.2 MapUtil), made by the server from the terrain generator on a background
      thread when first wanted: the Earth in the Moon sky, on the galaxy map and on Display Screens is a strip of the
      world round 0, 0 (a pixel for 8 blocks) recoloured with the Earth icon's palette; a Display Screen's picture 1
      shows, in the Overworld, a map of the 352 blocks round it in 1.12.2's biome colours, elsewhere the Overworld
      turning. Flat worlds have none, as in 1.12.2
- [x] 1.12.2's own exhaust particles: launch flames (ParticleLaunchFlame; big once launched, tiny round a rocket
      counting down) that burn out into launch smoke (ParticleLaunchSmoke) falling, bouncing and billowing along the
      ground; the lander's blue-white flame (ParticleLanderFlame); each rocket's jets placed as its 1.12.2
      spawnParticles placed them (the Tier 2's fireball, the Tier 3's four turned jets, the cargo rocket's tighter jet,
      pushed harder coming in to land), building up through the countdown; the flames set living things alight for 3
      seconds, the crew aside; and the cold mist off a working Gas Liquefier (ParticleSmokeSmall)

### BuildCraft Reborn (1.12.2's BuildCraft compatibility)
Optional: everything below only switches on with [BuildCraft Reborn](https://github.com/Reiko45/BuildCraft-Reborn)
2.1 or later installed.
- [x] MJ power both ways at 1.12.2's 16 gJ to the MJ (BuildCraft Reborn's 10 FE to the MJ agrees with Galacticraft's
      1.6 gJ to the FE): machine inputs and aluminium wires take MJ from engines and kinesis pipes (1.12.2
      TileBaseUniversalElectrical and TileEntityAluminumWire were IMjReceivers), machine outputs connect, machines and
      wires give power to BuildCraft's receivers (MJ tried before FE, as 1.12.2 did), and machines show their stored
      power to BuildCraft's readers; the server settings `disableBuildCraftInput` and `disableBuildCraftOutput`
- [x] MJ as an energy display unit (`energyDisplayUnit`, and `/gcenergyunits MJ` with BuildCraft installed)
- [x] Wrenches both ways: the Standard Wrench is in `c:tools/wrench` (1.12.2 ItemUniversalWrench was BuildCraft's
      IToolWrench), and any wrench in that tag turns Galacticraft machines and sets their faces (1.12.2 BlockAdvanced)
- [x] Galacticraft fuel burns in the combustion engine at 1.12.2's 5 MJ a tick for 9,000 ticks a bucket (1.12.2
      BuildcraftFuelRegistry.fuel.addFuel), from a `buildcraftreborn:fuels` data map entry; oil and fuel go both ways
      through `c:oil` and `c:fuel`
- [x] Sealable BuildCraft pipes (1.12.2 BlockEnclosed's BuildCraft types): stone and cobblestone transport pipes,
      stone and cobblestone fluid pipes, stone and golden kinesis pipes, each a working BuildCraft pipe in a tin block
      that keeps the air in. Made as in 1.12.2, the pipe between two Tin Decoration Blocks (1.12.2 left the kinesis two
      without a recipe only because BuildCraft 8 had no kinesis pipes yet); they break into themselves and drop what the
      pipe carried, and are only listed (creative tab, JEI, EMI) with BuildCraft Reborn installed

### Playtest fixes (after 0.1.0-alpha)
Found playing the first jar:
- [x] Rocket engine heard while riding: the sound stays in the world, kept at the camera while riding (1.21 fixes a
      sound's attenuation and whether it follows the listener when it starts, so switching to "follow the listener" on
      boarding left it playing at 0, 0, 0); heard within 96 blocks of a rocket on its pad; started again if the sound
      engine drops it
- [x] Rockets and landers fly smoothly and stay drawn: the client glides them to each position the server sends (as
      boats do) instead of jumping, and draws them in parts of the sky whose blocks aren't ready for drawing yet (1.21
      skips entities there, so a rocket vanished on the way up below the build height)
- [x] Dungeon spawners spawn: evolved mobs from spawners use 1.12.2's light rule (up to light 7, less often the
      brighter), as the dungeons' unlit space torches still give light 3 and 1.21 allows monsters no block light at all
- [x] Boss bars: shown to players within 64 blocks in the boss's world, and taken away on leaving it, the world, or
      when the boss goes (1.12.2 showed them to every player tracking the boss, at any height above its dungeon)
- [x] 3D items fit their slots: rocket items lie across the slot at 45 degrees, sized to it (1.12.2 ItemModelRocket),
      and the grapple, beam receiver and reflector, telepad and buggy are centred
- [x] The lander checked against 1.12.2: Jump brakes to 1 block a tick (0.15 below y 90); let go, gravity adds 0.008
      a tick each tick, so it speeds up only slowly; Sneak dives. The Venus Entry Pod has no controls (it brakes itself)

## Settings

Three files, all editable from the mod list's settings screen; their names come from 1.12.2's own `gc.configgui`
translations (25 languages).

| File | Read | Settings |
|---|---|---|
| `config/galacticraftcore-client.toml` | any time | `energyDisplayUnit`, `moreStars`, `disableSpaceshipParticles`, `disableVehicleCameraChanges`, `oxygenIndicatorLeft`, `oxygenIndicatorBottom`, `mapScrollSensitivity`, `invertMapScroll` |
| `config/galacticraftcore-common.toml` | at start-up | `quickMode`; `oilGenFactor`; the ore switches (`enableTinOreGen`, `enableAluminumOreGen`, `enableSiliconOreGen`, `disable<Ore>Moon`, `disable<Ore>Mars`, `disable<Ore>Asteroids`, `disable<Ore>Venus`); `disableMoonVillageGen` |
| `serverconfig/galacticraftcore-server.toml` (per world, sent to players) | any time | `hardMode`, `meteorSpawnMod`, `meteorBlockDamage`, `dungeonBossHealthMod`, `spaceStationEnergyScalar`, `disableSpaceStationCreation`, `enableSpaceRaceManagerPopup`, `disableSmallAsteroids`, `maximumAstroMiners`, `disableRocketLaunchDimensions`, `disableRocketsToOverworld`, `otherPlanetWorldBorders`, `forceOverworldRespawn`, `disableLander`, `disableSpaceshipGrief`, `spaceStationsRequirePermission`, `suffocationDamage`, `suffocationCooldown`, `enableSealerEdgeChecks`, `rocketFuelFactor`, `allowLiquidGratings`, `launchControllerChunkLoad`, `disableAmbientLightning` |

What they do, where 1.12.2 needed more than a number:

* **Quick mode** (1.12.2 RecipeManagerGC#setConfigurableRecipes): Aluminum Wire makes 9 instead of 6 and Batteries 3
  instead of 2 (each recipe exists twice, loaded by a `galacticraftcore:config` recipe condition); the Compressor and
  Electric Compressor make two of anything "compressed"; Silicon Ore drops one more Raw Silicon after Fortune (a
  `galacticraftcore:config` loot condition). JEI and EMI show the doubled plates.
* **Hard mode** (a server setting here, as players' own games need it for zero-g drifting): machines use more power
  (Circuit Fabricator 40, Electric Furnace 60 and Arc Furnace 90, compressors, Refinery, Gas Liquefier, Methane
  Synthesizer and Deconstructor 90, Electrolyzer 150, Terraformer 60, Short-Range Telepad 115, Astro Miner base 20 gJ a
  tick), meteors hit for 12 instead of 6, thrown meteor chunks for 3.2 instead of 1.6, drifting in orbit has a third of
  the thrust and less lift (but a top speed of 0.9), mining in orbit off the floor is no longer five times as fast, and
  suffocation damage grows by one each time it hurts.
* **Ore switches** are a `galacticraftcore:config` placement filter on each ore's placed feature (a world made with an
  ore switched off simply has none of it); the asteroids' ores leave the asteroid cores' block lists, and their loose
  ore blocks are left out (nothing placed there, as in 1.12.2).
* **disableRocketLaunchDimensions** takes world ids (`minecraft:the_nether`, `minecraft:the_end` by default) or `*` for
  every world that isn't the Overworld or Galacticraft's (1.12.2's separate "all non-Galacticraft" switch). A rocket
  counted down there doesn't take off ("the gyroscopes are spinning wildly").
* **rocketFuelFactor** multiplies the tank and every mB burned, so a flight takes that many times the fuel; the
  creative pre-fuelled rocket still comes full.
* **disableLander**: on the Moon you come down from y 250 by parachute with a para-chest from y 220 within 4 blocks;
  Mars's landing balloons start from y 250. Other worlds keep their landers.
* **otherPlanetWorldBorders** (Moon, Mars, the asteroids and Venus): past the limit the far coordinate is brought in and
  the other scaled with it, so you land in the same direction from spawn; then the world's own border applies.
* **forceOverworldRespawn** off (the default, as in 1.12.2): dying in a Galacticraft world brings you back in it, on your
  bed there or at its spawn (the surface above the world spawn; the station you were on; on the asteroids, the top of
  the column you died in, or the Overworld if that is empty space). With it on, respawning is vanilla's.
* **disableVehicleCameraChanges** (new to the port with it: the camera itself): getting into a rocket, lander, landing
  balloons or entry pod switches to third person once, and the third-person camera stands 15 blocks back while riding.
* **moreStars** (on by default): 20,000 stars on the Moon and in orbit and 35,000 on Mars, Venus and the asteroids, each
  coordinate pushed out its own random distance; off, 6,000 on a sphere (what the port drew before).

1.12.2 settings with nothing to do in 1.21 or deliberately left out:

* Dimension, biome, schematic and achievement ids, the old oil and fuel fluid ids: registry names replace them.
* `challengeMode` and its flags (the asteroids start): already mostly stripped in Galacticraft Legacy.
* `staticLoadDimensions`, `keepLoadedNewSpaceStations`: 1.21 never unloads a world.
* `enableCopperOreGen`: copper ore is vanilla's. `disableGalacticraftHelium`: there is no helium gas.
* `sealableIDs`, `detectableIDs`, `otherModsSilicon`, `recipesRequireGCAdvancedMetals`: block and item tags
  (`galacticraftcore:sealable`, the Sensor Glasses tag, `c:` tags) do this.
* `externalOilGen`, `retrogenOil`, `oregenIDs`, `enableOtherModsFeatures`, `whitelistCoFHCoreGen`,
  `enableThaumCraftNodes`: other mods' world generation goes through datapacks and biome modifiers.
* `alternateCanisterRecipe`: the canister recipe has no conflict to avoid here. `overrideCapes`: no donor capes.
  `disableUpdateCheck`, `enableDebug`: there is no update check, and logging is set in the log configuration.
* `launchControllerAllDims`: cargo rockets fly to a controller on any loaded world.
* `disableRocketIconRotation`: the rocket items' slow spin in inventories, which the port's rocket icons don't do.

## Intentional differences from 1.12.2 (so far)

* Shift descriptions are drawn in the tooltip's own colour (1.12.2 also left them plain), and every Dungeon Locator
  shows the same needle (1.12.2 animated one shared texture). In Earth orbit, with no dungeons, its needle spins (1.12.2
  pointed it north there, from a dungeon spacing of 0).
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
  `c:ingots/bronze` (the 1.12.2 OreDictionary check). Quick mode's recipe counts load by a recipe condition (see
  Settings); adventure mode's recipes are not ported.
* Fluid Pipe and Pull Fluid Pipe are one block with a `pull` state that the wrench toggles; there is no separate
  pull pipe item. Pipes only take fluid in; they can't be drained by other blocks, as in 1.12.2.
* The amount of liquid drawn inside a pipe is synced in 5% steps, and only when it changes.
* Oil generation follows the `oilGenFactor` setting (1.8 by default); there is no retrogen or extra-dimension list.
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
* Suffocation follows the `suffocationDamage` and `suffocationCooldown` settings (2 damage every 100 ticks by default);
  in hard mode the damage grows by one each time, as in 1.12.2.
* Machines report "running" when they used power for work that tick (1.12.2 looked at the progress counter, which
  made the Refinery's status flicker).
* The galaxy map draws its grid, orbits and belt as thin quads (core OpenGL has no wide lines): one pixel for the grid
  and three for the orbits, as 1.12.2's line widths. 1.12.2 timed its animations by adding each rendered frame's
  partial tick, so they ran faster at higher frame rates; here they run on real time at the 60 fps rate (1.5 of its
  units a game tick), and the panning that 1.12.2 applied to every frame until the next tick is applied once, three
  times over. Stations are listed one per station (1.12.2 keyed them by owner, one each); Escape cancels a rename
  instead of unselecting the station under the dialog; LAUNCH closes the map at once (1.12.2 showed its "teleporting"
  screen; if the trip doesn't go through, the server opens the map again).
* The NASA Workbench recipes are hard-coded as in 1.12.2 (`RecipeManagerGC`), not JSON recipes; JEI and EMI show them
  from the pages' own rules. Page ids are names (`rocket_t1`, `moon_buggy`, ...) instead of the
  configurable 1.12.2 numbers, and pages are opened by the server (1.12.2 let the client ask for any page id).
* Rocket types 0-3 (18/36/54 cargo slots) are a `rocket_storage` data component on one item instead of item damage; the
  creative "pre-fuelled" rocket is a rocket item with a full `rocket_fuel` component.
* The Para-chest's colour is a block state (16 colours) instead of tile-entity data, so its model needs no tile renderer.
* Exhaust particles are hidden behind blocks like any others (1.12.2 drew them with the depth test off, so they showed
  through walls), and launch smoke is stopped by Grating like any other block (1.12.2 let it through). The burns are
  worked out by the server, in the column under a launched rocket or lander and round the foot of a rocket counting
  down (1.12.2's flames each told the server whom they touched, S_SET_ENTITY_FIRE, which a client could abuse).
* The Fuel Loader takes power on its left face and fuel on its right until sneak + wrench moves them, as in 1.12.2; the
  layout is a block state (`sides`, shared with the storage modules). A placed loader draws its fuel face with
  `machine_oil_input` and a free side with `machine_side`, as 1.12.2's blockstate did; the item keeps 1.12.2's item
  model (`machine_fuel_input`). Fuel Loaders take any fluid in the `c:fuel` tag.
* The Buggy Fueling Pad is its own block (`buggy_fueling_pad`, 1.12.2 `landing_pad:1`); launch pads and fueling pads
  next to each other never join into one pad.
* The Moon Buggy is steered with the normal movement keys (1.12.2 had separate key bindings defaulting to the same
  keys) and its storage uses the Spaceship Inventory key. Like 1.12.2 the driver's client moves it; it now rolls to
  a stop on the ground when out of fuel (1.12.2 coasted). The 1.12.2 radar dish had no texture (drawn white); it uses
  white concrete. The item's hand and GUI transforms approximate the 1.12.2 matrices.
* Evolved Zombies don't call reinforcements (vanilla would spawn plain zombies), don't drown into anything, and Evolved
  Skeletons don't freeze into strays. 1.12.2 "challenge mode" drops are not ported. The Evolved Enderman's rare drops
  leave out the oxygen mask: 1.12.2 listed it as an eleventh case of a ten-way roll, so it never dropped.
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
* Zero-g jetpack boosting compares the player's motion with where their last floating tick left it (1.12.2: with
  where it stood before vanilla moved them that tick, as vanilla movement still ran there); the sums are 1.12.2's,
  including its signed check that only a gain in +X or +Z counts as a sideways change.
* `/gchouston` asks for confirmation for 250 ticks after the first use, like 1.12.2, and clears the rocket cargo kept
  in transit.
* Dungeons need "Generate Structures" like any structure; `/place structure galacticraftcore:moon_dungeon` (or
  `mars_dungeon`) places one.
  Without terrain adaptation the dungeon digs its own crater, as in 1.12.2.
* Footprints are tracked only on the server and sent to players who can see them (1.12.2 also drew your own prints
  on your own client). They are still not saved. Prints sit on top of the turf; 1.12.2's server put other players'
  prints one block lower.
* The Overworld's pictures are worked out again each time the server starts (1.12.2 kept them in files under the save
  and the client's `assets/galacticraftMaps`), on one low-priority thread, in some seconds. Heights come from the
  generator's terrain density stepped down from just above its quick estimate (to a block or two of the generator's
  own heights, six times quicker than asking it); biomes are the generator's, taken in 1.12.2's numbering for its
  colour table (newer biomes by their nearest kin or their tags). The strip is the 192 x 48 pixels of 1.12.2's
  texture, 8 blocks each, round 0, 0 (1.12.2's strip also wrapped part of the way round); a local map is sampled
  every 4 blocks with the heights blended between (1.12.2: every block), centred on the screen's chunk, and only
  made for screens within 128 blocks of the player asking.
* Meteors leave no trail: 1.12.2 asked for "distanceSmoke" particles, which its client never drew. Meteors spawn
  within the simulation distance rather than the view distance so that they actually move.
* The Fallen Meteor takes a pickaxe's speed on plain stone, since 1.21's tool rules would leave a pickaxe below diamond
  at hand speed on a block that needs diamond; 1.12.2's pickaxes dug any rock at full speed. Galacticraft's pickaxes
  keep their 1.12.2 harvest levels for the bonus (heavy duty, desh and volcanic 3, titanium 4); other tiers count by
  the blocks they cannot harvest (netherite 4). An iron pickaxe still gets no meteoric iron from it.
* Mars blocks have their own ids (`mars_fine_regolith`, `desh_ore`, ...) under the `galacticraftcore` namespace instead of
  the planets mod's `galacticraftplanets:mars` metadata block; recipes from the planets JSON files are converted too.
* Mars Fine Regolith and Mars Regolith keep 1.12.2's Material.ROCK: they need a shovel to drop, and sound like stone.
* Desh tools are repaired with Desh Ingots like the armour (1.12.2 gave the tools no repair item).
* Mars's day is vanilla's length (1.12.2: 24,660 ticks). Mars's sky colour hooks `ClientLevel#getSkyColor` (a client
  mixin) so the sky dome and vanilla's fog blending both get the 1.12.2 colours; 1.12.2's black square behind the sun
  was drawn with additive blending and hid nothing, so it is left out.
* MapGenCavernMars carved the same ellipsoid on each of its ~50 steps and kept only the last step's vines and sludge;
  the port carves and decorates once (same caverns). Sludge pools are ticked once after generation so they settle.
* ChunkProviderSpace re-tuned two noise generators between terrain passes; they are split into fixed-frequency copies
  with the same seeds. Craters only dug down from y 127, so in taller columns they are hidden pockets, as in 1.12.2.
* The Tier 2 Rocket's four "blueflame" jets are left out: 1.12.2 asked for them, but its client never drew them. Its
  item is drawn with the OBJ
  model at a fixed angle (1.12.2 slowly turned the icon in the inventory).
* Landing balloons are simulated on the server (1.12.2 let the rider's client move them).
* Cavernous Vines keep a `hanging` count (vines from here down, up to 5) in their block state, which sets their glow:
  1.12.2 counted the strand on every light query, but a 1.21 light engine thread that reads other blocks can deadlock
  chunk generation. Generated strands are recounted once all carvers have run.
* The Moon and Mars dungeons share one structure type with a `config` field (`moon` or `mars`); the Moon's structure
  set keeps the salt it had, and the Mars set adds Mars's 1.12.2 dimension id (-29) to it as 1.12.2 did.
* Fire immunity in 1.21 also stops fireball damage, which 1.12.2's did not; the Creeper Boss lets fireballs through so
  it can still be beaten with its own TNT. /kill also works on it (1.12.2: onKillCommand).
* The TNT projectile is drawn centred on itself (1.12.2 copied the primed-TNT renderer's offsets without its rotation
  and drew the block one block off).
* In the dungeon loot the enchanted books use 1.21's
  "on random loot" enchantments (1.12.2: treasure enchantments allowed).
* 1.12.2 left the Mars dungeon bricks' "sludgeDrip" particle as a TODO and played their drip sound (and the sludge's
  lava bubbling) for a null player on the client, which plays nothing. Here the bricks drip water drips from ceilings
  and both sounds are heard.
* Slimelings take their name from a name tag or their panel (the vanilla custom name, shown when you look at them);
  1.12.2 kept a separate name that was never shown in the world. The feed buttons need a meal first: 1.12.2 trusted the
  client, so any feed packet worked. The owner must be within 8 blocks to use the panel's buttons.
* A Slimeling's panel and feeding screen are opened by the server (1.12.2 opened them on the client); its inventory's
  27 bag slots are always in the menu but only usable while the bag is in (1.12.2 added and removed the slots).
* The three Slimeling Egg colours are three blocks (`red_slimeling_egg`, ...) instead of one block with a colour.
* Slimelings jump with 1.12.2's 0.48 strength but, like the evolved mobs, without its extra jump in low gravity (the
  gravity attribute already makes every jump higher). Their size follows their age (1.12.2 only resized them when
  vanilla's breeding age changed).
* The Creeper Egg and Slimeling Egg tooltips, and the Treasure Chests', replace 1.12.2's shift descriptions.
* There is no liquid methane: the Gas Liquefier makes fuel from methane, as 1.12.2's did in practice. Gases are
  fluids in tanks only (no world blocks), and an oxygen mB is an oxygen mB everywhere (1.12.2's gas machines mixed
  units of 1, 2 and 4 per mB). The gas machines push their products out through the standard fluid capability.
* Canisters are one item per contents; an empty canister of any kind is an empty Oil Canister, and it turns into
  whichever canister it is filled as (1.12.2 kept separate empty canisters per kind).
* The Methane Synthesizer's first slot takes hydrogen containers (1.12.2 had the slot but never used it).
* The Cryogenic Chamber's occupant is drawn standing on its floor and its camera looks out through the opening
  (1.12.2 hooked RenderPlayer and the camera setup with its own events; here the player keeps the sleeping pose
  vanilla gives sleepers, drawn upright).
* The Launch Controller's dropdown is drawn like 1.12.2's GuiElementDropdown but opens on click and closes after a
  choice; its pages switch on the client (1.12.2 reopened the container). Its owner is a UUID (1.12.2: the name).
  Values typed into its boxes are sent as they change.
* Cargo rockets keep the chunk they fly through loaded (a timed ticket refreshed every second), and the destination
  controller's chunk is force-loaded unless `launchControllerChunkLoad` is off; 1.12.2 force-loaded the controller's
  chunk and its pads' chunks. A cargo rocket can't target a pad on a world that isn't loaded (1.12.2 loaded it; here the controller's
  forced chunk keeps it loaded once placed).
* Rockets, landers and buggies are loaded and unloaded through `CargoCarrier` (1.12.2 ICargoEntity); the loaders find
  the vehicle on their pad themselves (1.12.2 asked the pad's tile entity).
* The Cargo Rocket recipe takes one to three chests in any chest slots (1.12.2: one chest in the first slot, or two
  in the second and third, or three).
* The asteroids' noise fields are seeded from the world seed (1.12.2: also the world seed, through its own Random);
  which grid points hold asteroids, and their heights, sizes and cores, come from 1.12.2's unseeded position hashes, so
  every world has its asteroids in the same places, as in 1.12.2. The 1.12.2 quirks are kept: the noise offsets add the
  chunk's corner (so asteroids show seams on chunk borders), the hollow-asteroid decoration reads the heights from
  chunk-local indices, and the sunward lighting counts the same block twice.
* Bright Air is a light-emitting invisible block (1.12.2 also placed Bright Air, but wrote the light level straight into
  the chunk's block light at generation). The pod's landing light is full Bright Air.
* 1.12.2 kept a saved list of every asteroid generated (WorldProviderAsteroids#addAsteroid). Here the centres near a
  point are worked out from the hashes when needed, generated or not; only the asteroids landed on are saved.
* Hollow asteroids grow vanilla oaks with 1.12.2's 2-4 log trunks, and their lakes are vanilla's lake feature centred
  where 1.12.2 centred WorldGenLakes. 1.12.2's tall grass, flower and lake generators are replaced by the vanilla
  equivalents; populate runs once per chunk in vanilla's decoration step (1.12.2 kept a set of populated chunks).
* The asteroid belt's sun stays on the horizon with a fixed day time of 12,785 (celestial angle 0.25, 1.12.2's fixed
  value). Beds can be placed there but never slept in (1.12.2's `isDaytime` was always true), and solar panels there go
  by the height map, as 1.12.2's `canBlockSeeSky` did, since the world has no sky light.
* The asteroid belt sits at a fixed phase on the celestial map (1.12.2 chose a random one each time the game started).
* Titanium tools are repaired with titanium ingots; the titanium axe (1.12.2: the TOOL_STEEL material, 768 uses) with
  compressed steel. Asteroid rock has no blast resistance, as in 1.12.2.
* The Tier 3 Rocket's recipe takes one to three chests in any chest slots, like the Tier 2's.
* The Entry Pod falls under its own rules on the server (1.12.2 moved it on the client and sent its position up).
* The thermal check spares spectators as well as creative players. Thermal padding pieces sync with the rest of the
  gear look (1.12.2 PlayerGearData) rather than separate gear ids.
* Moon villages come one per 32x32 chunk region, at a random chunk of its first 24x24, the rule 1.12.2's /locate used.
  1.12.2's own check seeded each chunk with that chunk's coordinates instead of the region's, so its villages came at
  random (about one per region on average, sometimes none or several) and /locate pointed elsewhere.
* As in 1.12.2, a village well is oriented by a random facing among down, up, north and south; with up or down it has
  no horizontal orientation, so (in 1.12.2 too) half of the wells are not built. The paths still start from it.
* The village pieces settle on the surface height map (1.12.2 getTopSolidOrLiquidBlock, which skipped liquids; the
  1.21 height map counts them, which makes no difference on the Moon).
* Alien Villagers have no 1.12.2 village (door and reputation) system to belong to, as their 1.12.2 counterparts had
  none on the Moon either (no doors). Their restock gives sold-out trades 2-12 more uses as 1.12.2 did, but no longer
  appends a second copy of the whole trade list each time (1.12.2's populateBuyingList added the list again).
  The canned carrot trade accepts any oil canister (1.12.2: only an empty one). Struck by lightning they turn into an
  Evolved Witch, as in 1.12.2.

* Space Race members are kept by UUID (with the name last seen, updated at login); 1.12.2 kept names only, so a renamed
  player lost their team. Team ids come from a saved counter (1.12.2 counted from 1 again after every restart, so a
  pending /joinrace could point at a different team).
* Joining a team takes the player out of their old team, which is deleted only once empty (1.12.2 deleted every team
  the joining player was in, with its other members). The server keeps its own member list and ignores the one the
  client sends (1.12.2 took the owner's list as sent).
* Every team is sent to every player at login and when it changes (1.12.2 sent them on request, and the client asked
  again for each flag it saw); there is no client-side flag cache file.
* The flag in the manager and join screens is drawn flat, cell by cell (1.12.2 drew the 3D pole and flag model in the
  GUI). The flag's cloth ripples on render time rather than whole ticks.
* The first-person view doesn't tip the flag forward while charging (ItemModelFlag); the hand transforms approximate
  1.12.2's.
* Carrying a rocket, the Moon Buggy or the Astro Miner only draws the player crouching (1.12.2's isSneaking override
  while rendering): they move and their eyes sit as when standing. Not on the ground, in orbit or in a rocket or lander
  they are drawn standing. The poses are applied after vanilla's, through a mixin into HumanoidModel#setupAnim (1.12.2
  patched ModelBiped#setRotationAngles); `HoldableItem` is the port of the IHoldableItem interface.

* Walkway Pipes can't be dyed or switched to pull mode (1.12.2 had no way to reach them either). Space Wart follows
  vanilla nether wart's rules (it needs soul sand). The grapple is drawn like a bow, as 1.12.2's (EnumAction.BOW); it
  never was an IHoldableItem.
* Small asteroids drift past the player they spawned for (1.12.2 looked for a closer player, but always found the same
  one).
* A Beam Receiver also takes from other mods' machines that only give power (1.12.2 attached only to ones that could
  take it). A telepad jump costs each pad 2,500 gJ (1.12.2 set the target pad's energy to the sending pad's, less 2,500)
  and lands on the centre of the target block.
* Astro Miners are kept loaded with NeoForge chunk tickets (1.12.2 ticked their chunks by hand); its "out of range"
  state is not shown in the dock (no 1.12.2 code set it). Its laser beams are drawn as a pink haze over the blocks
  being cut rather than 1.12.2's line rendering. A blocked dock names the block in the way (1.12.2 said "air"). The
  Astro Miner item doesn't spin slowly in GUIs, and in first person it is held in a plainer pose (1.12.2's first-person
  matrix, four times the size and 6.8 blocks up, is beyond what a model file allows).
* An abandoned base is built into the nearest asteroid within 64 blocks of its chunk (1.12.2: the nearest asteroid
  generated so far, however far; a 1.21 structure can't reach more than 8 chunks), or floats at y 148 with none, as in
  1.12.2 before any asteroid had generated. Its blocks are placed after the asteroid's own decoration, as in 1.12.2.
* The bases' NASA Workbenches and Cryogenic Chambers are whole (1.12.2 placed their parts, then overwrote most with
  the rest of the room); the store's launch pad is joined from the start (1.12.2's single pads joined on their first
  tick). Walls, stairs, bars, walkways and wires settle against their neighbours when the chunk is finished (1.12.2
  drew their connections as it rendered).
* Only a locked Cargo Loader loses its items when broken (1.12.2 cleared every Cargo Loader's items on breaking it).
* The Dark Decoration Slab is named in English (1.12.2 had no name for it) and cut from the block in a stonecutter
  (1.12.2 had no recipe).

* Venus's day is vanilla's length (1.12.2: 720,000 ticks, 30 Earth days), on the Overworld's clock like the other
  worlds. Its weather is its own (1.12.2 updateWeatherOverride) through a mixin on `ServerLevel#advanceWeatherCycle`:
  in 1.21 every other world follows the Overworld's rain.
* Venus's terrain noise is seeded from the world seed through `RandomState`, and its surface and bedrock random mixes
  in the seed too (1.12.2 gave every world the same surface pattern). 1.21 biome sources aren't told the seed, so the
  chunk generator hands it to the biome layers when the world is created. Caves, lava tubes and the surface are laid
  in the surface step, in 1.12.2's order.
* The Juicer walks the floor like a spider and keeps still while it hangs (1.12.2 steered it with a ceiling path-finder
  that let it creep along under a ceiling). Kept from 1.12.2: it counts as hanging only on the first tick of each leap
  (one flag did for both), and the Web Shot is aimed like an arrow allowing for a fall it never makes, so far off it
  sails over all but tall targets.
* The Geothermal Generator's wave runs on the world clock (1.12.2: its own tick counter, sent to nearby clients), so
  the client times the exhaust itself; the vents' lit look is a block state the server sets.
* Solar Array Modules have no block entity: the controller looks for its modules itself once a second, at most 4,096
  (1.12.2 kept them in a module network of tile entities). Its column check is kept: a module counts only if the
  column over the controller is clear from as high above the controller as the controller is above the bottom of the
  world, so a low roof over the controller doesn't matter. Its sun-visible tooltip follows the array live (1.12.2
  worked it out once, when the screen opened), and its status and sun icon still read "blocked" unless every module in
  range sees the sun.
* The Laser Turret looks for targets once a second in a box round it: living things within 15 blocks and meteors within
  90 across (1.12.2 kept a list of every living thing in the world, and of meteors as they appeared). Spectators are
  left alone. The owner's Space Race team is looked up when a target is chosen (1.12.2 remembered it from when the
  turret loaded), and its pets' owners by UUID (1.12.2 needed the owner online).
* Only the turret's owner, within reach, can change it: the server checks (1.12.2 checked only in the GUI). A list
  holds at most 64 names. The three priorities go to the server together, and the edit screens open over the turret's
  own, Escape going back to it. Kept from 1.12.2: blacklist mode fires only at what is listed, meteors too; with
  "never fire at your team" off, the owner is on the list like anyone else; a meteor is picked without a sight check;
  priorities only order things, so spinners can climb past 3.
* The turret's barrels are drawn turning smoothly between ticks (1.12.2 moved them a tick at a time). Its death message
  is new English (1.12.2 had none), and the "BETA Testing Phase" lines in its tooltip are left out.
* A Lighting Panel can't take on the look of the block it is placed against by sneaking. Grating holds water only.
* An Air Lock Controller never loads a chunk to look for its frame (1.12.2 did, so a controller at the edge of the
  loaded area kept loading more), and its frame search starts from the widest box (1.12.2 started from +-6,000,000, so
  past that its box came out wrong). Its settings screen is opened by the server. Kept from 1.12.2: a flat air lock's
  seal checks the west side twice (never the east).
* The Concealed Repeater is one block with a POWERED state (1.12.2: two blocks).
* Sensor Glasses point out the blocks tagged `galacticraftcore:sensor_valuable` (1.12.2: blocks that called themselves
  valuable, plus a config list). The leak they show follows each block's own way in from the sealer (1.12.2 started its
  trace one step the wrong way, so it seldom found one); the client asks the nearest sealer within 96 blocks once a
  second. Mobs are drawn glowing by an extra render layer on the same mobs as 1.12.2.
* The Magnetic Crafting Table shows its remembered result in a slot that can't be used (1.12.2 drew it). Shift-clicking
  into it, the hoppers' rules and the results kept for the next take are 1.12.2's.
* A Telemetry Unit keeps its link as the entity's UUID, so it finds a respawned player, or one coming back, again
  (1.12.2 held the entity itself). Ocelot skins aren't shown (1.21 ocelots have none).
* Display Screens are joined on the server, which sends the result (1.12.2's client worked it out again itself), and
  are drawn once, from a big screen's top left block. Picture 3 shows no space stations;
  orbit lines are cut at the frame segment by segment (1.12.2 used clip planes). The up-and-down and left-and-right
  middle blocks of a big screen use the full frame model turned to the facing (1.12.2 left them facing south).
* The Communications Dish needs room for all its parts to be placed (1.12.2 replaced whatever was there). Like 1.12.2 it
  only turns and nods: its sun-following angle (never drawn) and its energy store (which nothing could fill) are left
  out.
* Galacticraft's green dye recipe takes any yellow and any blue dye (1.12.2: dandelion yellow and lapis lazuli, which
  was the blue dye then).
* A hot Meteor Chunk is the same item with a burn time on it (1.12.2: a second metadata), so one smelting recipe heats
  only a plain cold chunk. Kept from 1.12.2: a hot chunk alight in flight sets what it hits alight for 2 seconds, and
  cools in 1,800 ticks in an inventory (its tooltip counts half-speed seconds, 22.5 at the start).
* The Prelaunch Checklist's worlds and their entries are written into it, in 1.12.2's order by rocket tier (1.12.2
  gathered them from each reachable world in the celestial registry).
* An Emergency Post keeps its kit in a block state (1.12.2: metadata), so a post broken with one drops a post item that
  still has it, and crafting a post with a kit gives a post item with that state.
* While the Hydraulic Platform lift runs, the server lets the player through the platform passed on the way by taking
  its collision away (1.12.2 turned off the player's collisions with the world).
* The Deconstructor's NASA Workbench pages are written into it (without their storage chests, as in 1.12.2); crafting
  recipes are searched in order of their ids (1.12.2: in registration order) for the first that makes just one of the
  item.
* Paint colours are kept for each player, as in 1.12.2: everyone near an applicator sees the glass in its colours, and
  keeps them after walking off. Glass in a tin frame keeps its frame's colour, as in 1.12.2.


* The 1.12.2 music tracks ending in `_jc` (Mars, Mimas, Orbit, Space Race; by Jackson Cordes, as far as secondary
  sources say) are not shipped: their licence can't be confirmed from the MIT-licensed Galacticraft Legacy repository,
  which has no record of how they came in. The space music event (`galacticraftcore:music.space`) ships with the one
  other track, `scary_ambience`; a resource pack can add the others (see the README). The music plays in every
  Galacticraft world every 10 to 20 minutes and stops other music on arrival, as 1.12.2's MusicTickerGC did.
* The rocket's engine is heard within 96 blocks of it, and everywhere only by its rider (1.12.2: at full volume
  wherever it was). The parachute is heard by its wearer only, as in 1.12.2. The Electric and Advanced Compressors'
  pitches are 1.12.2's after the sound engine's limits (it asked for -9.5 and 9.5); the Advanced Compressor uses
  1.12.2's `player/unlockchest` sound, which 1.12.2's treasure chests had stopped using.
* Advancements are laid out by 1.21 itself. "Worse than a wither" (the Moon dungeon) is given to every player who
  comes within 20 blocks of a Skeleton Boss (1.12.2 then checked whether the first entity loaded in the world was a
  nearby boss, so it seldom was). "Full" oxygen tanks are tanks holding their whole capacity (1.12.2: the tank items
  at damage 0).
* Jade and EMI support is new (1.12.2 had JEI only). The `c:` and Galacticraft item and fluid tags have English names
  for them.

* BuildCraft Reborn is compiled against through a copy of the five MJ API types it uses (`src/buildcraftApi`, MIT,
  never packaged); `-PbuildcraftJar=<jar>` loads BuildCraft Reborn in the dev runs and GameTests. Engines and kinesis
  pipes get exactly what a machine can take that tick (1.12.2 told BuildCraft 30 times that, because BuildCraft 7 sent
  only a trickle). Use BuildCraft Reborn 2.1.1 or later for the GameTests: 2.1.0 sent its `volume_boxes` payload to
  every player joining a level, and the GameTests' mock players have no channel for it.
* The screenshot tour can run where there is no sound device: `-PtourNullAudio=true` gives OpenAL its silent "null"
  output, so sounds still play (unheard) and the tour checks the rocket's engine is playing at the rider.
* The sealable BuildCraft pipes hold BuildCraft Reborn's own pipe holder block entity, as 1.12.2's held BuildCraft's
  pipe tile, so they connect to whatever the bare pipe would. They join that block entity type during NeoForge's
  BlockEntityTypeAddBlocksEvent, before capabilities are given out, through a mixin accessor: the event itself only
  takes blocks of BuildCraft's pipe block class, and nothing of the pipe holder or its renderer looks at its block. The
  pipe is given, ticked, told of neighbour changes and removed by calling BuildCraft's pipe block through Minecraft's
  own Block methods, so no BuildCraft class is named. One put down by a command or a structure gets its pipe on the
  next tick.
* `/gckeeploaded` is not ported: it kept a world loaded with nobody in it, and 1.21 never unloads a world. `/gcinv`
  takes player names or selectors (1.12.2: names only), keeps the whole 11-slot Galacticraft inventory (1.12.2 saved 6
  slots), and its saves now survive a restart (1.12.2's loader looked for the wrong kind of tag and never read them
  back). `/gcenergyunits` offers gJ and FE (RF is taken as FE), and MJ with BuildCraft Reborn; 1.12.2's EU and J
  belonged to mods that aren't here. `/gchelp` links 1.12.2's wiki address. `/dimensiontp` sends a player into space
  as if from the world spawn in an empty Tier 1 Rocket with 1,000 fuel, every world open, as in 1.12.2.

## Workflow for porting more content

1. Register the block/item in `GCBlocks`/`GCItems` (flatten metadata variants).
2. Add `textures`, `lang` and `recipes.items` entries to `tools/legacy_mapping.json` (`models` for 1.12.2 JSON
   models, `obj_models` for OBJ models and their `.mtl`). A `recipes.items` value can be an item, a `#tag`, a list of
   either, or a whole ingredient object (e.g. `neoforge:components` for a full canister); `recipes.result_items`
   names the single item a recipe makes where the ingredient mapping is a tag.
3. `python3 tools/port_legacy_assets.py --legacy /path/to/Galacticraft-Legacy`. It copies textures, converts
   translations and re-converts recipes, and lists every recipe still blocked on unmapped items.
4. Add block states/models/loot/tags to the datagen providers, then run `./gradlew runData`.
5. `python3 tools/check_assets.py` and `./gradlew runGameTestServer` (a fresh world in `run/gametest` each run; the
   result is at the end of `run/gametest/logs/latest.log`).
6. Entity and block-entity models written as 1.12.2 `ModelRenderer` code: `python3 tools/convert_java_model.py
   Model.java` prints the 1.21 `LayerDefinition` body (boxes, pivots, rotations, mirroring, texture offsets).
7. `xvfb-run -a ./gradlew runScreenshotTour` (or plain `./gradlew runScreenshotTour` with a display) builds scenes and
   saves screenshots of every GUI and model to `run/screenshots/` for a visual check.
