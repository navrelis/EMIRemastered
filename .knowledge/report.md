# Report: EMI Remastered 2.0.0+1.21.1

Final state 2026-10-26 (session 9), branch `mc/1.21.1` (navrelis/EMIRemastered, default branch; renamed from `remastered` in session 10), pushed. One jar
`EMIRemastered-1.21.1-2.0.0-Fabric.jar` (sha256 bf778e43..., code identical to the pack-checked a388815d; only icon and summary differ) replaces the 8 old jars in the Nytheria pack (old jars in
`<pack>/mods_backup/`) and is installed there; it is the exact jar checked in the real pack on 2026-10-26 (PACK-REAL3).
Build incl. 483 JUnit tests green. Sessions 8-9 (owner extras) are summarised in section 8; sections 1-7 describe 2.0.0
as of session 7, with numbers updated where newer measurements exist.

## 1. Requirements and what was implemented
| Requirement | Result |
|---|---|
| Fork EMI 1.1.24 + 7 add-ons into ONE Fabric 1.21.1 mod | Done. Mod id `emi` (D1), version `2.0.0+1.21.1`; add-ons are modules under `dev.emi.remastered.<module>` with their old resource namespaces and plugin ids (D3); `provides` the 7 old ids so a leftover add-on jar fails loudly. fzzy_config, TXNI Lib, Architectury, ForgeConfigAPIPort no longer needed (D4). |
| Compatible with every mod of the pack | Done and checked in the real pack twice (PACK-REAL, PACK-REAL2): 640 mods, world reached, clean exit; mixins of sporran, ali, hollowhorizon, clientsort, polymorph into EMI apply; 38 EMI plugins, 0 plugin errors; production dedicated server with 560 pack mods clean (PACK-SRV). All `dev.emi.emi` names/signatures kept. |
| Bugs fixed | EMI core: reload after world gone (KB1), unreadable saved entries kept, unknown stack types one WARN (FX-SER), category-less recipe, repair recipe error, fill-max overflow (KB9), tag search, favourites remainders (KB7) and more. Let's Do compat crash on every reload (Vinery 1.5.5 / Meadow renames) fixed. Module fixes listed in `CHANGELOG.md` "Fixes". |
| Faster | Warm reload -40..58 % on a 92k-recipe bench, plugin phase -49..66 %, join -21 %, ~60 MB less heap (Q3). Real pack (2026-10-26): EMI join reload 18.1 s (30.0 s with the old jars, 23.0 s on 2026-10-19), emi_enchanting 134 ms (was 2261), emi_loot 151 ms (was 5631 with the old add-on), emiffect 133 ms (was 1077); login -> joined 2.3 s (was ~24 s before LOOT-ASYNC); chest structure lookup 1.6 s (0.6 s from the cache) instead of 10-45 s. |
| Many info features (A1-A12, tier B, more) | Item Info screen (I) with How to Get / Used For / Details + provider API; enchantment max level, incompatibilities, sources; effect sources; villager trades per profession and level; food, fuel, compost, tool/weapon/armour stats; mob pages; loot structure/dimension filters, condition icons, Lootr note; ores in modded dimensions, Y-range highlight; distraction-free extras; one config screen with module switches and plugin manager; biome and structure pages; tag browser (T); copy recipe as text/JSON (Ctrl+C); advancement links; `/emiexport`. Details: `CHANGELOG.md`, `docs/MODULES.md`, `docs/INFO_PROVIDERS.md`. |
| Owner session-6 additions: verify with the pack, description + summary, pixel-art logo | Pack verified (above). `docs/DESCRIPTION.md` (summary EN 187 chars, DE 181, tagline, CurseForge/Modrinth text), `fabric.mod.json` description. Logo: `scripts/logo/make_logo.py` (deterministic), `docs/logo/icon.png`, `icon-native.png` (40x40), `banner.png`, mod icon `src/main/resources/icon.png`. |
| Translations | English + German for all new strings. |
| No Graphify | Respected (no graph built or updated). |
| No CurseForge upload without OK | Nothing uploaded. |

