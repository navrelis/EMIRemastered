## 2.1.0+1.21.1 (2026-10-26)

### New
* Resource packs can recolour EMI panel text via `assets/emi_remastered/text_colors.json` (see `docs/THEMING.md`); no
  visual change without such a pack.

## 2.0.0+1.21.1 (2026-10-26)

EMI Remastered: EMI 1.1.24 and seven add-ons merged into one Fabric 1.21.1 mod (mod id `emi`, jar
`EMIRemastered-1.21.1-2.0.0-Fabric.jar`). **Remove the jars of EMI and of the seven add-ons when you install it**: the mod
provides their ids, and a leftover jar stops the game at start-up. Credits and licences: `CREDITS.md`.

### Merged mods
* EMI 1.1.24 (`1.1.24+1.21.1`, the base; all `dev.emi.emi` classes keep their names and signatures)
* Distraction Free Recipes 1.2.2 (module `distraction_free`, built into EMI's screen code, no mixins)
* EMIffect 2.1.5 (module `effects`)
* EMI Professions 1.0.3 (module `professions`)
* EMI Enchanting 0.1.2 (module `enchanting`)
* EMI Ores 1.3 (module `ores`, LGPL-3.0-only headers kept)
* Let's Do EMI Compat 2.0.3 (module `letsdo`, no Architectury needed)
* EMI Loot 0.7.9 (module `loot`, fzzy_config replaced by EMI's config, Fabric networking)
* Each module keeps the resource namespace and plugin id of its old add-on (`emi_loot`, `emiffect`, ...), so resource
  packs, data packs and saved favourites keep working. fzzy_config, TXNI Lib, Architectury and ForgeConfigAPIPort are no
  longer needed. Fabric 1.21.1 only, NeoForge and Forge are dropped.

### New: one config screen and plugin manager
* EMI's config screen has a **Modules** group with an on/off switch per module, and a section with a jump button for every
  module. All former add-on settings live there. English and German texts.
* **Plugin manager** (button "Plugins" next to "Presets"): every EMI plugin of every mod with last reload time, number of
  recipes and stacks, error marker and an on/off switch; closing the screen starts an EMI reload. Turn off duplicate
  plugins of other mods by mod id.
* Every module jump button has its own icon, including the world icon. As many module jumps as fit next to EMI's own
  jumps are shown, "Modules" first.

### New features
**Item Info (module `info`)**
* Hover a stack and press **I** to open the Item Info screen: **How to Get** (recipes by category, loot, world generation,
  trades, ...), **Used For** and **Details** (fuel burn time, composting chance, advancements with the item as their icon,
  EMI info pages, item tags). Items nothing in EMI produces are marked "No Known Source".
* Provider API for other mods and plugins (`dev.emi.remastered.info.api`, `docs/INFO_PROVIDERS.md`).
* `/emiexport <recipes|items|sources|nosource|tags|all> [json|csv]` writes recipes, the item index, the sources of every
  stack, the stacks nothing produces and all tags to `emi_export/` for pack developers (`docs/EXPORT.md`).
* **Item view tabs**: EMI's recipe screen and the Item Info screen of a stack share a tab bar **Info | Recipes | Uses**
  with the number of recipes and uses on the tabs. **R** (and left click) opens the tab set by the setting
  `info.primary-view` (Recipes by default, or Info), **U** (right click) opens Uses, **I** opens Info. Tabs switch in
  place, so Back leaves the view instead of stepping through the tabs. R or U on a stack without recipes or without
  uses opens Info instead of an empty screen. The bar also shows when another mod opens the recipes or uses of a stack,
  and it is left out on views without one stack (all recipes, a category, a tag, a recipe tree pick). Settings
  `info.tab-bar` (default on; off gives EMI's plain behaviour) and `info.primary-view`. The "Item Info" key hint in
  item tooltips (`info.tooltip-hint`) is now on by default for new configs.
* **Search by source**: `~text` in EMI's search bar finds stacks by where they come from: `~stronghold` (stronghold
  chests), `~cleric` (what a cleric sells), `~"ore generation"`, `~/desert|ancient/` (regular expression),
  `~nosource` (the "No Known Source" stacks), `-~crafting` (everything that cannot be crafted), and `a ~b | ~c d` as with
  EMI's other prefixes. The labels are the recipe categories and, for loot, mob, block, trade and ore recipes, the loot
  table, mob, block, villager profession, structure and dimension names. Other mods' recipes can add labels by
  implementing `dev.emi.remastered.info.search.SourceLabels`. Syntax and examples: `docs/SEARCH.md`.
* **Other Sources**: a category for item sources that are given in code and have no loot table or recipe. A pack or a
  mod declares them in `assets/<namespace>/emi_sources/*.json` (item, a sentence, optional stacks, chance and
  `requires_mod`; `docs/SOURCES.md`). EMI shows them as recipes, so they appear in the recipe screen, under "How to Get"
  on the Item Info page, in `/emiexport sources`, and the item loses its "No Known Source" marker. Built in: turtle
  scute, armadillo scute, dragon's breath and the dragon egg. Setting `info.extra-sources` (default on); the plugin
  manager lists the feature as plugin `emi_info`.

**Loot**
* Structures a chest is found in (read from the structure templates on a background thread), condition icons, hints for
  drops that depend on Looting, Fortune, Silk Touch, fire or a player kill, a Lootr note, a choice of icon, tooltip or
  plain conditions, and loot sections on the Item Info page.
* Vanilla chests that the game places in code instead of a structure template (desert pyramid, jungle temple, igloo,
  mineshaft, nether fortress, stronghold, end city, buried treasure, shipwreck, ocean ruin, woodland mansion, ruined
  portal) show their structure too, on the chest page and in "Chests in Structures": 53 of the 56 vanilla chest tables
  have a structure now.
* Very small chances (rare fishing junk, for example) show as "<0.1" instead of rounding to 0 or printing an exponent.

**Ores**
* Ore and geode generation for every dimension including modded ones, a compact biome list ("+N more", "all biomes"),
  the player's height on the graph, the best height highlighted, and a World generation section on the Item Info page.
* The data is sent in start, batch and complete messages, so a reload in the middle of a sync never shows a partial list.

**Effects**
* Where a status effect comes from: potions with splash, lingering and arrow variants, food, hand-written sources (mobs,
  beacon, conduit, totem, ...) and cures; the effects an item applies on the Item Info page.

**Enchanting**
* Real maximum level (including level caps from other mods), the enchantments that cannot be combined, where to find each
  enchantment (enchanting table, loot, librarian trades, equipment, treasure, curse), enchantments on the Item Info page.
  Enchanted books above the vanilla maximum open the page (setting `enchanting.show-above-max`).

**Professions**
* The real trades of every villager profession by level and of the wandering trader, including trades added by other
  mods (every trade factory is rolled to find random variants); who sells or buys an item on the Item Info page.

**Distraction free**
* Pin key, "Show Recipes" / "Hide Recipes" buttons, configurable fade, separate switches for inventory, crafting table
  and other containers, an exclusion list of screens, and EMI's recipe screens keep their sidebars.

**New modules**
* `mobs`: a Mob Info page for every mob with health, armour, attack damage, speed, spawn biomes with weight and group size
  (data from the server), and a link to its drops. Mobs whose spawn egg is missing or not in the index (ender dragon,
  wither, giant, illusioner, armor stand, ...) are mob stacks in the index, drawn as the mob's portrait: you can search
  them by name, R opens their Mob Info page, U lists Mob Info and Mob Drops, and the Item Info screen opens for them.
  Mob stacks can be favourites and survive a restart. Setting `mobs.eggless-in-index` (default on, needs an EMI reload).
* `stats`: a Food category, and Food, Tool, Weapon and Armour sections on the Item Info page.
* `world`: Biome pages (climate, tags, dimensions, mobs, structures, ores) and Structure pages (dimensions, biomes,
  generation step, spacing, stronghold rings, chest loot links); structures are searchable. The Item Info screen of a
  biome or structure stack has a Details section (temperature, downfall, dimensions; generation step, number of biomes)
  with a link to its page. Biome stacks are added to the index by the world module (setting `world.biomes-in-index`,
  default on) whenever the ores module does not add them (`ores.add-biomes-to-index`); a biome is never in the index
  twice.
* `tools`: tag browser (**T**) and copy recipe as text or JSON (**Ctrl+C**, Shift for the other format).

**Misc**
* New mod icon and logo (pixel-art slot, gem and magnifier), generated by `scripts/logo/make_logo.py`; the files are in
  `docs/logo/` and `src/main/resources/icon.png`.

### Fixes
**EMI core**
* The EMI reload no longer keeps running after the world is gone (kick or disconnect while joining): it stops cleanly, a
  reload thread that waits for a plugin is woken up, and the reload thread no longer keeps the game from closing. (The
  test pack logged one 71 s reload with about 31,700 errors from this.)
* Favourites, history, hidden stacks and recipe defaults that cannot be read (for example a stack of a mod that is not
  loaded) are kept in `emi.json` instead of being dropped at the next save.
* Saved EMI data with a stack type nobody provides any more (a disabled plugin or a removed mod) logs one warning per
  type instead of an error with a stack trace for every entry; the entries stay in `emi.json`.
* A recipe with no category no longer aborts the whole reload (upstream PR 1222).
* The "Exception thrown registering repair recipes" error (28 tools per reload in the test pack) and the anvil and
  grindstone recipes it cost are fixed.
* `index-source: creative` no longer lists items that are in no creative tab.
* Searching `#<block tag>` finds blocks (EMI commit 4c22e3b0).
* Remainders of shaped recipes (trapped chest, chest boats) are read with the right index.
* Shift+fill ("fill max") on crafting tables no longer overflows and clears the grid (upstream PR 1194).
* The mod name stays in item tooltips during a reload.
* EMI Remastered's own mixins are no longer logged as "EMI class is tainted".
* Hiding stacks no longer rewrites `emi.json` and re-bakes the index on every click; saves are debounced.
* The check for a focused text field only looked at the first container of a screen; fixed.
* Favourite remainder counts: the remaining amount of a synthetic favourite now includes what recipe remainders already
  serve (for example the stick left over from one stick recipe that a later step needs; upstream 1113, 895, 766). Port of
  aMelonRind/emi commits 2508321 and e7fd7d9.

**Modules**
* Loot: the structure lookup for chests no longer blocks the server thread when a player joins (in a large pack the
  join waited about 24 s). Players get the result when it is ready, and chest pages, Item Info and structure pages
  update without an EMI reload; the loot links of the world module are no longer cut after 25 s.
* Loot: the `copy_name` condition showed a raw key and a typo; a mob loot build no longer logs thousands of stack traces
  when the world disappears (the first five are kept); Slime and Magma Cube are no longer drawn huge; a client list
  cleared during the build no longer skips the whole mob loot tab.
* Ores: the client is no longer kicked with "Invalid player data" when a mod registers an unbound placed feature, players
  without the mod can join, big feature lists are sent in parts, and a saved biome favourite no longer fails while no
  world is loaded.
* Effects: effects hidden in edit mode stay hidden after a restart; the effect index is built in one pass (63 to 93 ms
  instead of about 1.1 s in the dev world).
* Enchanting: books above the vanilla maximum have an entry; pressing U on a book opens its own enchantment page first.
* Let's Do: the plugin crash on every reload is fixed. Vinery 1.5.5 renamed classes (`NoClassDefFoundError` for
  `RecipeTypesRegistry`) and Meadow renamed `FONDUE` to `FONDUE_POT`; the module now uses the names of the current
  versions, loads the part of each Let's Do mod on its own (one incompatible mod only loses its own pages), and no
  longer duplicates the categories of the native Vinery, Bakery and Farm and Charm plugins. The refmap warning at every
  start is gone.
* Distraction free: the mixin crashes with JEI versions are gone (the module has no mixins), hidden item icons can no
  longer be clicked, and it works with current EMI versions.
* Professions: profession recipe ids no longer cause warnings from the recipe manager. Trade rows on the Item Info page
  show the amount of the viewed item in the row title (for example "Cleric - Master · 22×") and the stacks next to it
  show their own amounts.
* Stats: armour sections show again (read through `ItemStack#applyAttributeModifiers`), numbers use the language of the
  game (9.6 instead of 9,6 in English), the honey bottle takes 2 s and leaves a glass bottle, the food page is wide
  enough for long effect rows, the Weapon section is only for melee weapons (axes kept), the mace has no Tool section and
  tag names are readable.
* Tools: copying a shaped or shapeless crafting recipe as JSON gives real data-pack JSON (pattern and key, tags where the
  items equal a tag); a notice is drawn top right after copying (EMI hides toasts on its screens); in the tag browser
  numbers follow the game language, the selection is cleared when the search filters it out, long tag names are no longer
  cut (the registry label and the count give way first, the full id is in the row tooltip), all registries have friendly
  titles, biome members are translated and empty tags are dimmed with a red count.
* Loot, ores and mobs: the Item Info loot rows no longer show an empty slot and the block drop cost has a single entry;
  condition tooltips read "pickaxe of iron or better" and small chances "x% of a roll"; the geode page skips the empty
  filling slot; ore rows that only differ by feature are merged and the biomes are shown as catalysts; the barrier tooltip
  explains the chance; mob portraits fit the rendered model; mob stats use points with hearts in brackets. Biome lists and
  dimensions of ore pages are sorted (translated name), so they no longer change order between launches.
* Item Info: the tag list has one "Show all N tags" link and translated or readable tag names, the raw id is in the tooltip.
  Two tags with the same name (for example "Fruits") are no longer shown without a way to tell them apart.
* Mobs: the mob page shows the wither skeleton's real attack damage (it is set when the mob spawns), and no speed for
  flying mobs such as the bat and the ghast, where the walking speed was wrong.
* Stats: pages keep a right margin for right-aligned numbers.
* Item Info: right-aligned details (counts, chances, trade amounts), example slots and bar values keep 3 px to the edge of
  the scroll area instead of touching it.
* Loot: the "Trial Chamber Loot" category is only registered when `loot.split-chest-loot-by-trial` is on; with the
  setting off (the default) the recipe screen listed an empty tab.
* Loot: the log line of the structure lookup counts the chest loot pages that are really sent to clients ("Chest loot
  pages with a known structure: N of M") and is written once per build on the server thread, together with the number
  of loot tables by path, which is a different set.

### Performance
* Warm reload on a bench pack with about 92,000 recipes: 5761 ms to 2445 ms in the paired run, about 40 % faster pooled
  over all runs; the EMI plugin phase is 49 to 66 % faster; joining a world is about 21 % faster.
* About 60 MB less heap (identical tag ingredient stack lists are shared) and about 37 % less allocation per reload.
* The baked state (index, recipes, lookups, tags, search) is identical to stock EMI, checked section by section.
* Joining a world no longer waits for the chest structure lookup of the loot module (about 24 s in a large
  pack); it runs on a background thread and the result is sent to players when it is ready.
* Packs with Merge Enchantments (every enchantment accepts every item): the enchantments that share the same item set
  build their item columns once, and the item index for enchantments is built on first use. The enchanting plugin
  (`emi_enchanting`) went from 2261 ms to 134 ms in the real-pack run below.
* The mob loot recipe data (`emi_loot`) is started while the plugins initialise instead of when the loot plugin
  registers: 2036 ms to 151 ms in the real-pack run below.
* Search bake: the suffix arrays of the item search (names, tooltips, mods, aliases) are built by a faster sort that
  gives exactly the same arrays, and the four arrays are built at the same time. If the fast build is unavailable, the
  original build is used, and a check at start-up tests the faster sort against the original. The index for `~` source
  queries is built in the background, and hiding no longer builds the id of every stack when there are no hide filters.
  The search bake is 45 to 75 % faster in the dev benchmark and 58 test queries return identical results.
* Loot: the structure lookup for the "Found in" lines of chests reads the structure templates on a small number of
  worker threads (at most 4, and two cores stay free for the game), each template once, and merges the results in the
  order of the structures, so the result is identical. A lean reader takes only the loot tables and jigsaw pools from a
  template, and the build ends when the server stops or the data packs reload. On a production server with a pack of
  560 mods it went from 10.2 s to 1.7 s (993 structures; 433 of 789 chest loot tables are placed in a structure and
  563 of 1196 chest loot pages show a structure, the same as before). It always ran off the server thread, so joining a
  world was never blocked by it.
* Loot: the structure lookup cache (new setting `loot.structure-cache`, on by default). The expensive part of the lookup,
  which loot tables the pieces of each jigsaw structure contain, is saved to `emi_remastered/cache/structure-lookup.dat`
  in the game directory (one file for all worlds of that game directory) and used again while a fingerprint matches: the
  mods (id, version, and the size and date of the jar file), the data packs in their order (and their files), and the
  structures and template pools. Everything else (dimensions, chests that are placed by code, tables reached through
  other tables) is computed fresh on every start. A build in which a structure could not be read is not saved, and files
  that change while the game runs switch the cache off for that run. A dedicated server always uses the default (on),
  like the other loot settings. In the real pack below a start without the file took 1.6 s for the lookup plus 5 ms to
  save the file (54 KB), a start with it 0.6 s; on a production server with a pack of 560 mods a hit takes about 0.2 s
  instead of 1.5 s.
* Real pack: the owner's pack (about 640 mods), a client joining a copied world, run of 2026-10-26, compared with the
  run of 2026-10-19 of the same version line. EMI reload at join 23.0 s to 18.1 s, with `emi_enchanting` 2261 to 134 ms
  and `emi_loot` 2036 to 151 ms (the Merge Enchantments and mob loot entries above); search bake about 5.1 s to 4.0 s;
  the `~` source index is built in the background in 1.7 s and is ready 0.9 s after the reload; structure lookup 45.4 s
  to 1.6 s (0.6 s from the cache), which now ends before the player joins, so chest pages get their structures right
  away instead of by a late update; login to joined 4.6 s to 2.3 s. The chest counts are unchanged (563 of 1196 chest
  loot pages with a structure). 39 EMI plugins, 0 errors, and the five third-party mods that mix into EMI (sporran, ali,
  certain_questing_additions, clientsort, polymorph) still apply.
* Method: `docs/DEVTOUR.md`, "EMI reload benchmark".

### Known limits
* At GUI scale 3 in a 1280x720 window only the "Modules" jump button of the config screen fits next to EMI's own jumps.
* Sodium's optimised vertex path is not used for batched unlit items.
* Drops that mods give in code instead of a loot table are not read from the mod. A pack or a mod can declare them in an
  `emi_sources` resource file (`docs/SOURCES.md`), and the Other Sources category shows them. Ore features of custom
  configured feature types (for example Mekanism) are not shown.
* Enchanted books far above the listed range (more than 5 levels over the maximum, or above level 10) do not open the
  enchantment page.
* All known bugs: `docs/KNOWN_BUGS.md`.

## Earlier versions (EMI 1.1.24)

### Tweaks
* Adjusted mixin notification logging to properly avoid printing when empty.

### Fixes
* Fix inconsistent crashes due to mixin order, typically on Fabric 1.19.2 or 1.20.1 #1171
* Fixed double applications of self-mutation mixins
