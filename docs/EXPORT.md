# Pack developer export (`/emiexport`)

The `info` module adds a client command that dumps what EMI knows into JSON or CSV files: recipes, the item index,
where each stack comes from, the stacks nothing produces, and tags. It is meant for pack developers who check
balance, find unobtainable items or diff two pack versions.

```
/emiexport <recipes|items|sources|nosource|tags|all> [json|csv]
/emiexport cancel
```

The format is `json` unless given. The command needs the `info` module switched on (when the game started) and EMI
finished loading with a world open; otherwise it prints a translated error. Only one export runs at a time.

## Output

`<game dir>/emi_export/<yyyy-MM-dd_HH-mm-ss>/`, one file per kind (`recipes.json`, `items.json`, `sources.json`,
`nosource.json`, `tags.json`, or `.csv`). When the export is done a chat message shows the folder; clicking it opens
it in the file manager.

- JSON: a pretty printed array of objects (Gson), UTF-8.
- CSV: one flat table per kind, UTF-8 without BOM, header line first, lines end with CRLF, fields with commas,
  quotes, line breaks or leading/trailing spaces are quoted and quotes doubled (RFC 4180). List cells are joined with `;`.
  Excel needs "import as UTF-8" for non-ASCII names.

## Columns

JSON uses the same names as the CSV columns unless noted.

### `recipes`

All recipes in EMI's recipe manager, one row per recipe.

| Column | Content |
|---|---|
| `id` | recipe id; empty for recipes without an id |
| `category` | EMI category id (`minecraft:crafting`, ...) |
| `inputs` | inputs, one entry per slot |
| `catalysts` | catalysts (workstations that are not consumed) |
| `outputs` | outputs |

Empty slots are left out. One CSV entry is `[AMOUNTx ]id[|id...]` (alternatives separated by `|`) or
`[AMOUNTx ]#tag_id` for a tag slot, followed by ` @CHANCE` when the chance is not 1. Entries are separated by `;`.
In JSON every entry is an object: `{"stacks": [ids], "amount": n}` or `{"tag": id, "amount": n}` (the members of a tag
are in `tags`), plus `"chance"` when it is not 1. Amounts are EMI amounts (fluids in droplets, 81000 per bucket).
Only stack ids are exported, not component data.

### `items`

Every stack of EMI's index (`EmiApi.getIndexStacks()`).

| Column | Content |
|---|---|
| `id` | registry id of the item or fluid |
| `components` | component changes (for example an enchanted book's enchantment); empty for plain stacks (omitted in JSON) |
| `name` | plain display name in the current game language |
| `mod` | namespace of the id |
| `type` | `item`, `fluid` or `other` (stack types of other mods) |
| `hidden` | true when the stack is hidden or disabled in EMI's index |

### `sources`

Every index stack with the "How to Get" sections the Item Info providers produce for it (the same collection as the
Item Info screen, run once per stack without touching the screen's page cache).

| Column | Content |
|---|---|
| `id`, `components`, `name`, `mod`, `type`, `hidden` | as in `items` |
| `has_source` | true when at least one section counts as a source |
| `source_count` | number of OBTAIN sections |
| `source_ids` | ids of the OBTAIN sections (`emi_info:produced_by`, `emi_loot:chest_loot`, ...), `;` separated |
| `source_titles` | their titles in the current language (for example `Recipes (3)`), `;` separated |

JSON has `has_source` and a `sources` array of `{id, provider, title, source}` instead of the last three columns;
`source` is false for sections that do not count as a source (for example a recipe that only turns the stack into
itself). The "No Known Source" marker section itself is never listed, so the result does not depend on the
`info.show-no-source` setting.

### `nosource`

The stacks without a known source: the rows of `sources` with `has_source` false, restricted to items and fluids
(the rule of the "No Known Source" marker). Same columns as `items`. It is the list of things that only creative
mode, commands or sources unknown to EMI give.

### `tags`

Every tag EMI knows (items, blocks, fluids and the other registries EMI has adapters for).

| Column | Content |
|---|---|
| `registry` | registry id (`minecraft:item`, `minecraft:block`, ...) |
| `id` | tag id |
| `count` | number of members |
| `members` | member ids, sorted, `;` separated (JSON: array) |

## Performance

Info providers run on the render thread by contract (`docs/INFO_PROVIDERS.md`), so the export collects on the render
thread in slices of about 10 ms per client tick; the game stays playable but frames drop while it runs. Progress is
printed to the chat every 10 %. Only writing the files happens on a background thread. `sources` and `nosource` need
a page collection for every index stack and are the slow ones (a big pack has tens of thousands of stacks); `nosource`
costs the same as `sources`, `all` collects once for both.

The export stops with a message when EMI reloads or the world is left while it runs, and `/emiexport cancel` stops it.
A stack or recipe that throws is skipped, counted in the final message and logged (first five with stack trace).

## Dev tour

`gradlew runClientTour -Ptour=export` (see `docs/DEVTOUR.md`) starts `all` as JSON through
`ExportCommand.startAll()` (the `call` step), waits and shows `ExportCommand.isRunning()`. The files end up in
`run/emi_export/`.
