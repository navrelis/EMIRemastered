package dev.emi.remastered.devtour;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiFavorites;
import dev.emi.emi.runtime.EmiHidden;
import dev.emi.emi.runtime.EmiPersistentData;
import dev.emi.emi.runtime.EmiSidebars;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.registry.Registry;

/**
 * Dev-only: writes EMI's baked state (index, recipes, recipe lookups, tags, BoM defaults, search results) as
 * deterministic text plus a SHA-256 per section, so two runs (e.g. before/after an optimisation, same world) can be
 * compared with {@code diff} / by hash. Map-backed data is sorted, everything with a defined order keeps it.
 */
public final class StateDump {
	private static final String[] QUERIES = {"iron", "sword", "@minecraft", "#minecraft:logs", "$sharpness", "item_00",
		"-stone", "a", "pick | axe", "emi_bench"};

	private final DynamicOps<JsonElement> ops;
	private final Map<ComponentChanges, String> componentCache = new java.util.HashMap<>();
	private final Map<EmiRecipe, Integer> recipeIndex = new IdentityHashMap<>();
	private final Map<EmiRecipeCategory, Integer> categoryIndex = new IdentityHashMap<>();
	private boolean normalized;
	private boolean sortAllIds;
	private final Map<String, String> hashes = new LinkedHashMap<>();
	private final Path dir;

	private StateDump(Path dir) {
		this.dir = dir;
		this.ops = MinecraftClient.getInstance().world.getRegistryManager().getOps(JsonOps.INSTANCE);
	}

	/** Writes the dump into {@code dir} and returns the section hashes. */
	public static Map<String, String> write(Path dir) throws Exception {
		Files.createDirectories(dir);
		StateDump d = new StateDump(dir);
		d.run();
		return d.hashes;
	}

