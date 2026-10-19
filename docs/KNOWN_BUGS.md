# Known bugs survey (Q2)

Research only, written 2026-10-19 against branch `wip/q2-bugs` (= `remastered` at 2fd06957: EMI 1.1.24 core plus the merged ports P1-P7, A1, A4).
Nothing in this document is implemented. Paths are relative to `src/main/java/dev/emi/`; `emi/...` is `dev.emi.emi`, `remastered/...` is `dev.emi.remastered`.

Sources used

- Upstream trackers (GitHub, open and closed): emilyploszaj/emi (1,090 issues, all PRs), txnimc/DistractionFreeRecipes, Prismwork/EMIffect, Mrbysco/EMIProfessions, fzzyhmstrs/EMI_enchanting, Abbie5/emi-ores, Pandaismyname1/Let-s-Do-EMI-Compat, fzzyhmstrs/EMI_loot.
- EMI commits after the 1.1.24 tag (322171e): the `1.21` branch has 8 commits, only one is a code fix (4c22e3b0, tag search, = KB6). No 1.1.25 release exists (latest release is 1.1.24, 2026-05-13). Open PRs that matter: #1222 (KB2), #1194 (KB9), #933 (KB12).
- Post-release commits of the add-ons: emi-ores `1.21` branch (e905cf4 skip unbound features, ff47294 canSend; both already in our port), EMIffect `stonecutter` (8b32a7d edit-mode stacks; already handled in our port). The other add-ons have nothing newer than the commit we ported.
- Pack logs: the 58 rotated logs plus `latest.log` (59 files, 2026-10-03 to 2026-10-19 17:36), 67 EMI reloads, and the single crash report. Reload time: min 4.8 s, median 17.7 s, max 71.0 s. The only crash report (2026-10-17 13:54, glintsmith vs modelfix mixin on `ItemModelGenerator`) is not related to EMI.
- Pack facts used: `config/emi.css` has `index-source: creative`; the pack contains Item Obliterator, Sophisticated Storage 1.3.7.9 plus Chipped 4.0.2, ali (Advanced Loot Info) and jeed; the pack has no JEI, so JEMI code never runs there.

Rows that affect us come first, sorted by severity; rows marked `no` or `fixed` follow at the end of each table.

Columns: "affects us" is `yes`, `no`, or `fixed (<module>)`. `yes (pending Q3)` means the fix already exists in the unmerged branch `wip/q3-perf`. Severity: crash (reload, plugin or client fails), wrong data (wrong or lost content), visual, perf. Effort S under 1 h, M up to a day, L more.

## The 10 most valuable fixes

Ranked by (severity x how often the pack hits it) / effort.

