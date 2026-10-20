# Search by source (`~`)

EMI Remastered adds a query type to EMI's search bar: `~text` finds stacks by where they come from. It belongs to the
`info` module (switch `modules.info`); with the module off a `~` query matches nothing (and `-~x` matches everything).

## Syntax

Same rules as EMI's other prefixes (`@` mod, `#` tag, `$` tooltip):

| Query | Meaning |
|---|---|
| `~text` | some source label of the stack contains `text` (case-insensitive substring) |
| `-~text` | negated: no source label contains `text` (stacks without any source also match) |
| `~"mob drops"` | quotes for text with spaces |
| `~/stronghold\|ancient/` | regular expression, searched in the labels (case-insensitive); `(?m)^...$` anchors one label |
| `~nosource` | stacks the Item Info screen marks as "No Known Source" (exact keyword) |
| `a ~b` | space is AND, `\|` is OR: `~stronghold diamond \| ~cleric emerald` |
| `~` | every stack that has at least one source |

The search bar colours `~` queries orange.

## What a source label is

For a stack, EMI's own index of recipes that produce it (`EmiRecipeManager#getRecipesByOutput`) gives its sources. Each
recipe contributes:

* the translated name of its category and the category id (`Chest Loot`, `emi_loot:chest_loot`, `chest_loot`),
* more labels when the recipe implements `dev.emi.remastered.info.search.SourceLabels`:

| Recipe | Labels |
|---|---|
| Chest loot | loot table id (`chests/stronghold_library`, also with spaces), the structures the chest is found in (translated and id) and their dimensions |
| Mob drops | mob name, mob id, loot table id |
| Block drops | block name and id |
| Gameplay and archaeology loot | table name and id (`fishing`, `piglin bartering`) |
| Villager trades and wandering trader | profession (or trader) name and the level or pool |
| Ore generation | dimensions, the feature id and the ore block names |

Recipes of the categories Information, Tag, Ingredient, Resolution and Fuel and recipes that also consume the stack
(repairing, cloning, dyeing) are left out. This is the rule of the Item Info screen's "Produced by" section, so
`~nosource` agrees with the "No Known Source" marker.

Third-party recipes get the category labels; a mod can implement `SourceLabels` on its recipe class for richer ones.

## Examples

| Query | Finds |
|---|---|
| `~nosource` | items and fluids that only the creative inventory or commands give |
| `~stronghold` | everything found in stronghold chests |
| `~cleric` | everything a cleric sells |
| `~"ore generation"` | the ore blocks that generate in the world |
| `~ore` | block drops of ores (diamond from diamond ore) and ore generation |
| `-~crafting` | everything that cannot be crafted |
| `~/desert\|ancient/` | desert pyramid or ancient city chest loot |
| `~nether ~chest` | chest loot found in the Nether |

## Notes

* `~village` also finds everything the villagers sell (the category is called "Villager Trades").
* The labels are built once per EMI reload, in the background after the search bake (log line
  `Source search index: ...`); until they are ready a `~` query works out the labels of each stack itself (same
  results, slower). Chest structures arrive from the server after joining; the index is rebuilt in the background
  then and a running `~` search is repeated.
* Switching the `info` module on while playing needs an EMI reload before `~` finds anything.
* `~nosource` ignores the setting `info.show-no-source` and sources that other mods add to the Item Info screen through
  its provider API (those only exist when a page is built).