	private void run() throws Exception {
		EmiRecipeManager manager = EmiRecipes.manager;
		List<EmiRecipe> recipes = manager.getRecipes();
		for (int i = 0; i < recipes.size(); i++) {
			recipeIndex.put(recipes.get(i), i);
		}
		for (EmiRecipeCategory c : manager.getCategories()) {
			categoryIndex.putIfAbsent(c, categoryIndex.size());
		}

		List<String> out = new ArrayList<>();
		for (EmiStack s : EmiStackList.stacks) {
			out.add(stack(s));
		}
		section("stacks", out);

		out = new ArrayList<>();
		for (EmiStack s : EmiStackList.filteredStacks) {
			out.add(stack(s));
		}
		section("filtered_stacks", out);

		out = new ArrayList<>();
		for (EmiStack s : EmiStackList.stacks) {
			out.add((EmiHidden.isDisabled(s) ? "D" : "-") + (EmiHidden.isHidden(s) ? "H" : "-") + " "
				+ EmiStackList.getIndex(s) + " " + stack(s));
		}
		section("stack_flags", out);

		out = new ArrayList<>();
		for (EmiRecipe r : recipes) {
			out.add(recipe(r));
		}
		section("recipes", out);

		out = new ArrayList<>();
		for (EmiRecipeCategory c : manager.getCategories()) {
			out.add("category " + c.getId() + " workstations=" + ingredients(manager.getWorkstations(c)));
			for (EmiRecipe r : manager.getRecipes(c)) {
				out.add("  " + recipeKey(r));
			}
		}
		section("categories", out);

		// Upstream builds the recipe lookups by iterating a HashMap keyed by recipe category (identity hash), so the
		// order ACROSS categories differs between JVM runs; *_norm sections stable-sort each list by category order.
		for (boolean norm : new boolean[] {false, true}) {
			normalized = norm;
			String suffix = norm ? "_norm" : "_raw";
			section("by_input" + suffix, lookup(manager, "byInput"));
			section("by_output" + suffix, lookup(manager, "byOutput"));
			List<String> lookups = new ArrayList<>();
			for (EmiStack s : EmiStackList.stacks) {
				lookups.add(stack(s) + " in=" + keys(manager.getRecipesByInput(s)) + " out=" + keys(manager.getRecipesByOutput(s)));
			}
			section("index_lookups" + suffix, lookups);
			TreeMap<String, String> sorted = new TreeMap<>();
			for (Map.Entry<EmiStack, List<EmiRecipe>> e : EmiRecipes.byWorkstation.entrySet()) {
				sorted.merge(stack(e.getKey()), keys(e.getValue()), (a, b) -> a + " | " + b);
			}
			List<String> workstations = new ArrayList<>();
			sorted.forEach((k, v) -> workstations.add(k + " -> " + v));
			section("by_workstation" + suffix, workstations);
		}
		normalized = false;
		stableSections(manager, recipes);

		List<String> tags = new ArrayList<>();
		for (EmiTagKey<?> key : EmiTags.TAGS) {
			tags.add(key.raw().registry().getValue() + " #" + key.id() + " = " + stacks(EmiTags.getValues(key)));
		}
		section("tags", tags);

		List<String> sortedTags = new ArrayList<>();
		List<Registry<?>> registries = new ArrayList<>(EmiTags.ADAPTERS_BY_REGISTRY.keySet());
		registries.sort(Comparator.comparing(r -> r.getKey().getValue().toString()));
		for (Registry<?> registry : registries) {
			sortedTags.add("registry " + registry.getKey().getValue());
			for (EmiTagKey<?> key : EmiTags.getTags(registry)) {
				sortedTags.add("  #" + key.id() + " " + key.stream().count());
			}
		}
		section("tags_by_size_raw", sortedTags);
		// upstream sorts by size only, so equal-size tags keep a HashMap order (identity hashes): sort those by id
		List<String> normTags = new ArrayList<>();
		for (Registry<?> registry : registries) {
			normTags.add("registry " + registry.getKey().getValue());
			List<EmiTagKey<?>> keys = new ArrayList<>(EmiTags.getTags(registry));
			keys.sort(Comparator.<EmiTagKey<?>>comparingLong(k -> -k.stream().count()).thenComparing(k -> k.id().toString()));
			for (EmiTagKey<?> key : keys) {
				normTags.add("  #" + key.id() + " " + key.stream().count());
			}
		}
		section("tags_by_size_norm", normTags);

		TreeMap<String, String> sorted = new TreeMap<>();
		for (Map.Entry<EmiIngredient, EmiRecipe> e : BoM.defaultRecipes.entrySet()) {
			sorted.put(ingredient(e.getKey()), recipeKey(e.getValue()));
		}
		List<String> bom = new ArrayList<>();
		sorted.forEach((k, v) -> bom.add(k + " -> " + v));
		section("bom_defaults", bom);

		List<String> search = new ArrayList<>();
		for (String q : QUERIES) {
			EmiSearch.CompiledQuery compiled = new EmiSearch.CompiledQuery(q);
			List<String> hits = new ArrayList<>();
			for (EmiStack s : EmiStackList.stacks) {
				if (compiled.test(s)) {
					hits.add(stack(s));
				}
			}
			search.add("query '" + q + "': " + hits.size() + " " + hash(hits));
			for (int i = 0; i < Math.min(40, hits.size()); i++) {
				search.add("  " + hits.get(i));
			}
		}
		section("search", search);

		// what EmiPersistentData.save() would write to emi.json (built in memory, the file is not touched)
		JsonObject persistent = new JsonObject();
		persistent.add("favorites", EmiFavorites.save());
		EmiSidebars.save(persistent);
		persistent.add("recipe_defaults", BoM.saveAdded());
		persistent.add("hidden_stacks", EmiHidden.save());
		section("persistent_data", List.of(EmiPersistentData.GSON.toJson(persistent).split("\\R")));

		List<String> summary = new ArrayList<>();
		hashes.forEach((k, v) -> summary.add(k + " " + v));
		Files.write(dir.resolve("hashes.txt"), summary, StandardCharsets.UTF_8);
	}

	/**
	 * Run-to-run noise that does not come from the reload code under test: emi_loot samples loot tables (random
	 * outputs), emi_ores builds its biome lists from hash sets, upstream EMI registers world interaction and tag
	 * recipes in identity-hash order (which also shifts every later recipe index). The *_stable sections name recipes
	 * by id instead of index, compare emi_loot/emi_ores recipes by id only, leave emi_loot recipes out of the lookups
	 * and sort the order-noise categories by id; every other order is kept. Two runs of the same build must give
	 * identical *_stable hashes.
	 */
	private static boolean orderNoise(EmiRecipeCategory category) {
		String id = category.getId().toString();
		return id.equals("emi:world_interaction") || id.equals("emi:tag") || category.getId().getNamespace().equals("emi_loot");
	}

	private static boolean contentNoise(EmiRecipe recipe) {
		String namespace = recipe.getCategory().getId().getNamespace();
		return namespace.equals("emi_loot") || namespace.equals("emi_ores");
	}