## 2. Changed files (main..remastered: 1316 files, +60,956/-3,508)
- `src/main/java/dev/emi/emi/**`: EMI core 1.1.24, fixes marked `// EMI Remastered:`; no renames.
- `src/main/java/dev/emi/remastered/{module,config,plugin,api,runtime}`: module framework, unified config, plugin manager, reload guard.
- `src/main/java/dev/emi/remastered/{distractionfree,effects,professions,enchanting,ores,letsdo,loot}`: the 7 ported add-ons.
- `src/main/java/dev/emi/remastered/{info,stats,mobs,world,tools}`: new modules (Item Info, stats, mobs, biome/structure pages, tag browser/copy/export).
- `src/main/resources/assets/**`: lang (en_us/de_de per namespace), textures incl. ores biome icons (CC BY-NC-SA 3.0), icon.
- `src/devtour/**`: dev-only screenshot tour harness, muted/no-grab/hidden-window mixins, 30 tours (not in the release jar).
- `src/test/**`: JUnit 5 tests (381 cases).
- `docs/**`, `README.md`, `CREDITS.md`, `CHANGELOG.md`, licence files; `scripts/logo/`.
- Session 7: `mobs/data/SpawnOverrides.java`, `MobStats`, `MobInfoRecipe`, `stats/client/FoodRecipe`, `info/core/TagNames`, `info/providers/RecipesInfoProvider`, emi_mobs lang, tour `polish.json`, `CHANGELOG.md`.

## 3. Key decisions
D1-D19 in `.knowledge/decisions.md`. Most important: keep id `emi` and every `dev.emi.emi` member (D1/D2); modules keep their namespaces (D3); one config (D4); worktree per agent (D7/D11); never sound, never take the owner's mouse/focus, hidden dev windows (D12/D13/FOCUS1); structure lookup results delivered asynchronously (D18); PACK-REAL2 in parallel with POLISH, final jar copied without a third launch (D19).

## 4. Checks run
- `gradlew build` (compile, 381 tests, ModulesTest) green on the merged head 561b05ae.
- Dev tours (hidden, muted): T2 regression 22/23 (all-steps fails only its intentional bedrock step); session 6/7 tours loot-van 22/22, loot-async 51/51, trade-row 45/45, p8 132/132, polish 32/32, mobs 43/43, stats 100/100. Lead viewed key screenshots (wither skeleton damage 4, golden apple tags disambiguated, earlier world/tools/p8/stats/logo).
- Dedicated dev server smoke clean; production Knot dedicated server with 560 pack mods clean (PACK-SRV).
- Real pack (owner-authorised, copied world, offline name, muted, options/emi.json backed up and restored, game lock): PACK-REAL (33d82605 build) and PACK-REAL2 (c6861cb4 build): logged in -> joined 4 s, structures of 563 chest tables delivered late and pages updated in place, no second reload, no new emi warnings, cleanup verified by the lead.
- Leftover check: no println/printStackTrace in remastered code; TODOs only in ported upstream EMI Loot condition parsers (kept as upstream).

## 5. Manual test checklist (for a human in the pack)
- [ ] Join speed with the final jar; chest "Found in" lines and structure page loot links appear a few seconds after EMI's reload.
- [ ] Item Info (I) on several items: trade rows ("Cleric - Master · 22×"), tag list (equal names with grey ids), loot, ores, effects.
- [ ] Mob pages: wither skeleton damage 4, bat/ghast without speed, bee/parrot "Flying"; portraits of modded mobs.
- [ ] Stats pages: food page right margin, armour of modded armour.
- [ ] New icon in Mod Menu; config screen module jump icons at GUI scales 1-3.
- [ ] Loot/ores/effects/enchanting/professions/Let's Do/distraction-free in a real world; tag browser (T) + Ctrl+C JSON; `/emiexport`.
- [ ] Favourites with remainders (KB7); ali interrupt (KB38); hide click debounce (KB14); German number formatting.
- [ ] Multiplayer on a dedicated server with real clients (note: two other pack mods crash dedicated servers, see 7).

