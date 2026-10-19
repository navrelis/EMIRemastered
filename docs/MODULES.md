# EMI Remastered modules

EMI Remastered is EMI (mod id `emi`) plus seven built-in modules. Each former add-on is a module under
`dev.emi.remastered.<id>`. A module keeps its old resource namespace (lang, textures, data) and its old EMI plugin id.

| id | package | class prefix | namespace = plugin id | EMI plugin |
|---|---|---|---|---|
| `loot` | `dev.emi.remastered.loot` | `Loot` | `emi_loot` | yes |
| `ores` | `dev.emi.remastered.ores` | `Ores` | `emi_ores` | yes |
| `effects` | `dev.emi.remastered.effects` | `Effects` | `emiffect` | yes |
| `enchanting` | `dev.emi.remastered.enchanting` | `Enchanting` | `emi_enchanting` | yes |
| `professions` | `dev.emi.remastered.professions` | `Professions` | `emiprofessions` | yes |
| `letsdo` | `dev.emi.remastered.letsdo` | `LetsDo` | `emi_letsdo_compat` | yes |
| `distraction_free` | `dev.emi.remastered.distractionfree` | `DistractionFree` | `distraction_free_recipes` | no (EMI core screen code) |
| `info` | `dev.emi.remastered.info` | `Info` | `emi_info` | no (new module: Item Info screen, provider API in `docs/INFO_PROVIDERS.md`) |

## Files of a module

| File | Owner | Purpose |
|---|---|---|
| `<Prefix>Module.java` | framework | Descriptor (id, lang namespace, settings class, default on, needs server, apply mode), the module switch `@ConfigValue("modules.<id>") public static boolean enabled`, `init()` -> `<Prefix>Content.init()`, `initClient()` -> `<Prefix>Content.initClient()` |
| `<Prefix>EmiPlugin.java` | framework | `emi` entrypoint, extends `ModuleEmiPlugin`: plugin id = old add-on id, returns early while the module is off, otherwise calls `<Prefix>Content.register(registry)` |
| `<Prefix>Content.java` | porter | `public static void init()` (both sides), `initClient()` (client, module on), `register(EmiRegistry)` (EMI reload, module on) |
| `<Prefix>Settings.java` | porter | Settings: plain class, `public static` fields with EMI's `@ConfigValue("<id>.<key>")` |
| `mixin/` + `src/main/resources/emi_remastered_<id>.mixins.json` | porter | Mixins of the module (config already listed in fabric.mod.json) |
| `assets/<namespace>/lang/en_us.json`, `de_de.json` | porter | All lang keys of the module |
| `module/Modules.java` (one line), `fabric.mod.json`, `assets/emi/lang/*` (switch keys) | framework | Registration |

## Lifecycle

- `<Prefix>Content.init()`: runs on **both sides, always** (dedicated server, integrated server, client), whatever the
  client switch says. A server has no EMI config. Register payload types (`PayloadTypeRegistry.playS2C()/playC2S()`),
  server events (`ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS`, ...) here.
- `<Prefix>Content.initClient()`: client only, after `config/emi.css` is loaded, **only if the module is on at startup**.
  Register client receivers (`ClientPlayNetworking.registerGlobalReceiver`), key binds, client events here. Put
  client-only code in classes that are only reached from here (a dedicated server must never load them).
- `<Prefix>Content.register(EmiRegistry)`: every EMI reload while the module is on and its plugin is not switched off
  in the plugin manager. `ModuleEmiPlugin` also gates `initialize`.
- Server -> client data: the server sends only if `ServerPlayNetworking.canSend(player, ID)` (clients without the
  module switched on, or without the mod, do not register the receiver and must not be kicked). Clear client-side
  holders on `ClientPlayConnectionEvents.DISCONNECT`.
- Module on/off state on the client: `<Prefix>Module.enabled` (or `Modules.<ID>.isEnabled()`). Never read it in
  server code.

## Settings

```java
public final class LootSettings {
	@Comment("Decimal places of drop chances.")          // file comment and fallback tooltip
	@ConfigValue("loot.chance-decimal-places")
	public static int chanceDecimalPlaces = 1;

	@ConfigGroup("loot.display")                         // optional visual sub group (first field) ...
	@ConfigValue("loot.compact-chests")
	public static boolean compactChests = false;

	@ConfigGroupEnd                                      // ... and its last field
	@ConfigValue("loot.compact-mobs")
	public static boolean compactMobs = false;
}
```

- Keys are exactly `<id>.<key>` (one dot); `<key>` lowercase with `-`. The group must be the module id.
- Types with a widget: `boolean`, `int`, `double`, `String`, any `enum` implementing EMI's `ConfigEnum`, `EmiBind`
  (key binds), `IntGroup`, `ScreenAlign`, `SidebarPages`, `SidebarSubpanels`. Strings must not contain `"` or `;`.
- `@ConfigFilter("<id>.<key>")` on a static `Predicate<T>` field of the settings class filters enum choices.
- Fields must be `public static`, not final (except mutable objects such as `EmiBind`), and their class initialiser
  must not touch Minecraft classes (it runs very early and in unit tests).
