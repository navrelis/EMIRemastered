"""Run one EMI reload benchmark session in the dev client and summarise the timings.

What it does (project root = the folder containing gradlew):
 1. Makes a pristine copy of the dev world once (run/saves/emi_perf_template, from run/saves/devworld
    or run/world), moves the current run/saves/devworld aside and replaces it with a fresh copy
    (the reload loop counter lives in the world's scoreboard). The original devworld is restored at the end.
 2. Generates the load-test datapack (gen_datapack.py) into that world, or only the reload loop with --vanilla.
 3. Starts `gradlew runClientWorld` (quick-plays run/saves/devworld), waits until the log shows
    1 + --reloads "Reloaded EMI in" lines, then stops ONLY the game JVM it started
    (matched by this project's Loom launch.cfg path on the command line).
 4. Copies run/logs/latest.log to build/emi_perf/<label>.log and prints / stores (<label>.json) the timings.

Usage:
  py -3 scripts/perf/run_bench.py --label before            # generated datapack (50k recipes, 3k tags, 40 enchantments)
  py -3 scripts/perf/run_bench.py --label before-vanilla --vanilla
  py -3 scripts/perf/summarize.py build/emi_perf/before.log  # re-parse a saved log
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
SAVES = os.path.join(ROOT, "run", "saves")
WORLD = os.path.join(SAVES, "devworld")
TEMPLATE = os.path.join(SAVES, "emi_perf_template")
BACKUP = os.path.join(SAVES, "devworld.emiperf-backup")
LOG = os.path.join(ROOT, "run", "logs", "latest.log")
OUT = os.path.join(ROOT, "build", "emi_perf")


def ps(cmd):
    return subprocess.run(["powershell", "-NoProfile", "-Command", cmd], capture_output=True, text=True).stdout


def game_pids():
    # Loom passes -Dfabric.dli.config=<project>/.gradle/loom-cache/launch.cfg to the game JVM
    marker = os.path.join(ROOT, ".gradle", "loom-cache", "launch.cfg").replace("\\", "\\\\")
    out = ps("Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | "
             "Where-Object { $_.CommandLine -like '*devlaunchinjector*' -and $_.CommandLine -like '*" + marker.replace("\\\\", "\\") + "*' } | "
             "ForEach-Object { $_.ProcessId }")
    return [int(x) for x in out.split() if x.strip().isdigit()]


def prepare_world(vanilla, gen_args):
    if not os.path.isdir(TEMPLATE):
        src = WORLD if os.path.isfile(os.path.join(WORLD, "level.dat")) else os.path.join(ROOT, "run", "world")
        if not os.path.isfile(os.path.join(src, "level.dat")):
            sys.exit("No dev world found (run/saves/devworld or run/world)")
        shutil.copytree(src, TEMPLATE, ignore=shutil.ignore_patterns("session.lock", "emi_perf*"))
    if os.path.isdir(WORLD) and not os.path.isdir(BACKUP):
        os.rename(WORLD, BACKUP)
    elif os.path.isdir(WORLD):
        shutil.rmtree(WORLD)
    shutil.copytree(TEMPLATE, WORLD)
    os.makedirs(os.path.join(WORLD, "datapacks"), exist_ok=True)
    args = [sys.executable, os.path.join(HERE, "gen_datapack.py"), "--out", os.path.join(WORLD, "datapacks", "emi_perf.zip")]
    if vanilla:
        args += ["--recipes", "0", "--tags", "0", "--enchantments", "0"]
    subprocess.run(args + gen_args, check=True)


def restore_world():
    if os.path.isdir(BACKUP):
        if os.path.isdir(WORLD):
            shutil.rmtree(WORLD)
        os.rename(BACKUP, WORLD)


def ensure_muted():
    """The dev client must never play sound: force soundCategory_master:0.0 in run/options.txt before every launch."""
    path = os.path.join(ROOT, "run", "options.txt")
    lines = []
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            lines = [l.rstrip("
") for l in f if not l.startswith("soundCategory_master:")]
    if not any(l.startswith("onboardAccessibility:") for l in lines):
        lines.append("onboardAccessibility:false")
    if not any(l.startswith("pauseOnLostFocus:") for l in lines):
        lines.append("pauseOnLostFocus:false")  # a paused integrated server would stop the reload loop
    lines.append("soundCategory_master:0.0")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="
") as f:
        f.write("
".join(lines) + "
")


EMI_CSS = os.path.join(ROOT, "run", "config", "emi.css")
EMI_CSS_BACKUP = EMI_CSS + ".emiperf-backup"


def pack_like_config():
    """Match the modpack's EMI settings that matter for timing: dev-mode false (dev mode adds an
    O(recipes^2) id check) and log-untranslated-tags false (thousands of log lines for generated tags).
    The original emi.css is restored after the run."""
    if not os.path.isfile(EMI_CSS):
        return
    shutil.copyfile(EMI_CSS, EMI_CSS_BACKUP)
    with open(EMI_CSS, encoding="utf-8") as f:
        text = f.read()
    text = text.replace("dev-mode: true;", "dev-mode: false;").replace("log-untranslated-tags: true;", "log-untranslated-tags: false;")
    with open(EMI_CSS, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def restore_config():
    if os.path.isfile(EMI_CSS_BACKUP):
        shutil.copyfile(EMI_CSS_BACKUP, EMI_CSS)
        os.remove(EMI_CSS_BACKUP)


def count(path, needle):
    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            return f.read().count(needle)
    except OSError:
        return 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--label", required=True)
    ap.add_argument("--vanilla", action="store_true", help="no generated content, only the reload loop")
    ap.add_argument("--reloads", type=int, default=5)
    ap.add_argument("--dev-config", action="store_true", help="keep run/config/emi.css as is (default: dev-mode and tag logging off, like the pack)")
    ap.add_argument("--timeout", type=int, default=1500, help="seconds")
    ap.add_argument("gen_args", nargs="*", help="extra args for gen_datapack.py (after --)")
    a = ap.parse_args()
    os.makedirs(OUT, exist_ok=True)
    if game_pids():
        sys.exit("A dev client of this project is already running")
    prepare_world(a.vanilla, ["--reloads", str(a.reloads)] + a.gen_args)
    ensure_muted()
    if not a.dev_config:
        pack_like_config()
    start = time.time()
    gradle_log = open(os.path.join(OUT, a.label + ".gradle.txt"), "w")
    proc = subprocess.Popen([os.path.join(ROOT, "gradlew.bat"), "runClientWorld", "--console=plain"], cwd=ROOT,
                            stdout=gradle_log, stderr=subprocess.STDOUT)
    want = a.reloads + 1
    ok = False
    try:
        while time.time() - start < a.timeout:
            time.sleep(5)
            if proc.poll() is not None:
                print("gradle exited early, see", gradle_log.name)
                break
            if os.path.exists(LOG) and os.path.getmtime(LOG) > start:
                n = count(LOG, "Reloaded EMI in")
                if n >= want:
                    time.sleep(15)  # let the async "Baked recipes after reload" line arrive
                    ok = True
                    break
    finally:
        for pid in game_pids():
            subprocess.run(["taskkill", "/PID", str(pid), "/F"], capture_output=True)
        try:
            proc.wait(timeout=120)
        except subprocess.TimeoutExpired:
            pass
        gradle_log.close()
        restore_world()
        restore_config()
    if os.path.exists(LOG) and os.path.getmtime(LOG) > start:
        dst = os.path.join(OUT, a.label + ".log")
        shutil.copyfile(LOG, dst)
        summarize.main([dst])
    if not ok:
        sys.exit("benchmark did not complete")


if __name__ == "__main__":
    main()
