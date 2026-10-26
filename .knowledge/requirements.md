# Requirements: EMI Remastered

Owner's task (2026-10-19): fork EMI and seven EMI add-ons for Fabric 1.21.1 into ONE mod "EMI Remastered", fully integrated and compatible with every mod in the pack `C:\Users\nikol\curseforge\minecraft\Instances\NytheriaDevelopment`. Fix bugs, optimise, add many new features that show useful information. No Graphify.

## Mods merged (source = exact commit of the installed jar; reference copies in `.upstream/`, git-ignored)
| Module | Upstream | Ref | Licence |
|---|---|---|---|
| EMI core 1.1.24 | emilyploszaj/emi | tag `1.1.24+1.21.1` (322171e) | MIT |
| Distraction Free Recipes 1.2.2 | txnimc/DistractionFreeRecipes | `template` 742836b | MIT file (jar says "Tonis MMC") |
| EMIffect 2.1.5 | Prismwork/EMIffect | `stonecutter` 5340688 | MIT |
| EMI Professions 1.0.3 | Mrbysco/EMIProfessions | `main` 9c2a7ac | MIT |
| EMI Enchanting 0.1.2 | fzzyhmstrs/EMI_enchanting | `master` 40e631d | MIT |
| EMI Ores 1.3 | Abbie5/emi-ores | tag `v1.3+1.21.1` (41c3537) | LGPL-3.0-only; biome icons CC BY-NC-SA 3.0 |
| Let's Do EMI Compat 2.0.3 | Pandaismyname1/Let-s-Do-EMI-Compat | `1.21.1` 5c8dad1 | MIT |
| EMI Loot 0.7.9 | fzzyhmstrs/EMI_loot | `architectury/1.21` 6ba879f | MIT (README/metadata; no LICENSE file) |

Owner statement (2026-10-19): he asked the authors for permission to use and change the code as much as needed. LGPL and CC notices are kept in CREDITS.

## Owner answers (2026-10-19)
1. Mod id: change if possible, else keep. -> kept `emi` (see decisions). Version: "do whatever is needed".
2. Fabric 1.21.1 only.
3. Dependencies/config: "do whatever is the best".
4. Duplicates with other mods: "do whatever is the best".
5. Mod on client and server: yes.
6. Repo `navrelis/EMIRemastered`, public, built on EMI history; work branch `remastered`, `main` = upstream base: yes. Session 10: owner wants one clean default branch `mc/1.21.1`, unneeded branches deleted.
7. At the end swap the 8 jars in the pack for the one jar (old jars to `mods_backup/`, game lock respected). No CurseForge upload without OK: yes.
8. Language: lead's choice -> English + German.
9. Features: ALL of tier A (A1-A12) and tier B, and more than specified. More subagents than the usual limit are allowed.

## Feature list
- A1 "How to get" overview page per item (loot, mob drops, ores, trades, crafting, brewing).
- A2 Enchantments: real max level (NoEnchantmentCapLevel aware), incompatibilities, sources (table, loot, librarian trades).
- A3 Effects: sources (potions, food, mobs, beacon, stew) and brewing path.
- A4 Villagers: real trades per profession/level incl. wandering trader and modded trades.
- A5 Food page (hunger, saturation, effects). A6 Fuel burn time page + composter chance page.
- A7 Mob page (health, armour, spawn biomes, drops).
- A8 Loot: structure/dimension filters, conditions (Looting, Silk Touch, Fortune), Lootr per-player note.
- A9 Ores: modded dimensions (Twilight Forest, BetterEnd/Nether, Aether), compact biome list, Y-range highlight.
- A10 Distraction-free: pin hotkey, fade, per-screen toggle, stays on recipe screens.
- A11 One config screen, a section and an on/off switch per module, plugin manager.
- A12 Faster reloads with before/after numbers.
- B: tool/weapon/armour stats, biome pages, tag browser, copy recipe as text/JSON, structure pages, advancement links.
- Extras chosen by the lead (see plan).

## Facts (verified 2026-10-19)
- Pack: MC 1.21.1, Fabric Loader 0.19.5, Fabric API 0.116.17, ~407 jars, lang en_us. Java 21 locally.
- 27 other mods ship EMI plugins; 16 call EMI internals; mixins into EMI by ali, certain_questing_additions, clientsort, polymorph, sporran (details `.upstream/compat/compat-report.md`). -> id `emi` and every `dev.emi.emi` class/member they use must stay.
- Live bugs: Let's Do compat plugin crashes every reload (Vinery 1.5.5 renamed classes, Meadow `FONDUE`->`FONDUE_POT`); duplicates: ALI vs EMI Loot, JEED vs EMIffect, native Vinery/Farm&Charm/Bakery plugins vs compat. Reload: EMI ~3 s, emiffect ~1.1 s.
- Mappings: EMI, Loot, Enchanting = Yarn; DFR, EMIffect, Professions, Ores, Let's Do = Mojang.
- EMI Maven moved to https://repo.sleeping.town/.

## Constraints
- Windows 11, PowerShell/Git Bash, Java 21, `py -3`. Work only in `F:\Coding\EMI_Remastered`; other `F:\Coding` projects read-only.
- Pack: change only our jars at the end. Game lock `F:\Coding\.knowledge\game.lock` (one session runs Minecraft from the pack at a time; younger than 2 h = wait). Never kill Java processes we did not start; no `gradlew --stop`.
- No new runtime libraries. JUnit 5 for tests only.
- Subagents implement; lead writes only `.knowledge/`, runs builds/tests/git. Subagents never commit.
- Never force-push; no secrets.
- Owner (2026-10-19): never start Minecraft with sound.
- Owner (2026-10-19, session 5): Phase 4 = once the owner has closed the game, lead backs up the 8 jars to mods_backup/ and installs the new jar; the OWNER launches the pack; lead then reads latest.log and reload times.
- Owner (2026-10-19 ~21:30): 'no you do all' - the lead's team launches the real pack itself for the Phase 4 check (supersedes 'owner launches'). Safeguards: muted, test copy of a world, offline name, options.txt/emi.json backed up and restored, game lock.
