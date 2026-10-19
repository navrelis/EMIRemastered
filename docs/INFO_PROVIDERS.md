# Item Info providers

The `info` module adds the **Item Info screen**: hover any stack (EMI index, favourites, inventory slot, recipe screen
slot, or a slot in the Item Info screen itself) and press the "Open Item Info" key (default `I`, setting
`info.open-info`) to get one overview of

- **How to Get**: recipes producing the stack, grouped by EMI category (count, example inputs; a click opens EMI's
  recipe screen on that tab), plus whatever providers add (loot, world generation, trades...). If nothing counts as a
  source, the page shows **No Known Source** (setting `info.show-no-source`).
- **Used For**: recipes using the stack, grouped the same way, and the categories it is a workstation for.
- **Details**: fuel burn time, composting chance, advancements with the item as icon, EMI info pages, item tags, and whatever providers add.

Everything on the page comes from **providers**. The built-in ones read EMI's recipe index; other modules (loot, ores,
effects, enchanting, stats, ...) and EMI plugins of other mods add their own sections through the API in
`dev.emi.remastered.info.api`.

## API at a glance

| Type | Purpose |
|---|---|
| `ItemInfoProvider` | `getId()`, `getOrder()`, `getModuleId()`, `appliesTo(EmiStack)`, `coveredCategories(EmiStack)`, `collect(EmiStack, InfoCollector)`, `onReload()` |
| `ItemInfoRegistry` | `register(provider)` (same id replaces), `unregister(id)`, `getProviders()`, `getVersion()` |
| `InfoCollector` | `getStack()`, `addSection(id, kind, title)`, `hasSource()` |
| `InfoSection` | building blocks: `icon`, `source`, `text`, `stacks`, `link`, `bar`, `entry`, `element` (advanced) |
| `InfoSection.Kind` | `OBTAIN` ("How to Get"), `USE` ("Used For"), `DETAILS` |
| `InfoEntry` | compact row: `detail`/`count`, `stacks`, `link`, `tooltip` |
| `InfoLink` | click targets: `recipe(id or EmiRecipe)`, `recipes(list)`, `category(id or category)`, `recipesOf(stack, focus)`, `usesOf(stack, focus)`, `info(stack)`; `open()` |
| `InfoElement` | advanced hook for a self-rendering element |

## Example

```java
public final class LootInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Identifier.of("emi_loot", "chest_loot");

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "loot";                              // skipped while the loot module is switched off
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return stack.getKey() instanceof Item;      // cheap check, no lookups
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		List<ChestDrop> drops = LootIndex.byItem(stack);   // your own index, built once per EMI reload
		if (drops.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(ID, InfoSection.Kind.OBTAIN, Text.translatable("emi_loot.info.chests"))
			.icon(EmiStack.of(Items.CHEST));
		for (ChestDrop drop : drops) {
			section.entry(EmiStack.of(Items.CHEST), drop.tableName())
				.detail(Text.literal(drop.percent() + "%"))
				.link(InfoLink.recipe(drop.recipeId()));
		}
	}

	@Override
	public void onReload() {
		LootIndex.clear();
	}
}

// LootContent.initClient():
ItemInfoRegistry.register(new LootInfoProvider());
```

From an EMI plugin of another mod (`EmiPlugin.register` runs on every EMI reload; re-registering the same id simply
replaces the old instance):

```java
@Override
public void register(EmiRegistry registry) {
	ItemInfoRegistry.register(new MyModInfoProvider());
}
```

## Rules

