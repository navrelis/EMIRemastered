# EMI Remastered: project page text

Text for the CurseForge and Modrinth pages. Every statement comes from `README.md`, `CHANGELOG.md`, `CREDITS.md`,
`docs/KNOWN_BUGS.md`, `docs/SEARCH.md`, `docs/SOURCES.md`, `docs/THEMING.md` and `fabric.mod.json`. The English summary is also the
`description` in `fabric.mod.json`. The page text below uses lists instead of tables because the CurseForge Markdown
editor does not document table support.

## Summary

EMI 1.1.24 and seven EMI add-ons (Loot, Ores, Effects, Enchanting, Professions, Let's Do, Distraction Free) merged into one Fabric 1.21.1 mod, plus Item Info pages and search by source.

### Kurzbeschreibung

EMI 1.1.24 und sieben EMI-Add-ons (Loot, Ores, Effects, Enchanting, Professions, Let's Do, Distraction Free) in einer Fabric-1.21.1-Mod, plus Item-Info-Seiten und Suche nach Herkunft.

## Tagline

EMI and its seven add-ons in one jar, for Fabric 1.21.1

## Description

![EMI Remastered](https://raw.githubusercontent.com/navrelis/EMIRemastered/mc/1.21.1/docs/logo/banner.png)

**EMI Remastered** is [EMI](https://github.com/emilyploszaj/emi), the item and recipe viewer, with seven popular EMI add-ons built in. It is **one Fabric mod for Minecraft 1.21.1** instead of eight jars. It adds pages and tools that show you where items, effects, enchantments, mobs, biomes and structures come from, and one view per item with the tabs **Info | Recipes | Uses**.

### Replaces these 8 jars

1. Remove the jars of **EMI, EMI Loot, EMI Ores, EMIffect, EMI Enchanting, EMI Professions, Let's Do EMI Compat and Distraction Free Recipes**.
2. Add the one EMI Remastered jar.

The old jars **must** be removed. EMI Remastered keeps the mod id `emi` and declares the mod ids of the seven add-ons (`emi_loot`, `emi_ores`, `emiffect`, `emi_enchanting`, `emiprofessions`, `emi_letsdo_compat`, `distraction_free_recipes`) as its own. A leftover jar therefore stops the game at start-up with an error that names the conflicting mod, instead of running two copies of the same code.

Your EMI settings (`config/emi.css`), favourites and history (`emi.json`) are read as before. Settings of the old add-ons are not imported; every setting now lives in EMI's config screen. The add-ons' own libraries (fzzy_config, TXNI Lib, Architectury, ForgeConfigAPIPort) are not needed by EMI Remastered. Other mods may still need them.

### What is inside

EMI 1.1.24 plus:

- EMI Loot 0.7.9
- EMI Ores 1.3
- EMIffect 2.1.5
- EMI Enchanting 0.1.2
- EMI Professions 1.0.3
- Let's Do EMI Compat 2.0.3
- Distraction Free Recipes 1.2.2

Every add-on is a module you can switch on or off. Modules keep the names of the old add-ons (`emi_loot`, `emiffect`, ...), so resource packs, data packs and saved favourites keep working.

### Find where things come from

**One view per item.** On EMI's recipe screen and on the Item Info screen of a stack, a tab bar shows **Info | Recipes N | Uses N** with the number of recipes and uses. The tabs switch in place, so Backspace leaves the view. R opens Recipes, U opens Uses, I opens Info. If a stack has no recipes or no uses, R or U opens Info instead of an empty screen. The tab bar, the tab that R opens (Recipes or Info) and the key hint in item tooltips can be set in the config screen (`info.tab-bar`, `info.primary-view`, `info.tooltip-hint`).

**Item Info.** Hover a stack and press **I**. The screen shows **How to Get** (recipes by category, loot, world generation, trades, ...), **Used For** and **Details** (fuel burn time, composting chance, advancements that use the item as their icon, tags). Items nothing in EMI produces are marked "No Known Source".

**Search by source with `~`.** Type `~text` in EMI's search bar to find stacks by where they come from, the same way `@` finds a mod and `#` a tag:

- `~stronghold`: everything found in stronghold chests
- `~cleric`: everything a cleric sells
- `~"ore generation"`: the ore blocks that generate in the world
- `~nosource`: stacks marked "No Known Source" (creative-only or unobtainable)
- `-~crafting`: everything that cannot be crafted
- `~nether ~chest`: chest loot found in the Nether (space is AND, `|` is OR)

The labels come from recipe categories and, for loot, mobs, blocks, trades and ore generation, from loot table, mob, block, profession, structure and dimension names. Syntax: [docs/SEARCH.md](https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/docs/SEARCH.md).

**Other Sources.** Some items are handed out in code (a mob head from a charged creeper's explosion, a mod's reward in an event handler), so no loot table or recipe shows them. Pack makers and mod authors can list such sources in `assets/<namespace>/emi_sources/*.json` of a resource pack or mod. EMI shows them as the category **Other Sources**, under "How to Get" on the Item Info page, and the item no longer counts as "No Known Source". Built in: turtle scute, armadillo scute, dragon's breath and the dragon egg. File format: [docs/SOURCES.md](https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/docs/SOURCES.md).

### Modules

**Loot.** Chest, block, mob, gameplay (fishing, gifts, bartering) and archaeology loot with drop chances, conditions, hints for Looting, Fortune, Silk Touch, fire and player kills, the structures a chest is found in, and a note when Lootr is installed.

**Ores.** Ore and geode generation with a height graph, biomes (compact list, "all biomes") and modded dimensions.

**Effects.** Every status effect has a page: description, attribute modifiers and where it comes from (potions, food, mobs, beacon, totem, cures).

**Enchanting.** Per enchantment: real maximum level (including level caps from other mods), the enchantments it cannot be combined with, the items it fits and where to find it.

**Professions.** The real trades of every villager profession by level, the wandering trader, and trades added by other mods. Item Info lists who sells or buys an item.

**Let's Do.** Recipe pages for Let's Do mods without their own EMI support. Categories that a mod's own plugin already adds are not duplicated.

**Distraction free.** EMI's sidebars stay hidden until you search. With a pin key, a fade and per-screen switches.

**Mobs.** A Mob Info page for every living mob: health, armour, attack damage, speed, and the biomes it spawns in with weight and group size. Mobs without a spawn egg in the index (ender dragon, wither, giant, illusioner, armor stand, ...) are stacks in the index drawn as the mob. You can search them by name, open their page with R and use them as favourites.

**World.** Biome pages (climate, tags, dimensions, mobs, structures, ores) and Structure pages (dimensions, biomes, generation step, spacing, stronghold rings, links to the chest loot). Structures and biomes are stacks in the index, so you can search them. The Item Info screen of a biome or structure stack links to its page.

**Stats and tools.**
- Food, tool, weapon and armour stats.
- Tag browser (**T**) for items, blocks, fluids and entity types.
- Copy a recipe as readable text or JSON (**Ctrl+C**, Shift for the other format).
- `/emiexport` writes recipes, items, sources and tags as JSON or CSV for pack developers.

**Resource pack text colours.** A resource pack that recolours EMI's panels to a dark colour can also recolour the panel text with one file, `assets/emi_remastered/text_colors.json`. Without such a pack nothing looks different. File format: [docs/THEMING.md](https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/docs/THEMING.md).

**One config screen.** A **Modules** group with an on/off switch per module and a section for each module's settings. A **plugin manager** (the Plugins button) lists every EMI plugin of every mod and can switch any of them off, for example a second loot viewer.

### Keys

Hover a stack and press the key. Every key can be changed in EMI's config screen.

- **R** or left click: recipes of the stack (Info when the stack has no recipes)
- **U** or right click: what the stack is used for (Info when it has no uses)
- **I**: Item Info
- **R** on a spawn egg, mob, biome or structure stack: Mob Info, Biome or Structure page
- **T**: tag browser
- **Ctrl+C** on a recipe: copy as text (Shift: as JSON)
- **Ctrl+Y**: all recipes
- **A**: favourite a stack
- **Backspace**: back to the previous EMI page
- **Ctrl+O**: show or hide EMI
- **Ctrl+F**: focus the search bar
- `~` in the search bar: search by source
- Pin or unpin distraction-free recipes: unbound by default (`distraction_free.pin-bind`)

### Fixes

- The Let's Do compat crash on every reload is fixed (Vinery 1.5.5 and Meadow renamed classes).
- The repair-recipe error on every reload, and the anvil and grindstone recipes it cost, are fixed.
- Favourites, history and hidden stacks of a mod that is not loaded are kept in `emi.json` instead of being dropped.
- A reload no longer keeps running after you are kicked or disconnected while joining.
- A recipe without a category no longer aborts the whole reload.
- Searching `#<block tag>` finds blocks, and Shift+fill on crafting tables no longer clears the grid.
- Ores: no more "Invalid player data" kick when a mod registers an unbound feature.

All known bugs and their status: [docs/KNOWN_BUGS.md](https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/docs/KNOWN_BUGS.md).

### Performance

EMI Remastered reloads faster than EMI plus the add-ons and uses less memory.

Measured on a generated test pack with about 92,000 recipes, compared with EMI 1.1.24 in the same test setup:

- Warm reload (F3+T): **5761 ms to 2445 ms** in the paired run, about 40 % faster over all runs.
- The EMI plugin phase is 49 to 66 % faster, joining a world is about 21 % faster.
- About 60 MB less heap and about 37 % less allocation per reload.
- The resulting index, recipes and tags are identical to stock EMI.

Measured on a real pack of about 640 mods (a client joining a copied world, compared with an earlier build of EMI Remastered 2.0.0):

- EMI reload when joining a world: **23.0 s to 18.1 s**
- Enchanting plugin: 2261 ms to 134 ms
- Loot plugin (mob loot data): 2036 ms to 151 ms
- Search index bake: about 5.1 s to about 4.0 s
- Chest structure lookup: 45.4 s to 1.6 s (0.6 s from the cache file)
- Login until joined: 4.6 s to 2.3 s

The structures a chest is found in are looked up before you join, so chest pages show them right away. The lookup runs on a few background threads and is saved to a cache file (`emi_remastered/cache/structure-lookup.dat` in the game directory, setting `loot.structure-cache`, on by default). While your mods and data packs stay the same, the next start loads the structures from the file. Any change makes the next start look them up again and replace the file. The chest pages with a structure are the same as before (563 of 1196 in the real pack).

Numbers on your pack will differ. They depend on its recipes and plugins.

### Compatibility

- The mod id stays `emi`. Every class, method and field of `dev.emi.emi` keeps its name and signature, so EMI plugins and add-ons of other mods keep working.
- Client and server. The server part is optional. Without it the loot, ore, mob spawn and structure pages lack their data; vanilla clients and clients without the mod are not kicked.
- The JEI bridge of EMI is included and only starts when JEI is installed.
- Duplicates with other mods are your choice: the plugin manager turns off any EMI plugin by mod id.
- Fabric 1.21.1 only. NeoForge and Forge are not part of this project.

### Languages

English and German for all texts, plus EMI's existing translations.

### Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.14 or newer
- Fabric API
- Java 21

### Credits and licences

Built on the work of Emi (EMI), fzzyhmstrs (EMI Loot, EMI Enchanting), Abbie (EMI Ores), Flamarine and Prismwork (EMIffect), Mrbysco and ShyNieke (EMI Professions), Pandaismyname1 (Let's Do EMI Compat), Toni/txnimc (Distraction Free Recipes), lxly9, unilock and aMelonRind. Repositories, exact commits and licences: [CREDITS.md](https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/CREDITS.md).

Most of the code is MIT. The ores module (from EMI Ores by Abbie) is LGPL-3.0-only. The vanilla biome icons are CC BY-NC-SA 3.0 (Minecraft Fandom Wiki contributors) and are not covered by the MIT or LGPL licence. The complete source code is public.

### Known limitations

- Drops that mods give in code instead of a loot table are not read from the mod. A pack or a mod can declare them in an `emi_sources` file, and the Other Sources category shows them (KB21). Ore features of custom feature types (for example Mekanism) are not shown (KB26).
- Enchanted books far above the vanilla maximum (more than 5 levels over it, or above level 10) do not open the enchantment page (KB28).
- Sodium logs "does not support optimized vertex writing code paths" for batched unlit items; this is not fixed yet (KB13).
- At GUI scale 3 in a 1280x720 window only the "Modules" jump button of the config screen fits next to EMI's own jumps.

### Links

- Source: https://github.com/navrelis/EMIRemastered
- Issues: https://github.com/navrelis/EMIRemastered/issues

## Upload checklist (for the owner, not part of the page text)

- File: `build/libs/EMIRemastered-1.21.1-2.1.0-Fabric.jar` (name `EMIRemastered-1.21.1-2.1.0-Fabric.jar`, version `2.1.0+1.21.1`).
- Project name: "EMI Remastered" (CurseForge asks for no version numbers or class names in the name). Logo: `docs/logo/icon.png` (400x400 PNG, the size CurseForge asks for). Banner in the description: `docs/logo/banner.png`.
- Game version: 1.21.1. Loader: Fabric. Java: 21. Environment: client and server (`fabric.mod.json` has `environment: *`).
- Release type: Release (CurseForge syncs only Release files to the app by default and needs at least one).
- Relations: required dependency **Fabric API**. Incompatible (one entry each, names as listed on CurseForge):
  - EMI (EmilyPloszaj)
  - EMI Loot (fzzyhmstrs)
  - EMI Ores (Abbie34)
  - EMIffect (Status Effects EMI Plugin)
  - EMI Enchanting (fzzyhmstrs)
  - EMI professions (EMIP) (Mrbysco)
  - [Let's Do Addon] EMI Compat (pandaismyname1)
  - Distraction Free Recipes (EMI / REI / JEI) (Txni)
- Licence: the repository `LICENSE` file is the original EMI MIT licence ("Copyright (c) 2022 Emi"). `fabric.mod.json` declares `MIT` and `LGPL-3.0-only`, and CREDITS.md adds CC BY-NC-SA 3.0 for the 64 biome icons. A single dropdown choice cannot express all three. Recommended: **Custom License** with the text "MIT for the code, LGPL-3.0-only for the ores module, CC BY-NC-SA 3.0 for the biome icons. See CREDITS.md and LICENSE in https://github.com/navrelis/EMIRemastered". If you want a plain dropdown choice, pick MIT, which covers most of the code. CurseForge moderation follows the licence shown on the project page when licences conflict.
- Summary (CurseForge limit not stated in its docs, see the report; 255 characters is the practical limit): use the English summary above. The German text is only for Modrinth or a localised page; CurseForge requires English first and allows other languages after it.
- Changelog for the file: paste `docs/CURSEFORGE_CHANGELOG.md` (everything below its first comment line) with the editor in Markdown mode. The full technical list stays in `CHANGELOG.md`.
- Description: switch the editor to Markdown before pasting the "Description" text. Check that the banner image shows after pasting.
- Source and issues links: set the issue tracker to https://github.com/navrelis/EMIRemastered/issues and the source to https://github.com/navrelis/EMIRemastered.
