package dev.emi.remastered.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Raw JSON entries of one list section of {@code emi.json} that could not be read (stack of a missing mod, bad NBT,
 * unknown recipe id). They are kept verbatim and written back on save, so nothing is lost while a mod is missing.
 * <p>
 * Each entry remembers the raw element that preceded it in the file (its anchor) and its original index. On save it
 * is put right after its anchor, or at its old index when the anchor is gone. Not thread safe; callers use it from the
 * same places that already touch the section.
 */
public final class PreservedEntries {
	private final List<Entry> entries = new ArrayList<>();

	public void clear() {
		entries.clear();
	}

	/**
	 * @param raw the entry as read from the file (keep a pristine copy, deserializers may mutate their input)
	 * @param anchor the raw element directly before it in the file, or null when it was first
	 * @param index its index in the file
	 */
	public void keep(JsonElement raw, @Nullable JsonElement anchor, int index) {
		entries.add(new Entry(raw, anchor, index));
	}

	public int size() {
		return entries.size();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/** Returns {@code current} with the kept entries inserted; entries equal to an existing element are skipped. */
	public JsonArray mergeInto(JsonArray current) {
		if (entries.isEmpty()) {
			return current;
		}
		List<JsonElement> out = new ArrayList<>(current.size() + entries.size());
		for (JsonElement el : current) {
			out.add(el);
		}
		for (Entry entry : entries) {
			if (out.contains(entry.raw)) {
				continue;
			}
			int pos = entry.anchor == null ? -1 : out.indexOf(entry.anchor);
			pos = pos >= 0 ? pos + 1 : Math.min(entry.index, out.size());
			out.add(pos, entry.raw.deepCopy());
		}
		JsonArray arr = new JsonArray(out.size());
		out.forEach(arr::add);
		return arr;
	}

	/**
	 * Adds every key of {@code preserved} that {@code current} lacks; when both hold an array under the same key the
	 * missing elements are appended.
	 */
	public static void mergeObject(JsonObject current, JsonObject preserved) {
		for (Map.Entry<String, JsonElement> e : preserved.entrySet()) {
			JsonElement have = current.get(e.getKey());
			if (have == null) {
				current.add(e.getKey(), e.getValue().deepCopy());
			} else if (have.isJsonArray() && e.getValue().isJsonArray()) {
				JsonArray arr = have.getAsJsonArray();
				for (JsonElement el : e.getValue().getAsJsonArray()) {
					if (!arr.contains(el)) {
						arr.add(el.deepCopy());
					}
				}
			}
		}
	}

	/** Adds {@code raw} to the array kept under {@code key}, creating it when needed. */
	public static void keepInObject(JsonObject preserved, String key, JsonElement raw) {
		JsonElement have = preserved.get(key);
		if (have == null || !have.isJsonArray()) {
			have = new JsonArray();
			preserved.add(key, have);
		}
		have.getAsJsonArray().add(raw);
	}

	private record Entry(JsonElement raw, @Nullable JsonElement anchor, int index) {
	}
}
