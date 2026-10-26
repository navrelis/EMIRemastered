# EMI Remastered

EMI Remastered is [EMI](https://github.com/emilyploszaj/emi), the featureful and accessible item and recipe viewer, with
seven popular EMI add-ons built in, plus new pages and tools that show you where things come from. It is **one Fabric mod
for Minecraft 1.21.1** instead of eight jars.

![EMI Interface](https://user-images.githubusercontent.com/14813658/224562247-1588064e-39ef-475a-9108-d7a357af6939.png)

![Recipe Tree](https://user-images.githubusercontent.com/14813658/224562258-1a5ee67a-fd7f-489f-9eed-ae67c184ddac.png)

*(Screenshots of EMI's main interface and recipe tree, which are unchanged.)*

| | |
|---|---|
| Name / mod id | EMI Remastered / `emi` |
| Version | `2.0.0+1.21.1` |
| Jar | `EMIRemastered-1.21.1-2.0.0-Fabric.jar` |
| Minecraft / loader | 1.21.1, Fabric Loader 0.16.14 or newer, Fabric API, Java 21 |
| Sides | client and server (the server part is optional, see [Multiplayer](#multiplayer)) |

## Keys

Hover a stack (in EMI's index, a recipe, your inventory or an Item Info page) and press the key. Every key can be
changed in EMI's config screen (groups Binds, Item Info and Tools).

| To open | Key |
|---|---|
| Recipes of a stack (the "Recipes" tab; "Info" instead when `info.primary-view` is Info or the stack has no recipes) | **R** or left click |
| What a stack is used for (the "Uses" tab; "Info" when it has no uses) | **U** or right click |
| Item Info of a stack (how to get it, what it is used for, details) | **I** |
| Mob Info, Biome or Structure page | **R** on the spawn egg, mob, biome or structure stack |
| Tag browser (over a tag or an item: that tag, or the tags of the item) | **T** |
| Copy the hovered recipe as text (**Shift**: as JSON) on EMI's recipe screen | **Ctrl+C** |
| All recipes | **Ctrl+Y** |
| Search by source, for example `~stronghold` or `~nosource` | `~` in the search bar, see [Search by source](#search-by-source-) |
| Favourite a stack | **A** |
| Back to the previous EMI page | **Backspace** |
| Show or hide EMI | **Ctrl+O** |
| Focus the search bar | **Ctrl+F** |
| EMI config screen | the **EMI** button next to the index; the **Plugins** button there opens the plugin manager |
| Pin or unpin distraction-free recipes | unbound by default, set `distraction_free.pin-bind` |
| Export lists for pack developers | the chat command `/emiexport`, see [below](#emiexport-for-pack-developers) |

On the recipe screen and the Item Info screen of a stack a tab bar **Info | Recipes | Uses** switches between the three
views in place; Backspace leaves the view.

## What is inside

EMI 1.1.24 plus:

- [Distraction Free Recipes](https://github.com/txnimc/DistractionFreeRecipes) 1.2.2
- [EMIffect](https://github.com/Prismwork/EMIffect) 2.1.5
- [EMI Professions](https://github.com/Mrbysco/EMIProfessions) 1.0.3
- [EMI Enchanting](https://github.com/fzzyhmstrs/EMI_enchanting) 0.1.2
- [EMI Ores](https://github.com/Abbie5/emi-ores) 1.3
- [Let's Do EMI Compat](https://github.com/Pandaismyname1/Let-s-Do-EMI-Compat) 2.0.3
- [EMI Loot](https://github.com/fzzyhmstrs/EMI_loot) 0.7.9

All authors, repositories, exact commits and licences are in [CREDITS.md](CREDITS.md).

## Installation

1. Install Minecraft 1.21.1 with Fabric Loader and Fabric API.
2. Put `EMIRemastered-1.21.1-2.0.0-Fabric.jar` into `mods/`.
3. **Remove the jars it replaces:** EMI, Distraction Free Recipes, EMIffect, EMI Professions, EMI Enchanting, EMI Ores,
   Let's Do EMI Compat and EMI Loot. EMI Remastered declares the mod ids of the seven add-ons (`emi_loot`,
   `emi_ores`, `emiffect`, `emi_enchanting`, `emiprofessions`, `emi_letsdo_compat`, `distraction_free_recipes`) as
   its own (`provides`), and it is the mod `emi`. A leftover jar of any of them therefore makes
   Fabric Loader stop at start-up with an error that names the conflicting mod, instead of running two copies of the same
   code.
4. The add-ons' own libraries (fzzy_config, TXNI Lib, Architectury, ForgeConfigAPIPort) are not needed by EMI Remastered.
   Other mods may still need them.

Your EMI settings (`config/emi.css`), favourites and history (`emi.json`) are read as before. The settings of the old
add-ons are not imported: every setting now lives in EMI's config screen (see below).

## Features by module

Every module can be switched on or off. The module's own pages and data use the old add-on's names, so resource packs, data
packs and saved favourites that refer to `emi_loot`, `emiffect`, `emi_ores` and so on keep working.

| Module (`modules.<id>`) | What it adds |
|---|---|
| `loot` (EMI Loot) | Categories for chest loot, block drops, mob drops, gameplay drops (fishing, gifts, bartering) and archaeology loot, optionally trial chamber loot as its own category (`loot.split-chest-loot-by-trial`; the category is only listed when that is on). Drop chances with conditions shown as icons (or tooltip lines or hidden), grouped identical drops, the structures a chest is found in, hints for drops that depend on Looting, Fortune, Silk Touch, fire or a player kill, and a note when Lootr is installed (every player gets their own loot). |
| `ores` (EMI Ores) | Ore and geode generation pages: height graph, biomes (compact icon list with "+N more", whole dimensions summed up as "all biomes"), the player's current height, the best height highlighted, modded dimensions, and every biome as a searchable stack in the index. A "World generation" section on the Item Info page of ores. |
| `effects` (EMIffect) | Every status effect as a stack in the index with a page: description, attribute modifiers, and where it comes from (potions with their splash, lingering and tipped arrow variants, food, hand-written sources such as mobs, beacon, conduit and totem, and cures). Item Info shows the effects an item applies. |
| `enchanting` (EMI Enchanting) | An "Enchantment Info" page per enchantment: real maximum level (including level caps changed by other mods), the enchantments it cannot be combined with, the items it applies to, and where it can be found (enchanting table, loot, librarian trades, villager and mob equipment, treasure, curse). Enchanted books above the vanilla maximum open the page too. The Item Info screen lists the enchantments of an item and those that fit it. |
| `professions` (EMI Professions) | A page per villager profession with its workstations and the villager model, plus the real trades of every profession by level and of the wandering trader, including trades added by other mods. The Item Info page of an item lists who sells or buys it. |
| `letsdo` (Let's Do EMI Compat) | Recipe pages for Let's Do mods that have no EMI support of their own: grape mashing (Vinery), cake cutting (Bakery), fondue, cheese forming, cooking cauldron and woodcutting (Meadow), cauldron and kettle brewing (Herbal Brews), mini fridge and tiki bar mixing (Beach Party). Categories that a mod's own EMI plugin already adds (Vinery, Bakery, Farm & Charm) are not duplicated. A mod whose classes changed only loses its own page, not the rest. |
| `distraction_free` (Distraction Free Recipes) | Hides EMI's index and recipe sidebars until you search. A "Show Recipes" / "Hide Recipes" button, a pin key (unbound by default), a configurable fade, separate switches for the inventory, the crafting table and other container screens, an exception list of screen classes, and EMI's recipe screens keep their sidebars. |
| `info` (Item Info) | Hover any stack and press **I** to open the Item Info screen: **How to Get** (recipes grouped by category, loot, world generation, trades, ...), **Used For**, and **Details** (fuel burn time, composting chance, advancements that use the item as their icon, EMI info pages, item tags). Items nothing in EMI produces are marked "No Known Source". The page is built from providers that all modules and other mods' EMI plugins can feed, see [docs/INFO_PROVIDERS.md](docs/INFO_PROVIDERS.md). A tab bar **Info | Recipes | Uses** joins EMI's recipe screen and the Item Info screen of a stack (settings `info.tab-bar` and `info.primary-view`). Also provides the `~` search by source ([below](#search-by-source-)), the **Other Sources** category for sources given in code ([docs/SOURCES.md](docs/SOURCES.md)) and `/emiexport` (see below). |
| `mobs` | A "Mob Info" page for every living mob: health, armour, toughness, attack damage, speed, knockback resistance, follow range, size, the biomes it spawns in with spawn weight and group size, and a link to its drops (with the loot module). Spawn data comes from the server. Mobs whose spawn egg is missing or not in the index (ender dragon, wither, giant, illusioner, armor stand, ...) are stacks in the index drawn as the mob, so you can search them and open their page with R (`mobs.eggless-in-index`). |
| `tools` | **T** opens the tag browser: all tags of items, blocks, fluids and entity types with search and member lists; over a tag ingredient it opens at that tag, over an item it lists the tags the item is in. **Ctrl+C** over a recipe on EMI's recipe screen copies it as readable text or as JSON (the vanilla recipe JSON where EMI knows it); Shift switches to the other format. |
| `stats` | A "Food" category with a page per food (hunger, saturation, eating time, effects, leftovers), and Food, Tool, Weapon and Armour sections on the Item Info page (mining speed, durability, repair material, attack damage and speed, damage per second, armour, toughness). Client only. |
| `world` | "Biome" pages (temperature, downfall, precipitation, tags, dimensions, spawning mobs, structures, ores) and "Structure" pages (dimensions, biomes, generation step, structure set weight, spacing and separation, concentric rings for strongholds, links to the chest loot). Structures are searchable in the index, and so are biomes (`world.biomes-in-index`; added by this module whenever the ores module does not add them, never twice). The Item Info screen of a biome or structure stack has a Details section with a link to its page. |

### One config screen

Open EMI's config screen. After EMI's own groups come a **Modules** group with one on/off switch per module, and a
section for each module with its settings and a jump button per module. The tooltip of a module switch says when a change
takes effect: immediately, with an EMI reload, or after a restart (the last one for `loot`, `ores`, `mobs` and `world`,
which have a server part). Settings that change recipes or the index say "Needs an EMI reload" in their tooltip.

The **Plugins** button next to "Presets" opens the **plugin manager**: every EMI plugin of every mod (including the module
plugins above) with its last reload time, the number of recipes and stacks it added and an error marker, and a switch to turn
it off. Closing the screen after a change saves it and starts an EMI reload. This is also the way to switch off
duplicates, for example another mod's loot tab when you prefer the loot module's.

The English and German texts are included.

## Search by source (`~`)

`~text` in EMI's search bar finds stacks by where they come from, the same way `@` finds a mod and `#` a tag:

| Query | Finds |
|---|---|
| `~stronghold` | everything found in stronghold chests |
| `~cleric` | everything a cleric sells |
| `~"ore generation"` | the ore blocks that generate in the world |
| `~nosource` | stacks marked "No Known Source" on the Item Info screen (creative-only or unobtainable) |
| `-~crafting` | everything that cannot be crafted |
| `~/desert\|ancient/` | desert pyramid or ancient city chest loot (regular expression) |
| `~nether ~chest` | chest loot found in the Nether (space is AND, `\|` is OR) |

The labels come from the recipe categories and, for chest, mob, block and gameplay loot, trades and ore generation, from
the loot table, mob, block, profession, structure and dimension names. Mods can add labels to their own recipes with
`dev.emi.remastered.info.search.SourceLabels`. Part of the `info` module. Syntax, label rules and notes:
[docs/SEARCH.md](docs/SEARCH.md).

## Other Sources (`emi_sources`)

Some items are handed out in code (a mob head from a charged creeper's explosion, a mod's reward in an event handler),
so no loot table or recipe shows them. A pack developer or mod author lists such sources in
`assets/<namespace>/emi_sources/*.json` of a resource pack or mod; EMI shows them as the category **Other Sources**, under
"How to Get" on the Item Info page, and the item no longer counts as "No Known Source". Built in: turtle scute, armadillo
scute, dragon's breath and the dragon egg. Switch: `info.extra-sources`; plugin id `emi_info`. File format and errors:
[docs/SOURCES.md](docs/SOURCES.md).

## `/emiexport` for pack developers

A client command of the `info` module that writes what EMI knows to `<game dir>/emi_export/<date>/` as JSON or CSV:

```
/emiexport <recipes|items|sources|nosource|tags|all> [json|csv]
/emiexport cancel
```

`recipes` (every recipe with inputs, catalysts, outputs), `items` (the whole index), `sources` (every index stack with the
"How to Get" sections that produce it), `nosource` (stacks nothing produces: creative-only or unobtainable), `tags`.
Use it to find unobtainable items, check balance or diff two pack versions. Details, columns and cost:
[docs/EXPORT.md](docs/EXPORT.md). The tag browser and Ctrl+C recipe copy (module `tools`) are aimed at pack developers as
well.

## Performance

EMI Remastered reloads faster than EMI plus the add-ons and uses less memory. Measured with the reload bench in the dev
tooling ([docs/DEVTOUR.md](docs/DEVTOUR.md), "EMI reload benchmark"), on a generated large pack with about 92,000
recipes, 12,000 items and 4,000 tags, compared with EMI 1.1.24 in the same harness:

| | Before | After |
|---|---|---|
| Warm reload (`F3+T`), paired run | 5761 ms | 2445 ms |
| Warm reload, pooled over all runs | | about 40 % faster |
| Reload when joining a world | | about 21 % faster |
| Heap after a reload | | about 60 MB less (identical tag ingredient lists are shared) |
| Allocation per reload | | about 37 % less |

The baked state (index, recipes, lookups, tags, search results) was compared section by section with a hash and is
identical to stock EMI. Numbers on your pack will differ; they depend on the recipes and the plugins. The effects index
alone went from about 1.1 s (EMIffect 2.1.5, cold) to 63-93 ms on the dev world.

In packs with Merge Enchantments (every enchantment accepts every item) the enchanting plugin builds the item list of
enchantments that share the same item set once, and the mob loot data is started while the plugins initialise. Dev
benchmark estimates for the test pack: the enchanting plugin about 2.3 s to 0.3 s, the mob loot plugin about 2.0 s to
0.15 s on the first join, together about 3.8 s less of a 23 s join reload.

The search index of the item list is built 45 to 75 % faster in the dev benchmark (same results; 58 test queries
returned identical lists). The "Found in" lines of chest loot are looked up in the background on a few worker threads:
10.2 s to 1.7 s on a production server with a pack of 560 mods, with the same result as before.
<!-- DOC6: structure cache + real-pack numbers pending -->


## Compatibility

- Every class, method and field of `dev.emi.emi` (the API and the internals) keeps its name and signature. Mods that
  register an EMI plugin, call EMI internals or mix into EMI classes continue to work against EMI Remastered; the mod id is
  still `emi`, and the version `2.0.0+1.21.1` satisfies version ranges such as `emi >=1.1.x`.
- EMI Remastered's own mixins into EMI are not reported as "EMI class is tainted" in the log.
- The JEI bridge (JEMI) of EMI is included and only starts when JEI is installed.
- Duplicates with other mods are your choice: the plugin manager turns off any EMI plugin by mod id (for example a second
  loot viewer), and the Let's Do module skips categories the Let's Do mods already register.
- Fabric 1.21.1 only. NeoForge and Forge builds of the add-ons are not part of this project.

## Multiplayer

EMI Remastered runs on client and server; the server part is **optional**. Without it everything works except the pages that
need data only the server has:

| Module | Needs the mod on the server for |
|---|---|
| `loot` | the loot tables (parsed on the server and sent to the client) |
| `ores` | the ore and geode generation features |
| `mobs` | the biome spawn lists |
| `world` | the structure list and their placement |

Single-player always has them (the integrated server). The server sends these payloads only to clients that can receive
them, so vanilla clients and clients without the mod or with the module switched off are **not** kicked. A dedicated server
loads no client classes. The other modules (effects, enchanting, professions, Let's Do, distraction free, info, tools,
stats) are client-side and need nothing on the server.

## For developers

The API is EMI's: nothing in `dev.emi.emi.api` changed, so a plugin written against EMI 1.1.24 for 1.21.1 works unchanged
and `EmiPlugin` entrypoints (`"emi"`) are loaded as before. Add EMI as a dependency exactly as the original EMI describes
(the API jar `dev.emi:emi-fabric:1.1.24+1.21.1:api`; EMI's maven is `https://repo.sleeping.town/`).

New in this version is the Item Info provider API (`dev.emi.remastered.info.api`) that lets your mod or EMI plugin add
sections to the Item Info screen: [docs/INFO_PROVIDERS.md](docs/INFO_PROVIDERS.md).

To add a module to this project, see [docs/MODULES.md](docs/MODULES.md).

### Building

```
gradlew build
```

Java 21. The jar is `build/libs/EMIRemastered-1.21.1-2.0.0-Fabric.jar`. `gradlew build` also compiles the dev-only
screenshot tour (`src/devtour`), which is never part of the jar.

## Documentation

| File | Content |
|---|---|
| [CHANGELOG.md](CHANGELOG.md) | what changed |
| [CREDITS.md](CREDITS.md) | authors, repositories, refs and licences |
| [docs/MODULES.md](docs/MODULES.md) | the module contract: files, lifecycle, settings, plugin ids |
| [docs/INFO_PROVIDERS.md](docs/INFO_PROVIDERS.md) | Item Info provider API |
| [docs/EXPORT.md](docs/EXPORT.md) | `/emiexport` |
| [docs/SEARCH.md](docs/SEARCH.md) | the `~` search by source |
| [docs/SOURCES.md](docs/SOURCES.md) | `emi_sources` files and the Other Sources category |
| [docs/KNOWN_BUGS.md](docs/KNOWN_BUGS.md) | bug survey of EMI and the add-ons and what this mod fixes |
| [docs/DEVTOUR.md](docs/DEVTOUR.md) | dev screenshot tour and reload benchmark |
| [CONTRIBUTING.md](CONTRIBUTING.md) | contributing |

## Licence

MIT (EMI core and the MIT add-ons) and LGPL-3.0-only for the ores module's code; the biome icons are CC BY-NC-SA 3.0.
Details in [CREDITS.md](CREDITS.md) and `LICENSE`.