	private void stableSections(EmiRecipeManager manager, List<EmiRecipe> recipes) throws Exception {
		List<String> out = new ArrayList<>();
		for (EmiRecipe r : recipes) {
			out.add(contentNoise(r) ? r.getId() + " | " + r.getClass().getName() + " | " + r.getCategory().getId() : recipe(r));
		}
		out.sort(null);
		section("recipes_stable", out);

		out = new ArrayList<>();
		for (EmiRecipeCategory c : manager.getCategories()) {
			out.add("category " + c.getId() + " workstations=" + ingredients(manager.getWorkstations(c)));
			List<String> ids = new ArrayList<>();
			for (EmiRecipe r : manager.getRecipes(c)) {
				ids.add("  " + r.getId());
			}
			if (orderNoise(c)) {
				ids.sort(null);
			}
			out.addAll(ids);
		}
		section("categories_stable", out);

		section("by_input_stable", stableLookup(manager, "byInput"));
		section("by_output_stable", stableLookup(manager, "byOutput"));
		out = new ArrayList<>();
		for (EmiStack s : EmiStackList.stacks) {
			out.add(stack(s) + " in=" + stableKeys(manager.getRecipesByInput(s)) + " out=" + stableKeys(manager.getRecipesByOutput(s)));
		}
		section("index_lookups_stable", out);
		// byWorkstation also keeps the lists of the unsorted first manager (registration order, e.g. composting from
		// an identity-hash map): sorted by category and id
		sortAllIds = true;
		section("by_workstation_stable", stableLookup(EmiRecipes.byWorkstation, false));
		sortAllIds = false;

		out = new ArrayList<>();
		for (EmiTagKey<?> key : EmiTags.TAGS) {
			out.add(key.raw().registry().getValue() + " #" + key.id() + " = " + stacks(EmiTags.getValues(key)));
		}
		out.sort(null);
		section("tags_stable", out);
	}

	@SuppressWarnings("unchecked")
	private List<String> stableLookup(EmiRecipeManager manager, String fieldName) throws ReflectiveOperationException {
		Field f = manager.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		return stableLookup((Map<EmiStack, List<EmiRecipe>>) f.get(manager), true);
	}

	/**
	 * Upstream keys these maps by the first stack copy that reached them, whose amount, components and remainder
	 * depend on the category iteration order (identity hashes): {@code keyIdOnly} prints the keys as class and id
	 * only. Entries whose keys print the same are listed in sorted order.
	 */
	private List<String> stableLookup(Map<EmiStack, List<EmiRecipe>> map, boolean keyIdOnly) {
		TreeMap<String, List<String>> sorted = new TreeMap<>();
		for (Map.Entry<EmiStack, List<EmiRecipe>> e : map.entrySet()) {
			if (e.getValue().stream().anyMatch(r -> !r.getCategory().getId().getNamespace().equals("emi_loot"))) {
				EmiStack s = e.getKey();
				String key = keyIdOnly ? s.getClass().getSimpleName() + ":" + s.getId() : stack(s);
				sorted.computeIfAbsent(key, k -> new ArrayList<>()).add(stableKeys(e.getValue()));
			}
		}
		List<String> out = new ArrayList<>();
		sorted.forEach((k, v) -> {
			v.sort(null);
			out.add(k + " -> " + String.join(" | ", v));
		});
		return out;
	}

	private String stableKeys(List<EmiRecipe> list) {
		List<EmiRecipe> kept = new ArrayList<>(list.size());
		for (EmiRecipe r : list) {
			if (!r.getCategory().getId().getNamespace().equals("emi_loot")) {
				kept.add(r);
			}
		}
		kept.sort(Comparator.<EmiRecipe>comparingInt(r -> categoryIndex.getOrDefault(r.getCategory(), Integer.MAX_VALUE))
			.thenComparing(r -> sortAllIds || orderNoise(r.getCategory()) ? String.valueOf(r.getId()) : ""));
		List<String> parts = new ArrayList<>(kept.size());
		for (EmiRecipe r : kept) {
			parts.add(String.valueOf(r.getId()));
		}
		return kept.size() + ":" + (kept.size() <= 8 ? parts.toString() : hash(parts));
	}

	@SuppressWarnings("unchecked")
	private List<String> lookup(EmiRecipeManager manager, String fieldName) throws ReflectiveOperationException {
		Field f = manager.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		Map<EmiStack, List<EmiRecipe>> map = (Map<EmiStack, List<EmiRecipe>>) f.get(manager);
		TreeMap<String, String> sorted = new TreeMap<>();
		for (Map.Entry<EmiStack, List<EmiRecipe>> e : map.entrySet()) {
			sorted.merge(stack(e.getKey()), keys(e.getValue()), (a, b) -> a + " | " + b);
		}
		List<String> out = new ArrayList<>();
		sorted.forEach((k, v) -> out.add(k + " -> " + v));
		return out;
	}

