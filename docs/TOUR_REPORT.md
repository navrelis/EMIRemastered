# Tour report (T1)

Branch `wip/t1-tours`, 2026-10-19. Every merged feature was driven through the screenshot tour (`gradlew runClientTour -Ptour=<name>`, muted, one client at a time, no-grab dev mod active, window 1280x720 unless noted). Every screenshot named below was opened and looked at. Finding IDs are `T1-n`; severity is crash / wrong-data / visual / minor.

Scope of what was changed by T1: `config_jump.png` (mobs and stats icons), tour JSON files (`config`, `tools`, `loot-extras`, `enchanting-extras`, `stats`, `advancements`, new `professions`), this report. No module/core code, no `docs/DEVTOUR.md`, no `docs/KNOWN_BUGS.md`.

## Result overview

| Tour | Result | Steps | Notes |
|---|---|---|---|
| config | ok | 90/90 | extended: per-module jump buttons at 1080p scale 2 and 1440p scale 3, scale 1; icons 9 and 11 seen |
| plugin-manager | ok | 48/48 | |
| mobs | ok | 27/27 | |
| tools | ok (after tour fix) | 57/57 | first run: step 28 failed because the tour assumed the first tag member has no recipes (see T1-28); fixed in JSON |
| loot | ok | 26/26 | |
| loot-extras | ok | 70/70 | added hover tooltips of the condition icons |
| ores | ok | 24/24 | |
| ores-extras | ok | 37/37 | |
| enchanting | ok | 14/14 | |
| enchanting-extras | ok | 33/33 | added Where to Find / Exclusions pages and a bow page |
| effects | ok | 37/37 | |
| stats | ok | 58/58 | added scroll-to-end shots |
| advancements | ok | 52/52 | rewritten: before and after `/advancement grant @a everything` (revoked again at the end) |
| export | ok | 12/12 | 5 files written, see below |
| distraction-free | ok | 33/33 | |
| professions (new) | ok | 24/24 | covers villager trades, wandering trader uses of emerald, loot placeholder slots |
| default | ok | 19/19 | |
| focus-check | ok (steps) | 8/8 | but reports `focused=true`, see T1-1 |
| all-steps | not run | | by design its last step fails (shows failure reporting); every step type it contains is exercised by the other tours |

Log scan of every `run/logs/latest.log` (copies in `run/latest-<tour>.log`, ignored by git): no ERROR/exception from `emi`, `dev.emi.remastered`, no mixin application failure, no reload warning (`emi.reloadWarningCount` = 0 in every summary). Recurring lines, all benign:

- `[EMI] The following EMI classes have mixins applied to them ... is tainted by` (3 lines: `EmiApi` by info `EmiApiMixin`, `EmiScreenManager` by info + tools `EmiScreenManagerMixin`). This is EMI's own notice, WARN level, appears in `summary.json` `logWarnings` of every tour. Decision for the lead: either accept (documented) or whitelist, see T1-2.
- `(FabricLoader/Mixin) Error loading class: mezz/jei/library/load/PluginCaller` at start-up (JEMI mixin target without JEI; EMI core, harmless).
- vanilla: `Missing sound for event: minecraft:item.goat_horn.play` and `entity.goat.screaming.horn_break`, shader sampler warning `rendertype_entity_translucent_emissive` (dev environment, not ours).
- Reload times: plugins init 0 to 700 ms, EMI reload 2.1 to 3.5 s.

## Icons (part 1)

`ModuleJumpButton` reads the texture at `u = icon * 16, v = 0`, icon = registry position + 1 (`RemasteredConfigScreens.addJumpButtons`), so slot 9 = mobs and slot 11 = stats as stated. The interrupted earlier drawing was a finished creeper face for slot 9 and an unfinished bar chart for slot 11; both were redrawn from the committed PNG (Pillow is not installed, a 60 line pure Python PNG reader/writer in the scratch directory was used). Style: white (255,255,255,255) 1 px outline in the 12x12 box (2..13), no fill, as all other slots.

