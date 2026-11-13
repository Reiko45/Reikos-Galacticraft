#!/usr/bin/env python3
"""Port assets from a Galacticraft Legacy (1.12.2) checkout into this NeoForge 1.21.1 project.

Galacticraft Legacy: https://github.com/TeamGalacticraft/Galacticraft-Legacy (MIT, Team Galacticraft).

Driven by tools/legacy_mapping.json. For every entry it:
  * copies textures to their 1.21 location/name                 (assets/galacticraftcore/textures/...)
  * copies sounds                                                 (assets/galacticraftcore/sounds/...)
  * copies hand-made block models, rewriting texture references (assets/galacticraftcore/models/...)
  * converts the .lang files of every locale to .json           (assets/galacticraftcore/lang/*.json)
  * converts 1.12 JSON crafting recipes (metadata + ore dictionary -> item ids + tags)
                                                                  (data/galacticraftcore/recipe/*.json)

Recipes whose items are not in the mapping yet are skipped and listed, so the script can be re-run as
more content is ported. Block states, item models, loot tables, tags, smelting and worldgen are not
handled here; they come from Java datagen (./gradlew runData).

Usage:
    git clone --depth 1 https://github.com/TeamGalacticraft/Galacticraft-Legacy /path/to/legacy
    python3 tools/port_legacy_assets.py --legacy /path/to/legacy
"""
import argparse
import json
import re
import os
import shutil
import sys

MOD_ID = "galacticraftcore"


class Unmapped(Exception):
    pass


PLANETS = "planets:"
PLANETS_ID = "galacticraftplanets"


def asset_source(legacy_assets, old):
    """Mapping values starting with "planets:" come from the Galacticraft Planets assets (Mars, asteroids, Venus)."""
    if old.startswith(PLANETS):
        return os.path.join(os.path.dirname(legacy_assets), PLANETS_ID), old[len(PLANETS):]
    return legacy_assets, old


def texture_ref(old):
    """How 1.12 models and .mtl files refer to a mapped texture: galacticraftcore:blocks/x or galacticraftplanets:blocks/x."""
    return f"{PLANETS_ID}:{old[len(PLANETS):]}" if old.startswith(PLANETS) else f"{MOD_ID}:{old}"


def load_lang(path):
    entries = {}
    with open(path, encoding="utf-8-sig") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            entries[key.strip()] = value
    return entries


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def port_textures(mapping, legacy_assets, out_assets):
    count = 0
    for new, old in mapping["textures"].items():
        root, path = asset_source(legacy_assets, old)
        src = os.path.join(root, "textures", path + ".png")
        if not os.path.isfile(src):
            raise SystemExit(f"missing legacy texture {src}")
        dst = os.path.join(out_assets, "textures", new + ".png")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
        if os.path.isfile(src + ".mcmeta"):
            shutil.copyfile(src + ".mcmeta", dst + ".mcmeta")
        count += 1
    print(f"textures: {count} copied")


def port_sounds(mapping, legacy_assets, out_assets):
    """Copy .ogg files; sounds.json itself is hand-written (event names follow 1.21 conventions)."""
    for new, old in mapping.get("sounds", {}).items():
        root, path = asset_source(legacy_assets, old)
        src = os.path.join(root, "sounds", path + ".ogg")
        if not os.path.isfile(src):
            raise SystemExit(f"missing legacy sound {src}")
        dst = os.path.join(out_assets, "sounds", new + ".ogg")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
    print(f"sounds: {len(mapping.get('sounds', {}))} copied")


