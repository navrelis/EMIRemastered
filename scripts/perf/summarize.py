"""Parse EMI reload timings from a Minecraft client log.

  py -3 scripts/perf/summarize.py run/logs/latest.log [more.log ...]

One "cycle" = everything from an EMI reload start up to its "Reloaded EMI in Xms" line.
Reported per metric: value of cycle 1 (the join; includes JIT warm-up) and the median of the
later cycles (the /reload runs). Metrics: total, per-plugin register time, "Baked N recipes in",
the asynchronous "Baked recipes after reload in" (sorted manager), and any "PERF <name> <n>ms"
lines (temporary fine-grained timing, if compiled in).
"""
import json
import re
import statistics
import sys

PAT = [
    (re.compile(r"Reloaded plugin from (\S+) in (\d+)ms"), lambda m: ("plugin " + m.group(1), int(m.group(2)))),
    (re.compile(r"Baked (\d+) recipes in (\d+)ms"), lambda m: ("bake (Baked N recipes)", int(m.group(2)))),
    (re.compile(r"Baked recipes after reload in (\d+)ms"), lambda m: ("bake async (sorted manager)", int(m.group(1)))),
    (re.compile(r"PERF (\S+) (\d+)ms"), lambda m: ("perf " + m.group(1), int(m.group(2)))),
    (re.compile(r"Reloading item groups on client thread took (\d+)ms"), lambda m: ("item groups (render thread)", int(m.group(1)))),
]
TOTAL = re.compile(r"Reloaded EMI in (\d+)ms")
COUNT = re.compile(r"Baked (\d+) recipes in")
START = re.compile(r"\[EMI\] (Starting EMI reload\.\.\.|Clearing data)$")


def parse(path):
    cycles, cur, last_closed = [], None, None
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.rstrip()
            if "(EMI)" not in line and "[EMI]" not in line:
                continue
            if START.search(line):
                if cur is None:
                    cur = {"metrics": {}, "recipes": None}
                continue
            m = TOTAL.search(line)
            if m and cur is not None:
                cur["metrics"]["TOTAL Reloaded EMI"] = int(m.group(1))
                cycles.append(cur)
                last_closed, cur = cur, None
                continue
            m = COUNT.search(line)
            target = cur if cur is not None else last_closed
            if m and target is not None:
                target["recipes"] = int(m.group(1))
            for pat, fn in PAT:
                m = pat.search(line)
                if m and target is not None:
                    k, v = fn(m)
                    # async line may arrive after the cycle closed: attribute to the last cycle
                    if k.startswith("bake async") and cur is None and last_closed is not None:
                        last_closed["metrics"][k] = v
                    else:
                        target["metrics"][k] = target["metrics"].get(k, 0) + v
    return cycles


def summary(cycles):
    keys = []
    for c in cycles:
        for k in c["metrics"]:
            if k not in keys:
                keys.append(k)
    rows = {}
    for k in keys:
        first = cycles[0]["metrics"].get(k) if cycles else None
        warm = [c["metrics"][k] for c in cycles[1:] if k in c["metrics"]]
        rows[k] = {"first": first, "warm_median": statistics.median(warm) if warm else None, "warm": warm}
    return rows


def main(argv):
    for path in argv:
        cycles = parse(path)
        rows = summary(cycles)
        recipes = sorted({c["recipes"] for c in cycles if c["recipes"] is not None})
        print(f"== {path}: {len(cycles)} cycles, Baked N recipes: {recipes}")
        order = sorted(rows, key=lambda k: (not k.startswith("TOTAL"), not k.startswith("bake"), k))
        for k in order:
            r = rows[k]
            if (r["warm_median"] or 0) < 5 and (r["first"] or 0) < 5 and k.startswith("plugin"):
                continue
            print(f"  {k:55s} first={r['first']!s:>7}  warm median={r['warm_median']!s:>8}  warm={r['warm']}")
        if path.endswith(".log"):
            with open(path[:-4] + ".json", "w") as f:
                json.dump({"cycles": cycles, "summary": rows, "recipes": recipes}, f, indent=1)


if __name__ == "__main__":
    main(sys.argv[1:])
