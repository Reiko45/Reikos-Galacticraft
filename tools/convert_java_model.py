#!/usr/bin/env python3
"""Convert a 1.12.2 Galacticraft box model (ModelBase with ModelRenderer fields built in the constructor) into a
1.21 LayerDefinition method body, keeping texture offsets, boxes, pivots, rotations and mirroring.

Only the straightforward pattern used by ModelRocketTier1 and ModelLander is understood:
    this.part = new ModelRenderer(this, u, v);       (or ModelRendererGC)
    this.part.addBox(x, y, z, w, h, d[, inflate]);
    this.part.setRotationPoint(x, y, z);
    this.part.mirror = true;
    this.setRotation(this.part, rx, ry, rz);
The parts are emitted in the order the model's render() method draws them.

A variable inflate argument (``par1``, ``scale``...) counts as 0; a texture size chosen with ``a ? b : c`` takes the
second value. ``--json`` prints the parts (texture offset, boxes, pivot, rotation, mirror) instead, to rebuild a model
around vanilla part names.

Usage: python3 tools/convert_java_model.py [--json] Legacy/ModelRocketTier1.java > snippet.java
"""
import re
import sys

NUM = r"(-?[0-9.]+)F?"


def f(value):
    value = float(value)
    return ("%sF" % repr(value)).replace(".0F", ".0F")


def parse(path):
    src = open(path, encoding="utf-8").read()
    # Array parts (ModelSpiderQueen's leg1[0]...) become plain names (leg1_0)
    src = re.sub(r"(\w+)\[(\d+)\]", r"\1_\2", src)
    # "this." is optional: some 1.12.2 models (ModelJuicer, ModelSpiderQueen) leave it out
    src = re.sub(r"(?<![\w.])(textureWidth|textureHeight|setRotation)\b", r"this.\1", src)
    src = re.sub(r"(?m)^(\s*)(\w+)(\s*=\s*new ModelRenderer|\.addBox\(|\.setRotationPoint\(|\.mirror = |\.render\()", r"\1this.\2\3", src)
    src = re.sub(r"this\.setRotation\((\w+),", r"this.setRotation(this.\1,", src)
    tex = re.search(r"this\.textureWidth = (?:[^;?]*\?\s*\d+\s*:\s*)?(\d+);\s*this\.textureHeight = (?:[^;?]*\?\s*\d+\s*:\s*)?(\d+);", src)
    parts = {}
    order = []
    for m in re.finditer(r"this\.(\w+) = new ModelRenderer(?:GC)?\(this, (\d+), (\d+)\);", src):
        parts[m.group(1)] = {"uv": (int(m.group(2)), int(m.group(3))), "boxes": [], "pivot": (0, 0, 0), "rot": (0, 0, 0), "mirror": False}
        order.append(m.group(1))
    for m in re.finditer(r"this\.(\w+)\.addBox\(" + r",\s*".join([NUM] * 6) + r"(?:,\s*(" + NUM[1:-4] + r"|[A-Za-z_]\w*)F?)?\);", src):
        if m.group(1) not in parts:
            continue
        inflate = m.group(8)
        inflate = float(inflate) if inflate and re.match(r"-?[0-9.]+$", inflate) else 0.0
        parts[m.group(1)]["boxes"].append(tuple(float(g) for g in m.groups()[1:7]) + ((inflate,) if inflate else ()))
    for m in re.finditer(r"this\.(\w+)\.setRotationPoint\(" + r",\s*".join([NUM] * 3) + r"\);", src):
        parts[m.group(1)]["pivot"] = tuple(float(g) for g in m.groups()[1:])
    for m in re.finditer(r"this\.(\w+)\.mirror = (true|false);", src):
        if m.group(1) in parts:
            parts[m.group(1)]["mirror"] = m.group(2) == "true"
    for m in re.finditer(r"this\.setRotation\(this\.(\w+), " + r",\s*".join([NUM] * 3) + r"\);", src):
        parts[m.group(1)]["rot"] = tuple(float(g) for g in m.groups()[1:])
    return src, tex, parts, order


def main(path, as_json=False):
    src, tex, parts, order = parse(path)
    if as_json:
        import json
        print(json.dumps({"texture": [int(tex.group(1)), int(tex.group(2))] if tex else None,
                          "order": order, "parts": parts}, indent=1))
        return
    render = re.search(r"public void render\(Entity.*?\{(.*?)\n    \}", src, re.S)
    drawn = re.findall(r"this\.(\w+)\.render\(", render.group(1)) if render else order
    drawn = [name for name in dict.fromkeys(drawn) if name in parts]
    if render and "[i]" in render.group(1):
        drawn = order  # parts drawn in a loop: keep the order they were made in
    out = ["        final MeshDefinition mesh = new MeshDefinition();", "        final PartDefinition root = mesh.getRoot();"]
    for name in drawn:
        p = parts[name]
        cubes = "CubeListBuilder.create().texOffs(%d, %d)" % p["uv"] + (".mirror()" if p["mirror"] else "")
        for box in p["boxes"]:
            args = ", ".join(f(v) for v in box[:6])
            if len(box) > 6 and box[6] != 0:
                args += ", new CubeDeformation(%s)" % f(box[6])
            cubes += ".addBox(%s)" % args
        pose = "PartPose.offsetAndRotation(%s)" % ", ".join(f(v) for v in p["pivot"] + p["rot"])
        out.append('        root.addOrReplaceChild("%s", %s,\n                %s);' % (name.lower(), cubes, pose))
    out.append("        return LayerDefinition.create(mesh, %s, %s);" % (tex.group(1), tex.group(2)))
    print("\n".join(out))


if __name__ == "__main__":
    if sys.argv[1] == "--json":
        main(sys.argv[2], True)
    else:
        main(sys.argv[1])
