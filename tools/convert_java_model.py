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

Usage: python3 tools/convert_java_model.py Legacy/ModelRocketTier1.java > snippet.java
"""
import re
import sys

NUM = r"(-?[0-9.]+)F?"


def f(value):
    value = float(value)
    return ("%sF" % repr(value)).replace(".0F", ".0F")


def main(path):
    src = open(path, encoding="utf-8").read()
    tex = re.search(r"this\.textureWidth = (\d+);\s*this\.textureHeight = (\d+);", src)
    parts = {}
    order = []
    for m in re.finditer(r"this\.(\w+) = new ModelRenderer(?:GC)?\(this, (\d+), (\d+)\);", src):
        parts[m.group(1)] = {"uv": (int(m.group(2)), int(m.group(3))), "boxes": [], "pivot": (0, 0, 0), "rot": (0, 0, 0), "mirror": False}
        order.append(m.group(1))
    for m in re.finditer(r"this\.(\w+)\.addBox\(" + r",\s*".join([NUM] * 6) + r"(?:,\s*" + NUM + r")?\);", src):
        parts[m.group(1)]["boxes"].append(tuple(float(g) for g in m.groups()[1:7]) + ((float(m.group(8)),) if m.group(8) else ()))
    for m in re.finditer(r"this\.(\w+)\.setRotationPoint\(" + r",\s*".join([NUM] * 3) + r"\);", src):
        parts[m.group(1)]["pivot"] = tuple(float(g) for g in m.groups()[1:])
    for m in re.finditer(r"this\.(\w+)\.mirror = true;", src):
        parts[m.group(1)]["mirror"] = True
    for m in re.finditer(r"this\.setRotation\(this\.(\w+), " + r",\s*".join([NUM] * 3) + r"\);", src):
        parts[m.group(1)]["rot"] = tuple(float(g) for g in m.groups()[1:])
    render = re.search(r"public void render\(Entity.*?\{(.*?)\n    \}", src, re.S)
    drawn = re.findall(r"this\.(\w+)\.render\(", render.group(1)) if render else order
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
    main(sys.argv[1])
