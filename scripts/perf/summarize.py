"""Summarise EMI reload benchmark results written by run_bench.py.

  py -3 scripts/perf/summarize.py build/emi_perf/<before> build/emi_perf/<after> [build/emi_perf/<control> ...]

Per result folder: the mean (and min/max) of every phase over the measured warm reloads (labels "warm-*") from
bench.jsonl, plus the join reload (cold: first reload of the JVM, on world join). With two or more folders: a
markdown before/after table (first folder = before, second = after; warm means and the cold join reload) and a
matrix of the dump section hashes (dump-final/hashes.txt) of all folders. Sections that also differ between two
runs of the same build (a control folder) are run-to-run noise of upstream EMI (identity-hash map orders); see
the *_norm sections for order-normalised variants.
"""
import json
import os
import statistics
import sys

ORDER = ["total", "clear", "init", "tags", "stack list", "plugins", "stack bake", "late recipes", "recipe bake",
         "bom+persistent", "search bake", "finishing", "async sorted manager (ms)", "item groups (render thread, ms)",
         "heap before reload (MB, after GC)", "heap after reload (MB, after GC)",
         "allocated during reload (MB, all threads)"]


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


def change(b, a):
    return f"{(a - b) / b * 100:+.0f}%" if isinstance(b, (int, float)) and isinstance(a, (int, float)) and b else ""


def fmt(v):
    return f"{v:.1f}" if isinstance(v, (int, float)) else ""


def main(argv):
    results = []
    for folder in argv:
        rows, status, hashes = load(folder)
        stats = warm_stats(rows)
        join = next((r for r in rows if r.get("label") == "join"), {})
        results.append((folder, stats, hashes, join))
        print(f"== {folder}: tour status {status}, {len(rows)} records")
        for k in ordered_keys(list(stats)):
            mean, lo, hi, n = stats[k]
            print(f"  {k:42s} mean {mean:10.1f}  min {lo:10.1f}  max {hi:10.1f}  (n={n})  join {join.get(k, '')}")
    if len(results) >= 2:
        base, other = results[0], results[1]
        bn, an = os.path.basename(base[0]), os.path.basename(other[0])
        print(f"\n| phase | {bn} warm | {an} warm | change | {bn} cold (join) | {an} cold (join) | change |")
        print("|---|---:|---:|---:|---:|---:|---:|")
        for k in ordered_keys([k for k in base[1] if k in other[1]]):
            b, a = base[1][k][0], other[1][k][0]
            bj, aj = base[3].get(k), other[3].get(k)
            print(f"| {k} | {b:.1f} | {a:.1f} | {change(b, a)} | {fmt(bj)} | {fmt(aj)} | {change(bj, aj)} |")
        sections = []
        for r in results:
            for k in r[2]:
                if k not in sections:
                    sections.append(k)
        if sections:
            names = [os.path.basename(r[0]) for r in results]
            print("\n| dump section (lines hash) | " + " | ".join(names) + " |")
            print("|---|" + "---|" * len(names))
            for s in sections:
                ref = base[2].get(s)
                cells = []
                for r in results:
                    v = r[2].get(s)
                    cells.append("(missing)" if v is None else v if r is base else "same" if v == ref else "DIFF " + v)
                print(f"| {s} | " + " | ".join(cells) + " |")


if __name__ == "__main__":
    main(sys.argv[1:])
