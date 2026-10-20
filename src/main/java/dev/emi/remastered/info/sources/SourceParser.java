package dev.emi.remastered.info.sources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Parses and validates one {@code emi_sources} file (format: docs/SOURCES.md). Pure Java, no Minecraft classes, so it is
 * unit tested. An invalid entry is reported through {@link Problems} and skipped; nothing here throws for bad input.
 */
public final class SourceParser {
	/** Largest {@code count} of an output. */
	public static final int MAX_COUNT = 64;
	/** Longest accepted {@code text}. */
	public static final int MAX_TEXT = 1000;
	private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
	private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

	private SourceParser() {
	}

	/** Receives one line per skipped entry ({@code index} is -1 for a problem of the whole file). */
	@FunctionalInterface
	public interface Problems {
		void report(String fileId, int index, String reason);
	}

	/**
	 * @param fileId    resource id of the file, e.g. {@code emi_info:emi_sources/vanilla.json}
	 * @param json      the file content
	 * @param modLoaded whether a mod id is loaded (entries with another {@code requires_mod} are skipped silently)
	 */
	public static List<SourceEntry> parse(String fileId, String json, Predicate<String> modLoaded, Problems problems) {
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException | IllegalStateException e) {
			problems.report(fileId, -1, "not valid JSON: " + e.getMessage());
			return List.of();
		}
		if (!root.isJsonObject()) {
			problems.report(fileId, -1, "the file must contain a JSON object");
			return List.of();
		}
		JsonElement sources = root.getAsJsonObject().get("sources");
		if (sources == null || !sources.isJsonArray()) {
			problems.report(fileId, -1, "missing \"sources\" array");
			return List.of();
		}
		List<SourceEntry> result = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		JsonArray array = sources.getAsJsonArray();
		for (int i = 0; i < array.size(); i++) {
			try {
				SourceEntry entry = parseEntry(fileId, i, array.get(i), modLoaded);
				if (entry == null) {
					continue;
				}
				if (!ids.add(entry.id())) {
					problems.report(fileId, i, "duplicate id " + entry.id() + " in this file");
					continue;
				}
				result.add(entry);
			} catch (EntryException e) {
				problems.report(fileId, i, e.getMessage());
			}
		}
		return result;
	}

	/** {@code namespace:emi_sources/<path>.json} becomes {@code namespace:<path>/<index>}. */
	public static String defaultId(String fileId, int index) {
		int colon = fileId.indexOf(':');
		String namespace = colon < 0 ? "minecraft" : fileId.substring(0, colon);
		String path = colon < 0 ? fileId : fileId.substring(colon + 1);
		if (path.startsWith("emi_sources/")) {
			path = path.substring("emi_sources/".length());
		}
		if (path.endsWith(".json")) {
			path = path.substring(0, path.length() - ".json".length());
		}
		return namespace + ":" + path + "/" + index;
	}

	/** Whether {@code id} is {@code namespace:path} (or a bare path) made of the characters Minecraft ids allow. */
	public static boolean isValidId(String id) {
		int colon = id.indexOf(':');
		if (colon < 0) {
			return PATH.matcher(id).matches();
		}
		return NAMESPACE.matcher(id.substring(0, colon)).matches() && PATH.matcher(id.substring(colon + 1)).matches();
	}

	private static SourceEntry parseEntry(String fileId, int index, JsonElement element, Predicate<String> modLoaded) {
		if (!element.isJsonObject()) {
			throw new EntryException("entry is not an object");
		}
		JsonObject object = element.getAsJsonObject();
		String requiresMod = optionalString(object, "requires_mod");
		if (requiresMod != null && !modLoaded.test(requiresMod)) {
			return null;
		}
		String output = requiredString(object, "output");
		if (!isValidId(output)) {
			throw new EntryException("output \"" + output + "\" is not a valid id");
		}
		String id = optionalString(object, "id");
		if (id == null) {
			id = defaultId(fileId, index);
		} else if (!isValidId(id)) {
			throw new EntryException("id \"" + id + "\" is not a valid id");
		} else if (id.indexOf(':') < 0) {
			id = fileId.substring(0, Math.max(0, fileId.indexOf(':'))) + ":" + id;
		}
		String text = requiredString(object, "text");
		if (text.length() > MAX_TEXT) {
			throw new EntryException("\"text\" is longer than " + MAX_TEXT + " characters");
		}
		int count = 1;
		if (object.has("count")) {
			count = integer(object.get("count"), "count");
			if (count < 1 || count > MAX_COUNT) {
				throw new EntryException("\"count\" must be between 1 and " + MAX_COUNT + ", is " + count);
			}
		}
		Double chance = null;
		if (object.has("chance")) {
			chance = number(object.get("chance"), "chance");
			if (chance < 0 || chance > 1) {
				throw new EntryException("\"chance\" must be between 0 and 1, is " + chance);
			}
		}
		List<String> from = new ArrayList<>();
		if (object.has("from")) {
			if (!object.get("from").isJsonArray()) {
				throw new EntryException("\"from\" must be an array of item ids or #tags");
			}
			for (JsonElement e : object.getAsJsonArray("from")) {
				if (!isString(e)) {
					throw new EntryException("\"from\" must only contain strings");
				}
				String value = e.getAsString();
				String bare = value.startsWith("#") ? value.substring(1) : value;
				if (!isValidId(bare)) {
					throw new EntryException("\"from\" entry \"" + value + "\" is not a valid id");
				}
				from.add(value);
			}
		}
		return new SourceEntry(fileId, index, id, output, count, List.copyOf(from), text, chance, requiresMod);
	}

	private static boolean isString(JsonElement e) {
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString();
	}

	private static String requiredString(JsonObject object, String key) {
		JsonElement e = object.get(key);
		if (e == null || e.isJsonNull()) {
			throw new EntryException("missing \"" + key + "\"");
		}
		if (!isString(e)) {
			throw new EntryException("\"" + key + "\" must be a string");
		}
		String value = e.getAsString().strip();
		if (value.isEmpty()) {
			throw new EntryException("\"" + key + "\" is empty");
		}
		return value;
	}

	private static String optionalString(JsonObject object, String key) {
		JsonElement e = object.get(key);
		if (e == null || e.isJsonNull()) {
			return null;
		}
		return requiredString(object, key);
	}

	private static double number(JsonElement e, String key) {
		if (!(e instanceof JsonPrimitive p) || !p.isNumber()) {
			throw new EntryException("\"" + key + "\" must be a number");
		}
		double value = p.getAsDouble();
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			throw new EntryException("\"" + key + "\" must be a finite number");
		}
		return value;
	}

	private static int integer(JsonElement e, String key) {
		double value = number(e, key);
		if (value != Math.rint(value)) {
			throw new EntryException("\"" + key + "\" must be a whole number");
		}
		if (Math.abs(value) > Integer.MAX_VALUE) {
			throw new EntryException("\"" + key + "\" is out of range");
		}
		return (int) value;
	}

	private static final class EntryException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		EntryException(String message) {
			super(message, null, false, false);
		}
	}
}
