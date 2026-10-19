# Plan

Usage tally (work units: short = 1, long = 2): Opus 0 / Sonnet 2 (research x2) -> Opus 0 %

Status: open / in progress / in review / done. `||` = can run in parallel.

## Phase 0 Foundation (sequential)
| ID | Task | DoD | Dep | Model | Status |
|---|---|---|---|---|---|
| S0 | Repo, branches, GitHub, `.knowledge` | origin navrelis/EMIRemastered, `main` = 1.1.24 tag, `remastered` work branch | - | lead | in progress |
| F1 | Single Fabric Loom project (D1, D2) | `gradlew build` -> one jar; dev client reaches title + world with EMI working; neoforge/forge/xplat gone; metadata per D1; compile-only deps for modules (Let's Do jars matching pack, JEI, modmenu) | S0 | Opus long | open |
| F2 | Module framework + unified config + plugin manager (D3, D4, D8) | module registry, per-module config groups + enable switch in EMI config screen, plugin manager by mod id (after Sporran's hook), module stubs + mixin configs + entrypoints registered, JUnit set up, en_us/de_de | F1 | Opus long | open |

## Phase 1 Ports (parallel, worktrees) — each: module under `dev.emi.remastered.<m>`, Yarn, own config group, no dependency on the old mod
| ID | Task | Model | Status |
|---|---|---|---|
| P1 | Distraction Free Recipes into EMI core | Sonnet short | open |
| P2 | EMIffect -> `effects` | Sonnet short | open |
| P3 | EMI Professions -> `professions` | Sonnet short | open |
| P4 | EMI Enchanting -> `enchanting` | Sonnet short | open |
| P5 | EMI Ores -> `ores` (LGPL headers kept) | Sonnet long | open |
| P6 | Let's Do compat -> `letsdo` (Vinery/Meadow fixes, no Architectury, dedupe native) | Sonnet long | open |
| P7 | EMI Loot -> `loot` (fzzy_config -> EMI config, Fabric networking, AW merge) | Sonnet long | open |

## Phase 2 Quality
| ID | Task | Model | Status |
|---|---|---|---|
| Q1 | Dev-client screenshot tour + log check harness | Sonnet long | open |
| Q2 | Known bugs (EMI repair AIOOBE, upstream issues of each module) | Sonnet | open |
| Q3 | A12 reload performance, before/after numbers | Opus long | open |

## Phase 3 Features (after ports) — details written when phase starts
A1 source index core (Opus) + page (Sonnet); A2-A11, tier B, extras (Sonnet unless noted).

## Phase 4 Acceptance
Pack install under game lock, end-to-end check, report, push.