| rank | id | why |
|---|---|---|
| 1 | KB1 | One kicked join produced about 31,700 error events and a 71 s reload in `latest.log`; 17 plugins (EMI itself, Farmer's Delight, BetterEnd, our Ores and Let's Do ports, ...) lost their recipes; the same race happened in 6 of 59 logs. |
| 2 | KB3 | The reload above also failed to parse the owner's `emi.json` entries (enchanted book, biome stack); the next save drops them for good. Silent data loss. |
| 3 | KB4 | 28 modded tools throw in every reload of every session (1,204 log lines in 37 logs) and lose their anvil-repair and grindstone recipes as collateral. Fix already exists in `wip/q3-perf`, only needs merging. |
| 4 | KB5 | The pack uses `index-source: creative` plus Item Obliterator: obliterated items come back into the index (exactly upstream #1263). Small fix. |
| 5 | KB2 | One third-party recipe with a null category aborts the whole reload (3 retries, then EMI is dead). Upstream has a ready PR. The pack has 27 foreign plugins. |
| 6 | KB6 | Block tag search (`#minecraft:...`) returns nothing; upstream fixed it after 1.1.24. Small. |
| 7 | KB9 | Shift+fill ("fill max") overflows an int and can empty the vanilla crafting grid; one-line guard, upstream PR #1194 already exists. |
| 8 | KB10 | Mod name is missing from all item tooltips for the whole reload (13-70 s), and a failed reload leaves it off for the session. Small. |
| 9 | KB12 | Pack bakes about 113,000 recipes (Sophisticated Storage + Chipped, upstream #1187 "tens of millions of ItemEmiStack"); tag stack lists are not shared. Memory. |
| 10 | KB17 | Loot tooltip shows the raw key `emi_loot.function.copy_name.BLOCK_ENTITY`; one-line fix plus a lang typo. |

Next in line: KB7 (recipe-tree remainder counts), KB13 (Sodium slow path), KB14 (hide click rewrites emi.json on the render thread), KB22 (ores sync window, unverified).

## Counts

42 rows (KB1-KB42) plus 7 watch-list items (W1-W7).

| area | yes | yes (pending Q3 merge) | fixed in our port | no |
|---|---|---|---|---|
| EMI core | 14 | 1 | 0 | 1 |
| loot | 2 | 0 | 2 | 1 |
| ores | 2 | 0 | 2 | 1 |
| effects | 0 | 0 | 1 | 0 |
| enchanting | 2 | 0 | 0 | 0 |
| professions | 0 | 0 | 0 | 2 |
| letsdo | 0 | 0 | 3 | 0 |
| distraction_free | 0 | 0 | 3 | 0 |
| pack interplay | 1 | 0 | 0 | 4 |
| total | 21 | 1 | 11 | 9 |

The 22 open rows by severity: wrong data 12, perf 6, crash 2 (KB1, KB2), visual 2 (KB11, KB17). Effort: 15 x S, 7 x M.

## EMI core (`emi/...`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB1 | Reload keeps running after the world is gone (kick or disconnect during join): every world access throws | Pack `latest.log` 17:28:51-17:30:02: reload of 71,010 ms, 5,726 "Exception thrown when parsing vanilla recipe", 25,925 "EMI caught an exception while baking search", 3 "Error parsing NBT", 17 "Exception loading plugin" (emi, bakery, bclib, betterend, betternether, emi_letsdo_compat, emi_ores, eternal_starlight, farm_and_charm, farmersdelight, justenoughbreeding, sawmill, sophisticatedbackpacks, sophisticatedstorage, supplementaries, twilightforest, vinery); also 2026-10-03-2/4/5/6 and 2026-10-04-2 (151 lines). Triggers were a kick by Open Parties and Claims (latest.log:59578) and a SpectreLib ClassCastException ("Invalid player data", :60238), not EMI. Upstream [#1056](https://github.com/emilyploszaj/emi/issues/1056), [#926](https://github.com/emilyploszaj/emi/issues/926), [#1081](https://github.com/emilyploszaj/emi/issues/1081) | yes | `emi/runtime/EmiReloadManager.java:150` (world checked once, at the start); `:78`, `:92` (`restart` only set by `emi/mixin/MinecraftClientMixin.java:38-42`, which logged nothing in `latest.log`: no "EMI data cleared" line); `emi/registry/EmiStackList.java:69`, `emi/registry/EmiRegistryImpl.java:55`, `emi/runtime/EmiTagKey.java:57`, `emi/api/stack/ItemEmiStack.java:168` | crash | In `ReloadWorker` add a cheap `worldGone()` check (client.world null or client.player null) and call it in `step(...)`, in the two plugin loops (`:166-216`), before `EmiStackList.reload()/bake()`, `EmiSearch.bake()` and `EmiPersistentData.load()`; on true log one line, set `restart=false`, leave `status=0` and return. Also register `ClientPlayConnectionEvents.DISCONNECT` (Fabric API, present) that calls `EmiReloadManager.clear()` and interrupts a running worker thread, so a plugin that blocks (ali waits up to 63 s, see KB38) wakes up. Keep `ReloadWorker.run()` signature (clientsort mixin). | M |
| KB2 | A recipe with a null category aborts the whole reload (`category.getId()` NPE while only trying to log) | Upstream [#1210](https://github.com/emilyploszaj/emi/issues/1210), [#1218](https://github.com/emilyploszaj/emi/issues/1218) (caused by JEMultiblocks + Mekanism Extras), [PR #1222](https://github.com/emilyploszaj/emi/pull/1222) open; no hit in the pack logs | yes | `emi/registry/EmiRecipes.java:170` (inside `Manager` constructor), filter list built at `:88-98` | crash | Add an invalidator next to `:90` that returns true for `r.getCategory() == null` and logs `EmiReloadLog.warn` with recipe id and `r.getClass().getName()` (same as PR #1222), so one bad plugin cannot kill the reload. Also guard `:170` with `category != null`. | S |
| KB3 | Favourites, lookup history, hidden stacks and recipe defaults that fail to deserialize are dropped and then overwritten on the next save | Pack: `latest.log:60625-60651` "Error parsing NBT in deserialized stack" (enchanted book with `nova_structures:ghasted`, `betternether:ruby_fire`, a `biome:` stack); the owner's `emi.json` `lookup_history` contains exactly such entries. Upstream [#1199](https://github.com/emilyploszaj/emi/issues/1199) (id of a removed mod), [#1122](https://github.com/emilyploszaj/emi/issues/1122) | yes | load: `emi/runtime/EmiFavorites.java:62-65`, `emi/runtime/EmiHidden.java:68-70`, `emi/runtime/EmiSidebars.java:98-100`, `emi/bom/BoM.java:116-127`, `emi/runtime/EmiPersistentData.java:32-52`; saves that rewrite the file: `EmiFavorites.java:146,189`, `EmiHidden.java:133`, `EmiSidebars.java:48,61`, `BoM.java:191-226` | wrong data | In `EmiPersistentData.load()` return early when the world is null (KB1) and keep every entry whose deserialized stack is empty as a raw `JsonElement` in a per-section "preserved" list; `save()` appends the preserved entries unchanged, so stacks of a temporarily missing mod or registry survive. | M |
| KB4 | "Exception thrown registering repair recipes" (`ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0`) every reload | Pack: 1,204 lines in 37 of 59 logs, 28 per reload (448 lines in the last three days). The try block covers the whole item, so the same 28 items also lose their "anvil/repairing/tool" and grindstone recipes. Upstream [#784](https://github.com/emilyploszaj/emi/issues/784) (open since 2024-11) | yes (pending Q3) | `emi/VanillaPlugin.java:499` (armor) and `:503` (tools: `getMatchingStacks()[0]` of a repair ingredient that matches nothing, for example an empty tag); collateral recipes at `:509-510` | wrong data | Merge `wip/q3-perf` (commit 228cea8f): it reads `getMatchingStacks()` once, skips the material recipe when the array is empty and keeps the other two recipes. Optionally also build the id from the tag name so the material recipe is not lost. | S |
| KB5 | `index-source: creative` still lists items that are in no creative tab (also: more obliterated or disabled items make everything reappear) | Upstream [#1234](https://github.com/emilyploszaj/emi/issues/1234), [#1263](https://github.com/emilyploszaj/emi/issues/1263) (Item Obliterator). Pack has `index-source: creative` and `item_obliterator` | yes | `emi/registry/EmiStackList.java:71-81` (registry fallback group always built), `:119-126` (suppressed only if one creative tab holds at least a third of the namespace), `:135` (all fallback groups added) | wrong data | While iterating the creative groups collect `creativeNamespaces`; for `IndexSource.CREATIVE` remove those namespaces from `namespaceGroups` before `:135` (keep the fallback only for namespaces without any creative entry). Drop the one-third heuristic. | S |
| KB6 | Searching `#<block tag>` finds nothing | Upstream [#1186](https://github.com/emilyploszaj/emi/issues/1186), fixed by EMI commit [4c22e3b0](https://github.com/emilyploszaj/emi/commit/4c22e3b0) (unreleased, 1.21 branch) | yes | `emi/search/TagQuery.java:17-19,35`; `emi/search/RegexTagQuery.java:25-27,44` | wrong data | Apply 4c22e3b0: stream `EmiTags.TAGS` (all registries) instead of item plus block registries, and in `matches` also accept a `BlockItem` whose block is in the valid set. | S |
| KB7 | Remaining or missing amounts of synthetic favourites are wrong when a recipe's remainder feeds a later step | Upstream [#1113](https://github.com/emilyploszaj/emi/issues/1113), [#895](https://github.com/emilyploszaj/emi/issues/895), [#766](https://github.com/emilyploszaj/emi/issues/766); reference patch in the fork commits aMelonRind/emi 2508321 and e7fd7d9. Not reproduced here | yes | `emi/runtime/EmiFavorites.java:250-275` (`countRecipes` sums batches without remainders); `emi/bom/TreeCost.java:57-62` (`addRemainder`) and `calculateCost` | wrong data | Port the fork patch: let `countRecipes` subtract the amounts already satisfied by remainders recorded in `TreeCost.remainders`, and reset that progress when the tree is recalculated. Verify with a recipe whose output is used twice (rods for bolts and ring). | M |
| KB8 | "Exception thrown setting remainders" for trapped chest, chest minecart, ten chest boats | Pack: `2026-10-03-4.log` (33 lines, 3 reloads); `ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1` from `DefaultedList.get` | yes | `emi/recipe/EmiShapedRecipe.java:46` (list index `(i / 3 * width) + (i % 3)` taken from the cropped recipe input, not checked against the list size) | wrong data | Compute the index from the cropped `cri` (`row*cri.getWidth()+col` with the offset of the first non-empty cell) and check `idx < list.size()` before `get`, otherwise skip the stack. | S |
| KB9 | Shift+fill ("fill max") on crafting tables can clear the grid: `amount + batchesAlreadyPresent` overflows | Upstream [#1191](https://github.com/emilyploszaj/emi/issues/1191), [PR #1194](https://github.com/emilyploszaj/emi/pull/1194) open | yes | `emi/registry/EmiRecipeFiller.java:215` | wrong data | Guard the addition (`amount < Integer.MAX_VALUE - batches`) exactly like PR #1194, or compute in `long` and clamp. | S |
| KB10 | Mod name disappears from all item tooltips during a reload; a failed bake leaves it off | `EmiSearch.bake` flips the global `EmiConfig.appendItemModId` for the whole bake (13-70 s in the pack) and restores it only at the end | yes | `emi/search/EmiSearch.java:64-65,124`; reader `emi/mixin/ItemStackMixin.java:28` (only exempts `EmiSearch.searchThread`, not the bake thread) | wrong data | Replace the global flip by a `ThreadLocal<Boolean>` (or a volatile `bakingThread`) that `ItemStackMixin` checks; at minimum wrap `:65-124` in try/finally. | S |
| KB11 | Own mixins are reported as "EMI class is tainted" and are written into the `sourceFile` of the target (ugly stack traces "Mixins: ... :195") | Pack: up to 6 tainted classes per start (sporran, hollowhorizon, clientsort, polymorph, distractionfreerecipes) in 42 start-ups; our A1 mixins into `EmiApi` and `EmiScreenManager` (`remastered/info/mixin/EmiApiMixin.java`, `EmiScreenManagerMixin.java`) will add two more; known A1 follow-up | yes | `emi/mixinsupport/EmiMixinTransformation.java:58` (only `dev.emi.emi.mixin` is exempt) | visual | Also skip tainters starting with `dev.emi.remastered.`. | S |
| KB12 | Tag ingredients keep one private `List<EmiStack>` each; large packs use hundreds of MB | Pack bakes 112,836-115,851 recipes (latest.log:`Baked 115616 recipes`). Upstream [#1187](https://github.com/emilyploszaj/emi/issues/1187) (Sophisticated Storage + Chipped), [PR #933](https://github.com/emilyploszaj/emi/pull/933) (saves 80 MB in Craftoria) | yes | `emi/registry/EmiRecipes.java:96-106` (after the filter in `bake`), `emi/api/stack/TagEmiIngredient.java:43,68,91` | perf | After filtering, share equal stack lists: walk recipe inputs, `addOrGet` the `TagEmiIngredient` list in an `ObjectOpenCustomHashSet` with a strict strategy (item, components, remainder, chance, amount) and assign it back through a package-private setter. Ship PR #933's strategy class as a unit test target. | M |
| KB13 | Sodium logs "does not support optimized vertex writing code paths"; batched unlit items use the slow vertex path | Pack: 13 of 59 logs (once per session), Sodium 0.8.13 installed | yes | `emi/screen/StackBatcher.java:346-359` (`UnlitFacade.Consumer implements VertexConsumer`) | perf | Avoid the wrapper: render unlit batchable items into their own `BufferBuilder` and rewrite the normal bytes after `end()`, or implement Sodium's `VertexBufferWriter` through a mixin-free `compileOnly` stub and forward to the delegate when it supports it. Measure with the perf bench of Q3 before deciding. | M |
| KB14 | Hiding or unhiding a stack rewrites `emi.json` and re-bakes the filtered index synchronously on the render thread; "hide similar" loops the whole index | Upstream [#1114](https://github.com/emilyploszaj/emi/issues/1114), [#979](https://github.com/emilyploszaj/emi/issues/979), [#1166](https://github.com/emilyploszaj/emi/issues/1166). Pack `emi.json` has `hidden_stacks` | yes | `emi/runtime/EmiHidden.java:116-135` (`setVisibility` ends in `EmiPersistentData.save()` and `EmiStackList.bakeFiltered()`) | perf | Debounce: mark dirty and run `EmiPersistentData.save()` on a daemon scheduler 500 ms after the last change; let `bakeFiltered()` run once per batch (collect ids first, then one rebuild). | S |
| KB15 | One idle non-daemon thread leaks per resource reload; the reload thread itself is non-daemon | Code review: `Executors.newFixedThreadPool(1)` is created on every `reloadResources` and never shut down | yes | `emi/mixin/MinecraftClientMixin.java:34`; `emi/runtime/EmiReloadManager.java:96` (`setDaemon(false)`; the `clear()` branch at `:81` is daemon) | perf | Use one static daemon executor (or `MinecraftClient` as executor) for `thenRunAsync`; make the reload thread a daemon so closing the window does not wait for a 71 s reload. | S |
| KB16 | Fill-recipe overwrite and JEMI crashes reported upstream | [#1124](https://github.com/emilyploszaj/emi/issues/1124) fixed by PR #1125 (before the 1.1.24 tag, checks present in `network/FillRecipeC2SPacket.java:100-140`); [#904](https://github.com/emilyploszaj/emi/issues/904), [#949](https://github.com/emilyploszaj/emi/issues/949), [#969](https://github.com/emilyploszaj/emi/issues/969), [#1243](https://github.com/emilyploszaj/emi/issues/1243), [#1245](https://github.com/emilyploszaj/emi/issues/1245), [#1264](https://github.com/emilyploszaj/emi/issues/1264) are JEMI (JEI bridge) or a third-party mod | no | JEMI is only started when JEI is loaded (`emi/runtime/EmiReloadManager.java:161`); the pack has no JEI | crash | none | - |

## Module `loot` (`remastered/loot`, namespace `emi_loot`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB17 | Tooltip shows the raw key `emi_loot.function.copy_name.BLOCK_ENTITY` (the lang key is lower case) and the English text has a typo ("this blocks block entity") | Upstream [EMI_loot #154](https://github.com/fzzyhmstrs/EMI_loot/issues/154) (open) | yes | `remastered/loot/parser/function/CopyNameFunctionParser.java:19` (`source.name()`); `assets/emi_loot/lang/en_us.json:124` | visual | Build the key from `source.name().toLowerCase(Locale.ROOT)`; change the English text to "the block entity of this block". German (`de_de.json:124`) is already correct. | S |
| KB18 | If the world disappears while loot recipes are built, every recipe logs a full stack trace (13,790 block recipes) | Code review; the world-null storm of KB1 reaches the async builders. Pack shows `Built 0 mob loot recipes` once (`latest.log`) | yes | `remastered/loot/emi/LootEmiRegistration.java:60-78` and `:88-96` (per-entry `LOGGER.error(..., e)`) | perf | Log the first 5 failures with stack, then only a counter at the end of the batch; skip the whole batch when `MinecraftClient.world == null` (use KB1's helper). | S |
| KB19 | `NullPointerException: ... LootReceiver.getId() because "lr" is null`; the whole mob loot tab is skipped | Pack `latest.log:2288` (client list cleared while the async stream still read it) | fixed (loot, P7) | `remastered/loot/emi/LootEmiRegistration.java:62-72` snapshots the list and catches per entry | crash | none | - |
| KB20 | Slime and Magma Cube drawn huge in the mob loot tab | Upstream [EMI_loot #152](https://github.com/fzzyhmstrs/EMI_loot/issues/152) | fixed (loot, P7) | `remastered/loot/emi/MobLootRecipe.java:285-288` resizes the slime before measuring its bounding box (upstream measured first) | visual | none (check once in the tour) | - |
| KB21 | Mob drops that are coded, not loot tables (Piglin head from a charged creeper [#155](https://github.com/fzzyhmstrs/EMI_loot/issues/155), Botania [#156](https://github.com/fzzyhmstrs/EMI_loot/issues/156), loot injected by other mods [#158](https://github.com/fzzyhmstrs/EMI_loot/issues/158)) are not shown | Upstream, open | no (design limit of reading loot tables) | `remastered/loot/parser/LootTableParser.java` cannot see code-driven drops | wrong data | Optional feature for A8: a small data file of extra direct drops (charged creeper skulls), nothing generic. | M |

## Module `ores` (`remastered/ores`, namespace `emi_ores`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB22 | Ore tab can be built from an empty or partial feature list: the client merges the server payloads as they arrive, EMI reads them in a deferred step | Upstream [emi-ores #11](https://github.com/Abbie5/emi-ores/issues/11), [#8](https://github.com/Abbie5/emi-ores/issues/8) (closed); P5 review follow-up for A9. Not proven in the pack: the ore recipes are built in EMI's late-recipe step (10-70 s into a reload), so the join case is probably safe; the window is a `/reload` (the server re-sends, `receive(S2CSendBiomeInfoPayload)` empties the map first) | yes | `remastered/ores/client/OresFeatures.java:44-55`, `remastered/ores/client/OresEmiRegistration.java:46-52` (deferred step logs "No ore or geode features received" and gives up), `remastered/ores/client/OresClient.java:13-19` | wrong data | A9 batch-complete marker: the last payload sets `complete=true`; the deferred step waits (bounded, a few seconds) while `!complete`, and `OresFeatures` calls `EmiReloadManager.reload()` once if the marker arrives after a reload already ran. Check the existing "Registered N ore generation ..." line (`OresEmiRegistration.java:72`) after a join and after `/reload` first. | M |
| KB23 | `BiomeEmiStack` dereferences the world without a check when the registry is needed (deserializing a saved `biome:` favourite, `getId()` while baking) | Pack `latest.log:60639` (upstream class in the same place) | yes | `remastered/ores/emi/stack/BiomeEmiStack.java:131-133`, used at `:102` and `:144` | wrong data | Make `getBiomeRegistry()` return null when `world == null`; `getId()` returns null and `Serializer.create` returns `EmiStack.EMPTY` (which KB3 then preserves). | S |
| KB24 | Client kicked with "Invalid player data" when any mod registers an unbound placed feature | Upstream [emi-ores #48](https://github.com/Abbie5/emi-ores/issues/48), fixed in emi-ores e905cf4 | fixed (ores, P5) | `remastered/ores/network/OresFeaturesSender.java:57-59` (`hasKeyAndValue` check) | crash | none | - |
| KB25 | Players without the mod cannot join; "String too long" for big feature lists | Upstream [emi-ores #47](https://github.com/Abbie5/emi-ores/issues/47), [#42](https://github.com/Abbie5/emi-ores/issues/42), [#30](https://github.com/Abbie5/emi-ores/issues/30), [#24](https://github.com/Abbie5/emi-ores/issues/24), emi-ores ff47294 | fixed (ores, P5) | `OresFeaturesSender.java:42-45` (`canSend`), `:35-37,100-107` (100 features per payload, `isSendable` length check) | crash | none | - |
| KB26 | Mekanism and other custom configured features show no ore generation | Upstream [emi-ores #22](https://github.com/Abbie5/emi-ores/issues/22) | no (only vanilla `OreFeatureConfig` and `GeodeFeatureConfig` are understood) | `OresFeaturesSender.java:60` | wrong data | The A9 module could add a provider API; not a bug fix. | L |

## Module `effects` (`remastered/effects`, plugin id `emiffect`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB27 | Effects hidden in edit mode reappear after a restart (stack type string changed) | Upstream [EMIffect #34](https://github.com/Prismwork/EMIffect/issues/34), EMIffect 8b32a7d | fixed (effects, P2) | `remastered/effects/EffectEmiStack.java:125-186` keeps type `emiffect:effect` and reads the legacy `emiffect:` and `mob_effect:` spellings | wrong data | none | - |

## Module `enchanting` (`remastered/enchanting`, plugin id `emi_enchanting`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB28 | Books above the vanilla maximum level (surenchant mods, NoEnchantmentCapLevel) have no entry | Upstream [EMI_enchanting #16](https://github.com/fzzyhmstrs/EMI_enchanting/issues/16) | yes | `remastered/enchanting/recipe/EnchantmentCatalog.java:50,74-83` (books only up to `getMaxLevel()`) | wrong data | A2 already plans the real maximum: ask the cap mod's level through its API or scan loot and trades; additionally give the output books a comparison that ignores the stored level, so a book of any level opens the page. | M |
| KB29 | Pressing U on a book (for example Smite) opens the page of an enchantment that excludes it (Sharpness) first | Upstream [EMI_enchanting #1](https://github.com/fzzyhmstrs/EMI_enchanting/issues/1) | yes | `remastered/enchanting/recipe/EnchantmentRecipe.java:62-65` (own books are outputs only; the exclusion books are inputs) | wrong data | Add `info.books()` as catalysts (`getCatalysts`) so uses and recipes both list the own page; sort own page first with the category's sorter. | S |

## Module `professions` (`remastered/professions`, plugin id `emiprofessions`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB30 | Villager preview with a server-side or stale villager entity crashes the render | EMIProfessions commits 10f6d76, 47422ea (1.0.3 and later) | no | `remastered/professions/VillagerPreview.java:31-47` creates a client entity per world and disables itself after one render error | crash | none | - |
| KB31 | Modded trade factories that `ali` cannot read (Meadow `SellItemFactory`, Farmer's Delight `FDItemListing`, Sawmill, Supplementaries, Repurposed Structures, Clutter, Tide, Beach Party; 20 warnings each) | Pack logs, `[ali] Missing trade item listings for ...` | no | A4 rolls each factory with dummy entities (`remastered/professions/trade/TradeCollector.java:74-87`, per-factory catch at `:210`), so it does not depend on introspection | wrong data | none | - |

## Module `letsdo` (`remastered/letsdo`, plugin id `emi_letsdo_compat`)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB32 | `NoClassDefFoundError: net/satisfy/vinery/core/registry/RecipeTypesRegistry` kills the plugin and every sub-module after it | Pack: 21 lines in 17 logs (16 in the last three days); upstream [Let's Do compat #9](https://github.com/Pandaismyname1/Let-s-Do-EMI-Compat/issues/9), [#8](https://github.com/Pandaismyname1/Let-s-Do-EMI-Compat/issues/8) | fixed (letsdo, P6) | `remastered/letsdo/LetsDoContent.java:63-73` (sub-modules loaded by name after `isModLoaded`, `LinkageError` caught per sub-module) | crash | none | - |
| KB33 | Duplicate tabs next to the native Vinery, Farm and Charm and Bakery plugins | `.upstream/compat/compat-report.md` section 6 | fixed (letsdo, P6) | `remastered/letsdo/LetsDoContext.java` (dedupe against native categories) | wrong data | none | - |
| KB34 | `Reference map 'emi-letsdo-compat-common-refmap.json' ... could not be read` at every start | Pack: 58 of 59 logs, from the old standalone jar | fixed (letsdo, P6) | the port has no such mixin config | visual | none (disappears when the jar is swapped) | - |

## Module `distraction_free` (`remastered/distractionfree`, no plugin)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB35 | Mixin crashes with JEI versions (`screenPropertiesCache`, missing `configButton`) | DFR [#18](https://github.com/txnimc/DistractionFreeRecipes/issues/18), [#21](https://github.com/txnimc/DistractionFreeRecipes/issues/21), [#24](https://github.com/txnimc/DistractionFreeRecipes/issues/24), [#26](https://github.com/txnimc/DistractionFreeRecipes/issues/26), [#9](https://github.com/txnimc/DistractionFreeRecipes/issues/9), [#11](https://github.com/txnimc/DistractionFreeRecipes/issues/11) | fixed (distraction_free, P1) | module has no mixins; hooks are plain calls from `emi/screen/EmiScreenManager.java:610,633` | crash | none | - |
| KB36 | Hidden item icons can still be clicked | DFR [#2](https://github.com/txnimc/DistractionFreeRecipes/issues/2) | fixed (distraction_free, A10) | `emi/screen/EmiScreenManager.java:453,565,859` use `isVisible()` which includes `DistractionFree.isHidden` (`:1680-1682`) | visual | none | - |
| KB37 | Incompatible with EMI 1.1.23 | DFR [#23](https://github.com/txnimc/DistractionFreeRecipes/issues/23) | fixed (distraction_free, P1) | built into the core | crash | none | - |

## Pack interplay (third-party mods, config, logs)

| id | title | source | affects us | our file:line | severity | fix proposal | effort |
|---|---|---|---|---|---|---|---|
| KB38 | ali blocks the EMI reload thread up to 63 s waiting for server data after the connection died ("Data not ready ... Attempt 1/3", "CRITICAL: Could not fetch loot data from server after 3 attempts", `[EMI] Reloaded plugin from ali in 62966ms`) | Pack `latest.log:2209-60433` | yes | `emi/runtime/EmiReloadManager.java:187-216` (plugin loop has no timeout or interruption) | perf | The KB1 disconnect hook should also `interrupt()` the reload thread, so ali's blocking wait ends early (ali's own logout handler already cancels its future, `CoreClientRegistry.loggingOut`, latest.log:60281, but the reload was already past that point); optionally show a plugin that exceeds 30 s as "slow" in the plugin manager (`PluginManager.afterRegister` already measures it). | S |
| KB39 | clientsort waits for the EMI reload ("Search order update blocked by EMI reload, waiting...") so long reloads delay its sorting | Pack logs | no (follows from KB1, KB12) | indirect through `EmiReloadManager.java` | perf | Follows from KB1, KB12 and the Q3 reload work; nothing separate. | - |
| KB40 | Foreign mixins into EMI that our changes must not break: ali (`RecipeScreen` mouseScrolled and mouseDragged), certain_questing_additions (`RecipeScreen`), clientsort (`ReloadWorker.run`), polymorph (`EmiRecipeFiller.performFill`), sporran (`EmiAgnosFabric`), iceberg (`EmiScreenManager`) | `.upstream/compat/compat-report.md` section 2 and 4 | no (constraint, not a bug) | `emi/runtime/EmiReloadManager.java` (`ReloadWorker.run`), `emi/registry/EmiRecipeFiller.java` | crash | KB1, KB2, KB4, KB9 must keep these method signatures; early `return` from `run()` is fine for clientsort (`@Inject RETURN`), but `break out of the do-loop` must still reach the end of `run()`. | - |
| KB41 | Duplicate functionality: ali vs `loot` (block, mob and gameplay drops), jeed vs `effects` (effect info tab and effect stack) | compat-report section 6; D8 | no (design decision D8) | `emi/runtime/PluginManager` (plugin manager) | wrong data | No code fix. Document in the pack notes which side to switch off in the plugin manager (ali keeps trades, our module keeps chests and archaeology). | - |
| KB42 | `framedblocks_fabric` plugin fails with `IllegalArgumentException: a (-1) must be >= 0` in `FramingSawRecipeCalculation.getMaterialLCM` | Pack `2026-10-15-6.log` (1 line) | no (third-party recipe data) | - | wrong data | Report to FramedBlocks; the plugin manager already shows the error marker. | - |

## Watch list (unverified in our build, no confirmed file:line)

| id | title | source | note |
|---|---|---|---|
| W1 | Amounts drawn behind the item icon in recipe-tree cost lists | upstream [#1179](https://github.com/emilyploszaj/emi/issues/1179), [#1159](https://github.com/emilyploszaj/emi/issues/1159), [#987](https://github.com/emilyploszaj/emi/issues/987), [#1239](https://github.com/emilyploszaj/emi/issues/1239) | check once in a tour screenshot (`BoMScreen` draw order and z) |
| W2 | Partial recipe defaults are not saved after leaving the world | upstream [#939](https://github.com/emilyploszaj/emi/issues/939) | `emi/bom/BoM.java:55-76` writes `resolutions`, so it may already work; test with a multi-output recipe |
| W3 | Favourited recipe hover clashes with the vanilla recipe book | upstream [#1017](https://github.com/emilyploszaj/emi/issues/1017) (Fabric 1.21.1) | needs a screenshot to judge |
| W4 | Tooltip of remaining items from a tag ingredient gets very long | upstream [#1048](https://github.com/emilyploszaj/emi/issues/1048) | cosmetic |
| W5 | "Untranslated alias" warnings in non-dev mode | upstream [#1138](https://github.com/emilyploszaj/emi/issues/1138) | `emi/search/EmiSearch.java:104-105`; no hit in the pack logs |
| W6 | A mod that mutates stacks during the bake destroys recipes | upstream [#1158](https://github.com/emilyploszaj/emi/issues/1158) | EMI already warns at `emi/registry/EmiRecipes.java:252` |
| W7 | Performance of CIT item rendering in the index | upstream [#1254](https://github.com/emilyploszaj/emi/issues/1254) | no CIT mod in the pack |

## Evidence: EMI-related pack log messages

Counts over the 59 logs, only messages that name EMI, an EMI plugin, or one of the 7 add-ons.

| message | mod | count |
|---|---|---|
| EMI caught an exception while baking search for `<stack>` (NullPointerException; world null) | emi | 25,925 (all in `latest.log`) |
| Exception thrown when parsing vanilla recipe `<id>` (NullPointerException; world null) | emi | 5,726 in `latest.log`, 151 in 2026-10-04-2, 3 each in 2026-10-03-2/4/5/6 |
| Exception thrown registering repair recipes (AIOOBE) | emi | 1,204 lines, 37 logs, 28 per reload |
| Exception loading plugin provided by `<id>` (world null) | emi + 16 plugins | 17 in the 71 s reload; plugin `emi` also in 5 older logs |
| Exception thrown when reloading fuel / composting step (world null) | emi | 4 + 4 (2026-10-03-2, 2026-10-04-2) |
| Exception thrown setting remainders for ... (AIOOBE) | emi | 33 (2026-10-03-4) |
| Exception loading plugin provided by emi_letsdo_compat (NoClassDefFoundError, `RecipeTypesRegistry`) | letsdo (old jar) | 21 in 17 logs |
| Error parsing NBT in deserialized stack (registry access, world null) | emi, emi_ores | 3 |
| Critical error encountered while creating all Mob Recipes ("lr" is null) | emi_loot | 1 |
| Reference map 'emi-letsdo-compat-common-refmap.json' could not be read | letsdo (old jar) | 58 |
| `<class>` is tainted by `<mixin>` (sporran, hollowhorizon, clientsort, polymorph, distractionfreerecipes) | emi | 42 start-ups, 7 lines each |
| StackBatcher$UnlitFacade$Consumer does not support optimized vertex writing | emi + Sodium | 13 |
| `[ali]` Timeout / "Could not fetch loot data" / missing trade listings | ali | 2 / 1 / 20 per start |
| Exception loading plugin provided by framedblocks_fabric (`gcd` of -1) | framedblocks | 1 |
| Crash report 2026-10-17 13:54 (glintsmith vs modelfix mixin) | not EMI | 1 |

No log line from `emiffect`, `emiprofessions`, `emi_enchanting` or `distraction_free_recipes` is a warning or error (apart from the world-null cascade, which touches `emi_ores`, `emi_letsdo_compat` and `emi` itself).
