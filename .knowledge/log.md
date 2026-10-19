# Log
2026-10-19 | research sources/licences/build (Sonnet, long) | done: all 8 sources found, EMIffect not decompiled; refs in requirements
2026-10-19 | pack compatibility survey (Sonnet, long) | done: `.upstream/compat/compat-report.md`
2026-10-19 | S0 lead: clone EMI tag 1.1.24, branches main/remastered, `.upstream/` reference copies
2026-10-19 | F1 single Fabric Loom build (Opus, long) | delegated
2026-10-19 | Y0 convert 5 Mojang add-ons to Yarn into `.upstream/yarn/` (Sonnet, long) | delegated (prep for P1-P6)
2026-10-19 | R2 feature data research -> `.upstream/feature-research.md` (Sonnet, long) | delegated
2026-10-19 | R2 feature research | done: client lacks spawns/features/structures/loot -> server sync; EMI has fuel+composting already; emiffect O(effects x items); reload hot spots listed
2026-10-19 | F1 review (lead) | accepted: clean build ok, jar metadata ok, GlobalMixin 404 targets = upstream, only EmiAgnos forName lines removed; runClientWorld 1273 ms reload (vanilla)
2026-10-19 | F2 module framework + unified config + plugin manager (Opus, long) | delegated, main checkout
2026-10-19 | Q1 dev-client screenshot tour (Sonnet, long) | delegated, own worktree
2026-10-19 | Q1 review (lead) | accepted: 19/19 steps, devtour not in release jar; committed d054564 on worktree branch; merge after F2. Note: Agent isolation worktrees start from main, not remastered -> lead creates worktrees for later parallel tasks
2026-10-19 | owner asked for more speed -> ports start before F2 with a fixed contract (scratchpad port-contract.md; F2 told via message)
2026-10-19 | P7 EMI Loot port (Sonnet, long) | delegated, worktree wip/p7-loot
2026-10-19 | P4 EMI Enchanting port (Sonnet, short) | delegated, worktree wip/p4-enchanting
2026-10-19 | Q3 reload performance + repair AIOOBE (Opus, long) | delegated, worktree wip/q3-perf
2026-10-19 | P3 EMI Professions port (Sonnet, short) | delegated, worktree wip/p3-professions
2026-10-19 | P5 EMI Ores port (Sonnet, long) | delegated, worktree wip/p5-ores
2026-10-19 | F2 review (lead) | accepted: EMI diffs small + marked, build ok, 26 tests green; committed 13f59cf; Q1 merged 7c49e57; tour 19/19 ok, config screen shows Plugins button + module jumps, 0 EMI warnings
2026-10-19 | P1 DFR into core + A10 (Sonnet, long) | delegated, wip/p1-distraction-free (from F2)
2026-10-19 | P2 EMIffect port + perf + A3 sources (Sonnet, long) | delegated, wip/p2-effects (from F2)
2026-10-19 | Q1b tour steps click/scroll/config jump/plugin manager + config visual review (Sonnet, short) | delegated, wip/q1b-tour-steps
2026-10-19 | A1 Item Info screen + provider API (Opus, long) | delegated, wip/a1-info
2026-10-19 | P3 review (lead) | accepted: 6 classes, no mixins, server-safe; merged (add/add on stubs -> porter versions), build+tests green
2026-10-19 | A4 villager trades in professions module (Sonnet, long) | delegated, wip/a4-trades
2026-10-19 | auto-handoff: all agents stopped, partial work uncommitted in worktrees; see F:\Coding\EMI_Remastered\handoff-2026-10-19-5f3846e1.md
2026-10-19 | session 2: handoff checked against repo (matches; Q3 also touched EmiHidden/EmiPersistentData/EmiSearch); F1-based worktrees P4/P5/P7/Q3 got WIP commits + merge of remastered (stub add/add -> porter versions); all worktrees muted (run/options.txt)
2026-10-19 | wave 1 delegated (continue in worktree, shared rules file): P7 loot, P5 ores, P4 enchanting, P1 DFR+A10, P2 effects+A3, Q1b tour+mute enforcement, A4 trades (Sonnet); P6 letsdo new (Sonnet, long); Q3 perf, A1 info (Opus, long)
2026-10-19 | B3+B4 new module tools (Sonnet, long) | delegated, wip/b-tools (no jump icon: lead adds later)
2026-10-19 | P5 review (lead) | accepted: faithful port, LGPL headers, OresEmiPlugin initialize forwarding (needed for biome serializer), canSend guard; merged a123b92, build+tests green. Follow-up for A9: batch-complete marker so a reload mid-sync can't see partial features
2026-10-19 | P7 review (lead) | accepted: faithful port, 11 AW class lines (no EMI lines removed), 6 S2C payloads canSend-guarded, settings restored to upstream set; merged 5e68fda, build+tests green. Note: lootify condition/function types registered like upstream
2026-10-19 | A8 loot extras (Sonnet), A9 ores extras + sync marker (Sonnet), A7 mobs module (Sonnet, long) | delegated, own worktrees from c7cf633
2026-10-19 | P1+A10 review (lead) | accepted: upstream behaviour, no mixins, marked edits in EmiScreenManager (isVisible delegates to new isBaseVisible + DF hiding, same as upstream mixin; no signature changes) and EmiSearchWidget; module off = stock; merged, build+tests green. Manual: real clicks on Show/Hide buttons, crafting-table scope
2026-10-19 | owner: dev clients grab his mouse -> all agents told: no OS input, no client launches until no-grab fix lands (Q1b, urgent)
2026-10-19 | P4 review (lead) | accepted: dynamic-registry enchantments, porter's EmiApi mixin dropped (no mixins into EMI); merged, build green. Lost extra: show-recipes on multi-enchant books (needs EMI hook, optional)
2026-10-19 | A4 review (lead) | accepted: 485 villager + 70 wanderer trades, per-factory try/catch, SellMap accessor placeholder, Villager API reflection isolated (untested live); also fixed P3 recipe ids (26 EMI errors gone); merged
2026-10-19 | P6 review (lead) | accepted: sub-modules loaded by name after isModLoaded, LinkageError caught per sub-module, dedupe vs native vinery/bakery/farm_and_charm plugins, tested with real pack jars in run/mods; merged, build+tests green
2026-10-19 | B3+B4 (Sonnet) | code done, build green; client checks pending (no-client rule); default binds T / Ctrl+C to be checked against text fields
2026-10-19 | P2+A3 review (lead) | accepted: one-pass index (63-93 ms vs ~1.1 s upstream cold), emiffect:effect id kept + favourites string bug fixed, sources table, EffectsEmiPlugin initialize forwarding; merged, build green. Steps.java click step will clash with Q1b (resolve at merge). Manual: module-off client check
2026-10-19 | A1 review (lead) | accepted: module info (ns emi_info), API in info.api + docs/INFO_PROVIDERS.md, recipes/no-source/fuel/compost providers, mixins into EmiApi/EmiScreenManager (HEAD, cancellable only when InfoScreen open), icon index 8; merged, build green. Follow-up: exempt dev.emi.remastered mixins from EMI 'tainted' warning
2026-10-19 | A7 mobs (Sonnet) | code done, build+server green; client checks pending
2026-10-19 | wave 3 delegated (Sonnet, no client launches until D13 fix): A2 enchanting extras, Q2 bug survey, A5+B1 stats module, B5 advancement provider, X1 /emiexport
2026-10-19 | auto-handoff: all agents stopped; partial work uncommitted in worktrees; see F:\Coding\EMI_Remastered\handoff-2026-10-19-5b513619.md
2026-10-19 | session 3: handoff matches repo; WIP commits + merge of remastered into Q1b, Q3, A7, A8, A9, tools (conflicts in Steps/DEVTOUR for Q1b, Modules/fabric.mod.json/lang/ModulesTest for A7+tools left to agents); empty worktrees fast-forwarded
2026-10-19 | wave 4 delegated: Q1b finish no-grab (Sonnet short, only agent allowed client launches), Q3 continue (Opus long), A8, A9, A7, tools continue (Sonnet), A2, A5+B1, B5, X1 (Sonnet, fresh), Q2 survey (Sonnet); all others: no client launches
2026-10-19 | A7 review (lead) | accepted: conflicts resolved (mobs after info), payload emi_mobs:spawns canSend-guarded, client data cleared on disconnect, server-safe, spawn-egg info provider; merged eab051f, build green, pushed. Client tour pending (mobs.json)
2026-10-19 | B3+B4 review (lead) | code accepted: tools mixin HEAD hooks gated by module switch, own text-field guard (core hasFocusedTextField returns after first container: separate fix task), icon slot; re-merge after mobs -> correction sent (order info, mobs, tools; icon slot 10)
2026-10-19 | B3+B4 tools merged (lead): re-merge resolved (info, mobs, tools; tools icon slot 10), build green
2026-10-19 | X1 review (lead) | accepted: /emiexport client command, render-thread slices (10 ms/tick), compute() bypasses page cache, abort on reload/leave, async write; merged, build green, pushed. Client run pending (tour export.json via call step)
2026-10-19 | B5 review (lead) | correction: listener replay calls setListener(null) and would drop other mods listeners -> use accessor on progress map
2026-10-19 | A8 review (lead) | correction: structure prefetch on server thread at start -> background build with join/timeout, skip when setting off
2026-10-19 | A9 review (lead) | protocol start/batch/complete + OresSyncState accepted; correction: OresSelfCheck dev-only, cache sync plan per data generation
2026-10-19 | A2 review (lead) | accepted: measured level cap, incompatibility matrix (either direction), tag sources page, info provider; merged, build green
2026-10-19 | Q2 review (lead) | accepted: docs/KNOWN_BUGS.md (42 rows, 22 affect us, top 10); merged
2026-10-19 | B5, A8, A9 corrections | accepted (accessor instead of listener; async structure build; dev-only self check + cached plan); merged, build green
2026-10-19 | A5+B1 review (lead) | accepted: stats module (food category, tool/weapon/armour sections), order info-mobs-tools-stats; merged, build green
2026-10-19 | Q1b review (lead) | accepted: mixins cancel lockCursor/unlockCursor/setCursorParameters/raw mouse/fullscreen, no-focus hints, never pause on lost focus; devtour on all client runs, not in release jar (0 entries); 4 tour launches clean; merged, pushed. Client ban lifted (D13 fix in place)
2026-10-19 | Q3 code review (lead) | accepted: tag index (skips only tags with no shared value), RecipeBuckets (identity adjacency when allDistinct), repair fix; benchmark + KB2/KB12 sent
2026-10-19 | wave 5 delegated: T1 tours+icons (Sonnet long), FX1 reload/persistence (Opus long), FX2 core small fixes (Sonnet), FX3 module fixes (Sonnet), P8 trades/effects providers (Sonnet), B2 world module (Sonnet long); client launches allowed (one per agent)
