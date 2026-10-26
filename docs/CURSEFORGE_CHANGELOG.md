<!-- Paste everything below this line into the CurseForge file changelog (Markdown editor). -->
## EMI Remastered 2.0.0 for Minecraft 1.21.1 (Fabric)

First release. EMI 1.1.24 and seven add-ons in one mod.

**Important: remove the jars of EMI and of the seven merged add-ons before you install this mod. A leftover jar stops the game at start-up. Requires Fabric API, Fabric Loader 0.16.14 or newer and Java 21.**

### Included mods
- EMI 1.1.24 (the base)
- Distraction Free Recipes 1.2.2
- EMIffect 2.1.5
- EMI Professions 1.0.3
- EMI Enchanting 0.1.2
- EMI Ores 1.3
- Let's Do EMI Compat 2.0.3
- EMI Loot 0.7.9

### New
- One config screen with a **Modules** group (an on/off switch per module) and a **Plugin manager** to turn EMI plugins of any mod on or off.
- **Item Info** screen (hover a stack and press **I**) with How to Get, Used For and Details. Items nothing produces are marked "No Known Source".
- Item view tabs **Info | Recipes | Uses** on EMI's recipe screen and on the Item Info screen.
- **Search by source**: type `~stronghold`, `~cleric` or `~nosource` in the search bar to find stacks by where they come from.
- **Other Sources** category for sources that are given in code, declared by packs or mods in `emi_sources` files.
- Loot pages show the structures a chest is found in, condition icons and hints for Looting, Fortune, Silk Touch and more.
- Ore and geode generation for every dimension, including modded ones.
- Where a status effect comes from, enchantment details and the real trades of every villager profession.
- New `mobs` module: a Mob Info page for every mob. Mobs without a spawn egg (ender dragon, wither, ...) can be searched and favourited.
- New `world` module: Biome pages and Structure pages, and both are searchable.
- New `stats` module (Food, Tool, Weapon and Armour sections) and `tools` module (tag browser on **T**, copy recipe as text or JSON with **Ctrl+C**).
- `/emiexport` writes recipes, items, sources and tags to `emi_export/` for pack developers.
- New mod icon and logo. English and German texts.

### Fixes
- The EMI reload stops cleanly when the world is gone (kick or disconnect while joining).
- Favourites, history and hidden stacks of mods that are not loaded are kept in `emi.json` instead of being dropped.
- A recipe without a category no longer aborts the whole reload. Anvil and grindstone repair recipes work again.
- Shift+fill on crafting tables no longer overflows and clears the grid.
- Let's Do: the plugin crash on every reload is fixed (Vinery 1.5.5 and Meadow renames).
- Distraction free: the mixin crashes with JEI are gone.
- Ores: the client is no longer kicked with "Invalid player data" when a mod registers an unbound placed feature.
- Many display fixes on the Loot, Stats, Tools, Mob and Item Info pages (numbers, tooltips, layout, tag names).

### Performance
- Warm reload on a bench pack with about 92,000 recipes: 5761 ms to 2445 ms in the paired run, about 40 % faster pooled over all runs. Joining a world is about 21 % faster.
- About 60 MB less heap and about 37 % less allocation per reload.
- In a real pack of about 640 mods: EMI reload at join 23.0 s to 18.1 s, structure lookup 45.4 s to 1.6 s (0.6 s from the cache), login to joined 4.6 s to 2.3 s.
- Joining a world no longer waits for the chest structure lookup. It runs in the background and is saved in a cache file (setting `loot.structure-cache`).

### Known limits
- At GUI scale 3 in a 1280x720 window only the "Modules" jump button of the config screen fits.
- Drops that mods give in code instead of a loot table are not read from the mod. Packs and mods can declare them as Other Sources.
- Ore features of custom configured feature types (for example Mekanism) are not shown.
- Enchanted books far above the listed range do not open the enchantment page.

Full changelog: https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/CHANGELOG.md

Based on EMI by Emi and add-ons by fzzyhmstrs, Abbie, Flamarine and Prismwork, Mrbysco and ShyNieke, Pandaismyname1 and Toni (txnimc). Licences and credits: https://github.com/navrelis/EMIRemastered/blob/mc/1.21.1/CREDITS.md
