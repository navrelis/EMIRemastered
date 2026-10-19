# EMI Remastered: project page text

Text for the CurseForge and Modrinth pages. Every statement comes from `README.md`, `CHANGELOG.md`, `CREDITS.md`,
`docs/KNOWN_BUGS.md` and `fabric.mod.json`. The English summary is also the `description` in `fabric.mod.json`.

## Summary

EMI 1.1.24 and seven EMI add-ons (Loot, Ores, Effects, Enchanting, Professions, Let's Do, Distraction Free) merged into one Fabric 1.21.1 mod, plus pages that show where things come from.

### Kurzbeschreibung

EMI 1.1.24 und sieben EMI-Add-ons (Loot, Ores, Effects, Enchanting, Professions, Let's Do, Distraction Free) in einer Fabric-1.21.1-Mod, plus Seiten, die zeigen, woher Dinge kommen.

## Tagline

EMI and its seven add-ons in one jar, for Fabric 1.21.1

## Description

![EMI Remastered](https://raw.githubusercontent.com/navrelis/EMIRemastered/remastered/docs/logo/banner.png)

**EMI Remastered** is [EMI](https://github.com/emilyploszaj/emi), the item and recipe viewer, with seven popular EMI add-ons built in. It is **one Fabric mod for Minecraft 1.21.1** instead of eight jars, and it adds new pages and tools that show you where items, effects, enchantments and structures come from.

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

### Replaces these 8 jars

1. Remove the jars of **EMI, EMI Loot, EMI Ores, EMIffect, EMI Enchanting, EMI Professions, Let's Do EMI Compat and Distraction Free Recipes**.
2. Add the one EMI Remastered jar.

The old add-on jars **must** be removed. EMI Remastered declares their mod ids as its own, so a leftover jar stops the game at start-up with an error that names the conflicting mod, instead of running two copies of the same code.

Your EMI settings, favourites and history are read as before. Settings of the old add-ons are not imported; they now live in EMI's config screen.

### Features

**Item Info.** Hover a stack and press **I**. The screen shows **How to Get** (recipes by category, loot, world generation, trades, ...), **Used For** and **Details** (fuel burn time, composting chance, advancements that use the item as their icon, tags). Items nothing in EMI produces are marked "No Known Source".

**Loot.** Chest, block, mob, gameplay (fishing, gifts, bartering) and archaeology loot with drop chances, conditions, hints for Looting, Fortune and Silk Touch, the structures a chest is found in, and a note when Lootr is installed.

**Ores.** Ore and geode generation with a height graph, biomes (compact list, "all biomes") and modded dimensions.

**Effects.** Every status effect has a page: description, attribute modifiers and where it comes from (potions, food, mobs, beacon, totem, cures).

**Enchanting.** Per enchantment: real maximum level (including level caps from other mods), the enchantments it cannot be combined with, the items it fits and where to find it.

**Professions.** The real trades of every villager profession by level, the wandering trader, and trades added by other mods. Item Info lists who sells or buys an item.

**Let's Do.** Recipe pages for Let's Do mods without their own EMI support. Categories that a mod's own plugin already adds are not duplicated.

**Distraction free.** EMI's sidebars stay hidden until you search. With a pin key, a fade and per-screen switches.

**More pages and tools.**
- Food, tool, weapon and armour stats.
- Mob pages with health, armour, attack damage and spawn biomes.
- Biome and structure pages. Structures are searchable.
- Tag browser (**T**) for items, blocks, fluids and entity types.
- Copy a recipe as readable text or JSON (**Ctrl+C**, Shift for the other format).
- `/emiexport` writes recipes, items, sources and tags as JSON or CSV for pack developers.

**One config screen.** A **Modules** group with an on/off switch per module and a section for each module's settings. A **plugin manager** lists every EMI plugin of every mod and can switch any of them off, for example a second loot viewer.

### Fixes

- The Let's Do compat crash on every reload is fixed (Vinery 1.5.5 and Meadow renamed classes).
- The repair-recipe error on every reload, and the anvil and grindstone recipes it cost, are fixed.
- Favourites, history and hidden stacks of a mod that is not loaded are kept in `emi.json` instead of being dropped.
- A reload no longer keeps running after you are kicked or disconnected while joining.
- A recipe without a category no longer aborts the whole reload.
- Searching `#<block tag>` finds blocks, and Shift+fill on crafting tables no longer clears the grid.
- Ores: no more "Invalid player data" kick when a mod registers an unbound feature.

All known bugs and their status: `docs/KNOWN_BUGS.md` in the repository.

### Performance

Measured on a generated test pack with about 92,000 recipes, compared with EMI 1.1.24 in the same test setup:

- Warm reload (F3+T): **5761 ms to 2445 ms** in the paired run, about 40 % faster over all runs.
- The EMI plugin phase is 49 to 66 % faster, joining a world is about 21 % faster.
- About 60 MB less heap and about 37 % less allocation per reload.
- The resulting index, recipes and tags are identical to stock EMI.

Numbers on your pack will differ. They depend on its recipes and plugins.

### Compatibility

- The mod id stays `emi`. Every class, method and field of `dev.emi.emi` keeps its name and signature, so EMI plugins and add-ons of other mods keep working.
- Client and server. The server part is optional. Without it the loot, ore, mob spawn and structure pages lack their data; vanilla clients and clients without the mod are not kicked.
- The JEI bridge of EMI is included and only starts when JEI is installed.

### Languages

English and German for all texts, plus EMI's existing translations.

### Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.14 or newer
- Fabric API
- Java 21

### Credits and licences

Built on the work of Emi (EMI), fzzyhmstrs (EMI Loot, EMI Enchanting), Abbie (EMI Ores), Flamarine and Prismwork (EMIffect), Mrbysco and ShyNieke (EMI Professions), Pandaismyname1 (Let's Do EMI Compat), Toni/txnimc (Distraction Free Recipes), lxly9, unilock and aMelonRind. Repositories, exact commits and licences: [CREDITS.md](https://github.com/navrelis/EMIRemastered/blob/remastered/CREDITS.md). The author of EMI Remastered asked the authors of the merged mods for permission to use and change their code.

Most of the code is MIT. The ores module is LGPL-3.0-only. The biome icons are CC BY-NC-SA 3.0. The complete source code is public.

### Known limitations

- Drops that mods give in code instead of a loot table are not shown, and ore features of custom feature types (for example Mekanism) are not shown.
- Enchanted books far above the vanilla maximum (more than 5 levels over it, or above level 10) do not open the enchantment page.
- Sodium logs "does not support optimized vertex writing code paths" for batched unlit items; this is not fixed yet.

### Links

- Source: https://github.com/navrelis/EMIRemastered
- Issues: https://github.com/navrelis/EMIRemastered/issues