- **Threading.** `register`/`unregister` are thread safe (EMI plugins run on EMI's reload thread). `appliesTo`,
  `collect` and `onReload` are only called on the client render thread, after EMI finished loading.
- **Lifecycle.** Pages are computed lazily, the first time a stack's page is opened, and cached (64 pages, LRU). The
  cache is dropped on every EMI reload (new recipe manager), when a provider is (un)registered, when a module is
  switched and when `info.show-no-source` changes. `onReload()` runs once per EMI reload before the first `collect`.
- **Performance.** `collect` runs once per stack and reload while the player waits for the screen: use indexes
  (`EmiApi.getRecipeManager().getRecipesByOutput/getRecipesByInput`, your own maps built in `onReload` or your EMI
  plugin), never a scan over every recipe or registry entry per call.
- **Module gate.** `getModuleId()` returns the module id (`loot`, `ores`, ...); the provider is skipped while that
  module is switched off. The whole screen and key are off while the `info` module is off.
- **Order.** Sections are shown by kind (`OBTAIN`, `USE`, `DETAILS`), then by provider order (`getOrder()`, ascending,
  then id), then in the order they were added. `ORDER_RECIPES` (0) is the built-in recipe provider, `ORDER_DEFAULT`
  (1000) the default, `ORDER_LAST` the "no known source" marker.
- **Covered categories.** If a provider lists the recipes of an EMI category itself (trades, effect pages, ...), it
  returns that category's id from `coveredCategories(stack)` and the built-in recipe provider leaves the category's
  "Produced by" and "Used in" rows out for that stack, so the same recipes are not shown twice. Only declare a category
  while the provider really shows that data for the stack: return an empty set when its setting is off or it has nothing
  for the stack, and the built-in row stays. The ids of all active providers (module on, `appliesTo` true) are merged
  once per page build; a provider that throws covers nothing. Categories are matched by `EmiRecipeCategory.getId()`.
- **Sources.** An `OBTAIN` section counts as a source unless it calls `source(false)` (use that for sections that
  only describe the stack or turn it into itself). `InfoCollector.hasSource()` tells whether one exists so far.
- **Rendering.** Providers do not draw; the screen lays out the building blocks (scrolling, every GUI scale, EMI
  style). Uncoloured text is drawn in EMI's dark grey; give `Text` a colour for emphasis. Stacks in `stacks(...)` and
  `entry(...).stacks(...)` are real EMI slots (tooltips, recipes/uses/favourite/info keys); their amount and chance
  come from the `EmiIngredient`. Only use `element(InfoElement)` when no building block fits.
- **Errors.** An exception in a provider is logged once per reload and shown as an error line; other providers still
  run. A `NoClassDefFoundError` (optional mod missing) is caught the same way.
- **Text.** Use lang keys in your module's namespace (en_us and de_de).

## Built-in providers (namespace `emi_info`)

| id | order | sections |
|---|---|---|
| `emi_info:recipes` | 0 | `emi_info:produced_by` (OBTAIN), `emi_info:used_in`, `emi_info:workstation` (USE), `emi_info:information`, `emi_info:tags` (DETAILS) |
| `emi_info:fuel` | 990 | `emi_info:fuel` (DETAILS): burn time from Fabric's `FuelRegistry` |
| `emi_info:composting` | 1000 | `emi_info:composting` (DETAILS): `ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE` |
| `emi_info:advancements` | 1500 | `emi_info:advancements` (DETAILS): advancements the client knows whose display icon is the item (components ignored; criteria are not synced), with done/open marker, frame and description; hidden ones only once done; a click opens the advancement screen on their tab. Setting `info.show-advancements` |
| `emi_info:no_source` | last | `emi_info:no_source` (OBTAIN, not a source) when `hasSource()` is false |

"Produced by" skips EMI's tag, ingredient, resolution and fuel pseudo categories; recipes that need the stack itself as
an input (repairing, cloning) are listed but do not count as a source. Info pages (`emi:info`) go to Details.

## Providers of other modules

| id | order | sections | covers |
|---|---|---|---|
| `emiprofessions:trades` (professions, setting `professions.item-info`) | 1000 | `emiprofessions:sold_by` (OBTAIN, trades that give the stack; "Earned from Villagers" for the emerald), `emiprofessions:bought_by` (USE, trades that take it): profession, level, price, max uses; many trades are grouped per profession | `emiprofessions:villager_trades`, `emiprofessions:wandering_trader` for stacks that have trades |
| `emiffect:effects` (effects, setting `effects.item-info`) | 1000 | `emiffect:item_effects` (DETAILS: effects of potions, tipped arrows, stew, ominous bottles with level and duration; foods are left to the stats Food section); for effect stacks `emiffect:source/*` (OBTAIN), `emiffect:cures/*`, `emiffect:attributes/*` (DETAILS) | `emiffect:status_effect_info` for items with effects and effects with sources |

## Opening the screen from code

`InfoLink.info(stack).open()` opens the Item Info screen for a stack on top of the current screen (Escape returns).
