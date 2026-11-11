# Reiko's Galacticraft — NeoForge 1.21.1 port

An **unofficial** port of [Galacticraft Legacy](https://github.com/TeamGalacticraft/Galacticraft-Legacy)
(the Minecraft 1.12.2 space mod by Team Galacticraft) to **Minecraft 1.21.1 / NeoForge 21.1**.
It is not affiliated with or endorsed by Team Galacticraft.

**Status: Phase 5b of 8 (alpha).** Build a Tier 1 Rocket, fuel it and fly to the Moon: you arrive in a Space Lander
and need oxygen gear, sealed rooms or oxygen bubbles to survive there. Evolved mobs roam the Moon, and its dungeons hide
the Skeleton Boss and a treasure chest with the Moon Buggy schematic. Fly back and you come down by parachute, with your
cargo following in a Para-chest. Galacticraft's power system, machines, solar panels and oil processing work. See
[PORTING.md](PORTING.md) for the roadmap and how each 1.12.2 system maps to NeoForge.

## What works

* **Overworld**: tin, aluminum and silicon ores (1.12.2 vein sizes and heights), metal storage blocks, tin
  decoration blocks with stairs/slabs/walls, Glowstone Torch.
* **Moon dimension** (`galacticraftcore:moon`): terrain, craters and caves ported from the 1.12.2 generator; moon
  turf, dirt and rock; copper, tin, cheese and sapphire ores; dungeon bricks. Gravity is 0.018 (≈ 22% of Earth),
  fall distance is ×0.18, and dropped items and arrows float too, the same as 1.12.2. The black sky shows stars even
  at noon and the Earth hanging overhead. You leave footprints in the moon turf, and meteor showers rain down hot
  Fallen Meteors (they burn you while they glow, then cool down to be mined for meteoric iron).
* **Moon mobs and dungeons**: Evolved Zombies, Skeletons, Spiders, Creepers and Endermen (wearing oxygen gear, tougher
  than vanilla, with rare drops of Galacticraft parts) spawn on the Moon. Each 704x704 area has a dungeon under a crater:
  corridors and rooms with spawners, cobwebs and loot chests lead to the Skeleton Boss, which grabs and throws you and
  leaves if you run from its room. Beat it to fill the locked Tier 1 Treasure Chest next door and get the key for it.
* **Schematics**: put a schematic from the treasure chest into the NASA Workbench's "Add New Schematic" page to unlock
  a new page for good; Back/Next flip between pages.
* **Moon Buggy**: built on the workbench's Moon Buggy page from heavy plates, a seat and four wheels, with up to three
  storage boxes (18 slots each). Drive it with the movement keys (it climbs over bumps when you push on), F opens its
  storage and fuel, and a Buggy Fueling Pad with a Fuel Loader refuels it.
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
* **Rockets**: the NASA Workbench (a 3x3x4 multiblock) builds a Tier 1 Rocket from a nose cone, eight heavy plates,
  four fins and an engine; each wooden chest added gives it 18 cargo slots. Nine Rocket Launch Pads in a square make a
  launch pad; a Fuel Loader next to it pumps fuel into the rocket. Sit in the rocket, press Jump to start the 20-second
  countdown (the first press warns you if you have no parachute) and F to open its cargo and fuel. The rocket climbs
  to the edge of space and the destination screen opens: pick the Moon or the Overworld.
* **Travel**: on the Moon you arrive at y 900 in a Space Lander holding your cargo, launch pads and rocket; steer it with
  the movement keys, Jump to brake and Sneak to dive, and land gently or it explodes. Back on Earth you arrive at y 250
  and fall slowly under your parachute, and a Para-chest with the cargo, the rocket and the leftover fuel lands nearby.
* **JEI** (optional): recipe categories for the Compressor, Circuit Fabricator and Refinery.
* Recipes: 117 crafting recipes converted from 1.12.2, compressor and circuit fabricator recipes, smelting,
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

![A Tier 1 Rocket on its launch pad with a Fuel Loader, the NASA Workbench, a Para-chest and a Space Lander](docs/screenshots/rocket_launch_site.png)

| NASA Workbench and Fuel Loader | Rocket cargo and Para-chest |
|---|---|
| ![NASA Workbench and Fuel Loader GUIs](docs/screenshots/rocket_guis.png) | ![Rocket cargo and Para-chest GUIs](docs/screenshots/cargo_guis.png) |

| Lift-off | Choosing a destination |
|---|---|
| ![A Tier 1 Rocket lifting off](docs/screenshots/rocket_launch.png) | ![The destination screen with the Moon selected](docs/screenshots/celestial_selection.png) |

![The Moon sky at noon and at midnight](docs/screenshots/moon_sky.png)

![A meteor coming down, and its crater with the hot Fallen Meteor; a cooled one beside](docs/screenshots/moon_meteors.png)

| Footprints | A Moon cave |
|---|---|
| ![Footprints across a crater](docs/screenshots/moon_footprints.png) | ![A cave under the Moon's surface](docs/screenshots/moon_cave.png) |

| Landing on the Moon | Back on Earth |
|---|---|
| ![Descending to the Moon in a Space Lander](docs/screenshots/lander_descent.png) | ![Coming down under a parachute](docs/screenshots/parachute_descent.png) |

| Evolved mobs and the Skeleton Boss | The Skeleton Boss in its room |
|---|---|
| ![Evolved Zombie, Skeleton, Creeper, Spider, Enderman and the Skeleton Boss on the Moon](docs/screenshots/moon_evolved_mobs.png) | ![The Skeleton Boss in a Moon dungeon](docs/screenshots/moon_dungeon_boss.png) |

| A Moon dungeon's crater | The treasure room |
|---|---|
| ![The crater above a Moon dungeon](docs/screenshots/moon_dungeon_crater.png) | ![The Tier 1 Treasure Chest](docs/screenshots/moon_dungeon_treasure.png) |

![Moon Buggies on a Buggy Fueling Pad with a Fuel Loader](docs/screenshots/moon_buggies.png)

| Driving the buggy | Treasure chest and schematic pages |
|---|---|
| ![Driving a Moon Buggy](docs/screenshots/moon_buggy_driving.png) | ![Treasure chest, Add New Schematic and Moon Buggy pages](docs/screenshots/schematic_guis.png) |

### Visiting the Moon

Fly there in a Tier 1 Rocket, or use a command (cheats on). Put on an Oxygen Mask, Oxygen Gear and a filled tank first
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
categories, shows the oxygen gear on the player, goes to the Moon for the oxygen HUD, footprints, meteors, the sky,
a cave, the evolved mobs, a dungeon (placed where the structure set puts the first one, since the tour world has
structures off), the Skeleton Boss and its treasure, the Moon Buggy and the NASA Workbench pages, then flies a rocket
to the Moon. It saves screenshots to `run/screenshots/` and quits. (Run it under `xvfb-run` on a machine without a display.)

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
