"""Run one EMI reload benchmark session in the (muted) dev client and store the results.

Before/after are two builds of the same tree: the normal project (after = this branch) and, with --stock, a copy
of it in build/stk in which every file under src/main/java/dev/emi/emi is put back to the merge base with
`remastered` (before = stock EMI code of the remastered branch; modules, devtour harness, world, config identical).

What one run does (project root = the folder containing gradlew):
 1. --stock: syncs build/stk (git ls-files of this tree, dev.emi.emi from the merge base; keeps build/stk's own
    Gradle caches so later runs build incrementally) and runs the game from there.
 2. Keeps a pristine copy of the dev world (run/saves/emi_perf_template of THIS project, made once from
    run/saves/devworld) and puts a fresh copy of it into the game project's run/saves/devworld (the original
    devworld is moved aside and restored at the end, whatever happens).
 3. Pack mode (default): generates the load-test datapack (gen_datapack.py) into that world and writes
    run/config/emi_bench.properties, which makes the dev-only BenchMod initializer (src/devtour) register the extra
    items. --vanilla: no datapack, no extra items. --repair-check: only a datapack that empties
    #minecraft:stone_tool_materials (stock EMI then throws ArrayIndexOutOfBoundsException for the 5 stone tools).
 4. Sets EMI's dev-mode and log-untranslated-tags to false like the pack (run/config/emi.css is restored after)
    and forces soundCategory_master:0.0 in run/options.txt (the dev client must never play sound).
 5. Runs `gradlew runClientTour -Ptour=perf-bench` (perf-profile with --profile). The tour records the join
    reload (cold), a warm-up reload and 5 warm reloads (EmiReloadManager.reload(), the reload F3+T triggers), dumps
    EMI's baked state (dump-final/) and quits the game by itself.
 6. Copies run/tour-out/<tour>/ (bench.jsonl, dump-final/, reload.jfr) and run/logs/latest.log to
    build/emi_perf/<label>/ of THIS project and prints the summary (summarize.py).

Usage (one batched session, after = no flag, before = --stock):
  py -3 scripts/perf/run_bench.py --label after-pack
  py -3 scripts/perf/run_bench.py --label before-pack --stock
  py -3 scripts/perf/run_bench.py --label before-pack-2 --stock         (control: stock vs stock)
  py -3 scripts/perf/run_bench.py --label after-vanilla --vanilla
  py -3 scripts/perf/run_bench.py --label before-vanilla --vanilla --stock
  py -3 scripts/perf/run_bench.py --label after-repair --repair-check
  py -3 scripts/perf/run_bench.py --label before-repair --repair-check --stock
  py -3 scripts/perf/run_bench.py --label after-prof --profile            (optional JFR, scripts/perf/jfr_top.py)
  py -3 scripts/perf/summarize.py build/emi_perf/before-pack build/emi_perf/after-pack build/emi_perf/before-pack-2
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
OUT = os.path.join(ROOT, "build", "emi_perf")
STOCK = os.path.join(ROOT, "build", "stk")
TEMPLATE = os.path.join(ROOT, "run", "saves", "emi_perf_template")
# put back to the merge base in the stock build (EMI code and its tests)
STOCK_PATHS = ("src/main/java/dev/emi/emi/", "src/test/java/dev/emi/emi/")

# Pack-like load: ~90k recipes in EMI (pack: 112,836), ~12k items, 4k extra tags
PACK = {"items": 9000, "tools": 800, "armor": 800, "recipes": 60000, "tags": 4000, "enchantments": 0}


class Game:
    """Paths of the project the game runs from (this project, or build/stk)."""

    def __init__(self, root):
        self.root = root
        self.run = os.path.join(root, "run")
        self.world = os.path.join(self.run, "saves", "devworld")
        self.backup = os.path.join(self.run, "saves", "devworld.emiperf-backup")
        self.emi_css = os.path.join(self.run, "config", "emi.css")
        self.emi_css_backup = self.emi_css + ".emiperf-backup"
        self.bench_props = os.path.join(self.run, "config", "emi_bench.properties")


def ps(cmd):
    return subprocess.run(["powershell", "-NoProfile", "-Command", cmd], capture_output=True, text=True).stdout


def game_pids(root):
    """PIDs of dev game JVMs of that project (Loom passes the project's launch.cfg path on the command line)."""
    marker = os.path.join(root, ".gradle", "loom-cache", "launch.cfg")
    out = ps("Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | "
             "Where-Object { $_.CommandLine -like '*devlaunchinjector*' -and $_.CommandLine -like '*" + marker + "*' } | "
             "ForEach-Object { $_.ProcessId }")
    return [int(x) for x in out.split() if x.strip().isdigit()]


def git(*args, binary=False):
    out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, check=True).stdout
    return out if binary else out.decode("utf-8")


