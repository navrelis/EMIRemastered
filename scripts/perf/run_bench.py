"""Run one EMI reload benchmark session in the (muted) dev client and store the results.

What it does (project root = the folder containing gradlew):
 1. Keeps a pristine copy of the dev world (run/saves/emi_perf_template, made once from run/saves/devworld),
    moves run/saves/devworld aside and replaces it with a fresh copy of the template. The original devworld
    is restored at the end, whatever happens.
 2. Pack mode (default): generates the load-test datapack (gen_datapack.py) into that world and writes
    run/config/emi_bench.properties, which makes the dev-only BenchItems initializer (src/devtour) register
    the extra items. --vanilla: no datapack, no extra items.
 3. Sets EMI's dev-mode and log-untranslated-tags to false like the pack (run/config/emi.css is restored after)
    and forces soundCategory_master:0.0 in run/options.txt (the dev client must never play sound).
 4. Runs `gradlew runClientTour -Ptour=perf-bench` (or perf-profile with --profile). The tour records the join
    reload, a warm-up reload and 5 warm reloads (EmiReloadManager.reload(), the reload F3+T triggers), dumps
    EMI's baked state and quits the game by itself.
 5. Copies run/tour-out/<tour>/ (bench.jsonl, dump-final/, reload.jfr) and run/logs/latest.log to
    build/emi_perf/<label>/ and prints the summary (summarize.py).

Usage:
  py -3 scripts/perf/run_bench.py --label before-pack
  py -3 scripts/perf/run_bench.py --label before-vanilla --vanilla
  py -3 scripts/perf/run_bench.py --label prof --profile
  py -3 scripts/perf/summarize.py build/emi_perf/before-pack build/emi_perf/after-pack
"""
import argparse
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import summarize  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RUN = os.path.join(ROOT, "run")
SAVES = os.path.join(RUN, "saves")
WORLD = os.path.join(SAVES, "devworld")
TEMPLATE = os.path.join(SAVES, "emi_perf_template")
BACKUP = os.path.join(SAVES, "devworld.emiperf-backup")
OUT = os.path.join(ROOT, "build", "emi_perf")
EMI_CSS = os.path.join(RUN, "config", "emi.css")
EMI_CSS_BACKUP = EMI_CSS + ".emiperf-backup"
BENCH_PROPS = os.path.join(RUN, "config", "emi_bench.properties")

# Pack-like load: ~100k recipes in EMI (pack: 112,836), ~12k items, 4k extra tags
PACK = {"items": 9000, "tools": 800, "armor": 800, "recipes": 60000, "tags": 4000, "enchantments": 0}


def ps(cmd):
    return subprocess.run(["powershell", "-NoProfile", "-Command", cmd], capture_output=True, text=True).stdout


def game_pids():
    """PIDs of dev game JVMs of THIS project (Loom passes the project's launch.cfg path on the command line)."""
    marker = os.path.join(ROOT, ".gradle", "loom-cache", "launch.cfg")
    out = ps("Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | "
             "Where-Object { $_.CommandLine -like '*devlaunchinjector*' -and $_.CommandLine -like '*" + marker + "*' } | "
             "ForEach-Object { $_.ProcessId }")
    return [int(x) for x in out.split() if x.strip().isdigit()]


def prepare_world(vanilla, gen_args):
    if not os.path.isdir(TEMPLATE):
        if not os.path.isfile(os.path.join(WORLD, "level.dat")):
            sys.exit("No dev world: run `gradlew runServer` once (see docs/DEVTOUR.md)")
        shutil.copytree(WORLD, TEMPLATE, ignore=shutil.ignore_patterns("session.lock", "emi_perf*"))
    if os.path.isdir(WORLD) and not os.path.isdir(BACKUP):
        os.rename(WORLD, BACKUP)
    elif os.path.isdir(WORLD):
        shutil.rmtree(WORLD)
    shutil.copytree(TEMPLATE, WORLD)
    if not vanilla:
        os.makedirs(os.path.join(WORLD, "datapacks"), exist_ok=True)
        subprocess.run([sys.executable, os.path.join(HERE, "gen_datapack.py"),
                        "--out", os.path.join(WORLD, "datapacks", "emi_perf.zip"),
                        "--recipes", str(PACK["recipes"]), "--tags", str(PACK["tags"]),
                        "--enchantments", str(PACK["enchantments"]), "--bench-items", str(PACK["items"]),
                        "--bench-tools", str(PACK["tools"]), "--bench-armor", str(PACK["armor"])] + gen_args, check=True)
        with open(BENCH_PROPS, "w", encoding="utf-8", newline="\n") as f:
            f.write(f"items={PACK['items']}\ntools={PACK['tools']}\narmor={PACK['armor']}\n")