- Registration is automatic: the framework reads `<Prefix>Module`'s settings class. Settings are loaded from and saved
  to `config/emi.css` (group `#<id> { ... }`), shown in EMI's config screen after EMI's own groups (group `modules`
  first, then each module group in registry order, each with a jump button). Unknown keys stay in the file.
- Values are read live; when a setting needs an EMI reload, say so in its tooltip.

### Lang keys (EMI's rules, verified in `ConfigScreen`)

All `-` in the config key become `_` in the lang key:

| What | Lang key | Example for `loot.chance-decimal-places` |
|---|---|---|
| Group title | `config.emi.group.<id>` | `config.emi.group.loot` |
| Setting name | `config.emi.<id>.<key>` | `config.emi.loot.chance_decimal_places` |
| Tooltip (optional, `\n` = new line; falls back to `@Comment`) | `config.emi.tooltip.<id>.<key>` | `config.emi.tooltip.loot.chance_decimal_places` |
| Sub group title (`@ConfigGroup("loot.display")`) | `config.emi.group.<id>.<sub>` | `config.emi.group.loot.display` |

Lang keys are global in Minecraft: keys from `assets/emi_loot/lang/en_us.json` are found by EMI's config screen like
keys from `assets/emi/lang/en_us.json`. Every module puts **all** its keys into its own namespace's `en_us.json` and
`de_de.json`. The module switch keys (`config.emi.modules.<id>`, `config.emi.tooltip.modules.<id>`,
`config.emi.group.modules`) are in EMI's own lang files (framework-owned). `config.emi.group.<id>` is also the
module's display name in the plugin manager.

## Plugin ids and the plugin manager

- EMI lists plugins by id. Third-party plugins use their mod id; module plugins use the old add-on id through
  `dev.emi.remastered.api.IdentifiedEmiPlugin` (resolved in `EmiAgnosFabric.getPluginsAgnos`). Log lines:
  `Initialized plugin from emi_loot in 0ms`, `Reloaded plugin from emi_loot in 0ms`.
- `plugins.disabled` in emi.css (comma separated plugin ids) is applied in `EmiReloadManager.ReloadWorker.run` after
  `EmiAgnos.getPlugins()` (after Sporran's additions): `Skipping plugin from <id> (disabled in the plugin manager)`.
  `emi` cannot be disabled.
- Plugin Manager screen: EMI config screen -> "Plugins" button (next to "Presets"). Per plugin: id, mod, last reload
  time, recipes/stacks added (incl. deferred recipes), error marker; on/off toggle; closing it after a change saves and
  starts an EMI reload.

## Rules for porting agents

1. Work only in `dev.emi.remastered.<id>/` (your `<Prefix>Content`, `<Prefix>Settings`, `mixin/`, any new classes),
   `src/main/resources/emi_remastered_<id>.mixins.json` and `src/main/resources/assets/<namespace>/`. Keep the
   resource namespace of the add-on (textures, models, data, lang); keep serializer/stack type ids (e.g.
   `emiffect:effect`) so `emi.json` favourites stay valid.
2. Do not edit shared files: `fabric.mod.json`, `build.gradle`, `emi.accesswidener`, `EmiConfig.java`,
   `assets/emi/lang/*`, `<Prefix>Module.java`, `<Prefix>EmiPlugin.java`, `module/*`, `config/*`, `plugin/*`. Need a
   change there? Report it to the lead.
3. Never rename, move or re-sign anything in `dev.emi.emi` (other mods mix into or call it).
4. Keep the exact signatures of `<Prefix>Content`: `public static void init()`, `public static void initClient()`,
   `public static void register(dev.emi.emi.api.EmiRegistry registry)`.
5. Settings only as `public static` `@ConfigValue("<id>.<key>")` fields in `<Prefix>Settings`; no other config
   library, no own config file. Translate every setting (name, and tooltip if useful) in en_us **and** de_de.
6. No access widener entries: use mixin `@Accessor`/`@Invoker` in your `mixin` package (decision D6), listed in your
   mixin json (`client` for client-only targets, `mixins` for both sides). `defaultRequire` is 1, so every injection
   must apply.
7. Common code (`init`) must not load client classes; server code must not read the module switch or other client
   settings.
8. Payloads: ids in your namespace (`emi_loot:...`), registered in `init()`, receivers in `initClient()`, sending
   guarded by `ServerPlayNetworking.canSend`.
9. All user-visible strings via lang keys in en_us and de_de (proper German).
10. Yarn names only; no new libraries; Java 21. Unit tests (JUnit 5, `src/test`) for pure logic, no Minecraft
    bootstrap. `gradlew build` and `gradlew test` must pass (`ModulesTest` checks the contract, including lang keys of
    every setting).

## Adding a new module (framework)

1. Package `dev.emi.remastered.<id>` with `<Prefix>Module` (copy one), `<Prefix>Settings`, `<Prefix>Content`,
   optionally `<Prefix>EmiPlugin`.
2. One line in `module/Modules.java`: `public static final XModule X = register(new XModule());`.
3. fabric.mod.json: `emi` entrypoint (if any) and `emi_remastered_<id>.mixins.json`.
4. Switch keys in `assets/emi/lang/{en_us,de_de}.json`; a 16x16 icon at index `registry position + 1` in
   `assets/emi_remastered/textures/gui/config_jump.png`.
