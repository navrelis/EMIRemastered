# Extra item sources (`emi_sources`)

Some items are handed out in code: a charged creeper's explosion drops a mob head, a mod gives a reward in an event
handler, another mod injects loot at run time. EMI cannot see such sources in a loot table or recipe, so the item looks
"without source" in the Item Info screen and in `/emiexport nosource`. A pack developer (or a mod) can declare these
sources in a small JSON file in a resource pack. EMI then shows them as recipes of the category **Other Sources**
(`emi_info:other_sources`).

The feature belongs to the `info` module: with the module off there is no category and no recipe. The setting
`info.extra-sources` (default on) switches only this feature off; both need an EMI reload (F3+T or rejoin) to apply.

## Where the files go

Every file `assets/<namespace>/emi_sources/**.json` (any folder depth) of any resource pack, mod or the `resourcepacks`
folder is read when EMI reloads. The files are read on the client from the resource manager, nothing is sent over the
network, so a pack's resource pack (or a mod's assets) is enough in multiplayer.

Resource packs stack as usual: a file with the **same resource id** (`assets/<namespace>/emi_sources/<path>.json`) in a
higher pack replaces the file of the lower one completely. To change a single entry of a file you do not own, copy the
file into your pack, edit it and keep the path. Files with different ids always add up. The built-in defaults of EMI
Remastered are `assets/emi_info/emi_sources/vanilla.json`; copy that path into your own pack to replace or empty them
(`{"sources": []}`).

## File format

```json
{
  "sources": [
    {
      "id": "mypack:piglin_head_charged_creeper",
      "output": "minecraft:piglin_head",
      "count": 1,
      "from": ["minecraft:creeper_spawn_egg"],
      "text": "emi_sources.mypack.charged_creeper_piglin",
      "chance": 1.0,
      "requires_mod": "botania"
    }
  ]
}
```

| Field | Type | Required | Meaning |
|---|---|---|---|
| `sources` | array | yes | the entries; anything else at the top level is ignored |
| `output` | string | yes | item id of the item this source gives (`minecraft:piglin_head`). Items only (no fluids, no tags) |
| `text` | string | yes | what happens, in a sentence. A translation key (used when the language file has the key, so packs can translate it) or literal text. Wrapped to three lines on the page, the whole text is the tooltip, 1000 characters at most |
| `id` | string | no | unique id of the entry, `namespace:path` or just `path` (then the namespace of the file). Default: `<file namespace>:<file path below emi_sources, without .json>/<index>`, e.g. `mypack:heads/0` for the first entry of `assets/mypack/emi_sources/heads.json`. Lower case letters, digits and `_ . - /` |
| `count` | integer | no | amount of the output, 1 to 64, default 1 |
| `from` | array of strings | no | stacks shown as the source: item ids or `#namespace:tag` item tags (a tag cycles through its items). Display only, up to six are drawn. An unknown item or an empty tag makes the entry invalid, so use `requires_mod` for items of optional mods |
| `chance` | number | no | 0 to 1, shown as a percentage (`0.025` is `2.5%`) on the page and in the slot tooltip. Default: not shown |
| `requires_mod` | string | no | mod id; the entry is skipped silently when that mod is not loaded |

Unknown keys are ignored. The `from` stacks are inputs of the recipe, so the page also shows up when you look at the uses
of the `from` stacks (R/U on a spawn egg shows what it is a source of).

## What EMI makes of an entry

Each entry becomes one EMI recipe with the id `emi_info:/other_sources/<namespace>/<path of the entry id>` in the category
`emi_info:other_sources`, with the output as its output. It is an ordinary recipe, so

- `R` on the output item shows the page in the recipe screen (tab "Other Sources" / "Weitere Quellen"),
- the Item Info screen lists it under **How to Get** (row "Other Sources" with the `from` stacks as examples) and the
  "No known source" marker disappears,
- `/emiexport sources` lists it and `/emiexport nosource` no longer lists the item,
- the plugin manager shows the plugin id `emi_info` for this feature.

## Errors

A broken file or entry never crashes anything. EMI logs one `WARN` per skipped entry (or per unreadable file) naming the
file id, the entry index and the reason, and carries on:

```
[WARN] (EMI Remastered) emi_sources: emi_devtour:emi_sources/test.json entry 1: output item emi_devtour:does_not_exist does not exist
```

Reasons: not valid JSON, no `sources` array, entry is not an object, missing or empty `output`/`text`, bad id, `count`
or `chance` out of range or not a number, `from` not an array of valid ids, unknown output or `from` item, empty or
unknown `from` tag, duplicate entry id (within a file, or across files: the file read first, in resource id order, wins).
The log line `emi_info: N other sources from M emi_sources files (K skipped)` summarises every reload.

## Limits

- The entry is a hint for players, EMI does not check that the claimed source really exists in your pack.
- Fluids and tags as output are not supported; multi-output sources need several entries.
- Entries are not tied to a world: they show whether or not the source is possible in the current world.
