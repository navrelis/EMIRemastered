# Plan

Usage tally (work units: short = 1, long = 2): Opus 51 (session 11: none yet; session 9: LOGO2 x2, X-PARALLEL finish x2, X-CACHE x2, PACK-REAL3 rerun x2; session 8 lead: X-PERF x2, X-HUB x2, X-PERF2 x2, PACK-REAL3 x2; LOOT-ASYNC x2, PACK-REAL x2, PACK-REAL2 x2, F1, F2, Q3 x6, A1 x2, FX1 x4, FOCUS1 x4 = two delegations, PACK-CLI x2, LOGO x2) / Sonnet 142 (session 11: MAP1, MAP2, TC x2, PK1 x2, VER x2, DOC7; session 10: CL1 x1; session 9: DOC6a x1, DOC6b x1, DESC2 x1; session 8 lead: X-SOURCES x2, X-SEARCH x2, X-UX x2, DOC5 x1, key map x1; session 7: POLISH x2, DOC4 x1) -> Opus 25 % (follow-up session's X-PAD/X-SUMMARY/X-PARALLEL/X-CACHE tallied there) (sessions 4+5 re-delegations counted again)

Status: open / in progress / in review / done. `||` = can run in parallel.

## Phase 0 Foundation (sequential)
| ID | Task | DoD | Dep | Model | Status |
|---|---|---|---|---|---|
| S0 | Repo, branches, GitHub, `.knowledge` | origin navrelis/EMIRemastered, `main` = 1.1.24 tag, `remastered` work branch | - | lead | done |
| F1 | Single Fabric Loom project (D1, D2) | `gradlew build` -> one jar; dev client reaches title + world with EMI working; neoforge/forge/xplat gone; metadata per D1; compile-only deps for modules (Let's Do jars matching pack, JEI, modmenu) | S0 | Opus long | done |
| F2 | Module framework + unified config + plugin manager (D3, D4, D8) | module registry, per-module config groups + enable switch in EMI config screen, plugin manager by mod id (after Sporran's hook), module stubs + mixin configs + entrypoints registered, JUnit set up, en_us/de_de | F1 | Opus long | done |

## Phase 1 Ports (parallel, worktrees) — each: module under `dev.emi.remastered.<m>`, Yarn, own config group, no dependency on the old mod
| ID | Task | Model | Status |
|---|---|---|---|
| P1 | Distraction Free Recipes into EMI core | Sonnet short | done (incl. A10) |
| P2 | EMIffect -> `effects` | Sonnet short | done (incl. A3) |
| P3 | EMI Professions -> `professions` | Sonnet short | done |
| P4 | EMI Enchanting -> `enchanting` | Sonnet short | done |
| P5 | EMI Ores -> `ores` (LGPL headers kept) | Sonnet long | done |
| P6 | Let's Do compat -> `letsdo` (Vinery/Meadow fixes, no Architectury, dedupe native) | Sonnet long | done |
| P7 | EMI Loot -> `loot` (fzzy_config -> EMI config, Fabric networking, AW merge) | Sonnet long | done |

## Phase 2 Quality
| ID | Task | Model | Status |
|---|---|---|---|
| Q1 | Dev-client screenshot tour + log check harness | Sonnet long | done |
| Q1b | Tour steps + D12 mute enforcement + D13 no-grab/no-focus dev mixin on all client runs (blocks every client check) | Sonnet short | done (merged; 4 muted no-grab launches clean; client ban lifted) |
| Q2 | Known bugs (EMI repair AIOOBE, upstream issues of each module) | Sonnet | done (docs/KNOWN_BUGS.md: 22 open rows; fixes in FX1-FX3, Q3) |
| Q3 | A12 reload performance, before/after numbers | Opus long | done (merged; warm reload -40..58 %, join -21 %, heap -60 MB; KB2, KB4, KB12) |

## Phase 3 Features (after the module each builds on is merged)
| ID | Task | Builds on | Model | Status |
|---|---|---|---|---|
| A1 | Item Info screen + provider API ("How to get": recipes by output grouped, loot, mob drops, ores, trades, brewing; "no known source" marker); key bind on hovered stack | F2 | Opus long | done (client module-off check pending) |
| A2 | Enchanting: real max level (cap mods), incompatibilities via canBeCombined, sources (table/loot/librarian), info provider | P4, A1 | Sonnet | done (merged; client tour in T1) |
| A3 | Effects sources (in P2) | P2 | Sonnet | done (P2) |
| A4 | Villager trades per profession/level (client dummy villager, wandering trader, Fabric TradeOfferHelper trades; Villager-API plans if reachable) | P3 | Sonnet long | done |
| A5+B1 | `stats` module: food page, tool/weapon/armour stats pages, fuel/compost in info screen (A6) | F2, A1 | Sonnet long | done (merged; client tour in T1) |
| A7 | `mobs` module: mob pages (health, armour, attack, speed, spawn biomes via server sync, drops link) | P7 | Sonnet long | done (merged eab051f; client tour pending after Q1b) |
| A8 | Loot: structure/dimension filters, condition icons, Lootr per-player note | P7 | Sonnet | done (merged; client tour in T1) |
| A9 | Ores: modded dimensions, compact biome list, Y-range highlight | P5 | Sonnet | done (merged; client tour in T1) |
| A10 | Distraction-free extras (in P1) | P1 | Sonnet | done (P1) |
| A11 | Unified config + plugin manager (in F2) | F2 | Opus | done |
| A12 | Reload performance (Q3) | - | Opus | done (Q3) |
| B2 | `world` module: biome pages (climate, tags, spawns, ores) + structure pages (server sync) | P5, A7 | Sonnet long | done (merged; 34 structures/64 biomes in dev; real multiplayer untested; jump icon slot 12 pending) |
| B3 | Tag browser screen (all tags, search, members) | F2 | Sonnet | done (merged; client tour pending after Q1b) |
| B4 | Copy recipe as text/JSON bind | F2 | Sonnet short | done (with B3) |
| B5 | Advancement links in item info (icon/display) | A1 | Sonnet short | done (merged; client tour in T1) |
| X1 | Pack-dev export command `/emiexport` (recipes, items, sources, nosource, tags; JSON/CSV) | A1 | Sonnet | done (merged; client run pending) |
| X2 | Let's Do port fixes + dedupe (P6) | - | Sonnet long | done (in P6) |

## Phase 3b Verification and fixes (session 3)
| ID | Task | Model | Status |
|---|---|---|---|
| T1 | Batched client tours of all merged features + jump icons mobs/stats -> docs/TOUR_REPORT.md | Sonnet long | done (merged; 17 tours ok, 29 findings T1-1..29) |
| FX1 | KB1, KB3, KB14, KB15, KB38: reload lifecycle (world gone, disconnect, daemon threads), keep unparsable emi.json entries, debounced hide save | Opus long | done (merged; ali interrupt and hide click not client-verified; crash shutdown may lose last 500 ms of changes) |
| FX2 | KB5, KB6, KB8, KB9, KB10, KB11 + hasFocusedTextField (EMI core, small) | Sonnet long | done (merged 714005d; manual: regex tag search #/pickax/ showed empty in tour, I with anvil field focused) |
| FX3 | KB17, KB18, KB22, KB23, KB28, KB29 (loot, ores, enchanting) | Sonnet | done (merged; limits: R/U on books only up to listed level (max+5, <=10), Grinding tab before Enchantments on U) |
| P8 | Info providers: villager trades (professions), effects | Sonnet long | done (merged; coveredCategories(stack) API; item-info toggle not client-checked) |
| DOC1 | KNOWN_BUGS.md rows from FX1-FX3/Q3 + DEVTOUR.md tour list (world, fx2, fx3, p8, WorldTour) | Sonnet short | done (open: KB7, KB13, KB28 partly, KB43, KB44) |
| FOCUS1 | T1-1: dev clients report focused=true; default hidden window (GLFW_VISIBLE false, cancel show/focus calls), opt-in -PdevtourVisible, foreground check | Opus long | done (merged; hidden window verified by foreground check in 3 runs; visible-mode launch not run, owner was gaming) |
| FX-STATS | T1-20 armour section missing, T1-21 honey bottle, T1-22 locale numbers, T1-23 effect rows cut, T1-24 weapon/tool sections | Sonnet long | done (merged; stats tour 100/100 ok, lead viewed armour + food screenshots) |
| FX-TOOLS | T1-5 crafting JSON copy, T1-6 toast, T1-8 locale, T1-9 stale selection, T1-10 truncation, T1-11 names, KB44 regex tag search | Sonnet long | done (merged; tools tour 89/89 ok; KB44 not reproducible: #/pickax/ = #pickax = 426) |
| FX-WD | T1-12..19 loot placeholder slot, cost line, grammar, geode slot, ores info merge, graph label, bat portrait, mob units; KB43 | Sonnet long | done (merged; tours mobs 43, ores-extras 44, loot-extras 85, ores 27 all ok) |
| CFG1 | world jump icon slot 12, T1-3 module jumps at small windows, T1-25 info tag wording | Sonnet short | done (merged; config tour 111/111 ok, one correction: Modules first) |
| DOC2 | README, CREDITS.md (authors, refs, licences), CHANGELOG 2.0.0, licence files in jar | Sonnet short | done (merged; jar has CREDITS.md, LICENSE, LGPL text; 0 devtour entries) |
| KB7 | Favourite remainder counts (port aMelonRind/emi 2508321, e7fd7d9) | Sonnet long | done (merged; kb7 tour before 2/2 -> after 3/3, 5 tests) |
| T2 | Regression: server smoke + all tours on merged remastered, findings list | Sonnet long | done (lead evaluated: 22/23 tours ok, all-steps fails only its intentional bedrock step; server smoke clean; tour fixes fx3/p8 merged; findings -> FX-SER, LOOT-VAN, screenshot review T2-SHOTS) |
| T2-SHOTS | Read-only screenshot review of all T2 tour shots, findings table | Sonnet short | done (partial: image reads failed for 7 tours; stats/mobs/ores/loot/config seen, T1-3,12..25 confirmed fixed; R1-R4 -> POLISH; R5 tour-only, R6 EMI core header, R7 tooltip at scale 3 accepted; lead spot-checked world/tools/p8 shots: fine) |
| FX-SER | Unknown stack type (disabled plugin/removed mod) logs 1 WARN per type instead of ERROR+NPE per entry (EmiIngredientSerializers) | Sonnet short | done (merged; plugin-manager tour: 1 WARN per unknown type, 0 ERROR, emi.json entries kept byte-identical; 3 tests) |
| LOOT-VAN | Code-placed vanilla chest tables mapped by structure type (desert pyramid, fortress, stronghold, ...) in StructureLookup | Sonnet short | done (merged; 53 of 56 chest tables now have a structure, loot-van tour 22/22, lead viewed nether wart Item Info) |
| DOC3 | KNOWN_BUGS (KB7, KB43, KB44) + TOUR_REPORT statuses after wave 6, CREDITS for KB7 port | Sonnet short | done (merged; T2 findings may need a follow-up) |
| PACK-CLI | Pack-mirror dev client (pack mods copied into run/mods, hidden, muted): mixins of other mods into EMI, EMI plugins, reload times, modded Item Info | Opus long | dropped (superseded by PACK-REAL; never merged; worktree removal blocked by permission, left unmerged) |
| PACK-SRV | Production dedicated server (release jar + copied pack mods, local files only): loading, provides, server-side modules | Sonnet long | done (production server, loader 0.19.5, 560 mods: started 21 s world/99 s total, clean stop, 0 emi warnings/errors, no provides conflicts; 993 structures, 433 of 789 chest tables with a structure, lookup 10.6 s off-thread; 2 other client-only pack mods crash any dedicated server: certain_questing_additions, miningspeedtooltips) |
| BRAND | docs/DESCRIPTION.md (summary EN/DE, tagline, CurseForge/Modrinth description), fabric.mod.json description | Sonnet short | done (merged; summary 187 chars, DE 181, tagline 55; one correction: product names untranslated, 'author') |
| LOGO | Pixel-art logo, banner, mod icon from a deterministic Pillow generator | Opus long | done (merged; slot + gem + magnifier + sparkle, 32 px grid x16, banner 192x48 x8, byte-identical regeneration; lead viewed icon dark/light + banner) |
| PACK-REAL | Owner-authorised launch of the real pack (copied world, offline, muted, options/emi.json backed up and restored), log check | Opus long | done (640 mods, world reached, clean exit; 5 third-party EMI mixins applied; 38 plugins, 0 errors, Let's Do + repair fixed; join reload 30.0 s cold (emi 3441 ms, emiffect 133 vs 1077 ms); emi.json byte-identical; F1 join blocked ~24 s by chest structure wait, F2 world loot links lost after 25 s -> LOOT-ASYNC) |
| LOOT-ASYNC | Structure lookup never blocks the server thread; late results pushed to players; no 25 s cut of world loot links; honest log lines | Opus long | done (merged; join and lookup decoupled (login->joined same second with 20 s delay), late payloads update chest pages, Item Info and structure pages without EMI reload, 9 new tests; needs re-check in real pack) |
| TRADE-ROW | Item Info trade rows: viewed-item amount in the title, not the count column next to the other side's stacks | Sonnet short | done (merged; 'Cleric - Master · 22×', stacks show own amounts; trade-row 45/45, p8 132/132) |
| POLISH | R3 wither skeleton damage (spawn-time base values), R4 bat/ghast speed 30.2, R2 stats right margin, R1 duplicate tag names | Sonnet long | done (merged 015f9119; wither skeleton 4 + phantom 6 spawn overrides, default walking speed omitted, Flying row, food page RIGHT_PAD 3, equal tag names get grey ns/id suffix; polish 32/32, mobs 43/43, stats 100/100; lead viewed wither skeleton + golden apple shots) |
| PACK-REAL2 | Real pack launch with LOOT-ASYNC build (c2b433ec installed 23:30): join not blocked, late structure delivery logged, same safeguards | Opus long | done (logged in->joined 4 s (was ~24 s); lookup 45.4 s off-thread, structures sent/received 8 s after reload, 563 pages updated in place, no 2nd reload; 640 mods, 38 plugins 0 errors, reload 22983 ms; no new emi warnings; all safeguards verified by lead: lock gone, test world + xaero removed, options sound restored, instance git traces restored) |
| DOC4 | CHANGELOG 2.0.0: FX-SER, LOOT-VAN, LOOT-ASYNC, TRADE-ROW, POLISH, icon; DESCRIPTION factual check | Sonnet short | done (merged 561b05ae) |
| -- | Later: KB13 (Sodium vertex path, needs pack measurement), T1-29 core string | - | open |

## Phase 4 Acceptance
Pack install under game lock, end-to-end check, report, push.
- Jar swap: done 2026-10-19 21:1x (owner's client closed; 8 jars in mods_backup/, EMIRemastered jar in mods/, lock created and removed). Owner changed the decision (2026-10-19 ~21:30, 'no you do all'): the lead's team launches the real pack (PACK-REAL). Newest jar (33d82605 build) installed before the launch.
- Session 7 (2026-10-19 23:30): jar of c6861cb4 (LOOT-ASYNC + TRADE-ROW) installed under game lock for PACK-REAL2; after POLISH merge the final jar is copied again (copy only).
- Final (2026-10-19 23:48): head 561b05ae built (381 tests green), jar 829b63e1 installed in the pack under game lock; report `.knowledge/report.md`. Status: DONE. Optional later: KB13, T1-29, R6.

## Phase 5 Owner extras (session 8, 2026-10-20)
Owner: "1. do that (structure cache) 2. sure (extra sources) 3. sure (search by source)", "one keybind + tabs, U reroutes, look good", "optimisations?".
| ID | Task | Model | Status |
|---|---|---|---|
| X-PAD / X-SUMMARY | Item Info right padding / lookup summary count (follow-up session) | Sonnet short | done (merged) |
| X-PARALLEL | Parallel structure template reads (follow-up session) | Opus long | done (merged bbd28d2f; pack server lookup 10.2 s -> 1.7 s, 433 of 789, 563 of 1196 pages, dumps equal as sets; build abandoned on server stop; 453 tests) |
| X-CACHE | Structure lookup disk cache, fingerprinted (D26) | Opus long | done (merged 04479dc3 after 1 correction: no save with failed walks; pack server hit 199-238 ms vs walk 1.4-1.55 s, same counts; 483 tests) |
| X-SEARCH | `~` source search + `~nosource`, SourceLabels API, index at bake (async since X-PERF2), late chest structures refresh | Sonnet long | done (merged a736e2cc; tours ok, ~1.5 % bake) |
| X-SOURCES | Other Sources category from `assets/*/emi_sources` resource files, 4 vanilla code-only built-ins, plugin `emi_info` (entrypoint, mixin removed after review) | Sonnet long | done (merged 8612fb91) |
| X-PERF | Merge Enchantments shared columns + lazy index; mob loot data prestart | Opus long | done (merged fa32dad4; dev emi_enchanting 348->52 ms join with ME, emi_loot 436->49; identical dumps) |
| X-HUB | Item view tab bar Info / Recipes / Uses on recipe + Item Info screens, primary view, R/U fallback to Info, hint on | Opus long | done (merged 92b825dc after 1 visual correction; pack mixin injection points listed, all intact) |
| X-UX | Mob stacks (emi_mobs:mob) for egg-less mobs, trial tab only when split, biome stacks rule, world Item Info provider | Sonnet long | done (merged a141b16b after 1 correction: namespaced serializer) |
| DOC5 | CHANGELOG/README keys table/MODULES/DEVTOUR/KB21 | Sonnet short | done (merged cdc2531f; placeholder for perf2 + cache) |
| X-PERF2 | Identical faster suffix-array build (parallel), source index async, EmiHidden fast path; skipped: remainders cache, professions cache, item groups (unsafe) | Opus long | done (merged 7dad04db; search bake -45..-75 %, 58 queries identical) |
| PACK-REAL3 | Real pack with jar a388815d (04479dc3): reload times, new mixins vs ali/CQA/hollowhorizon, cache miss then hit | Opus long | done (run4 2026-10-26: 2 launches miss/hit, reload 23.0 -> 18.1 s, enchanting 2261 -> 134 ms, loot 2036 -> 151 ms, lookup 1.6 s / cache hit 0.6 s before join, 563 of 1196, 39 plugins 0 errors, all own + 5 third-party EMI mixins applied, 0 EMI WARN/ERROR; instance restored hash-equal, git status identical, lock removed) |
| DOC6a | CHANGELOG/README perf2 + parallel entries, mobs eggless tooltip fix | Sonnet short | done (merged; 1 follow-up: same list in MobsSettings @Comment + javadoc) |
| DOC6b | CHANGELOG/README structure cache + real-pack numbers | Sonnet short | done (merged; estimate bullets replaced by measured numbers) |
- Session 9 (2026-10-26): Phase 5 DONE. Head built (483 tests), jar a388815d installed and checked in the real pack (PACK-REAL3); report section 8.

## Phase 6 Release package (session 9, owner: "summary, description and logo ... as well as jar file", "true pixelart logo")
| ID | Task | Model | Status |
|---|---|---|---|
| LOGO2 | True pixel-art icon (CurseForge >= 400 px, integer scale), banner, mod icon; deterministic generator | Opus long | done (merged; 40x40 grid, 17 colours, icon 400 x10, mod icon 160 x4, banner 1440x360 x6; lead verified block uniformity, alpha, determinism, viewed icon/banner/small previews) |
| DESC2 | docs/DESCRIPTION.md up to date (session 8-9 features, real-pack numbers), CF limits, upload checklist | Sonnet short | done (merged; 1 correction: logo line) |
| REL | Rebuild jar with new icon, hand jar + logo + text to owner (no upload without OK) | lead | done (jar bf778e43 = a388815d code + new icon + summary; installed in pack; 483 tests) |

## Phase 7 Clean release (session 10, owner: "changelog", "merge and delete unneeded branches ... all under mc/1.21.1", "jar ready to upload")
| ID | Task | Model | Status |
|---|---|---|---|
| CL1 | `docs/CURSEFORGE_CHANGELOG.md` (paste-ready file changelog from CHANGELOG facts), release date 2026-10-26, doc links to `mc/1.21.1`, permission sentence out of page credits | Sonnet short | done (merged; lead checked numbers against CHANGELOG) |
| GIT1 | Rename `remastered` -> `mc/1.21.1`, set as GitHub default, delete old `main` and merged local branches/worktrees | lead | done (old `main` deleted on GitHub and locally after owner OK; 61 local branches deleted; leftover worktree folders: owner deletes them) |
| JAR | Release jar unchanged (bf778e43; docs are not in the jar) | lead | done |

## Phase 8 Readable text on Nytheria blue (session 11, owner: "text isn't grey on the blue but rather white", upload mod + pack)
| ID | Task | Model | Status |
|---|---|---|---|
| MAP1 | Inventory of text draw sites + colours, own panels, reload listeners | Sonnet short | done (~91 dark-grey/black sites, 2 funnels, ~13 raw sites, baked Formatting colours) |
| MAP2 | Pack coverage audit of EMI Remastered textures vs Nytheria pack | Sonnet short | done (emi textures byte-identical to 1.1.24; pack gaps: empty-sidebar icon, corner alpha; restyle recipe pins old emi_loot jar) |
| TC | Colour remap hook (D29): TextColors class, reload listener, funnels, raw sites, tests, docs/THEMING.md, version 2.1.0, CHANGELOG | Sonnet long | done (498 tests green; funnels TextWidget + EmiDrawContext, 13 raw sites; tooltips untouched) |
| PK1 | Nytheria pack 2.2.0: text_colors.json (D30), widgets/background fixes (D31), recipe jar, version/changelog/page | Sonnet long | done (Nytheria repo a980b89 on v2.2.0; 69 py tests; 1 correction: lighter hues >= 4.4:1; owner greys kept) |
| VER | Dev client screenshots with and without the pack (hidden, muted) | Sonnet long | in progress |
| DOC7 | CF file changelog 2.1.0 (works as first release), DESCRIPTION theming line + version | Sonnet short | done (accepted first pass) |
| REL2 | Mod 2.0.1 + pack 2.2.0: build, install in instance, push, CF upload | lead | open |
