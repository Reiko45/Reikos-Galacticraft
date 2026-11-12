#!/usr/bin/env python3
"""Static checks for client assets, which a headless server/GameTest run never loads.

* every blockstate variant points at an existing model
* every model's parent (in our namespace) exists and every texture it references exists as a PNG
* every block and item has an en_us name
* every texture an OBJ model's .mtl uses outside block/ and item/ is stitched into the block atlas
  (assets/minecraft/atlases/blocks.json), or it renders as the missing texture

Run after ./gradlew runData:  python3 tools/check_assets.py
"""
import json
import os
import sys

MOD_ID = "galacticraftcore"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RESOURCE_ROOTS = [os.path.join(ROOT, "src", "main", "resources"), os.path.join(ROOT, "src", "generated", "resources")]


def find(*parts):
    for base in RESOURCE_ROOTS:
        path = os.path.join(base, "assets", MOD_ID, *parts)
        if os.path.isfile(path):
            return path
    return None


def list_dir(*parts):
    names = set()
    for base in RESOURCE_ROOTS:
        path = os.path.join(base, "assets", MOD_ID, *parts)
        if os.path.isdir(path):
            names.update(n for n in os.listdir(path) if n.endswith(".json"))
    return sorted(names)


def split(ref):
    ns, _, path = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
    return ns, path


def model_refs(blockstate):
    refs = []
    for variant in blockstate.get("variants", {}).values():
        for v in variant if isinstance(variant, list) else [variant]:
            refs.append(v["model"])
    for part in blockstate.get("multipart", []):
        apply = part["apply"]
        for v in apply if isinstance(apply, list) else [apply]:
            refs.append(v["model"])
    return refs


def main():
    errors = []
    checked_models = set()

    def check_model(ref, origin):
        ns, path = split(ref)
        if ns != MOD_ID or ref in checked_models:
            return
        checked_models.add(ref)
        file = find("models", path + ".json")
        if not file:
            errors.append(f"{origin}: model {ref} does not exist")
            return
        with open(file, encoding="utf-8") as f:
            model = json.load(f)
        if "parent" in model:
            check_model(model["parent"], ref)
        for slot, tex in model.get("textures", {}).items():
            if tex.startswith("#"):
                continue
            tns, tpath = split(tex)
            if tns == MOD_ID and not find("textures", tpath + ".png"):
                errors.append(f"{ref}: texture '{slot}' -> {tex} does not exist")

    for name in list_dir("blockstates"):
        with open(find("blockstates", name), encoding="utf-8") as f:
            for ref in model_refs(json.load(f)):
                check_model(ref, "blockstate " + name)
    for name in list_dir("models", "item"):
        check_model(f"{MOD_ID}:item/{name[:-5]}", "item model")

    lang_file = find("lang", "en_us.json")
    lang = json.load(open(lang_file, encoding="utf-8")) if lang_file else {}
    blocks = {n[:-5] for n in list_dir("blockstates")}
    for name in blocks:
        if f"block.{MOD_ID}.{name}" not in lang:
            errors.append(f"en_us: missing block.{MOD_ID}.{name}")
    for name in list_dir("models", "item"):
        item = name[:-5]
        if item not in blocks and f"item.{MOD_ID}.{item}" not in lang:
            errors.append(f"en_us: missing item.{MOD_ID}.{item}")

    atlas_file = os.path.join(ROOT, "src", "main", "resources", "assets", "minecraft", "atlases", "blocks.json")
    sources = json.load(open(atlas_file, encoding="utf-8"))["sources"] if os.path.isfile(atlas_file) else []
    singles = {s.get("resource") for s in sources if s.get("type") == "single"}
    dirs = [s.get("source") for s in sources if s.get("type") == "directory"]
    for base in RESOURCE_ROOTS:
        models_dir = os.path.join(base, "assets", MOD_ID, "models")
        for dirpath, _, files in os.walk(models_dir):
            for name in files:
                if not name.endswith(".mtl"):
                    continue
                for line in open(os.path.join(dirpath, name), encoding="utf-8"):
                    if not line.startswith("map_Kd "):
                        continue
                    tex = line.split(None, 1)[1].strip()
                    tns, tpath = split(tex)
                    if tns != MOD_ID or tpath.startswith(("block/", "item/")):
                        continue
                    if tex not in singles and not any(tpath.startswith(d + "/") for d in dirs):
                        errors.append(f"{name}: {tex} is not in the block atlas")

    for e in errors:
        print("ERROR", e)
    print(f"checked {len(blocks)} blockstates, {len(checked_models)} models, {len(lang)} translations: {len(errors)} problems")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
