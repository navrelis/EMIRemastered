# Report: EMI Remastered 2.0.0+1.21.1

Final state 2026-10-19 ~23:50, branch `remastered` (navrelis/EMIRemastered), pushed. One jar
`EMIRemastered-1.21.1-2.0.0-Fabric.jar` (sha256 829b63e1...) replaces the 8 old jars in the Nytheria pack (old jars in
`<pack>\mods_backup\`). Build incl. 381 JUnit tests green.

## 1. Requirements and what was implemented
| Requirement | Result |
|---|---|
| Fork EMI 1.1.24 + 7 add-ons into ONE Fabric 1.21.1 mod | Done. Mod id `emi` (D1), version `2.0.0+1.21.1`; add-ons are modules under `dev.emi.remastered.<module>` with their old resource namespaces and plugin ids (D3); `provides` the 7 old ids so a leftover add-on jar fails loudly. fzzy_config, TXNI Lib, Architectury, ForgeConfigAPIPort no longer needed (D4). |
| Compatible with every mod of the pack | Done and checked in the real pack twice (PACK-REAL, PACK-REAL2): 640 mods, world reached, clean exit; mixins of sporran, ali, hollowhorizon, clientsort, polymorph into EMI apply; 38 EMI plugins, 0 plugin errors; production dedicated server with 560 pack mods clean (PACK-SRV). All `dev.emi.emi` names/signatures kept. |
| Bugs fixed | EMI core: reload after world gone (KB1), unreadable saved entries kept, unknown stack types one WARN (FX-SER), category-less recipe, repair recipe error, fill-max overflow (KB9), tag search, favourites remainders (KB7) and more. Let's Do compat crash on every reload (Vinery 1.5.5 / Meadow renames) fixed. Module fixes listed in `CHANGELOG.md` "Fixes". |
| Faster | Warm reload -40..58 % on a 92k-recipe bench, plugin phase -49..66 %, join -21 %, ~60 MB less heap (Q3). Real pack: EMI reload 22.98 s (was 30.0 s), emiffect 133 ms (was 1077 ms), emi_loot 2036 ms (was 5631 ms); join no longer blocked by the structure lookup (4 s instead of ~24 s, LOOT-ASYNC). |
| Many info features (A1-A12, tier B, more) | Item Info screen (I) with How to Get / Used For / Details + provider API; enchantment max level, incompatibilities, sources; effect sources; villager trades per profession and level; food, fuel, compost, tool/weapon/armour stats; mob pages; loot structure/dimension filters, condition icons, Lootr note; ores in modded dimensions, Y-range highlight; distraction-free extras; one config screen with module switches and plugin manager; biome and structure pages; tag browser (T); copy recipe as text/JSON (Ctrl+C); advancement links; `/emiexport`. Details: `CHANGELOG.md`, `docs/MODULES.md`, `docs/INFO_PROVIDERS.md`. |
| Owner session-6 additions: verify with the pack, description + summary, pixel-art logo | Pack verified (above). `docs/DESCRIPTION.md` (summary EN 187 chars, DE 181, tagline, CurseForge/Modrinth text), `fabric.mod.json` description. Logo: `scripts/logo/make_logo.py` (deterministic), `docs/logo/icon.png`, `icon-400.png`, `banner.png`, mod icon `src/main/resources/icon.png`. |
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
- Structure lookup takes 10-45 s in this pack (off-thread now; chest structure info appears a few seconds after the EMI reload). Recommendation: parallel template reads.
- The lookup summary log line counts 433 of 789 "chest" tables while 563 tables are delivered (narrower set in the summary); cosmetic.
- Item Info screen: right-aligned counts ("x2", "100%") sit flush at the scroll area edge (`info/client/EntryImpl.java` ~line 104); one-line padding change recommended.
- Flying mobs show the raw flying-speed attribute (no verified blocks/s conversion); mobs that never set a walking speed show none.
- GUI scale 3 at 1280x720: only the "Modules" jump fits next to EMI's jumps; tooltip overlap at scale 3 accepted (R7). EMI config header glyphs (R6) and T1-29 "item(s)" are EMI core strings, untouched.
- Visible-window mode of dev clients (`-PdevtourVisible`) was never launched (owner focus rule).
- T2 screenshot review covered only part of the tours (image reads failed for 7); the lead spot-checked world/tools/p8.
- Other pack mods: `certain_questing_additions` (InvalidInjectionException into FTB Quests client code) and `miningspeedtooltips` (client API in the main entrypoint) crash any dedicated server. Not ours; tell their authors or keep them client-only.
- Old worlds list the data packs `emi_loot` and `emi_letsdo_compat`: one-time "Missing data pack" note per world.
- Release on CurseForge/Modrinth waits for the owner's OK.
