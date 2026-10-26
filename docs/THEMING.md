# Theming: recolouring EMI panel text from a resource pack

A resource pack that recolours EMI's panel textures (recipe pages, the Item Info screen, the tag browser, the copy
notice) to a dark colour makes EMI's dark grey text unreadable. EMI Remastered lets a pack recolour that text with one
small file, with no code and no config.

## The file

`assets/emi_remastered/text_colors.json`:

```json
{
  "replace": {
    "404040": "FFFFFF",
    "#6A6A6A": "9FA5A6"
  }
}
```

* `replace` maps a source colour to a replacement colour. Keys and values are exactly 6 hex digits (RGB), with an
  optional leading `#`, upper or lower case. The alpha part of the colour EMI draws with is kept as it is.
* Unknown top-level keys (for example `"_comment"`) are ignored.
* The mod itself ships this file with an empty `replace`, so without a pack that overrides it nothing looks different.

### Merging

The file of every pack is read and merged. Packs are applied from the lowest to the highest priority, so when two packs
map the same source colour the higher pack wins; colours that only one pack maps are all kept. A pack therefore only
needs to list the colours it wants to change.

### Errors

A file that is not valid JSON, or whose `replace` is not an object, is skipped as a whole and one warning naming the
pack is logged. An entry whose key or value is not 6 hex digits is skipped and one warning per file lists the bad keys;
the other entries of that file still apply. The table is rebuilt on every resource reload (F3+T).

## What is remapped

* All text EMI Remastered and EMI draw on a panel: recipe page text, Item Info, the tag browser panel, the copy notice.
* Text that other mods' EMI plugins draw through EMI's `TextWidget` (`WidgetHolder.addText`) is remapped too.
* Colours baked into a text (the `Formatting` colours such as dark gray or dark blue in a translated or styled `Text`)
  are remapped as well, so a label that is `DARK_GRAY` in code follows an entry for `555555`.

Not remapped: tooltips (they sit on the dark tooltip background), item count overlays, the dark list rows inside the tag
browser, the config and plugin manager screens, the bill of materials screen and the Distraction Free overlay.

## Colours EMI Remastered uses on panels

| Colour | Where |
|---|---|
| `404040` | body text: Item Info rows, recipe page text, tag browser title, copy notice title, ores captions |
| `202020` | emphasised values and headings (world, mobs and effects pages) |
| `000000`, `000008` | enchanting, ores, professions and trade text |
| `6A6A6A`, `6C6C6C`, `707070`, `555555` | secondary text: sub lines, counts, tag browser sub text, copy notice detail, "map placeholder" |
| `8B8B8B` | disabled tab label |
| `6E6E6E` | disabled link (mob and world pages) |
| `1E4BB0`, `2F6BFF` | links in Item Info (normal, hovered) |
| `1F3FBF`, `0A1678` | links on mob and world pages (normal, hovered) |
| `8B0000` | tag browser status and clear button |

Vanilla `Formatting` colours that appear baked into texts on panels:

| Formatting | Colour |
|---|---|
| `DARK_GRAY` | `555555` |
| `DARK_BLUE` | `0000AA` |
| `DARK_RED` | `AA0000` |
| `DARK_GREEN` | `00AA00` |
| `DARK_PURPLE` | `AA00AA` |
| `BLUE` | `5555FF` |
| `GRAY` | `AAAAAA` |

## Example: a dark blue pack (panel colour `326089`)

```json
{
  "_comment": "Light text for the dark blue panels of this pack.",
  "replace": {
    "404040": "FFFFFF",
    "202020": "FFFFFF",
    "000000": "FFFFFF",
    "000008": "FFFFFF",
    "6A6A6A": "C5D3E0",
    "6C6C6C": "C5D3E0",
    "707070": "C5D3E0",
    "555555": "C5D3E0",
    "8B8B8B": "93A8BC",
    "6E6E6E": "93A8BC",
    "1E4BB0": "9CC4FF",
    "2F6BFF": "FFFFFF",
    "1F3FBF": "9CC4FF",
    "0A1678": "FFFFFF",
    "8B0000": "FF9090",
    "0000AA": "9CC4FF",
    "AA0000": "FF9090"
  }
}
```

Put it at `assets/emi_remastered/text_colors.json` in the pack. Changing the colours only needs a resource reload.