- slot 9 mobs: creeper face (box outline, two 2x2 eyes, mouth and legs).
- slot 11 stats: bar chart (L shaped axes at column 2 / row 13, three solid 2 px bars of rising height).
- Verified: only slots 9 and 11 differ from `HEAD` (checked by pixel comparison, 256x256 file size unchanged, every other pixel identical).
- Seen in `config/05-scale-2-1080p-all-module-jumps.png` (GUI scale 2), `config/28-scale-1-all-module-jumps.png` (scale 1, 1280x720) and `config/27-scale-3-1440p-all-module-jumps.png` (scale 3, 2560x1440), plus zoomed crops. The creeper and the bars sit under the info "i" and next to the tools "#" icon, same line weight and size as the neighbours.
- At 1280x720 and GUI scale 2 or 3 only EMI's own jumps plus `modules` are shown (`trimJumpTargets` drops the per-module jumps when `(jumps + moduleJumps) * 16 > space`); that is the intended fallback. The config tour therefore resizes the window to 1920x1080 (scale 2) and 2560x1440 (scale 3) for the icon shots. See T1-3 for a design question.
- `letsdo` has no jump button (no settings group), world slot 12 left empty as requested.

## Findings

| ID | Module | Severity | What was seen | Evidence | Likely location |
|---|---|---|---|---|---|
| T1-1 | devtour | minor (dev infra, owner rule) | `focus-check` prints `focused=true visible=true cursorLocked=false windowFocused(mc)=true` for every `window_info`, DEVTOUR.md says it must be `focused=false`. The cursor is not locked, so the grab protection holds, but the OS focus attribute of the window is true after start. Did not observe the owner's focus being taken, but the document's claim is not met. | `tour-out/focus-check/summary.json` | `devtour/mixin/WindowMixin` (GLFW_FOCUSED hint is only an initial hint; Windows may still focus the window) |
| T1-2 | core/info/tools | minor | 3 WARN lines of EMI's "tainted" notice in every run, counted in `logWarningCount`. | `config/summary.json` logWarnings | `EmiApiMixin` (info), `EmiScreenManagerMixin` (info, tools); inherent to mixing into EMI, maybe document |
| T1-3 | config | minor (design) | Per-module jump buttons disappear at 1280x720 and GUI scale 2 (only the "Modules" jump remains) although 11 icons exist; the new icons are therefore not visible in the most common window. | `config/03-group-modules.png` vs `config/05-scale-2-1080p-all-module-jumps.png` | `RemasteredConfigScreens.trimJumpTargets`: could keep some, or make the column scroll |
| T1-4 | plugins | minor | Plugin Manager at 854x480 shows 6 of 9 rows (works, scrolls) and the header line "Plugin Manager: 9 plugins, 0 off, last reload ..." is partly covered by the tutorial toast only because the dev world shows the "Move with WASD" toast; not a bug. Info, tools and distraction-free have no plugin entry (they register no `EmiPlugin`), 9 plugins listed. | `plugin-manager/14-small-window.png` | none |
| T1-5 | tools | wrong-data | JSON copy of a vanilla crafting recipe falls back to generic JSON: `[Tools] Vanilla recipe codec failed for minecraft:iron_pickaxe: Cannot encode unpacked recipe` (twice per copy). Smelting recipes encode fine (real data-pack JSON). Result for crafting is `{id,category,inputs[...,null,...],outputs}` instead of the data-pack format. | log line 311/312 of `run/latest-tools.log`, `run/tour-out/tools/minecraft_iron_pickaxe-other.txt` | `RecipeClipboard.vanillaJson` (the client's `ShapedRecipe` is the unpacked client copy; `Recipe.CODEC.encodeStart` rejects it) |
| T1-6 | tools | minor | The copy toast could not be seen in any screenshot (3 and 10 ticks after the copy, see `tools/13-copy-crafting-toast.png`), although the clipboard and files are correct. The only toast visible in the dev world is the vanilla tutorial toast on non-container screens, so either the toast is not drawn on the EMI recipe screen or it is out of the frame. Unverified. | `tools/13-copy-crafting-toast.png`, scratch tour `toast.json` | `RecipeClipboard.copy` (`SystemToast.add`) |
| T1-7 | tools | minor | Copy gave a vanilla `GL ERROR ... Win32: Failed to open clipboard: Zugriff verweigert` (ERROR line in latest.log) in one of three runs: the OS clipboard was locked by another process on the dev PC. Environment issue, not code. | `run/latest-tools.log` (first run) | none |
| T1-8 | tools | minor | The registry filter button reads `All (1.005)` (German thousands separator in the English UI, JVM default locale). | `tools/01-tag-browser-list.png` | `TagBrowserScreen` number formatting (`String.format`/`NumberFormat` without locale of the game language) |
| T1-9 | tools | visual | In the `@husk` search the left list is filtered to entity-type tags but the right pane keeps the stale selection "Logs #minecraft:logs" (and a hover tooltip of a block). | `tools/04-search-member-husk.png` | `TagBrowserScreen` selection reset on filter change |
| T1-10 | tools | visual | With the "Entity Types" registry label column, long tag names are cut with an ellipsis in the left list (`minecraft:can_bre…`, `ignores_…`, `inverted_…`, `sensitive…`, `wither_f…`), the same in the reverse lookup (`completes_find_…`, `lava_pool_sto…`). The names are the only identity of a tag, no tooltip on the row was checked. | `tools/04-search-member-husk.png`, `tools/07-reverse-lookup-oak-log.png` | `TagBrowserScreen` row layout: truncate the count/registry column first, not the id |
| T1-11 | tools | minor | Biome tags show a friendly title and id (`Is Forest #minecraft:is_forest`), entity tags and block tags only the id; members of the biome registry are raw ids (`minecraft:forest`), items have names. 0 member tags are listed (`c:cold/nether 0`, `c:dead 0`). | `tools/06-registry-biomes.png` | `TagBrowserScreen`, `TagEntry` display names |
| T1-12 | loot | visual | Item Info pages show an empty grey slot for the loot placeholder: "Block Drops" row of diamond ore, emerald ore, crafting table show one empty grey slot `x1`; "Used in recipes" of the crafting table also lists "Block Drops" with the diamond-ore icon. On the loot page itself the placeholder is fine. | `ores-extras/06-info-diamond-ore.png`, `advancements/08-after-crafting-table-info-end.png` | `BlockLootRecipe.inputStacks` (`BlockLootEmiStack lootStack` renders as an empty stack in the Item Info row), `info/` recipe row builder |
| T1-13 | loot | minor | Block drop tooltip of the output slot: `Cost per batch:` followed by the ore icon, `1` and a second lone `1` without icon. | `loot-extras/06-diamond-drop-tooltip.png` | EMI core cost line (recipe has two inputs: ore stack plus loot placeholder, `BlockLootRecipe.getInputs`) |
| T1-14 | loot | minor | Condition tooltip grammar: "Needs or prefers a iron+ pickaxe" (a/an). Fishing tooltip chances read `<0.1 rolls`. | `loot-extras/04-diamond-condition-tooltip-1.png`, `13-ink-sac-fishing-tooltip.png` | `ConditionIcons.tooltipLines`, lang `emi_loot.*` |
| T1-15 | ores | visual | Geode page: the first input slot above budding amethyst is an empty grey slot (filling/air). | `ores/04-geode-amethyst.png` | `GeodeGenEmiRecipe` slot at (0,18) when `filling` is `EmiStack.EMPTY` |
| T1-16 | ores | minor | Item Info "World Generation" of diamond ore repeats `Overworld / Best Y -64 / All Overworld biomes` three times plus a fourth `Y -64 to -4` entry, one per placed feature; "Ore Generation" row has a biome tile (mushroom fields) as third ingredient icon. | `ores-extras/06-info-diamond-ore.png` | `OresInfoProvider` (merge identical entries), `OreGenEmiRecipe.getInputs` (`allBiomes`) |
| T1-17 | ores | minor (judgement) | Barrier slot at (142,0): rendered top right of every ore card, clear of the height graph labels (`bot+80` ends well left of it), the red no-entry icon is readable; the chance is only in the tooltip (not hovered). Verdict: position is good, but the red circle without text is cryptic, and the graph label `232` of emerald is overlapped by the orange player-Y marker line. | `ores/01-oregen-diamond.png`, `ores-extras/02-oregen-emerald-long-biome-list.png` | `OreGenEmiRecipe.addWidgets`, `AbstractPlacedFeatureEmiRecipe.addDistributionGraph` |
| T1-18 | mobs | visual | Bat portrait: the 3D portrait box is a plain grey square with the bat tiny in the lower left corner (model scale from the 0.5x0.9 hitbox is too small). | `mobs/03-bat.png` | `MobInfoRecipe` portrait widget scale |
| T1-19 | mobs | minor | Mob page "Damage" is the default attribute only (wither skeleton: heart 1 = 2.0; the held stone sword is ignored), zombie page shows hearts (Health 10, Damage 1.5, Armor 1) while the zombie spawn egg Item Info shows points (Health 20, Armor 2, Damage 3); different units per place. First two items of the `spawn egg` search are the (dark) spawner blocks. | `mobs/02-wither-skeleton.png`, `mobs/05-zombie-egg-item-info.png`, `mobs/04-spawn-egg-list.png` | `MobInfoRecipe.readStats`, mobs info provider |
| T1-20 | stats | wrong-data (missing) | No Armour section on any armour: iron chestplate, netherite chestplate, netherite boots, turtle helmet show only tags, enchantments, drop conditions (checked at the end of the page and in the middle). `armour()` in `StatsInfoProvider` should produce armour, toughness, knockback bars. Likely the 1.21.1 armour attribute modifier component is empty or the slot filter does not match (default armour modifiers live in the armour material, `ArmorItem` adds them at runtime in 1.21.1), so `item.get(ATTRIBUTE_MODIFIERS)` has nothing for the armour attributes. | `stats/12..16-*chestplate*.png`, `stats-armour/01..04` (scratch tour) | `StatsInfoProvider.armour` |
| T1-21 | stats | minor (wrong-data) | Honey bottle food page: no "Leaves behind: glass bottle" (the beetroot soup page shows its bowl) and eating time 1.6 s (the honey bottle takes 2.0 s in vanilla: `HoneyBottleItem` overrides the use time, the component keeps the default). | `stats/03-food-honey-bottle-converts.png`, `05-food-bowl-converts.png` | `FoodFacts.of` (read `Item#getMaxUseTime` for eat time; add known converts for `HoneyBottleItem`) |
| T1-22 | stats | visual | Locale: numbers use the JVM locale in the English UI: `Saturation: 9,6`, `Attack speed: 1,6 per second`, `Durability: 1.561`, `Damage per second: 11,2`. In English text these must be `9.6`, `1.6`, `1,561`. | `stats/01-food-golden-apple.png`, `stats/08-info-diamond-sword-end.png` | `StatsText.number` (use `Locale` of the selected game language or `Locale.ROOT` for en_us) |
| T1-23 | stats | visual | Food page effect rows are cut: `Regeneration II (00:…`, `Fire Resistance (05:…` (enchanted golden apple, four effects). | `stats/04-food-enchanted-golden-apple-four-effects.png` | `FoodRecipe` effect label width |
| T1-24 | stats | note (asked) | Diamond pickaxe (and any tool with an attack damage modifier) also gets a Weapon section: `Attack damage 5, Attack speed 1,2 per second, Fully charged after 0,83 s, Damage per second 6`. Numbers are right (5 = 1 + 4 pickaxe, 1.2 speed), the section is correct but noisy for tools. The mace gets a meaningless Tool section (`Mining speed otherwise: 1`, `Durability used per block 2`, durability 500) and Weapon (6, 0,6, 1,67 s, 3,6). Shears show raw tag ids `#minecraft:leaves`, `#minecraft:wool` next to friendly `Cobweb`, `Vines, Glow Lichen`. | `stats/10-info-diamond-pickaxe-details.png`, `stats/11-info-diamond-pickaxe-end.png`, `stats-armour/05-mace-end.png`, `07-shears-end.png` | `StatsInfoProvider.tool/weapon`, `StatsMath.targetKey` |
| T1-25 | enchanting | minor | Item Info of the sword: Item Tags list is shown with `...and 1 more, show all` while the pickaxe/diamond page uses `Show tags` for the same expandable list (inconsistent wording); raw `#c:tools/melee_weapon` tag ids. | `enchanting-extras/11-sword-item-info-details.png`, `advancements/12-after-diamond-info-end.png` | info tags provider (`emi_info.tags.*`) |
| T1-26 | advancements | note | Before the grant the client knows no advancement for crafting table, iron ingot or diamond (no Advancements section, expected: the client only receives visible advancements). After `/advancement grant @a everything` the section appears: `[x] Diamonds!` with `Task` in green. A click on the entry (opens the advancement screen) is not covered, `screen_info` does not list the entry buttons. | `advancements/12-after-diamond-info-end.png` vs `06-before-diamond-info-end.png` | none |
| T1-27 | tour | note | The enchanted book Item Info page cannot be reached by the tour (`emi_stack_key` builds plain stacks); not tested. All pages of an enchantment (Overview, Where to Find, Exclusions) are reached via the in-recipe page buttons; the Description page is not created in vanilla (no `.desc` keys), expected. | `enchanting-extras/02-sharpness-where-to-find.png`, `03-sharpness-exclusions.png` | none |
| T1-28 | tour (fixed) | note | `tools.json` step `clickFirstMemberUses` failed in the first run: the first member of the tag (Oak Log reverse lookup) now has loot recipes, so the first click already leaves the browser. The JSON now presses Escape after the recipes click and shows the uses path too. | first run log line 297 | `src/devtour/resources/tour/tools.json` |
| T1-29 | info | note | Fuel page text "Smelts 8 item(s)" (EMI core string, not changed). | `default/06-uses-coal-fuel.png` | EMI core |

### Looked at and fine (no finding)

- config: all `group-*` jumps (loot, ores, effects, enchanting, professions, distraction-free, info, mobs, tools, stats) scroll to the right group with English names; module switch toggles, tooltip, restore after reload.
- plugin manager: list with load times, "Always On" core, pending-change tooltip, reload on close, Enable All, scale 3.
- mobs: zombie, wither skeleton (fire immune, "No natural biome spawns"), bat (no armour row, "No drop data"), spawn egg list (80 eggs), Mob Info section in the Item Info page with a link back to the mob page.
- loot / loot-extras: block, chest (Weaponsmith "Found in: Village (Overworld)", Bastion with "(Nether)"), mob (zombie villager with condition icons, mooshroom and cow smelted-with-OR), gameplay (fishing, hero gifts), archaeology; condition tooltips (silk touch, fortune, player kill, looting); Item Info "Chests in Structures" and "Drop Conditions".
- ores: height graph, per chunk, best Y, biome compact lists (`+5 more`), "All Overworld biomes", dimension line, ancient debris nether pages, biome search (`plains` 3, `$overworld` 53 tiles).
- enchanting: overview page (curse/treasure/trade/selectable/max level/valid items), Where to Find page (Sharpness: table, loot, librarian, villager equipment, mob equipment; Mending: loot, librarian, treasure), Exclusions page, wrap-around paging, bow page.
- effects: every sampled effect (regeneration, speed paging, poison 3 pages with wrap, strength, raid omen, wither, night vision, fire resistance, health boost with no sources, uses of golden apple).
- stats food pages for golden apple, cooked beef, honey bottle, enchanted golden apple, beetroot soup (bowl).
- distraction-free: hidden index with "Search to View Recipes"/"Show Recipes", search reveals, pin ("Hide Recipes"), recipe screen keeps sidebars.
- export: `Item info export finished in 1.4 s`, `recipes.json` 5233 rows, `items.json` 1792, `sources.json` 1792, `nosource.json` 98, `tags.json` 385; valid JSON, readable fields.
- professions: iron pickaxe trades, emerald uses (81 trade pages), bell page.

## Tour JSON changes

- `config.json`: module jump icons at larger windows (1080p scale 2, 1440p scale 3), `emi_config_jump` for every module group (the previous `click` steps for the module groups were skipped as "no match": the per-module jump buttons are trimmed at 1280x720).
- `tools.json`: tag-browser flow robust against first member with recipes, copy toast shot after a short wait.
- `loot-extras.json`: hover shots over condition icons and fishing slots.
- `enchanting-extras.json`: Where to Find, Exclusions, Mending, bow pages through the page buttons.
- `stats.json`: scroll-to-end shots for sword, pickaxe, chestplates, golden apple.
- `advancements.json`: rewritten, before and after granting all advancements (grant uses `@a`; the revoke at the end and `announceAdvancements` toggled off and on again keep the world unchanged).
- `professions.json`: new.

## Commands run

`gradlew runClientTour -Ptour=<name>` for config (x2), plugin-manager, mobs, tools (x3), loot, loot-extras (x2), ores, ores-extras, enchanting, enchanting-extras (x3), effects, stats (x2), advancements (x4), export, distraction-free, default, focus-check, professions; scratch tours `stats-armour` and `toast` from the scratch directory (not in the repo); `gradlew build` (green, 18 s incremental).
