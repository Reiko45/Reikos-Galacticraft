# Reiko's Galacticraft — NeoForge 1.21.1 port

An **unofficial** port of [Galacticraft Legacy](https://github.com/TeamGalacticraft/Galacticraft-Legacy)
(the Minecraft 1.12.2 space mod by Team Galacticraft) to **Minecraft 1.21.1 / NeoForge 21.1**.
It is not affiliated with or endorsed by Team Galacticraft.

**Status: Phase 3 of 8 (alpha).** The Moon exists and has no air: you need oxygen gear, sealed rooms or oxygen
bubbles to survive there. Galacticraft's power system, machines, solar panels and oil processing work. Rockets are not
ported yet. See [PORTING.md](PORTING.md) for the roadmap and how each 1.12.2 system maps to NeoForge.

## What works

* **Overworld**: tin, aluminum and silicon ores (1.12.2 vein sizes and heights), metal storage blocks, tin
  decoration blocks with stairs/slabs/walls, Glowstone Torch.
* **Moon dimension** (`galacticraftcore:moon`): terrain and craters ported from the 1.12.2 generator; moon turf,
  dirt and rock; copper, tin, cheese and sapphire ores; dungeon bricks. Gravity is 0.018 (≈ 22% of Earth) and
  fall distance is ×0.18, the same as 1.12.2.
* **Items**: ingots, compressed plates, wafers, rocket/oxygen/buggy parts, canisters, Heavy Duty tools and armor.
  Food includes cheese, the cheeseburger, and canned food that gives back its tin canister.
* **Power** (gJ, with FE compatibility at 1 FE = 1.6 gJ): Coal Generator, Basic and Advanced Solar Panels, Energy
  Storage Module and Cluster, Aluminum Wire and Heavy Aluminum Wire networks and their redstone switches, Battery
  and Standard Wrench. Tier-2 power (a Cluster over heavy wire) makes the Arc Furnace, Electric/Advanced
  Compressor, Circuit Fabricator and Refinery faster, as in 1.12.2. Sneak-use the wrench on a machine to move its
  energy port.
* **Machines**: Compressor (burns fuel), Electric Compressor, Advanced Compressor, Electric Furnace, Electric Arc
  Furnace, Circuit Fabricator. Their GUIs use the original 1.12.2 artwork, and hoppers/pipes work per side.
* **Oil**: crude oil pockets underground in the Overworld, oil and fuel liquids and buckets, Oil/Fuel Canisters,
  the Refinery, stackable Fluid Tanks and dyeable Fluid Pipes (wrench a pipe to make it pull from the block next
  to it). Oil and fuel work with other mods' tanks and pipes, and the `c:oil` tag lets the Refinery take other
  mods' oil.
* **Oxygen**: on the Moon players need an Oxygen Mask, Oxygen Gear and a filled Oxygen Tank (light, medium or heavy)
  worn in the Galacticraft inventory tab, or breathable air; otherwise they suffocate, and so do mobs. Torches only
  burn where there is oxygen. Oxygen Collector (from leaves and crops, or from the air on Earth), Oxygen Compressor and
  Decompressor (fill and empty tanks), Oxygen Storage Module, Oxygen Bubble Distributor, Oxygen Sealer (fills a closed
  room with breathable air) and Oxygen Detector. Oxygen travels through Fluid Pipes. Parachutes can be worn already.
* **JEI** (optional): recipe categories for the Compressor, Circuit Fabricator and Refinery.
* Recipes: 114 crafting recipes converted from 1.12.2, compressor and circuit fabricator recipes, smelting,
  blasting and stonecutting. Also loot tables, `c:` tags, and translations for all 25 languages that 1.12.2
  shipped.

![Machines and an aluminum wire network](docs/screenshots/machines.png)

| Circuit Fabricator | Electric Furnace powered over wire |
|---|---|
| ![Circuit Fabricator GUI](docs/screenshots/circuit_fabricator.png) | ![Electric Furnace GUI](docs/screenshots/electric_furnace.png) |

![Oil tanks, pull pipes, the Refinery and a fuel tank; solar panels behind](docs/screenshots/oil_processing.png)

![Basic (flat) and Advanced (sun-tracking) Solar Panels](docs/screenshots/solar_panels.png)

| Refinery | JEI |
|---|---|
| ![Refinery GUI](docs/screenshots/refinery.png) | ![Refinery recipes in JEI](docs/screenshots/jei_refinery.png) |

![Oxygen gear on a player inside an Oxygen Bubble Distributor's bubble; the oxygen machines behind](docs/screenshots/oxygen_gear_and_bubble.png)

![On the Moon: a glass room filled by an Oxygen Sealer (its torch burns, the one outside does not) and the tank gauges](docs/screenshots/moon_sealed_room.png)

| Galacticraft inventory | Oxygen Sealer and Bubble Distributor |
|---|---|
| ![Galacticraft inventory tab](docs/screenshots/galacticraft_inventory.png) | ![Oxygen machine GUIs](docs/screenshots/oxygen_guis.png) |

Some parts (rocket components) have recipes but no use yet, and fuel has no use until rockets arrive.
Their machines and vehicles come in later phases.

### Visiting the Moon

There are no rockets yet, so use a command (cheats on). Put on an Oxygen Mask, Oxygen Gear and a filled tank first
(right-click them, or use the Galacticraft tab of the inventory):

```
/execute in galacticraftcore:moon run tp @s 0 120 0
```

## Building and running

Requires JDK 21.

```sh
./gradlew build               # mod jar in build/libs/
./gradlew runClient           # dev client
./gradlew runData             # regenerate src/generated/resources (models, loot, tags, worldgen, ...)
./gradlew runGameTestServer   # in-game tests (gametest/*Tests), exits non-zero on failure
python3 tools/check_assets.py # static check of models/textures/translations
```

`./gradlew runScreenshotTour` is a development aid. It creates a flat creative world, builds every machine, the
solar panels, an oil processing line and the oxygen machines, opens each GUI, the Galacticraft inventory and the JEI
categories, shows the oxygen gear on the player, goes to the Moon for the oxygen HUD, saves screenshots to
`run/screenshots/` and quits. (Run it under `xvfb-run` on a machine without a display.)

## Porting tools

`tools/port_legacy_assets.py` reads a Galacticraft Legacy checkout and `tools/legacy_mapping.json`. It copies
textures and models, converts the `.lang` files to JSON and converts the 1.12.2 recipes (metadata and ore
dictionary → item ids and tags):

```sh
git clone --depth 1 https://github.com/TeamGalacticraft/Galacticraft-Legacy ../Galacticraft-Legacy
python3 tools/port_legacy_assets.py --legacy ../Galacticraft-Legacy
```

## Project layout

| Path | Contents |
|---|---|
| `src/main/java/.../registry` | Blocks, items, armor materials, tiers, creative tabs, chunk generator codec |
| `src/main/java/.../world` | Moon chunk generator, dimension keys, per-dimension physics, ported noise, oil pockets |
| `src/main/java/.../energy` | gJ storage, energy capability, FE bridge, wire networks |
| `src/main/java/.../fluid` | Oil/fuel blocks, oxygen gas, fluid tags, fluid pipe networks |
| `src/main/java/.../oxygen`, `player` | Breathing and sealing rules, the seal check, player data and gear |
| `src/main/java/.../network` | Packets |
| `src/main/java/.../block/entity`, `menu`, `client/screen`, `client/render` | Machines, their menus, GUIs and block entity renderers |
| `src/main/java/.../compat/jei` | Optional JEI plugin |
| `src/main/java/.../recipe` | Compressing and circuit fabricating recipe types |
| `src/main/java/.../event` | Gravity and fall-damage handling |
| `src/main/java/.../data` | Datagen providers |
| `src/main/java/.../gametest` | GameTests |
| `src/main/resources` | Ported textures, models, translations, converted crafting recipes |
| `src/generated/resources` | Datagen output (committed; do not edit by hand) |

## License and credits

* This port: MIT, see [LICENSE](LICENSE).
* Galacticraft Legacy, from which code, textures, translations and recipes are derived: MIT, © Team Galacticraft,
  see [LICENSE-GALACTICRAFT-LEGACY](LICENSE-GALACTICRAFT-LEGACY). Both license files are bundled in the jar.
* Galacticraft was created by micdoodle8 and radfast with many contributors.

Only the MIT-licensed Legacy repository is used as a source. The original `micdoodle8/Galacticraft` repository
is under a different license that forbids ports (see PORTING.md).
