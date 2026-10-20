# Dev screenshot tour

An unattended dev-client tour for checking UI changes without clicking through the game. It launches the dev
client into the dev world, waits until EMI has finished loading, performs a scripted list of steps (open
screens, search, show recipes/uses, open EMI's config, ...), takes a screenshot after each requested step,
writes a machine-readable summary and quits the game by itself.

Nothing of it ends up in the release jar: the code lives in its own Gradle source set `devtour`
(`src/devtour`, mod id `emi_devtour`) that is on the classpath of the dev client run configs only.

## Run

One-time setup (creates the dev world, same as for `runClientWorld`):

```
gradlew runServer          # write run/eula.txt with eula=true first, stop the server with `stop`
```

Run the default tour and print the result:

```
powershell -File scripts\tour.ps1                  # default tour
powershell -File scripts\tour.ps1 -Tour my-tour     # run/tour/my-tour.json (or a built-in tour of that name)
powershell -File scripts\tour.ps1 -Tour C:\x\t.json # any json file
powershell -File scripts\tour.ps1 -SummaryOnly      # just print the last summary
```

or plain Gradle (no pretty summary): `gradlew runClientTour [-Ptour=<name or path>]`.

The script's exit code is 0 when all steps are ok, 1 when a step failed or the tour timed out, 2 when no summary
was written. `runClientTour` depends on `prepareDevWorld` and `prepareDevOptions` like `runClientWorld`.

Output, in `run/tour-out/<tour name>/` (the folder is emptied at the start of every run):

| File | Content |
|---|---|
| `NN-<name>.png` | one screenshot per `screenshot` step, numbered in order, full game window |
| `summary.json` | machine readable result, see below |

Built-in tours (in `src/devtour/resources/tour/`): `default` (EMI main UI), `config` (EMI config screen with the module
groups, jump buttons, module switches), `plugin-manager` (Plugin Manager: list, tooltips, toggles, reload on close),
`focus-check` (window focus / cursor lock check), `distraction-free`, `effects`, `enchanting`, `loot`, `ores` (one per
module) and `all-steps` (touches every step type; its last step is meant to fail to show how failures are reported).

Tours for single fixes and features (also in `src/devtour/resources/tour/`):

* `fx2.json`: block tag search (`#minecraft:mineable/pickaxe`, `#minecraft:logs`, regex `#/pickax/`), creative index source (search `barrier`) and the mod name tooltip before, during and after an EMI reload (`call` step on `EmiReloadManager.reload`).
* `fx3.json`: loot `copy_name` tooltip on the banner block drops, enchanted books above the maximum level (Sharpness 7 and 12), and `U` on a Smite book (own page first in the Enchantments tab).
* `p8.json`: Item Info sections of the professions (trades sold/bought by villagers) and effects modules (potions, tipped arrows, stews, effect stacks) and clicking their rows.
* `trade-row.json`: Item Info trade rows: the amount of the viewed item is in the row title (nether wart bought by the cleric "22x", bread sold by the farmer "6x"), the stacks on the right carry their own amounts (iron chestplate, bookshelf: 9 emeralds), grouped rows (emerald) keep the trade count; with hover tooltip of one row.
* `world.json`: biome and structure pages (village, stronghold, trial chambers, nether portal; plains, desert, nether wastes, deep dark), also after a `/reload`. Biome and structure stacks are no items, and tour clicks do not reach index stacks, so the tour opens the pages with the dev-only helper `src/devtour/java/dev/emi/remastered/devtour/WorldTour.java` through `call` steps (it does what a click on the stack in the index does).
* `kb7.json`: recipe tree remainder counts of synthetic favourites (KB7). The dev-only helper `src/devtour/java/dev/emi/remastered/devtour/Kb7Tour.java` (`call` steps `goalActivatorRail` and `report`) sets the activator rail as the recipe tree goal and notes per recipe the batches and the remaining amount with the old formula and with the fix. The stick recipe makes 4 per batch, so the stick of the redstone torch is served by the remainder: the stick favourite must show 3 with an empty inventory, 2 of 3 with one stick (`/give`) and be gone with three.
* `loot-van.json`: chests that vanilla places in code get their structure from a built-in list by structure type (`CodePlacedLoot`): the chest loot page of the dune armor trim template (desert pyramid) and nether wart (nether fortress) with the "Found in" line, and the Item Info "Chests in Structures" section of both.
* `loot-async.json`: the structure lookup finishes after the player joined (delayed by 20 s with `emi.devtour.lookupDelayMs` through `systemProperties`). The desert pyramid chest page ("Found in"), the dune armor trim template Item Info ("Chests in Structures") and the desert pyramid structure page (loot links) are shown before and after the delay, then the chest page and the structure page before and after the rebuilt lookup of a `/reload`. Nothing waits for the lookup and there is no EMI reload: the structures arrive in `emi_loot:chest_structures`, the structure loot links in `emi_world:structure_loot`.
* `x-sources.json`: the category Other Sources from `emi_sources` resource files (`docs/SOURCES.md`): the dev-only test file `src/devtour/resources/assets/emi_devtour/emi_sources/test.json` (command block with `from` stacks, long text with tooltip and chance, barrier without `from`, one invalid entry that logs a WARN) and the built-in `vanilla.json` (turtle scute, dragon egg). R pages, uses of the piglin spawn egg, Item Info "How to Get" (also the piglin head, covered by the loot module, and bedrock as "No Known Source" control), then the plugin manager: `emi_info` off removes the category, on brings it back.

## How it works

1. `TourMod` (client entrypoint) reads the system property `emi.tour` (set by `clientTour` from `-Ptour`, default
   `default`), installs a Log4j appender and a client-tick driver. Without the property it does nothing.
2. States: wait for the world (client in the world, no screen) -> wait for `EmiReloadManager.isLoaded()` ->
   warm-up ticks (`warmupTicks`) -> steps, one per tick (a step may take several ticks) -> write summary ->
   `MinecraftClient.scheduleStop()`.
3. A screenshot step waits `settleTicks` ticks and at least 3 rendered frames (counted by a HUD render callback)
   and then writes the main framebuffer, so the GUI is in the picture.
4. Every step runs in try/catch; an exception marks the step `failed` with the message and the tour continues.
   Each step has a timeout (`stepTimeoutSeconds`), the whole tour has a global timeout (`timeoutSeconds`, 300 s).
   A watchdog thread writes the summary and stops the client when the global timeout hits, even if the game thread
   hangs; if the JVM is still alive 45 s after the stop request it is halted.
5. `summary.json` is rewritten after every step, so a crash still leaves a partial result.

## Tour file format

Tours are found in this order: a path, `run/tour/<name>.json`, built-in resource `tour/<name>.json`.

```json
{
  "description": "free text",
  "warmupTicks": 60,
  "settleTicks": 10,
  "stepTimeoutSeconds": 30,
  "worldJoinTimeoutSeconds": 120,
  "timeoutSeconds": 300,
  "steps": [
    { "type": "open_inventory" },
    { "type": "screenshot", "name": "inventory" }
  ]
}
```

All settings are optional (defaults shown). `settleTicks` is how many ticks a screenshot waits before capturing.
`worldJoinTimeoutSeconds` applies to joining the world and to EMI loading.
`"systemProperties": {"key": "value"}` (optional) sets Java system properties when the tour is loaded, which is before
the integrated server starts, for dev-only switches such as the ones below.

## Dev-only system properties

Read only in a development environment (`FabricLoader.isDevelopmentEnvironment()`), ignored in a release:

| Property | Meaning |
|---|---|
| `emi.devtour.lookupDelayMs` | delays every build of the loot module's structure lookup (`StructureLookup`) by that many milliseconds on its background thread, to test what players see when the lookup finishes after they joined (`loot-async.json` sets 20000) |

## Step types

| type | arguments | what it does |
|---|---|---|
| `wait` | `ticks` (20) | lets the game run for that many ticks |
| `screenshot` | `name`, optional `settleTicks` | saves `<NN>-<name>.png` after the GUI was drawn |
| `open_inventory` | | opens the player inventory (EMI sidebars appear) |
| `close_screen` | | closes the current screen |
| `emi_search` | `text` | types into EMI's search box (needs an open screen); records the result count |
| `emi_recipes` | `item` (item id) or `effect` (status effect id, effects module) | `EmiApi.displayRecipes`; fails if EMI has no recipes for it; records the tabs |
| `emi_uses` | `item` | `EmiApi.displayUses` (includes fuel, workstations) |
| `emi_category` | `id` (e.g. `emi:fuel`) | switches the open recipe screen to that category's tab |
| `emi_recipe_tab` | `index`, optional `page` (0-based) | switches the open recipe screen to a tab by position |
| `emi_bom` | | opens the recipe tree (bill of materials) screen |
| `open_emi_config` | | opens EMI's config screen |
| `key` | `key`, optional `modifiers` (`shift`/`ctrl`/`alt`) | press + release of a key through the game's keyboard handler; `key` is a GLFW name without prefix (`E`, `F3`, `LEFT_CONTROL`, `KEY_R`, `GLFW_KEY_R`) or a number. Key bindings and open screens react as to a real key press (modifiers are passed to the event only; held-modifier checks via GLFW state do not see them) |
| `emi_stack_key` | `item`, `key` | a key press on that stack through EMI's stack interaction (what EMI does for the stack under the mouse: view recipes/uses, favourite, item info, ...); records whether it was handled and the screen change |
| `screen_scroll` | `amount` (-3) | mouse wheel in the middle of the current screen (negative = down) |
| `command` | `text` (`/time set day`), optional `waitTicks` | runs a command on the integrated server with op level 4 (works without cheats) |
| `set_gui_scale` | `n` (0 = auto) | changes the GUI scale option |
| `resize` | `w`, `h` | resizes the window (windowed mode) |
| `note` | `text` | writes a note into summary.json |
| `emi_wait_loaded` | | waits until EMI reports loaded again |
| `click` | `text` or `key` or `x`+`y`; optional `index`, `button` (0), `optional` (false) | presses a widget of the open screen. `text`: message (or tooltip line, which is how the icon-only config jump buttons are found) equals, else contains, the text, ignoring case; `key`: translation key of the widget's message (e.g. `screen.emi_remastered.plugins.button`); `x`,`y`: GUI coordinates (not the window pixels). Only widgets that are on screen match (rows scrolled out of a list do not). The click goes through `Screen.mouseClicked`/`mouseReleased` (never the OS cursor). A miss fails with the list of labels on screen; `optional: true` skips instead |
| `click_row` | `text`, optional `index` | EMI config screen only: scrolls the setting row whose name equals/contains the text into view and presses its widget (module switches, booleans, enum buttons); the note records the old value |
| `scroll` | `amount` (negative = down), optional `x`, `y` (screen centre) | mouse wheel through `Screen.mouseScrolled` |
| `hover` | `x`, `y` | moves the virtual mouse (GUI coordinates) so hover states and tooltips show in the next screenshot; stays until another step moves it |
| `screen_info` | | notes the screen class, size and every widget (class, text, translation key, tooltip, bounds) in `info` (use it to find what to click) |
| `emi_config_jump` | `group` (`general`, `ui`, `modules`, `loot`, `distraction_free`, ...) | `ConfigScreen.jump(group)`: the same as pressing that group's jump button; notes the scroll before/after (a group that does not exist leaves it unchanged) |
| `open_plugin_manager` | | opens EMI's config screen (unless open) and presses its "Plugins" button; fails if the Plugin Manager did not open |
| `plugin_toggle` | `id` (plugin id, e.g. `emi_ores`), optional `state` (`on`/`off`) | in the Plugin Manager: scrolls the plugin's row into view and presses its On/Off button; with `state` it only clicks if the plugin is not in that state; fails for locked plugins (`emi`) and unknown ids |
| `screen_close` | | `Screen.close()` of the current screen, like its Done button or Escape, so close hooks run (config saved; Plugin Manager saves and starts an EMI reload after a change; module switches with apply mode "reload" reload EMI). Follow with `emi_wait_loaded` when a reload is expected. `close_screen` just drops all screens without hooks |
| `window_info` | | notes `mode` (hidden/visible), `visible` (GLFW_VISIBLE), `pos`, `size`, `framebuffer`, `focused` (GLFW_FOCUSED, OS focus), `cursorLocked` and the GLFW cursor mode; fails unless `focused=false cursorLocked=false`, in the default hidden mode also unless `visible=false` |
| `call` | `class`, `method` | calls a public static no-arg method (escape hatch) |

The default window is 854x480; the default tour starts with `resize 1280x720` and `set_gui_scale 2`.
Steps that expect a screen (`emi_search`, `emi_category`, ...) fail with a clear message when the wrong one is open.

## summary.json

```
tour, source, status (ok | failed | timeout), reason, durationMs, outputDir
emi:   loaded, status, reloadMs, pluginInitMs{plugin: ms}, pluginReloadMs{plugin: ms},
       reloadWarningCount, reloadLog[]          (EmiReloadLog, what EMI shows in its own warning list)
stepsTotal, stepsOk, stepsFailed
steps[]: index, type, spec (the json of the step), status ok|failed, error, durationMs,
         info (e.g. recipe tabs or search result count), screenshot (file name), screen (screen class after the step)
logWarningCount, logWarningsDropped, logWarnings[]: tMs, level, logger, thread, message, thrown
```

`logWarnings` are all WARN/ERROR/FATAL lines of the whole session (including the start-up) whose logger or message
mentions emi/EMI/remastered as a word (`emissive` does not count), or whose exception has a `dev.emi` stack frame.
`reloadMs` and the plugin times come from EMI's own log lines.

## Adding a step type

In `src/devtour/java/dev/emi/remastered/devtour/Steps.java` add one `register` line to the static block:

```java
// {"type":"emi_favorite","item":"minecraft:iron_ingot"}
register("emi_favorite", (args, ctx) -> {
    String id = args.str("item");                 // args.str / intVal / bool, with or without default
    // ... do the work on the render thread, throw an exception to fail the step
    ctx.info("favorited " + id);                  // optional note in summary.json
    return waitTicks(2);                          // or StepRun.DONE, or a StepRun that is ticked until it returns true
});
```

`ctx.client` is the `MinecraftClient`; `ctx.screenshot(name)` takes a screenshot directly. Add the step to the table
above. No other file needs a change; the new type can then be used in any tour json.

## Dev clients never steal focus or the cursor (no-grab protection)

Several dev clients (and the developer's own work) share one PC, so a dev client must never capture the mouse or take
the keyboard focus. The `emi_devtour` mod (`src/devtour`, never shipped) carries the protection. It is on the classpath
of **every** client run config: `runClient`, `runClientWorld`, `runClientTour` (and Loom's `runClientRenderDoc`), but
not of `runServer`. Plain `runClient` / `runClientWorld` therefore behave like normal clients minus the grab, with a
hidden window unless `-PdevtourVisible=true` (below); the
screenshot tour itself only runs when `-Demi.tour` is set (only `runClientTour` sets it). Everything is dev-only: the
mixins are not in the release jar. The mixin config uses `defaultRequire: 1`, so a Minecraft change that removes a
target crashes the start-up instead of silently grabbing the cursor again.

### Hidden window (default)

The dev client window is **hidden** by default (`GLFW_VISIBLE=false` before `glfwCreateWindow`): it never appears on
the screen, has no taskbar button and can never become the foreground window, so it cannot take the keyboard focus
even on Windows (the `GLFW_FOCUSED`/`GLFW_FOCUS_ON_SHOW` hints alone were not enough, `focus-check` reported
`focused=true`, TOUR_REPORT T1-1). Minecraft renders into its own main framebuffer, so the game runs normally and
screenshots are correct while hidden, also after `resize` (checked up to 2560x1440: the window size and the framebuffer
follow `glfwSetWindowSize`). No off-screen fallback is needed; a hidden window is never shown by
`glfwSetWindowSize`/`glfwSetWindowMonitor` (windowed)/`glfwSetWindowTitle`/`glfwSetWindowIcon` (GLFW 3.4 of LWJGL
3.3.3 shows a window only in `glfwShowWindow`, in `glfwCreateWindow` with `GLFW_VISIBLE` or when a window is put on a
monitor for fullscreen, which is blocked). The start-up log says which mode is active:

```
(EmiDevTour) dev client window hidden (visible=false pos=853,480 size=854x480 framebuffer=854x480), focused=false (set -PdevtourVisible=true to show it)
```

To see or play the dev client, pass `-PdevtourVisible=true` (sets `-Demi.devtour.visible=true` on `runClient`,
`runClientWorld` and `runClientTour`):

```
gradlew runClientWorld -PdevtourVisible=true
```

The window is then visible but created unfocused and shown without activation (it comes up behind the active
window; click it to play). Without the property (IDE run configs generated before, `runClientRenderDoc`) the window is
hidden. `DevWindow.tick` is a safety net: if anything shows a hidden dev window, it is hidden again on the next
client tick and a WARN `dev client window was shown by someone, hidden again` is logged once.

`MinecraftClient#isWindowFocused` stays at its initial `true` (no focus event ever arrives), so vanilla behaviour that
depends on it is unchanged.

### Foreground check (independent, read-only)

`scripts/foreground-check.ps1` proves from outside the game that a dev client never owns the foreground window. It
polls `GetForegroundWindow` / `GetWindowThreadProcessId` / `GetWindowText` and enumerates the visible top-level windows
of the game JVM (java processes whose command line contains the worktree path and `devlaunchinjector`, or `-GamePid`).
It only reads (no input, no focus or window change) and prints window titles only for game windows (or with
`-ShowTitles`). Start it right before the launch, it stops when the game has exited:

```
powershell -File scripts\foreground-check.ps1 -Seconds 400 -StopWhenGameExits -ExpectHidden   # terminal 1
gradlew runClientTour -Ptour=focus-check                                                    # terminal 2
```

One line per poll (`fg pid=... (process) game=<pids> visibleGameWindows=<n>`, `GAME-FOREGROUND` when the game owned
the foreground), then `foreground-check result: polls=.. gamePidsSeen=.. gameForeground=0 pollsWithVisibleGameWindow=0`.
Exit code 0 = never foreground (and, with `-ExpectHidden`, never a visible game window), 1 = violated, 2 = no game
process seen. Options: `-Worktree <path>` (default: current directory), `-IntervalMs` (2000), `-Seconds` (120).

### Neutralised vanilla calls

(Yarn 1.21.1+build.3), `src/devtour/java/dev/emi/remastered/devtour/mixin`:

| Mixin | Target | Effect |
|---|---|---|
| `MouseMixin` | `Mouse#lockCursor`, `Mouse#unlockCursor` | cancelled: the cursor is never captured or released |
| `InputUtilMixin` | `InputUtil#setCursorParameters` | cancelled: the only caller of `glfwSetCursorPos` and `glfwSetInputMode(GLFW_CURSOR)` |
| `InputUtilMixin` | `InputUtil#setRawMouseMotionMode` | cancelled: no `glfwSetInputMode(GLFW_RAW_MOUSE_MOTION)` |
| `WindowMixin` | `Window.<init>` before `glfwCreateWindow` | `GLFW_VISIBLE=false` (true with `-Demi.devtour.visible=true`), `GLFW_FOCUS_ON_SHOW=false`, `GLFW_FOCUSED=false`, `fullscreen=false` |
| `WindowMixin` | `Window.<init>` `glfwCreateWindow` monitor argument | always 0: never created as a fullscreen window (GLFW shows and focuses those) |
| `WindowMixin` | `Window.<init>` tail | logs the window mode, visibility, position, sizes and focus (`DevWindow.created`) |
| `WindowMixin` | `Window#toggleFullscreen` | cancelled (F11, the fullscreen option, the start-up toggle) |
| `WindowMixin` | `Window#updateWindowRegion` | `fullscreen`/`currentFullscreen` forced to false: the only `glfwSetWindowMonitor` caller never enters fullscreen |
| `GameRendererMixin` | `GameRenderer#render` read of `GameOptions#pauseOnLostFocus` | always false: an unfocused client does not open the game menu |

Other GLFW calls of Minecraft only read state, register callbacks or change hidden-safe properties (title, icon,
vsync, size limits, monitors, clipboard, key/mouse callbacks). Checked in the bytecode of the 1.21.1 client (Yarn
build.3): `net.minecraft.client.util.Window` is the only class with window calls (`glfwDefaultWindowHints`,
`glfwWindowHint`, `glfwCreateWindow`, `glfwSetWindowSizeLimits`, `glfwSetWindowIcon`, `glfwSetWindowMonitor` in
`updateWindowRegion`, `glfwSetWindowTitle`); no Minecraft class calls `glfwShowWindow`, `glfwFocusWindow`,
`glfwRequestWindowAttention`, `glfwRestoreWindow`, `glfwMaximizeWindow`, `glfwIconifyWindow`, `glfwSetWindowPos`,
`glfwSetWindowSize` or `glfwSetWindowAttrib`. With the cursor
never locked, Minecraft's other cursor-lock code paths (`MinecraftClient#setScreen`, `Mouse#onMouseButton`) just do
nothing; screens open and the world renders as usual.

`prepareDevOptions` is a dependency of `runClient`, `runClientWorld` and `runClientTour` and runs before every launch
(never up to date). It rewrites exactly these keys in `run/options.txt` (creating the file if needed, keeping every
other line):

* `soundCategory_master:0.0` (dev clients never play sound)
* `onboardAccessibility:false` (a fresh client would show the accessibility onboarding screen and block quick play)
* `pauseOnLostFocus:false` (the unfocused game keeps ticking and rendering, so screenshots work)

Tour steps inject input in-process only: `Screen.mouseClicked/mouseReleased/mouseScrolled/mouseMoved`, the game's
`Keyboard.onKey` handler and the virtual mouse position of `Mouse` (`click`, `scroll`, `hover`, `key`, ...). Nothing
uses GLFW input, `java.awt.Robot` or any OS input. The only window call is `resize` (`glfwSetWindowSize`, no
activation, a hidden window stays hidden). Check the protection with `gradlew runClientTour -Ptour=focus-check`
(together with `scripts/foreground-check.ps1`): every `window_info` step must report
`mode=hidden visible=false ... focused=false cursorLocked=false cursorMode=212993` (GLFW_CURSOR_NORMAL); the step
fails otherwise. The tour also resizes to 1280x720 and 1920x1080 and takes screenshots there.
Limitation (visible mode only): a real mouse that moves over the game window still moves the in-game cursor, and
clicking the window focuses it (then `window_info` fails, as it should).

## EMI reload benchmark (perf tours)

Dev-only harness for measuring EMI's reload (cold on world join, warm on `EmiReloadManager.reload()` = F3+T) and for
checking that an optimisation leaves EMI's baked state unchanged. Code: `BenchMod` (main entrypoint of `emi_devtour`,
active only with `-Demi.tour`), `BenchSteps`, `PhaseTimer`, `StateDump` in `src/devtour`; scripts in `scripts/perf/`.

| Piece | What it does |
|---|---|
| `PhaseTimer` | Log4j appender on EMI's own log lines; turns the lines of one reload into phase durations (tags, stack list, plugins, per plugin, recipe bake, search bake, async sorted manager, ...) |
| `BenchMod` | registers the bench steps and, when `run/config/emi_bench.properties` exists (`items=`, `tools=`, `armor=`), that many load-test items (`emi_bench:*`) so the dev client resembles a big pack |
| step `bench_record` | `label` (`join`): records the last complete reload (the join reload) as one line of `bench.jsonl` |
| step `emi_reload` | `label`, `settleTicks` (40): full GC, triggers a reload, waits for EMI and its async recipe sorting, records phases, heap after GC before/after and bytes allocated during the reload |
| step `emi_dump` | `name`: writes EMI's baked state (index, recipes, lookups, tags, BoM defaults, search results) as text plus a SHA-256 per section to `dump-<name>/` (`hashes.txt`); `*_norm` sections are order-normalised where upstream iterates identity-hash maps; `*_stable` sections also remove the run-to-run noise of world interaction/tag recipe order, emi_loot (random loot samples) and emi_ores (biome list order) and name recipes by id. Two runs of the same build give identical `*_stable` hashes, so these (plus the sections that are already stable: stacks, stack_flags, tags_by_size_norm, bom_defaults, search, persistent_data) are the before/after identity check. `search` runs 58 queries (names, ids, tooltips, mods, aliases, tags, regex, long quoted text, `~` sources); `source_live` counts the stacks whose `~` labels computed on the fly (as while the source index is being built) differ from the index (must be 0). Known noise not covered by `*_stable`: the biome lists of `emi_world` structure pages (hash-set order) |
| steps `jfr_start` / `jfr_stop` | `name`: JFR recording with the JDK `profile` settings, written to `<name>.jfr` |
| tour `perf-bench` | join record, 1 warm-up and 5 measured warm reloads, `emi_dump final` |
| tour `perf-profile` | 2 warm-ups, then 3 reloads under JFR (`reload.jfr`) |

Scripts (Python 3, from the project root; one client at a time, they refuse to start while a dev client of the
project runs):

* `py -3 scripts/perf/run_bench.py --label <name> [--stock] [--vanilla | --repair-check] [--profile]` runs one bench
  session through `gradlew runClientTour` (no-grab protection on; `soundCategory_master:0.0` is forced in
  `run/options.txt` before the launch). It uses a fresh copy of the dev world (template `run/saves/emi_perf_template`,
  the real devworld is restored afterwards), generates the load datapack (`gen_datapack.py`, pack mode: ~92k EMI
  recipes, ~12k items, 4k tags) and sets `dev-mode`/`log-untranslated-tags` false like the pack. `--stock` runs a copy
  of the project in `build/stk` whose `dev.emi.emi` sources come from the merge base with `remastered` (the
  "before"). `--merge-enchantments` (pack mode): every enchantment accepts every item (`mergeEnchantments=true` in
  `emi_bench.properties`, dev mixin `EnchantmentBenchMixin`), like Merge Enchantments with `combineEnchantments: true`
  in the owner's pack (datapack enchantments cannot be used: they make the world experimental and quick play stops at
  the warning screen). `--vanilla`: no datapack, no extra items. `--repair-check`: only empties
  `#minecraft:stone_tool_materials` (KB4). `--boost` (off by default) raises only the bench's own game JVM to
  AboveNormal priority; use it only when the start-up hangs at "Datafixer Bootstrap" because of CPU starvation, since the
  owner may be playing at the same time. Results go to `build/emi_perf/<label>/` (`bench.jsonl`, `dump-final/`,
  `summary.json`, `latest.log`).
* `py -3 scripts/perf/summarize.py build/emi_perf/<before> build/emi_perf/<after> [<control> ...]` prints the warm
  means and the join reload per run, a before/after table and the matrix of dump section hashes. Run a second stock
  session as control: sections that differ between two stock runs are run-to-run noise of upstream EMI.
* `py -3 scripts/perf/jfr_top.py build/emi_perf/<label>/reload.jfr` lists the hottest methods of EMI's reload threads.

## Release safety

* `devtour` is a separate source set; the jar and sources jar only package `main`. `gradlew build` compiles it
  (`check` depends on `devtourClasses`) but never packages it.
* The dev client run configs (`runClient`, `runClientWorld`, `runClientTour`) have it on the classpath, `runServer` does not; the tour only runs with `-Demi.tour` (`runClientTour`).
* Check a built jar: `unzip -l build/libs/EMIRemastered-*.jar | grep -i -E "devtour|emi_devtour"` must print nothing.