def restore():
    if os.path.isdir(BACKUP):
        if os.path.isdir(WORLD):
            shutil.rmtree(WORLD)
        os.rename(BACKUP, WORLD)
    if os.path.isfile(EMI_CSS_BACKUP):
        shutil.copyfile(EMI_CSS_BACKUP, EMI_CSS)
        os.remove(EMI_CSS_BACKUP)
    if os.path.isfile(BENCH_PROPS):
        os.remove(BENCH_PROPS)


def ensure_muted():
    """The dev client must never play sound: force soundCategory_master:0.0 in run/options.txt before every launch."""
    path = os.path.join(RUN, "options.txt")
    lines = []
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            lines = [l.rstrip("\n") for l in f if not l.startswith("soundCategory_master:")]
    if not any(l.startswith("onboardAccessibility:") for l in lines):
        lines.append("onboardAccessibility:false")
    if not any(l.startswith("pauseOnLostFocus:") for l in lines):
        lines.append("pauseOnLostFocus:false")
    lines.append("soundCategory_master:0.0")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def pack_like_config():
    """Like the pack: dev-mode false (dev mode adds an O(recipes^2) id check) and log-untranslated-tags false."""
    if not os.path.isfile(EMI_CSS):
        return
    shutil.copyfile(EMI_CSS, EMI_CSS_BACKUP)
    with open(EMI_CSS, encoding="utf-8") as f:
        text = f.read()
    text = text.replace("dev-mode: true;", "dev-mode: false;").replace("log-untranslated-tags: true;", "log-untranslated-tags: false;")
    with open(EMI_CSS, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--label", required=True)
    ap.add_argument("--vanilla", action="store_true", help="no generated datapack and no extra items")
    ap.add_argument("--profile", action="store_true", help="run the perf-profile tour (JFR of 3 warm reloads)")
    ap.add_argument("--timeout", type=int, default=3600, help="seconds")
    ap.add_argument("gen_args", nargs="*", help="extra args for gen_datapack.py (after --), e.g. --empty-stone-tool-materials")
    a = ap.parse_args()
    tour = "perf-profile" if a.profile else "perf-bench"
    dest = os.path.join(OUT, a.label)
    if os.path.isdir(dest):
        sys.exit(dest + " exists")
    if game_pids():
        sys.exit("A dev client of this project is already running")
    os.makedirs(OUT, exist_ok=True)
    gradle_log_path = os.path.join(OUT, a.label + ".gradle.txt")
    ok = False
    try:
        prepare_world(a.vanilla, a.gen_args)
        pack_like_config()
        ensure_muted()
        start = time.time()
        with open(gradle_log_path, "w") as gradle_log:
            proc = subprocess.Popen([os.path.join(ROOT, "gradlew.bat"), "runClientTour", "-Ptour=" + tour, "--console=plain"],
                                    cwd=ROOT, stdout=gradle_log, stderr=subprocess.STDOUT)
            try:
                proc.wait(timeout=a.timeout)
                ok = proc.returncode == 0
            except subprocess.TimeoutExpired:
                print("timeout, stopping the game")
                for pid in game_pids():
                    subprocess.run(["taskkill", "/PID", str(pid), "/F"], capture_output=True)
                proc.wait(timeout=120)
        print(f"game run took {time.time() - start:.0f}s, gradle exit {proc.returncode}")
    finally:
        restore()
    tour_out = os.path.join(RUN, "tour-out", tour)
    if os.path.isdir(tour_out):
        shutil.copytree(tour_out, dest)
    os.makedirs(dest, exist_ok=True)
    log = os.path.join(RUN, "logs", "latest.log")
    if os.path.isfile(log):
        shutil.copyfile(log, os.path.join(dest, "latest.log"))
    summarize.main([dest])
    if not ok:
        sys.exit("benchmark run failed, see " + gradle_log_path)


if __name__ == "__main__":
    main()
