# Dev screenshot tour

An unattended dev-client tour for checking UI changes without clicking through the game. It launches the dev
client into the dev world, waits until EMI has finished loading, performs a scripted list of steps (open
screens, search, show recipes/uses, open EMI's config, ...), takes a screenshot after each requested step,
writes a machine-readable summary and quits the game by itself.

Nothing of it ends up in the release jar: the code lives in its own Gradle source set `devtour`
(`src/devtour`, mod id `emi_devtour`) that is on the classpath of the `clientTour` run config only.

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

Built-in tours (in `src/devtour/resources/tour/`): `default` (EMI main UI) and `all-steps` (touches every step
type; its last step is meant to fail to show how failures are reported).

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

## Step types

| type | arguments | what it does |
|---|---|---|
| `wait` | `ticks` (20) | lets the game run for that many ticks |
| `screenshot` | `name`, optional `settleTicks` | saves `<NN>-<name>.png` after the GUI was drawn |
| `open_inventory` | | opens the player inventory (EMI sidebars appear) |
| `close_screen` | | closes the current screen |
| `emi_search` | `text` | types into EMI's search box (needs an open screen); records the result count |
| `emi_recipes` | `item` (item id) | `EmiApi.displayRecipes`; fails if EMI has no recipes for it; records the tabs |
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

## Release safety

* `devtour` is a separate source set; the jar and sources jar only package `main`. `gradlew build` compiles it
  (`check` depends on `devtourClasses`) but never packages it.
* Only `runClientTour` has it on the classpath (`runClient`, `runClientWorld` and `runServer` do not).
* Check a built jar: `unzip -l build/libs/EMIRemastered-*.jar | grep -i -E "devtour|emi_devtour"` must print nothing.