## 6. Model usage
Work units (short 1, long 2): Opus 35 / Sonnet 121 -> about 22 % Opus. Precision overrides (above the "could go to Sonnet" line): PACK-CLI and LOGO (D17), PACK-REAL, LOOT-ASYNC (D18), PACK-REAL2 (D19, owner's real instance and data). PACK-CLI was dropped (superseded by the real-pack runs; its worktree is left unmerged).

## 7. Known limitations and recommendations
- KB13: Sodium's optimised vertex path is not used for batched unlit items (needs pack measurement).
- KB21: drops that mods give in code (not loot tables) and ore features of custom feature types (e.g. Mekanism) are not shown; modded code-placed structure chests have no structure.
- KB28/D14: enchanted books above the listed range (> max+5 or > 10) do not open the enchantment page.
- Flying mobs show the raw flying-speed attribute (no verified blocks/s conversion); mobs that never set a walking speed show none.
- GUI scale 3 at 1280x720: only the "Modules" jump fits next to EMI's jumps; tooltip overlap at scale 3 accepted (R7). EMI config header glyphs (R6) and T1-29 "item(s)" are EMI core strings, untouched.
- Visible-window mode of dev clients (`-PdevtourVisible`) was never launched (owner focus rule).
- T2 screenshot review covered only part of the tours (image reads failed for 7); the lead spot-checked world/tools/p8.
- Other pack mods: `certain_questing_additions` (InvalidInjectionException into FTB Quests client code) and `miningspeedtooltips` (client API in the main entrypoint) crash any dedicated server. Not ours; tell their authors or keep them client-only.
- Old worlds list the data packs `emi_loot` and `emi_letsdo_compat`: one-time "Missing data pack" note per world.
- Release on CurseForge/Modrinth waits for the owner's OK.

## 8. Sessions 8-9: owner extras (2026-10-20 / 2026-10-26)
Owner: structure cache "do that", extra sources, search by source, "one keybind and a menu with tabs", optimisations, "finish so it fully works on Fabric 1.21.1".

| Item | Result |
|---|---|
| Search by source (X-SEARCH) | `~stronghold`, `~cleric`, `~nosource`, `-~crafting`; labels from producing recipes + `SourceLabels` API; index built in the background (1.7 s in the pack, ready 0.9 s after the reload). `docs/SEARCH.md`. |
| Extra sources (X-SOURCES) | "Other Sources" category from `assets/*/emi_sources/**.json` (resource packs, no networking) + 4 vanilla built-ins; `docs/SOURCES.md`. |
| One-key item view (X-HUB) | Tab bar Info / Recipes N / Uses N on EMI's recipe screen and Item Info; R/U fall back to Info; settings `info.tab-bar`, `info.primary-view`, `info.tooltip-hint`. Mixins only; the 5 third-party EMI mixins still apply in the pack. |
| Mob/biome stacks, world info (X-UX) | `emi_mobs:mob` stacks for egg-less mobs (ender dragon, wither, giant, illusioner, armor stand), biome stacks rule, world Item Info provider, trial tab only when split. |
| Optimisations (X-PERF, X-PERF2, X-PARALLEL) | Merge Enchantments shared columns + lazy index, mob loot prestart, identical faster suffix-array build (4 in parallel), async source index, EmiHidden fast path, parallel structure lookup with a lean NBT reader (pack server 10.2 s -> 1.7 s, identical results as sets). Skipped as unprovable: remainder cache, professions cache, item groups off the render thread (D25). |
| Structure cache (X-CACHE) | `emi_remastered/cache/structure-lookup.dat`, jigsaw walks only, fingerprint per D26, setting `loot.structure-cache`; pack client miss 1.6 s + 5 ms save (54 KB), hit 0.6 s; pack server hit ~0.2 s. |
| Polish (X-PAD, X-SUMMARY) | Item Info right padding; summary line "Chest loot pages with a known structure: 563 of 1196 (chest loot tables by path: 433 of 789)". |
| Docs (DOC5, DOC6a/b) | CHANGELOG/README keys table and performance numbers measured in the real pack; egg-less mob examples corrected. |

Checks (session 9): `gradlew build` with 483 tests green on the head; dev servers for X-PARALLEL/X-CACHE clean; production pack server (560 mods): 993 structures, 433 of 789, cache miss/hit with identical counts; real pack PACK-REAL3 (run4, 2 launches, copied world, offline, muted): miss + hit identical (998 structures with the owner's new mods, 563 of 1196), 39 EMI plugins with 0 errors, all own mixins (incl. hub, suffix array, resource pack accessors) and the third-party EMI mixins (sporran, ali, certain_questing_additions, clientsort, polymorph) applied, 0 EMI WARN/ERROR apart from the known informational taint report and the one-time "Missing data pack emi_loot / emi_letsdo_compat" of old worlds; instance restored (4 hashes equal, git status identical, lock removed).

Manual checklist additions:
- [ ] Item view: R/left click -> Recipes tab, U -> Uses, I -> Info; switching tabs; Backspace; bedrock -> Info; with ali: scroll/drag on the recipe screen; certain_questing_additions "show recipe" -> back to the quest screen (known: the return link is lost when going through the Info tab).
- [ ] Search `~stronghold`, `~cleric`, `~nosource`, `-~crafting`.
- [ ] Other Sources pages (turtle scute, dragon egg).
- [ ] Ender dragon / wither stacks in the index; a favourite survives a restart.
- [ ] Second start of the same world: chest "Found in" lines right away, log "Loaded ... from the cache".

Model usage (lead tally, work units short 1 / long 2): Opus 49 / Sonnet 131 overall -> about 27 % Opus. Session 9 alone: Opus 6 (X-PARALLEL finish, X-CACHE, PACK-REAL3), Sonnet 2 (DOC6a, DOC6b). Precision overrides: X-PERF, X-PERF2 (identical-output perf in core, D25), X-HUB (UI hooks shared with pack mods, D23), X-PARALLEL (concurrency, D20), X-CACHE (silent stale-data risk, D26), PACK-REAL3 (owner's real instance).

Known limitations added in sessions 8-9:
- The structure lookup still runs in the background after server start; in the pack it finished before the join, but a very early join gets the structures by the late update (unchanged, correct).
- A cache hit on the real client took 0.6 s, mostly reading the identity of ~640 mod jars (0.1-0.4 s, I/O variance).
- `~nosource` differs from the Item Info marker for third-party OBTAIN providers (docs/SEARCH.md).
- During launch A of PACK-REAL3 the real game window came to the foreground 12 times (loading and idle); a watcher minimised it at once without input. Real-pack launches cannot use the dev hidden-window mode.
- Optional: empty data packs under the old ids `emi_loot`/`emi_letsdo_compat` would silence the one-time "Missing data pack" note of old worlds (not done; harmless).
- Remaining perf hotspots (not provably safe): `EmiRecipes$Manager` byInput, `EmiShapedRecipe.setRemainders`, item groups on the render thread.
- Graphify not used (owner: "No Graphify").

## 9. Session 10: clean release (2026-10-26)
- Jar for CurseForge (1.21.1, Fabric): `build/libs/EMIRemastered-1.21.1-2.0.0-Fabric.jar`, sha256 bf778e43..., unchanged (rebuilt, identical; build incl. 483 tests green). Docs are not part of the jar.
- Changelog: `docs/CURSEFORGE_CHANGELOG.md` (paste below the first comment line, Markdown editor); full list stays in `CHANGELOG.md` (release date now 2026-10-26). Facts checked against CHANGELOG.md by the lead.
- `docs/DESCRIPTION.md`: links point to `mc/1.21.1`; the "asked for permission" sentence is removed; the credits line and licences stay (D28, licence and CurseForge policy).
- GitHub: `remastered` renamed to `mc/1.21.1` (GitHub redirects old links) and made the default branch. Old `main` (upstream EMI 1.1.24, fully contained in mc/1.21.1) deleted after the owner said so; GitHub now has only `mc/1.21.1`. All 61 merged local work branches deleted; their worktree folders under `.claude/worktrees/` (gitignored) remain because folder deletion was blocked too.
- No CurseForge upload done.

## 9. Session 11 (2026-10-26): readable text on the Nytheria blue, release 2.1.0
Owner: "text isn't grey on the blue but rather white"; update in the modpack and upload mod + pack.

**Answer to the owner's question:** EMI Remastered ships the original grey EMI 1.1.24 textures (byte-identical); the blue look comes only from the resource pack "Mandala's GUI x Nytheria". Text colours are drawn in code, so a pack alone could not fix them.

**Implemented**
| Requirement | Result |
|---|---|
| Pack-overridable text colours (owner's choice) | `dev.emi.remastered.theme.TextColors` + `TextColorTable`: `assets/emi_remastered/text_colors.json` (`{"replace": {"404040": "FFFFFF"}}`), merged over all packs (higher pack wins per key), reloaded on F3+T. Applied at EMI's text funnels (`TextWidget.render`, `EmiDrawContext` text draws) and 13 raw draw sites; colours baked into Text styles (Formatting.BLACK/DARK_GRAY/...) are remapped too. Tooltips, item counts, config and BoM screens untouched. The mod ships an empty table: no visual change without a pack. Other mods' EMI plugins that draw through `addText` profit too. Docs: `docs/THEMING.md`. |
| White text on Nytheria blue | Pack 2.2.0 `assets/emi_remastered/text_colors.json`: dark greys/black -> #FFFFFF, mid greys -> #9FA5A6 (owner's choice; 2.6:1, dim but readable), disabled -> #899094, links/dark accent hues -> light versions (>= 4.4:1). |
| Pack update for EMI Remastered | EMI textures are unchanged, so the existing overrides fit. Fixed two inherited gaps: empty-sidebar icon (was transparent) and rounded panel corners (93 px). `patch_emi_overrides.py` (idempotent, tested); emi_loot restyle recipe reads the EMI Remastered jar. |
| Release | Mod 2.1.0+1.21.1 (jar sha256 9b61338b...) and pack 2.2.0 (zip c2537870...) installed in NytheriaDevelopment (old files in `mods_backup/`, `resourcepacks_backup/`, `options.txt.bak-2026-10-26-emi210`; options.txt differs by 1 byte, CRLF kept). Pack uploaded to CurseForge 1540564, file id 9338288. Mod upload: waiting for the owner's CurseForge project id (never uploaded with the uploader before). Pushed: EMI `mc/1.21.1`, Nytheria `v2.2.0`. |

**Changed files (mod):** `theme/TextColors.java`, `theme/TextColorTable.java`, `assets/emi_remastered/text_colors.json` (empty), `TextWidget`, `EmiDrawContext`, `EmiInfoRecipe` (marked `// EMI Remastered:`), `EmiRemasteredClient`, info (Elements, EntryImpl, InfoScreen, HubTabWidget), mobs/world LinkWidget, world PageParts, ores AbstractPlacedFeatureEmiRecipe, professions ProfessionRecipe/TradeRecipe, stats StatsInfoProvider; tests `theme/*Test`; `docs/THEMING.md`, `docs/CURSEFORGE_CHANGELOG.md`, `docs/DESCRIPTION.md`, README, CHANGELOG, gradle.properties, build.gradle comment; tour `src/devtour/resources/tour/theme.json`.
**Changed files (pack repo, commit a980b89):** `NytheriaGUI/assets/emi_remastered/text_colors.json`, EMI `widgets.png`/`background.png`, `scripts/patch_emi_overrides.py` + test, `scripts/restyle_light_gui.py`, VERSION, pack.mcmeta, CHANGELOG, CURSEFORGE_PAGE, README, dist zip.

**Decisions:** D29 (remap table instead of per-site roles), D30 (pack mapping), D31 (pack texture fixes).

**Checks:** `gradlew build` 498 tests 0 failures; pack 69 Python tests OK, zip deterministic (3301 files); audit folder == zip; dev tour `theme` 143/143 with and without the pack (hidden, muted), log "Text colour remap: 20 colours mapped", lead viewed Item Info, mob and biome pairs; pixel diff of the patched PNGs limited to the stated regions.

**Manual test checklist (owner, in the pack)**
- [ ] Item Info (I), recipe pages, mob/biome/structure pages, enchanting, effects, ores, loot, trades, tag browser (T): text white/light on blue.
- [ ] Other mods' EMI categories (Let's Do, Farmer's Delight style plugins, etc.): text that used #404040 is now white; check none sits on a light, non-recoloured background.
- [ ] EMI sidebar "empty" button icon visible; panel corners rounded.
- [ ] Without the pack (disable it): everything looks as in 2.0.0.

**Model usage (session 11):** Sonnet only: MAP1, MAP2, TC x2, PK1 x2, VER x2, DOC7 = 10 units; Opus 0. No precision overrides. Running tally in plan.md: Opus 51 / Sonnet 142 -> about 26 % Opus.

**Limitations / recommendations:** secondary #9FA5A6 is dim on #326089 (2.6:1); a lighter grey (e.g. #C8CDD0) would read better if the owner wants. Vanilla container titles (#404040) stay dark; that needs Polytone or a similar mod, not EMI. Other mods' EMI pages that draw text themselves (not via `addText`) keep their colours. No Graphify graph (owner's requirement).
