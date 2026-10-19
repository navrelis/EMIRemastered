package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One entry of an {@code effect_sources} JSON file, as written (no registry lookups yet, so it can be parsed and
 * tested without Minecraft). See {@link #parse} for the format.
 *
 * @param cure      true for cures (shown under "Cures"), false for sources
 * @param effects   effect ids; {@code "*"} stands for every effect that is not instant
 * @param entities  entity type ids, shown as their spawn egg
 * @param stacks    EMI serialized stacks ({@code "item:minecraft:conduit"} or objects), same format as extra_stacks
 * @param text      lang key of the short description, may be empty
 * @param amplifier level minus one, or -1 when not given
 * @param duration  ticks, or -1 when not given
 */
public record EffectSourceDef(boolean cure, List<String> effects, List<String> entities, List<JsonElement> stacks,
		String text, int amplifier, int duration) {
	public static final String ALL_EFFECTS = "*";

	/** True when the entry names entities or stacks (they must resolve for the entry to be shown). */
	public boolean hasReferences() {
		return !entities.isEmpty() || !stacks.isEmpty();
	}

	/**
	 * Parses a file: an object {@code {"sources": [...]}} or a plain array of entries. An entry:
	 * <pre>
	 * {
	 *   "kind": "source" | "cure",           optional, default source
	 *   "effect": "minecraft:poison",        or "effects": ["minecraft:regeneration", ...]
	 *   "entities": ["minecraft:cave_spider"], optional
	 *   "stacks": ["item:minecraft:conduit"], optional
	 *   "text": "emiffect.source.cave_spider", optional lang key
	 *   "amplifier": 0, "duration": 150       optional (level - 1, ticks)
	 * }
	 * </pre>
	 * Invalid entries are reported to {@code onError} and skipped.
	 */
	public static List<EffectSourceDef> parse(JsonElement root, Consumer<String> onError) {
		List<EffectSourceDef> result = new ArrayList<>();
		JsonArray array;
		if (root != null && root.isJsonArray()) {
			array = root.getAsJsonArray();
		} else if (root != null && root.isJsonObject() && root.getAsJsonObject().get("sources") instanceof JsonArray a) {
			array = a;
		} else {
			onError.accept("expected an object with a \"sources\" array");
			return result;
		}
		int index = 0;
		for (JsonElement element : array) {
			String where = "entry " + index++;
			try {
				EffectSourceDef def = parseEntry(element);
				if (def.effects.isEmpty()) {
					onError.accept(where + ": no effect given");
				} else if (def.text.isEmpty() && !def.hasReferences()) {
					onError.accept(where + ": neither text, entities nor stacks given");
				} else {
					result.add(def);
				}
			} catch (RuntimeException e) {
				onError.accept(where + ": " + e.getMessage());
			}
		}
		return result;
	}

	/** Parses an EMIffect {@code extra_stacks} file: {@code {"id": "minecraft:hunger", "stacks": ["item:..."]}}. */
	public static EffectSourceDef parseExtra(JsonObject json) {
		if (!json.has("id") || !json.has("stacks")) {
			throw new IllegalArgumentException("expected \"id\" and \"stacks\"");
		}
		List<JsonElement> stacks = new ArrayList<>();
		JsonElement s = json.get("stacks");
		if (s.isJsonArray()) {
			s.getAsJsonArray().forEach(stacks::add);
		} else {
			stacks.add(s);
		}
		if (stacks.isEmpty()) {
			throw new IllegalArgumentException("no stacks");
		}
		return new EffectSourceDef(false, List.of(json.get("id").getAsString()), List.of(), List.copyOf(stacks), "", -1, -1);
	}

	private static EffectSourceDef parseEntry(JsonElement element) {
		if (element == null || !element.isJsonObject()) {
			throw new IllegalArgumentException("not an object");
		}
		JsonObject json = element.getAsJsonObject();
		String kind = json.has("kind") ? json.get("kind").getAsString() : "source";
		if (!kind.equals("source") && !kind.equals("cure")) {
			throw new IllegalArgumentException("unknown kind '" + kind + "'");
		}
		List<String> effects = strings(json, "effect");
		effects.addAll(strings(json, "effects"));
		List<JsonElement> stacks = new ArrayList<>();
		JsonElement s = json.get("stacks");
		if (s != null && s.isJsonArray()) {
			s.getAsJsonArray().forEach(stacks::add);
		} else if (s != null && !s.isJsonNull()) {
			stacks.add(s);
		}
		String text = json.has("text") ? json.get("text").getAsString() : "";
		int amplifier = json.has("amplifier") ? Math.max(0, json.get("amplifier").getAsInt()) : -1;
		int duration = json.has("duration") ? Math.max(0, json.get("duration").getAsInt()) : -1;
		return new EffectSourceDef(kind.equals("cure"), List.copyOf(effects), List.copyOf(strings(json, "entities")),
				List.copyOf(stacks), text, amplifier, duration);
	}

	private static List<String> strings(JsonObject json, String key) {
		List<String> list = new ArrayList<>();
		JsonElement e = json.get(key);
		if (e == null || e.isJsonNull()) {
			return list;
		}
		if (e.isJsonArray()) {
			e.getAsJsonArray().forEach(x -> list.add(x.getAsString()));
		} else {
			list.add(e.getAsString());
		}
		return list;
	}
}
