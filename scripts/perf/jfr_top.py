"""Top methods of EMI's reload threads in a JFR recording (from run_bench.py --profile).

  py -3 scripts/perf/jfr_top.py build/emi_perf/<label>/reload.jfr [--top 40] [--filter dev.emi]

Uses the JDK's `jfr` tool (must be on PATH). Only execution samples whose stack contains the EMI reload
worker (EmiReloadManager$ReloadWorker.run) or the async recipe sorter (EmiRecipes$Worker.run) are counted.
Prints self time (top frame) and inclusive time (method anywhere on the stack) as % of those samples.
"""
import argparse
import collections
import re
import subprocess

ROOTS = ("EmiReloadManager$ReloadWorker.run", "EmiRecipes$Worker.run")
FRAME = re.compile(r"^\s+(\S+)\(.*\)\s+line:")


def samples(path):
    out = subprocess.run(["jfr", "print", "--events", "jdk.ExecutionSample", "--stack-depth", "256", path],
                         capture_output=True, text=True, encoding="utf-8", errors="replace").stdout
    frames = None
    for line in out.splitlines():
        if line.startswith("jdk.ExecutionSample"):
            frames = []
        elif frames is not None:
            m = FRAME.match(line)
            if m:
                frames.append(m.group(1))
            elif line.startswith("}"):
                yield frames
                frames = None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jfr")
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--filter", default="", help="only list methods starting with this prefix in the inclusive table")
    a = ap.parse_args()
    self_counts = collections.Counter()
    incl = collections.Counter()
    by_root = collections.Counter()
    total = 0
    for frames in samples(a.jfr):
        root = next((r for r in ROOTS if any(f.endswith(r) for f in frames)), None)
        if root is None:
            continue
        total += 1
        by_root[root] += 1
        self_counts[frames[0]] += 1
        for f in set(frames):
            incl[f] += 1
    print(f"{total} samples on EMI reload threads: {dict(by_root)}")
    print("\n-- self --")
    for m, c in self_counts.most_common(a.top):
        print(f"{c * 100 / total:6.2f}%  {m}")
    print("\n-- inclusive --")
    n = 0
    for m, c in incl.most_common():
        if a.filter and not m.startswith(a.filter):
            continue
        print(f"{c * 100 / total:6.2f}%  {m}")
        n += 1
        if n >= a.top:
            break


if __name__ == "__main__":
    main()
