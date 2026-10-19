# Plan

Usage tally (work units: short = 1, long = 2): Opus 8 (F1, F2, Q3, A1) / Sonnet 21 -> Opus 28 %

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
| P1 | Distraction Free Recipes into EMI core | Sonnet short | in progress (wip/p1-distraction-free, + A10) |
| P2 | EMIffect -> `effects` | Sonnet short | in progress (wip/p2-effects, + A3) |
| P3 | EMI Professions -> `professions` | Sonnet short | done |
| P4 | EMI Enchanting -> `enchanting` | Sonnet short | in progress (wip/p4-enchanting) |
| P5 | EMI Ores -> `ores` (LGPL headers kept) | Sonnet long | in progress (wip/p5-ores) |
| P6 | Let's Do compat -> `letsdo` (Vinery/Meadow fixes, no Architectury, dedupe native) | Sonnet long | open |
| P7 | EMI Loot -> `loot` (fzzy_config -> EMI config, Fabric networking, AW merge) | Sonnet long | in progress (wip/p7-loot) |

## Phase 2 Quality
| ID | Task | Model | Status |
|---|---|---|---|
| Q1 | Dev-client screenshot tour + log check harness | Sonnet long | done |
| Q2 | Known bugs (EMI repair AIOOBE, upstream issues of each module) | Sonnet | open |
| Q3 | A12 reload performance, before/after numbers | Opus long | in progress (wip/q3-perf) |

## Phase 3 Features (after the module each builds on is merged)
| ID | Task | Builds on | Model | Status |
|---|---|---|---|---|
| A1 | Item Info screen + provider API ("How to get": recipes by output grouped, loot, mob drops, ores, trades, brewing; "no known source" marker); key bind on hovered stack | F2 | Opus long | in progress (wip/a1-info) |
| A2 | Enchanting: real max level (cap mods), incompatibilities via canBeCombined, sources (table/loot/librarian), info provider | P4, A1 | Sonnet | open |
| A3 | Effects sources (in P2) | P2 | Sonnet | in P2 |
| A4 | Villager trades per profession/level (client dummy villager, wandering trader, Fabric TradeOfferHelper trades; Villager-API plans if reachable) | P3 | Sonnet long | open |
| A5+B1 | `stats` module: food page, tool/weapon/armour stats pages, fuel/compost in info screen (A6) | F2, A1 | Sonnet long | open |
| A7 | `mobs` module: mob pages (health, armour, attack, speed, spawn biomes via server sync, drops link) | P7 | Sonnet long | open |
| A8 | Loot: structure/dimension filters, condition icons, Lootr per-player note | P7 | Sonnet | open |
| A9 | Ores: modded dimensions, compact biome list, Y-range highlight | P5 | Sonnet | open |
| A10 | Distraction-free extras (in P1) | P1 | Sonnet | in P1 |
| A11 | Unified config + plugin manager (in F2) | F2 | Opus | done |
| A12 | Reload performance (Q3) | - | Opus | in progress |
| B2 | `world` module: biome pages (climate, tags, spawns, ores) + structure pages (server sync) | P5, A7 | Sonnet long | open |
| B3 | Tag browser screen (all tags, search, members) | F2 | Sonnet | open |
| B4 | Copy recipe as text/JSON bind | F2 | Sonnet short | open |
| B5 | Advancement links in item info (icon/display) | A1 | Sonnet short | open |
| X1 | Pack-dev export command `/emi export` (recipes, loot, sources to JSON/CSV) | A1 | Sonnet | open |
| X2 | Let's Do port fixes + dedupe (P6) | Y0 | Sonnet long | open |

## Phase 4 Acceptance
Pack install under game lock, end-to-end check, report, push.
