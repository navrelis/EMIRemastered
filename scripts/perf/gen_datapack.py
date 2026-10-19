"""Generate a load-test datapack for EMI reload measurements (dev only, never shipped).

The datapack adds many crafting/cooking/stonecutting/smithing recipes, many item tags
and data-driven enchantments (which multiply EMI's anvil/grindstone recipes). Datapacks
cannot add items; the extra items come from the dev-only BenchItems initializer in
src/devtour (config/emi_bench.properties, written by run_bench.py). Pass the same counts
with --bench-items/--bench-tools/--bench-armor so recipes and tags use those items and the
bench tools/armor join the vanilla enchantable tags (like modded tools do).

Vanilla item ids are read from the Minecraft jar in the Loom cache
(data/minecraft/recipe + data/minecraft/tags/item), so every generated recipe parses.

Usage (from the project root):
  py -3 scripts/perf/gen_datapack.py --out run/saves/devworld/datapacks/emi_perf.zip
Options: see --help. Output is deterministic for a given --seed.
"""
import argparse
import glob
import json
import os
import random
import shutil
import sys
import zipfile

PACK_FORMAT = 48  # 1.21.1
NS = "emi_perf"


def find_mc_jar():
    home = os.path.expanduser("~")
    candidates = [
        os.path.join(home, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-common.jar"),
        os.path.join(home, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-merged.jar"),
    ]
    candidates += glob.glob(os.path.join(home, ".gradle", "caches", "fabric-loom", "minecraftMaven", "net", "minecraft",
                                         "minecraft-common", "1.21.1-*", "*.jar"))
    for c in candidates:
        if os.path.isfile(c) and not c.endswith("-sources.jar"):
            return c
    return None


def collect_items(jar_path):
    items = set()

    def add(v):
        if isinstance(v, str) and not v.startswith("#"):
            items.add(v if ":" in v else "minecraft:" + v)

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in ("item", "id") and isinstance(v, str):
                    add(v)
                else:
                    walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)

    with zipfile.ZipFile(jar_path) as z:
        for name in z.namelist():
            if name.startswith("data/minecraft/recipe/") and name.endswith(".json"):
                walk(json.loads(z.read(name)))
            elif name.startswith("data/minecraft/tags/item/") and name.endswith(".json"):
                for v in json.loads(z.read(name)).get("values", []):
                    add(v if isinstance(v, str) else v.get("id"))
    items.discard("minecraft:air")
    return sorted(items)


VANILLA_TAGS = ["minecraft:planks", "minecraft:logs", "minecraft:wool", "minecraft:stone_tool_materials",
                "minecraft:coals", "minecraft:sand", "minecraft:fishes", "minecraft:flowers", "minecraft:saplings",
                "minecraft:leaves", "minecraft:wooden_slabs", "minecraft:terracotta"]


FILES = {}


def write_json(path, obj):
    FILES[path] = json.dumps(obj, separators=(",", ":"))


def write_text(path, text):
    FILES[path] = text


def flush(out):
    """Write the collected files as a folder, or as a zip when --out ends with .zip (much faster on Windows)."""
    if out.endswith(".zip"):
        os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=1) as z:
            for path, text in FILES.items():
                z.writestr(path.replace(os.sep, "/"), text)
    else:
        for path, text in FILES.items():
            full = os.path.join(out, path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", required=True, help="datapack folder to (re)create, e.g. run/saves/devworld/datapacks/emi_perf")
    ap.add_argument("--recipes", type=int, default=50000)
    ap.add_argument("--tags", type=int, default=3000, help="number of generated item tags")
    ap.add_argument("--enchantments", type=int, default=40)
    ap.add_argument("--bench-items", type=int, default=0, help="plain emi_bench:item_NNNNN items registered by BenchItems")
    ap.add_argument("--bench-tools", type=int, default=0, help="emi_bench:sword_/pickaxe_NNNNN tools registered by BenchItems")
    ap.add_argument("--bench-armor", type=int, default=0, help="emi_bench:armor_NNNNN pieces registered by BenchItems")
    ap.add_argument("--empty-stone-tool-materials", action="store_true",
                    help="also make #minecraft:stone_tool_materials empty (reproduces the repair AIOOBE in VanillaPlugin.addRepair)")
    ap.add_argument("--seed", type=int, default=1234)
    ap.add_argument("--mc-jar", default=None, help="Minecraft 1.21.1 jar containing data/ (default: Loom cache)")
    a = ap.parse_args()

    jar = a.mc_jar or find_mc_jar()
    if not jar:
        sys.exit("Minecraft 1.21.1 jar not found; pass --mc-jar")
    items = collect_items(jar)
    swords = [f"emi_bench:sword_{i:05d}" for i in range(0, a.bench_tools, 2)]
    pickaxes = [f"emi_bench:pickaxe_{i:05d}" for i in range(1, a.bench_tools, 2)]
    armor = [f"emi_bench:armor_{i:05d}" for i in range(a.bench_armor)]
    items = items + [f"emi_bench:item_{i:05d}" for i in range(a.bench_items)] + swords + pickaxes + armor
    rnd = random.Random(a.seed)

    out = a.out
    if os.path.isdir(out):
        shutil.rmtree(out)
    elif os.path.isfile(out):
        os.remove(out)
    write_json("pack.mcmeta", {"pack": {"pack_format": PACK_FORMAT, "description": "EMI reload load test (generated)"}})
    data = "data"

    # Tags: sizes skewed small, a few large; some include vanilla tags or other generated tags
    tag_ids = []
    for t in range(a.tags):
        r = rnd.random()
        size = rnd.randint(2, 8) if r < 0.7 else rnd.randint(9, 40) if r < 0.95 else rnd.randint(41, 200)
        values = rnd.sample(items, min(size, len(items)))
        if rnd.random() < 0.05:
            values.append("#" + rnd.choice(VANILLA_TAGS))
        if t > 10 and rnd.random() < 0.05:
            values.append("#" + rnd.choice(tag_ids))
        tid = f"{NS}:group/t{t:05d}"
        tag_ids.append(tid)
        write_json(os.path.join(data, NS, "tags", "item", "group", f"t{t:05d}.json"), {"replace": False, "values": values})
    if a.empty_stone_tool_materials:
        write_json(os.path.join(data, "minecraft", "tags", "item", "stone_tool_materials.json"), {"replace": True, "values": []})

    all_tags = tag_ids + VANILLA_TAGS

    def ingredient(tag_chance=0.35):
        r = rnd.random()
        if all_tags and r < tag_chance:
            return {"tag": rnd.choice(all_tags)}
        if r < tag_chance + 0.04:
            return [{"item": i} for i in rnd.sample(items, rnd.randint(2, 6))]
        return {"item": rnd.choice(items)}

    def result():
        # count 1: the item list does not know max stack sizes (count > max stack size fails to parse)
        return {"id": rnd.choice(items), "count": 1}

    rdir = os.path.join(data, NS, "recipe")
    for n in range(a.recipes):
        r = rnd.random()
        sub = f"r{n // 1000:03d}"
        path = os.path.join(rdir, sub, f"x{n:06d}.json")
        if r < 0.60:
            w, h = rnd.randint(1, 3), rnd.randint(1, 3)
            symbols = "ABCDEFGHI"
            key, pattern, used = {}, [], 0
            for y in range(h):
                row = ""
                for x in range(w):
                    if rnd.random() < 0.2 and not (x == 0 and y == 0):
                        row += " "
                        continue
                    if key and rnd.random() < 0.4:
                        row += rnd.choice(list(key.keys()))
                        continue
                    s = symbols[used]
                    used += 1
                    key[s] = ingredient()
                    row += s
                pattern.append(row)
            obj = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": result()}
        elif r < 0.85:
            obj = {"type": "minecraft:crafting_shapeless", "category": "misc",
                   "ingredients": [ingredient() for _ in range(rnd.randint(1, 9))], "result": result()}
        elif r < 0.92:
            t = rnd.choice(["smelting", "blasting", "smoking", "campfire_cooking"])
            obj = {"type": "minecraft:" + t, "category": "misc", "ingredient": ingredient(0.2), "result": {"id": rnd.choice(items)},
                   "experience": 0.1, "cookingtime": 100}
        elif r < 0.97:
            obj = {"type": "minecraft:stonecutting", "ingredient": ingredient(0.2), "result": result()}
        else:
            obj = {"type": "minecraft:smithing_transform", "template": ingredient(0.1), "base": ingredient(0.1),
                   "addition": ingredient(0.1), "result": {"id": rnd.choice(items), "count": 1}}
        write_json(path, obj)

    targets = ["#minecraft:enchantable/durability", "#minecraft:enchantable/weapon", "#minecraft:enchantable/armor",
               "#minecraft:enchantable/mining", "#minecraft:enchantable/sword", "#minecraft:enchantable/bow"]
    for e in range(a.enchantments):
        write_json(os.path.join(data, NS, "enchantment", f"e{e:03d}.json"), {
            "description": {"text": f"Perf {e}"},
            "supported_items": targets[e % len(targets)],
            "weight": 1,
            "max_level": 1 + e % 5,
            "min_cost": {"base": 1, "per_level_above_first": 10},
            "max_cost": {"base": 51, "per_level_above_first": 10},
            "anvil_cost": 1,
            "slots": ["any"],
        })

    # bench tools and armor join the vanilla enchantable tags (replace: false), like modded tools do
    def extend(tag, values):
        if values:
            write_json(os.path.join(data, "minecraft", "tags", "item", *tag.split("/")) + ".json", {"replace": False, "values": values})
    slots = {"head_armor": armor[0::4], "chest_armor": armor[1::4], "leg_armor": armor[2::4], "foot_armor": armor[3::4]}
    extend("swords", swords)
    extend("pickaxes", pickaxes)
    for tag, values in slots.items():
        extend("enchantable/" + tag, values)
    extend("enchantable/armor", armor)
    extend("enchantable/equippable", armor)
    extend("enchantable/sword", swords)
    extend("enchantable/sharp_weapon", swords)
    extend("enchantable/fire_aspect", swords)
    extend("enchantable/weapon", swords)
    extend("enchantable/mining", pickaxes)
    extend("enchantable/mining_loot", pickaxes)
    extend("enchantable/durability", swords + pickaxes + armor)
    extend("enchantable/vanishing", swords + pickaxes + armor)

    flush(out)
    print(f"items={len(items)} tags={a.tags} recipes={a.recipes} enchantments={a.enchantments} -> {out}")


if __name__ == "__main__":
    main()
