"""Summarise EMI reload benchmark results written by run_bench.py.

  py -3 scripts/perf/summarize.py build/emi_perf/<label> [build/emi_perf/<label2> ...]

Per result folder: the mean (and min/max) of every phase over the measured warm reloads (labels "warm-*"),
taken from bench.jsonl, plus the join reload. With two or more folders: a markdown before/after table
(first folder = before) and a comparison of the dump hashes (dump-final/hashes.txt).
"""
import json
import os
import statistics
import sys

ORDER = ["total", "clear", "init", "tags", "stack list", "plugins", "stack bake", "late recipes", "recipe bake",
         "bom+persistent", "search bake", "finishing", "async sorted manager (ms)"]


def load(folder):
    rows = []
    path = os.path.join(folder, "bench.jsonl")
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            rows = [json.loads(l) for l in f if l.strip()]
    status = None
    sp = os.path.join(folder, "summary.json")
    if os.path.isfile(sp):
        with open(sp, encoding="utf-8") as f:
            status = json.load(f).get("status")
    hashes = {}
    hp = os.path.join(folder, "dump-final", "hashes.txt")
    if os.path.isfile(hp):
        with open(hp, encoding="utf-8") as f:
            for l in f:
                k, v = l.rstrip("\n").split(" ", 1)
                hashes[k] = v
    return rows, status, hashes


def warm_stats(rows):
    warm = [r for r in rows if str(r.get("label", "")).startswith("warm-")]
    keys = []
    for r in warm:
        for k, v in r.items():
            if isinstance(v, (int, float)) and k not in keys:
                keys.append(k)
    stats = {}
    for k in keys:
        vals = [r[k] for r in warm if k in r]
        stats[k] = (statistics.mean(vals), min(vals), max(vals), len(vals))
    return stats


def ordered_keys(keys):
    first = [k for k in ORDER if k in keys]
    plugins = sorted(k for k in keys if k.startswith("plugin "))
    rest = [k for k in keys if k not in first and k not in plugins]
    return first + plugins + rest


def main(argv):
    results = []
    for folder in argv:
        rows, status, hashes = load(folder)
        stats = warm_stats(rows)
        join = next((r for r in rows if r.get("label") == "join"), {})
        results.append((folder, stats, hashes))
        print(f"== {folder}: tour status {status}, {len(rows)} records")
        for k in ordered_keys(list(stats)):
            mean, lo, hi, n = stats[k]
            print(f"  {k:34s} mean {mean:10.1f}  min {lo:10.1f}  max {hi:10.1f}  (n={n})  join {join.get(k, '')}")
    if len(results) >= 2:
        base = results[0]
        for other in results[1:]:
            print(f"\n| phase | {os.path.basename(base[0])} (ms) | {os.path.basename(other[0])} (ms) | change |")
            print("|---|---:|---:|---:|")
            keys = ordered_keys([k for k in base[1] if k in other[1]])
            for k in keys:
                b, a = base[1][k][0], other[1][k][0]
                change = f"{(a - b) / b * 100:+.0f}%" if b else ""
                print(f"| {k} | {b:.1f} | {a:.1f} | {change} |")
            if base[2] and other[2]:
                same = [k for k in base[2] if other[2].get(k) == base[2][k]]
                diff = [k for k in base[2] if other[2].get(k) != base[2][k]]
                print(f"\ndump sections identical: {len(same)}/{len(base[2])}" + (f", DIFFERENT: {diff}" if diff else ""))


if __name__ == "__main__":
    main(sys.argv[1:])