def port_models(mapping, legacy_assets, out_assets):
    reverse = {texture_ref(old): f"{MOD_ID}:{new}" for new, old in mapping["textures"].items()}
    for new, spec in mapping["models"].items():
        # A plain string names the 1.12 model; an object can also set a 1.21 render_type (e.g. minecraft:cutout).
        old = spec if isinstance(spec, str) else spec["from"]
        root, path = asset_source(legacy_assets, old)
        with open(os.path.join(root, "models", path + ".json"), encoding="utf-8") as f:
            model = json.load(f)
        if isinstance(spec, dict) and "render_type" in spec:
            model["render_type"] = spec["render_type"]
        for slot, ref in list(model.get("textures", {}).items()):
            if ref.startswith("#"):
                continue
            if ref not in reverse:
                raise SystemExit(f"model {old}: texture {ref} is not in the textures mapping")
            model["textures"][slot] = reverse[ref]
        write_json(os.path.join(out_assets, "models", new + ".json"), model)
    parts = {k: v for k, v in mapping.get("model_parts", {}).items() if not k.startswith("_")}
    for new, spec in parts.items():
        root, path = asset_source(legacy_assets, spec["from"])
        with open(os.path.join(root, "models", path + ".json"), encoding="utf-8") as f:
            source = json.load(f)
        model = {"textures": {}, "elements": [e for e in source["elements"] if e.get("__comment") in spec["elements"]]}
        if "render_type" in spec:
            model["render_type"] = spec["render_type"]
        if len(model["elements"]) != len(spec["elements"]):
            raise SystemExit(f"model_parts {new}: elements {spec['elements']} not all found in {spec['from']}")
        for element in model["elements"]:
            element.pop("__comment", None)
        for slot, ref in source.get("textures", {}).items():
            model["textures"][slot] = ref if ref.startswith("#") else reverse.get(ref) or sys.exit(f"{new}: unmapped texture {ref}")
        write_json(os.path.join(out_assets, "models", new + ".json"), model)
    objs = {k: v for k, v in mapping.get("obj_models", {}).items() if not k.startswith("_")}
    for new, spec in objs.items():
        # Copy the .obj under its new name and rewrite its .mtl: mtllib points at the renamed .mtl, and map_Kd
        # texture names go through the textures mapping (1.21 path, e.g. blocks/assembly -> block/assembly).
        # {"from": ..., "default_map": ...} also gives untextured materials (1.12.2 drew them white) a texture.
        old = spec if isinstance(spec, str) else spec["from"]
        default_map = None if isinstance(spec, str) else spec.get("default_map")
        materials = {} if isinstance(spec, str) else spec.get("materials", {})
        root, path = asset_source(legacy_assets, old)
        src = os.path.join(root, "models", path)
        dst = os.path.join(out_assets, "models", new)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        old_mtl, new_mtl = os.path.basename(path) + ".mtl", os.path.basename(new) + ".mtl"
        with open(src + ".obj", encoding="utf-8") as f:
            obj = f.read().replace("mtllib " + old_mtl, "mtllib " + new_mtl)
        if not os.path.isfile(src + ".mtl"):
            # 1.12.2 drew some OBJs with no materials at all, under whatever texture was bound: give them one
            if not default_map:
                raise SystemExit(f"obj {old} has no .mtl: give it a default_map")
            obj = f"mtllib {new_mtl}\nusemtl default\n" + obj
            with open(dst + ".obj", "w", encoding="utf-8", newline="\n") as f:
                f.write(obj)
            with open(dst + ".mtl", "w", encoding="utf-8", newline="\n") as f:
                f.write(f"newmtl default\nmap_Kd {default_map}\n")
            continue
        with open(dst + ".obj", "w", encoding="utf-8", newline="\n") as f:
            f.write(obj)
        lines = []
        textured = True
        material = None
        with open(src + ".mtl", encoding="utf-8") as f:
            for line in f.read().splitlines():
                if line.startswith("newmtl "):
                    if not textured and default_map:
                        lines.append("map_Kd " + default_map)
                    textured = False
                    material = line.split(" ", 1)[1].strip()
                if line.startswith("map_Kd ") and material in materials:
                    lines.append("map_Kd " + materials[material])
                    textured = True
                    continue
                if line.startswith("map_Kd "):
                    ref = line.split(" ", 1)[1].strip()
                    if ref not in reverse:
                        raise SystemExit(f"obj {old}: texture {ref} is not in the textures mapping")
                    line = "map_Kd " + reverse[ref]
                    textured = True
                lines.append(line)
        if not textured and default_map:
            lines.append("map_Kd " + default_map)
        with open(dst + ".mtl", "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lines) + "\n")
    print(f"models: {len(mapping['models'])} converted, {len(parts)} extracted, {len(objs)} OBJ copied")


def java_format(value):
    """1.21 translations only take %s / %n$s; 1.12 strings used printf-style %d and %f too."""
    return re.sub(r"%(\d+\$)?(\.\d+)?[dfi]", lambda m: "%" + (m.group(1) or "") + "s", value)


def port_lang(mapping, legacy_assets, out_assets):
    lang_dir = os.path.join(legacy_assets, "lang")
    planets_dir = os.path.join(asset_source(legacy_assets, PLANETS)[0], "lang")
    keys = mapping["lang"]

    def load_both(name):
        # Planets keys are looked up as "planets:<key>"
        entries = load_lang(os.path.join(lang_dir, name)) if os.path.isfile(os.path.join(lang_dir, name)) else {}
        planets = os.path.join(planets_dir, name)
        if os.path.isfile(planets):
            entries.update({PLANETS + k: v for k, v in load_lang(planets).items()})
        return entries

    english = load_both("en_us.lang")
    missing = [old for old in keys.values() if old not in english]
    if missing:
        raise SystemExit(f"en_us.lang lacks: {missing}")
    for name in sorted(os.listdir(lang_dir)):
        if not name.endswith(".lang"):
            continue
        locale = name[:-5].lower()
        entries = load_both(name)
        out = {new: java_format(entries[old]) for new, old in sorted(keys.items()) if old in entries}
        if locale == "en_us":
            out.update(mapping.get("lang_extra_en_us", {}))
        if out:
            write_json(os.path.join(out_assets, "lang", locale + ".json"), dict(sorted(out.items())))
    print(f"lang: {len(keys)} keys for {len([n for n in os.listdir(lang_dir) if n.endswith('.lang')])} locales")


def resolve_ref(value):
    """'#ns:tag' -> {'tag': ...}; 'ns:item' -> {'item': ...}; list -> list of those."""
    if isinstance(value, list):
        return [resolve_ref(v) for v in value]
    if isinstance(value, dict):
        return value  # a full ingredient (e.g. neoforge:components for a full canister)
    if value.startswith("#"):
        return {"tag": value[1:]}
    return {"item": value}


def lookup_item(items, item, data):
    if data not in (None, 0) and f"{item}:{data}" in items:
        return items[f"{item}:{data}"]
    if data in (None, 0):
        if f"{item}:0" in items:
            return items[f"{item}:0"]
        if item in items:
            return items[item]
        if item.startswith("minecraft:"):
            return item
    raise Unmapped(f"{item}" + (f":{data}" if data is not None else ""))


def convert_ingredient(rules, ing):
    if isinstance(ing, list):
        out = []
        for part in ing:
            converted = convert_ingredient(rules, part)
            out.extend(converted if isinstance(converted, list) else [converted])
        return out
    kind = ing.get("type")
    if kind == "forge:ore_dict":
        if ing["ore"] not in rules["ore_dict"]:
            raise Unmapped("ore:" + ing["ore"])
        return resolve_ref(rules["ore_dict"][ing["ore"]])
    if kind == f"{MOD_ID}:advanced_metal":
        if ing["metal"] not in rules["advanced_metal"]:
            raise Unmapped("metal:" + ing["metal"])
        return resolve_ref(rules["advanced_metal"][ing["metal"]])
    if kind == f"{MOD_ID}:item_silicon":
        return resolve_ref(rules["item_silicon"])
    if kind not in (None, "minecraft:item"):
        raise Unmapped("ingredient type " + kind)
    return resolve_ref(lookup_item(rules["items"], ing["item"], ing.get("data")))


def category_for(result_id):
    path = result_id.split(":", 1)[1]
    if path.endswith(("_pickaxe", "_axe", "_shovel", "_hoe", "_sword", "_helmet", "_chestplate", "_leggings", "_boots")):
        return "equipment"
    if any(part in path for part in ("stairs", "slab", "wall", "_block", "decoration", "torch", "ore")):
        return "building"
    return "misc"


def convert_recipe(rules, recipe):
    for cond in recipe.get("conditions", []):
        if cond.get("data") in rules["skip_conditions"]:
            return None
    kind = recipe["type"]
    result = recipe["result"]
    # a result needs one item, where the ingredient mapping may give a tag (e.g. dye colours): "result_items" wins
    result_key = result["item"] + (f":{result['data']}" if result.get("data") not in (None, 0) else "")
    result_id = rules.get("result_items", {}).get(result_key) or lookup_item(rules["items"], result["item"], result.get("data"))
    if not isinstance(result_id, str) or result_id.startswith("#"):
        raise Unmapped("ambiguous result " + result["item"])
    out_result = {"count": result.get("count", 1), "id": result_id}
    if kind in ("minecraft:crafting_shaped", "forge:ore_shaped", f"{MOD_ID}:replacable", f"{PLANETS_ID}:replacable"):
        return {
            "type": "minecraft:crafting_shaped",
            "category": category_for(result_id),
            "key": {k: convert_ingredient(rules, v) for k, v in sorted(recipe["key"].items())},
            "pattern": normalize_pattern(recipe["pattern"]),
            "result": out_result,
        }
    if kind in ("minecraft:crafting_shapeless", "forge:ore_shapeless"):
        return {
            "type": "minecraft:crafting_shapeless",
            "category": category_for(result_id),
            "ingredients": [convert_ingredient(rules, i) for i in recipe["ingredients"]],
            "result": out_result,
        }
    raise Unmapped("recipe type " + kind)


def mirror(recipe):
    if "pattern" not in recipe:
        return recipe
    width = max(len(row) for row in recipe["pattern"])
    flipped = dict(recipe)
    flipped["pattern"] = [row.ljust(width)[::-1] for row in recipe["pattern"]]
    return flipped


def normalize_pattern(pattern):
    """Pad rows to equal width so mirrored duplicates compare equal (1.21 trims blank rows/columns itself)."""
    width = max(len(row) for row in pattern)
    return [row.ljust(width) for row in pattern]


def port_recipes(mapping, legacy_assets, out_data):
    rules = mapping["recipes"]
    recipe_dir = os.path.join(legacy_assets, "recipes")
    out_dir = os.path.join(out_data, "recipe")
    if os.path.isdir(out_dir):
        for name in os.listdir(out_dir):
            if name.endswith(".json"):
                os.remove(os.path.join(out_dir, name))
    written, skipped, seen = [], {}, set()
    planets_dir = os.path.join(asset_source(legacy_assets, PLANETS)[0], "recipes")
    files = [(recipe_dir, n) for n in sorted(os.listdir(recipe_dir))]
    if rules.get("planets") and os.path.isdir(planets_dir):
        files += [(planets_dir, n) for n in sorted(os.listdir(planets_dir))]
    for directory, name in files:
        if not name.endswith(".json") or name.startswith("_"):
            continue
        with open(os.path.join(directory, name), encoding="utf-8") as f:
            recipe = json.load(f)
        try:
            converted = convert_recipe(rules, recipe)
        except Unmapped as e:
            skipped.setdefault(str(e), []).append(name[:-5])
            continue
        if converted is None:
            continue
        if converted["result"]["id"] in rules.get("skip_results", []):
            continue
        # 1.12 had exact duplicates (basic_item and food variants of the same food) and explicit mirrored
        # "_alt" patterns; 1.21 shaped recipes already match mirrored, so keep only the first of each.
        canonical = json.dumps(converted, sort_keys=True)
        mirrored = json.dumps(mirror(converted), sort_keys=True)
        if canonical in seen or mirrored in seen:
            continue
        seen.add(canonical)
        base = converted["result"]["id"].split(":", 1)[1]
        out_name, n = base, 2
        while out_name in written:
            out_name, n = f"{base}_{n}", n + 1
        write_json(os.path.join(out_dir, out_name + ".json"), converted)
        written.append(out_name)
    print(f"recipes: {len(written)} converted, {sum(len(v) for v in skipped.values())} skipped")
    for reason, names in sorted(skipped.items()):
        print(f"  unmapped {reason}: {', '.join(names)}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--legacy", required=True, help="path to a Galacticraft-Legacy checkout")
    parser.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."),
                        help="project root (default: this repository)")
    args = parser.parse_args()

    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "legacy_mapping.json"), encoding="utf-8") as f:
        mapping = json.load(f)
    legacy_assets = os.path.join(args.legacy, "src", "main", "resources", "assets", MOD_ID)
    if not os.path.isdir(legacy_assets):
        sys.exit(f"{legacy_assets} not found; --legacy must point at a Galacticraft-Legacy checkout")
    resources = os.path.join(args.out, "src", "main", "resources")
    out_assets = os.path.join(resources, "assets", MOD_ID)
    out_data = os.path.join(resources, "data", MOD_ID)

    port_textures(mapping, legacy_assets, out_assets)
    port_sounds(mapping, legacy_assets, out_assets)
    port_models(mapping, legacy_assets, out_assets)
    port_lang(mapping, legacy_assets, out_assets)
    port_recipes(mapping, legacy_assets, out_data)


if __name__ == "__main__":
    main()