def sync_stock(base_ref):
    """build/stk = this tree (tracked + untracked, not ignored) with STOCK_PATHS from the merge base."""
    base = git("merge-base", "HEAD", base_ref).strip()
    files = [f for f in git("ls-files", "-z", "-co", "--exclude-standard").split("\0") if f]
    for top in ("src", "gradle", "scripts", "docs"):
        shutil.rmtree(os.path.join(STOCK, top), ignore_errors=True)
    for f in files:
        if f.startswith(STOCK_PATHS) or not os.path.isfile(os.path.join(ROOT, f)):
            continue
        dest = os.path.join(STOCK, f)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        shutil.copyfile(os.path.join(ROOT, f), dest)
    stock_files = [f for f in git("ls-tree", "-r", "-z", "--name-only", base, "--", *STOCK_PATHS).split("\0") if f]
    for f in stock_files:
        dest = os.path.join(STOCK, f)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "wb") as out:
            out.write(git("show", base + ":" + f, binary=True))
    changed = git("diff", "--name-status", base, "--", *STOCK_PATHS).strip()
    untracked = git("ls-files", "-o", "--exclude-standard", "--", *STOCK_PATHS).strip()
    print(f"stock project {STOCK}: {', '.join(STOCK_PATHS)} from {base[:10]} (merge base with {base_ref}); this tree differs in:")
    print("  " + "\n  ".join((changed + "\n" + untracked).strip().splitlines()))
    # the game project needs a run folder with config and options like this project's
    os.makedirs(os.path.join(STOCK, "run", "config"), exist_ok=True)
    for f in ("config/emi.css", "options.txt", "eula.txt"):
        src = os.path.join(ROOT, "run", f)
        if os.path.isfile(src):
            shutil.copyfile(src, os.path.join(STOCK, "run", f))


def prepare_world(game, mode, gen_args):
    if not os.path.isdir(TEMPLATE):
        world = os.path.join(ROOT, "run", "saves", "devworld")
        if not os.path.isfile(os.path.join(world, "level.dat")):
            sys.exit("No dev world: run `gradlew runServer` once (see docs/DEVTOUR.md)")
        shutil.copytree(world, TEMPLATE, ignore=shutil.ignore_patterns("session.lock", "emi_perf*"))
    if os.path.isdir(game.world) and not os.path.isdir(game.backup):
        os.rename(game.world, game.backup)
    elif os.path.isdir(game.world):
        shutil.rmtree(game.world)
    shutil.copytree(TEMPLATE, game.world, ignore=shutil.ignore_patterns("emi_perf*"))
    if mode == "vanilla":
        return
    datapack = os.path.join(game.world, "datapacks", "emi_perf.zip")
    os.makedirs(os.path.dirname(datapack), exist_ok=True)
    gen = [sys.executable, "-I", os.path.join(HERE, "gen_datapack.py"), "--out", datapack]
    if mode == "repair":
        gen += ["--recipes", "0", "--tags", "0", "--enchantments", "0", "--empty-stone-tool-materials"]
    else:
        gen += ["--recipes", str(PACK["recipes"]), "--tags", str(PACK["tags"]),
                "--enchantments", str(PACK["enchantments"]), "--bench-items", str(PACK["items"]),
                "--bench-tools", str(PACK["tools"]), "--bench-armor", str(PACK["armor"])]
        os.makedirs(os.path.dirname(game.bench_props), exist_ok=True)
        with open(game.bench_props, "w", encoding="utf-8", newline="\n") as f:
            f.write(f"items={PACK['items']}\ntools={PACK['tools']}\narmor={PACK['armor']}\n")
    subprocess.run(gen + gen_args, check=True)


