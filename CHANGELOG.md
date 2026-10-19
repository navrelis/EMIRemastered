## 2.0.0+1.21.1 (2026-10-19)

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

**Loot**
* Structures a chest is found in (read from the structure templates on a background thread), condition icons, hints for
  drops that depend on Looting, Fortune, Silk Touch, fire or a player kill, a Lootr note, a choice of icon, tooltip or
  plain conditions, and loot sections on the Item Info page.
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
  (data from the server), and a link to its drops.
* `stats`: a Food category, and Food, Tool, Weapon and Armour sections on the Item Info page.
* `world`: Biome pages (climate, tags, dimensions, mobs, structures, ores) and Structure pages (dimensions, biomes,
  generation step, spacing, stronghold rings, chest loot links); structures are searchable.
* `tools`: tag browser (**T**) and copy recipe as text or JSON (**Ctrl+C**, Shift for the other format).

### Fixes
**EMI core**
* The EMI reload no longer keeps running after the world is gone (kick or disconnect while joining): it stops cleanly, a
  reload thread that waits for a plugin is woken up, and the reload thread no longer keeps the game from closing. (The
  test pack logged one 71 s reload with about 31,700 errors from this.)
* Favourites, history, hidden stacks and recipe defaults that cannot be read (for example a stack of a mod that is not
  loaded) are kept in `emi.json` instead of being dropped at the next save.
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
* Professions: profession recipe ids no longer cause warnings from the recipe manager.
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

### Performance
* Warm reload on a bench pack with about 92,000 recipes: 5761 ms to 2445 ms in the paired run, about 40 % faster pooled
  over all runs; the EMI plugin phase is 49 to 66 % faster; joining a world is about 21 % faster.
* About 60 MB less heap (identical tag ingredient stack lists are shared) and about 37 % less allocation per reload.
* The baked state (index, recipes, lookups, tags, search) is identical to stock EMI, checked section by section.
* Method: `docs/DEVTOUR.md`, "EMI reload benchmark".

### Known limits
* At GUI scale 3 in a 1280x720 window only the "Modules" jump button of the config screen fits next to EMI's own jumps.
* Sodium's optimised vertex path is not used for batched unlit items.
* Drops that mods give in code instead of a loot table are not shown, and ore features of custom configured feature types
  (for example Mekanism) are not shown.
* Enchanted books far above the listed range (more than 5 levels over the maximum, or above level 10) do not open the
  enchantment page.
* All known bugs: `docs/KNOWN_BUGS.md`.

## Earlier versions (EMI 1.1.24)

### Tweaks
* Adjusted mixin notification logging to properly avoid printing when empty.

### Fixes
* Fix inconsistent crashes due to mixin order, typically on Fabric 1.19.2 or 1.20.1 #1171
* Fixed double applications of self-mutation mixins
