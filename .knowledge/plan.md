# Plan

Usage tally (work units: short = 1, long = 2): Opus 33 (LOOT-ASYNC x2, PACK-REAL x2, F1, F2, Q3 x6, A1 x2, FX1 x4, FOCUS1 x4 = two delegations, PACK-CLI x2, LOGO x2) / Sonnet 113 -> Opus 23 % (sessions 4+5 re-delegations counted again)

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
| T2-SHOTS | Read-only screenshot review of all T2 tour shots, findings table | Sonnet short | in progress |
| FX-SER | Unknown stack type (disabled plugin/removed mod) logs 1 WARN per type instead of ERROR+NPE per entry (EmiIngredientSerializers) | Sonnet short | done (merged; plugin-manager tour: 1 WARN per unknown type, 0 ERROR, emi.json entries kept byte-identical; 3 tests) |
| LOOT-VAN | Code-placed vanilla chest tables mapped by structure type (desert pyramid, fortress, stronghold, ...) in StructureLookup | Sonnet short | done (merged; 53 of 56 chest tables now have a structure, loot-van tour 22/22, lead viewed nether wart Item Info) |
| DOC3 | KNOWN_BUGS (KB7, KB43, KB44) + TOUR_REPORT statuses after wave 6, CREDITS for KB7 port | Sonnet short | done (merged; T2 findings may need a follow-up) |
| PACK-CLI | Pack-mirror dev client (pack mods copied into run/mods, hidden, muted): mixins of other mods into EMI, EMI plugins, reload times, modded Item Info | Opus long | in progress (worktree pack-cli, port 25672) |
| PACK-SRV | Production dedicated server (release jar + copied pack mods, local files only): loading, provides, server-side modules | Sonnet long | done (production server, loader 0.19.5, 560 mods: started 21 s world/99 s total, clean stop, 0 emi warnings/errors, no provides conflicts; 993 structures, 433 of 789 chest tables with a structure, lookup 10.6 s off-thread; 2 other client-only pack mods crash any dedicated server: certain_questing_additions, miningspeedtooltips) |
| BRAND | docs/DESCRIPTION.md (summary EN/DE, tagline, CurseForge/Modrinth description), fabric.mod.json description | Sonnet short | done (merged; summary 187 chars, DE 181, tagline 55; one correction: product names untranslated, 'author') |
| LOGO | Pixel-art logo, banner, mod icon from a deterministic Pillow generator | Opus long | done (merged; slot + gem + magnifier + sparkle, 32 px grid x16, banner 192x48 x8, byte-identical regeneration; lead viewed icon dark/light + banner) |
| PACK-REAL | Owner-authorised launch of the real pack (copied world, offline, muted, options/emi.json backed up and restored), log check | Opus long | done (640 mods, world reached, clean exit; 5 third-party EMI mixins applied; 38 plugins, 0 errors, Let's Do + repair fixed; join reload 30.0 s cold (emi 3441 ms, emiffect 133 vs 1077 ms); emi.json byte-identical; F1 join blocked ~24 s by chest structure wait, F2 world loot links lost after 25 s -> LOOT-ASYNC) |
| LOOT-ASYNC | Structure lookup never blocks the server thread; late results pushed to players; no 25 s cut of world loot links; honest log lines | Opus long | in progress (worktree loot-async, port 25674) |
| -- | Later: KB13 (Sodium vertex path, needs pack measurement), T1-29 core string | - | open |

## Phase 4 Acceptance
Pack install under game lock, end-to-end check, report, push.
- Jar swap: done 2026-10-19 21:1x (owner's client closed; 8 jars in mods_backup/, EMIRemastered jar in mods/, lock created and removed). Owner changed the decision (2026-10-19 ~21:30, 'no you do all'): the lead's team launches the real pack (PACK-REAL). Newest jar (33d82605 build) installed before the launch.