def restore(game):
    if os.path.isdir(game.backup):
        if os.path.isdir(game.world):
            shutil.rmtree(game.world)
        os.rename(game.backup, game.world)
    if os.path.isfile(game.emi_css_backup):
        shutil.copyfile(game.emi_css_backup, game.emi_css)
        os.remove(game.emi_css_backup)
    if os.path.isfile(game.bench_props):
        os.remove(game.bench_props)


def ensure_muted(game):
    """The dev client must never play sound: force soundCategory_master:0.0 in run/options.txt before every launch."""
    path = os.path.join(game.run, "options.txt")
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


def pack_like_config(game):
    """Like the pack: dev-mode false (dev mode adds an O(recipes^2) id check) and log-untranslated-tags false."""
    if not os.path.isfile(game.emi_css):
        return
    shutil.copyfile(game.emi_css, game.emi_css_backup)
    with open(game.emi_css, encoding="utf-8") as f:
        text = f.read()
    text = text.replace("dev-mode: true;", "dev-mode: false;").replace("log-untranslated-tags: true;", "log-untranslated-tags: false;")
    with open(game.emi_css, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--label", required=True)
    ap.add_argument("--stock", action="store_true", help="run the stock build (build/stk, dev.emi.emi from the merge base)")
    ap.add_argument("--base", default="remastered", help="branch whose merge base is the stock code (default remastered)")
    mode = ap.add_mutually_exclusive_group()
    mode.add_argument("--vanilla", action="store_true", help="no generated datapack and no extra items")
    mode.add_argument("--repair-check", action="store_true", help="only empty #minecraft:stone_tool_materials")
    ap.add_argument("--profile", action="store_true", help="run the perf-profile tour (JFR of 3 warm reloads)")
    ap.add_argument("--timeout", type=int, default=3600, help="seconds")
    ap.add_argument("gen_args", nargs="*", help="extra args for gen_datapack.py (after --)")
    a = ap.parse_args()
    run_mode = "vanilla" if a.vanilla else "repair" if a.repair_check else "pack"
    tour = "perf-profile" if a.profile else "perf-bench"
    dest = os.path.join(OUT, a.label)
    if os.path.isdir(dest):
        sys.exit(dest + " exists")
    game = Game(STOCK if a.stock else ROOT)
    if game_pids(ROOT) or game_pids(STOCK):
        sys.exit("A dev client of this project is already running")
    if a.stock:
        sync_stock(a.base)
    os.makedirs(OUT, exist_ok=True)
    gradle_log_path = os.path.join(OUT, a.label + ".gradle.txt")
    ok = False
    try:
        prepare_world(game, run_mode, a.gen_args)
        pack_like_config(game)
        ensure_muted(game)
        start = time.time()
        with open(gradle_log_path, "w") as gradle_log:
            proc = subprocess.Popen([os.path.join(game.root, "gradlew.bat"), "runClientTour", "-Ptour=" + tour, "--console=plain"],
                                    cwd=game.root, stdout=gradle_log, stderr=subprocess.STDOUT)
            try:
                proc.wait(timeout=a.timeout)
                ok = proc.returncode == 0
            except subprocess.TimeoutExpired:
                print("timeout, stopping the game")
                for pid in game_pids(game.root):
                    subprocess.run(["taskkill", "/PID", str(pid), "/F"], capture_output=True)
                proc.wait(timeout=120)
        print(f"game run took {time.time() - start:.0f}s, gradle exit {proc.returncode}")
    finally:
        restore(game)
    tour_out = os.path.join(game.run, "tour-out", tour)
    if os.path.isdir(tour_out):
        shutil.copytree(tour_out, dest)
    os.makedirs(dest, exist_ok=True)
    with open(os.path.join(dest, "bench-info.txt"), "w", encoding="utf-8") as f:
        f.write(f"label={a.label}\nbuild={'stock' if a.stock else 'this tree'}\nmode={run_mode}\ntour={tour}\n"
                f"project={game.root}\nhead={git('rev-parse', 'HEAD').strip()}\n")
    log = os.path.join(game.run, "logs", "latest.log")
    if os.path.isfile(log):
        shutil.copyfile(log, os.path.join(dest, "latest.log"))
    summarize.main([dest])
    if not ok:
        sys.exit("benchmark run failed, see " + gradle_log_path)


if __name__ == "__main__":
    main()