	private void section(String name, List<String> lines) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(dir.resolve(name + ".txt"), StandardCharsets.UTF_8)) {
			for (String l : lines) {
				w.write(l);
				w.write('\n');
			}
		}
		hashes.put(name, lines.size() + " " + hash(lines));
	}

	private static String hash(List<String> lines) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			for (String l : lines) {
				md.update(l.getBytes(StandardCharsets.UTF_8));
				md.update((byte) '\n');
			}
			return HexFormat.of().formatHex(md.digest()).substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private String keys(List<EmiRecipe> list) {
		if (normalized) {
			list = new ArrayList<>(list);
			list.sort(Comparator.comparingInt(r -> categoryIndex.getOrDefault(r.getCategory(), Integer.MAX_VALUE)));
		}
		StringBuilder sb = new StringBuilder();
		sb.append(list.size()).append(':');
		List<String> parts = new ArrayList<>(list.size());
		for (EmiRecipe r : list) {
			parts.add(recipeKey(r));
		}
		if (list.size() <= 8) {
			sb.append(parts);
		} else {
			sb.append(hash(parts));
		}
		return sb.toString();
	}

	private String recipeKey(EmiRecipe r) {
		Integer i = recipeIndex.get(r);
		return (i == null ? "?" : i.toString()) + "=" + r.getId();
	}

	private String recipe(EmiRecipe r) {
		return r.getId() + " | " + r.getClass().getName() + " | " + r.getCategory().getId()
			+ " | in=" + ingredients(r.getInputs()) + " | out=" + stacks(r.getOutputs()) + " | cat=" + ingredients(r.getCatalysts())
			+ " | tree=" + r.supportsRecipeTree();
	}

	private String ingredients(List<? extends EmiIngredient> list) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(ingredient(list.get(i)));
		}
		return sb.append(']').toString();
	}

	private String stacks(List<EmiStack> list) {
		return ingredients(list);
	}

	@SuppressWarnings("unchecked")
	private String ingredient(EmiIngredient ing) {
		if (ing instanceof EmiStack s) {
			return stack(s);
		} else if (ing instanceof TagEmiIngredient t) {
			return "#" + t.key.id() + "x" + t.getAmount() + chance(t.getChance());
		} else if (ing instanceof ListEmiIngredient l) {
			try {
				Field f = ListEmiIngredient.class.getDeclaredField("ingredients");
				f.setAccessible(true);
				List<? extends EmiIngredient> parts = (List<? extends EmiIngredient>) f.get(l);
				return "L" + ingredients(parts) + "x" + l.getAmount() + chance(l.getChance());
			} catch (ReflectiveOperationException e) {
				return "L" + stacks(l.getEmiStacks());
			}
		}
		return ing.getClass().getSimpleName() + stacks(ing.getEmiStacks()) + "x" + ing.getAmount() + chance(ing.getChance());
	}

	private String stack(EmiStack s) {
		return stack(s, 0);
	}

	private String stack(EmiStack s, int depth) {
		if (s.isEmpty()) {
			return "empty";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(s.getClass().getSimpleName()).append(':').append(s.getId());
		ComponentChanges changes = s.getComponentChanges();
		if (changes != null && !changes.isEmpty()) {
			sb.append(componentCache.computeIfAbsent(changes, this::components));
		}
		sb.append('x').append(s.getAmount()).append(chance(s.getChance()));
		if (depth == 0 && !s.getRemainder().isEmpty()) {
			sb.append("->").append(stack(s.getRemainder(), 1));
		}
		return sb.toString();
	}

	private static String chance(float chance) {
		return chance == 1 ? "" : "@" + chance;
	}

	private String components(ComponentChanges changes) {
		try {
			JsonElement json = ComponentChanges.CODEC.encodeStart(ops, changes).getOrThrow();
			return canonical(json);
		} catch (Exception e) {
			return "{unencodable " + changes.size() + "}";
		}
	}

	private static String canonical(JsonElement e) {
		if (e instanceof JsonObject o) {
			TreeMap<String, String> sorted = new TreeMap<>();
			for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
				sorted.put(entry.getKey(), canonical(entry.getValue()));
			}
			StringBuilder sb = new StringBuilder("{");
			sorted.forEach((k, v) -> sb.append(k).append('=').append(v).append(';'));
			return sb.append('}').toString();
		} else if (e instanceof JsonArray a) {
			StringBuilder sb = new StringBuilder("[");
			for (JsonElement c : a) {
				sb.append(canonical(c)).append(',');
			}
			return sb.append(']').toString();
		}
		return e.toString();
	}
}
