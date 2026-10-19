package dev.emi.remastered.tools.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Data pack JSON of vanilla crafting recipes, rebuilt from what the client knows. The client only has the unpacked
 * recipe (a flat ingredient grid), the pattern and key of the data pack file are reconstructed: every distinct
 * ingredient gets one key character, empty cells are spaces, empty outer rows and columns are removed. Pure logic on
 * Gson elements, no Minecraft classes.
 */
public final class CraftingRecipeJson {
	private static final String SHAPED = "minecraft:crafting_shaped";
	private static final String SHAPELESS = "minecraft:crafting_shapeless";
	private static final String FALLBACK_KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private CraftingRecipeJson() {
	}

	/** One cell of the grid: the ingredient JSON, or null for an empty cell. */
	public record Cell(@Nullable JsonElement ingredient) {
		public static final Cell EMPTY = new Cell(null);
	}

	/** Pattern rows and the key (character to ingredient JSON) of a shaped recipe. */
	public record Pattern(List<String> rows, Map<Character, JsonElement> key) {
	}

	/**
	 * Builds the pattern of a grid.
	 *
	 * @param cells row-major, {@code width * height} cells
	 * @return the pattern, or null when the cell count does not match the size or the grid has no ingredient at all
	 */
	public static @Nullable Pattern pattern(int width, int height, List<Cell> cells) {
		if (width <= 0 || height <= 0 || cells.size() != width * height) {
			return null;
		}
		int minX = width, maxX = -1, minY = height, maxY = -1;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (cells.get(x + y * width).ingredient() != null) {
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}
		}
		if (maxX < 0) {
			return null;
		}
		Map<String, Character> assigned = new LinkedHashMap<>();
		Map<Character, JsonElement> key = new LinkedHashMap<>();
		List<String> rows = new ArrayList<>();
		for (int y = minY; y <= maxY; y++) {
			StringBuilder row = new StringBuilder();
			for (int x = minX; x <= maxX; x++) {
				JsonElement ingredient = cells.get(x + y * width).ingredient();
				if (ingredient == null) {
					row.append(' ');
					continue;
				}
				String canonical = ingredient.toString();
				Character symbol = assigned.get(canonical);
				if (symbol == null) {
					symbol = freeKey(hint(ingredient), key);
					assigned.put(canonical, symbol);
					key.put(symbol, ingredient);
				}
				row.append(symbol.charValue());
			}
			rows.add(row.toString());
		}
		return new Pattern(List.copyOf(rows), key);
	}

	/** Among tags with identical content the vanilla one wins, then the alphabetically first; null for no candidates. */
	public static @Nullable String chooseTag(List<String> candidates) {
		String best = null;
		for (String candidate : candidates) {
			if (best == null || rank(candidate) < rank(best) || (rank(candidate) == rank(best) && candidate.compareTo(best) < 0)) {
				best = candidate;
			}
		}
		return best;
	}

	private static int rank(String id) {
		return id.startsWith("minecraft:") ? 0 : 1;
	}

	/** The first letter of the hint that is still free (so iron ingots become {@code I}), else the next free letter. */
	private static char freeKey(String hint, Map<Character, JsonElement> used) {
		String upper = hint.toUpperCase(Locale.ROOT);
		for (int i = 0; i < upper.length(); i++) {
			char c = upper.charAt(i);
			if (Character.isLetterOrDigit(c) && c < 128 && !used.containsKey(c)) {
				return c;
			}
		}
		for (int i = 0; i < FALLBACK_KEYS.length(); i++) {
			char c = FALLBACK_KEYS.charAt(i);
			if (!used.containsKey(c)) {
				return c;
			}
		}
		throw new IllegalStateException("more than 36 distinct ingredients");
	}

	/** Last path segment of the item or tag an ingredient JSON names, or the first one of an array; empty if unknown. */
	static String hint(JsonElement ingredient) {
		if (ingredient.isJsonArray()) {
			JsonArray array = ingredient.getAsJsonArray();
			return array.isEmpty() ? "" : hint(array.get(0));
		}
		if (!ingredient.isJsonObject()) {
			return "";
		}
		JsonObject object = ingredient.getAsJsonObject();
		JsonElement id = object.has("item") ? object.get("item") : object.get("tag");
		if (id == null || !id.isJsonPrimitive()) {
			return "";
		}
		String text = id.getAsString();
		int cut = Math.max(text.lastIndexOf(':'), text.lastIndexOf('/'));
		return text.substring(cut + 1);
	}

	/**
	 * Data pack JSON of a shaped crafting recipe, or null when the grid cannot be turned into a pattern.
	 *
	 * @param group            empty for none (then it is left out)
	 * @param showNotification left out when true, which is the default
	 */
	public static @Nullable JsonObject shaped(String category, String group, int width, int height, List<Cell> cells,
			JsonElement result, boolean showNotification) {
		Pattern pattern = pattern(width, height, cells);
		if (pattern == null) {
			return null;
		}
		JsonObject json = header(SHAPED, category, group);
		JsonArray rows = new JsonArray();
		pattern.rows().forEach(rows::add);
		json.add("pattern", rows);
		JsonObject key = new JsonObject();
		pattern.key().forEach((symbol, ingredient) -> key.add(String.valueOf(symbol), ingredient));
		json.add("key", key);
		json.add("result", cleanResult(result));
		if (!showNotification) {
			json.addProperty("show_notification", false);
		}
		return json;
	}

	/** Data pack JSON of a shapeless crafting recipe. */
	public static JsonObject shapeless(String category, String group, List<JsonElement> ingredients, JsonElement result) {
		JsonObject json = header(SHAPELESS, category, group);
		JsonArray array = new JsonArray();
		ingredients.forEach(array::add);
		json.add("ingredients", array);
		json.add("result", cleanResult(result));
		return json;
	}

	private static JsonObject header(String type, String category, String group) {
		JsonObject json = new JsonObject();
		json.addProperty("type", type);
		json.addProperty("category", category);
		if (!group.isEmpty()) {
			json.addProperty("group", group);
		}
		return json;
	}

	/** The result without {@code "count": 1}, which is the default and which the item stack codec writes anyway. */
	private static JsonElement cleanResult(JsonElement result) {
		if (result.isJsonObject()) {
			JsonObject object = result.getAsJsonObject();
			JsonElement count = object.get("count");
			if (count != null && count.isJsonPrimitive() && count.getAsJsonPrimitive().isNumber() && count.getAsInt() == 1) {
				JsonObject copy = new JsonObject();
				object.asMap().forEach((key, value) -> {
					if (!key.equals("count")) {
						copy.add(key, value);
					}
				});
				return copy;
			}
		}
		return result;
	}
}
