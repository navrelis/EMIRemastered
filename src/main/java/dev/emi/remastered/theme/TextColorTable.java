package dev.emi.remastered.theme;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Immutable map from source RGB to replacement RGB, used by {@link TextColors} to recolour panel text.
 * <p>
 * Pure Java, no Minecraft classes. Colours are 24-bit RGB; {@link #remap(int)} keeps the alpha byte of the input.
 */
public final class TextColorTable {
	/** The table the mod ships: maps nothing. */
	public static final TextColorTable EMPTY = new TextColorTable(new Int2IntOpenHashMap());

	private final Int2IntOpenHashMap map;

	private TextColorTable(Int2IntOpenHashMap map) {
		this.map = map;
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	/**
	 * Replaces the RGB part of {@code argb} if it is mapped. The alpha byte is kept exactly, including 0 (Minecraft's
	 * text renderer makes alpha-0 colours opaque later). Unmapped colours are returned unchanged.
	 */
	public int remap(int argb) {
		if (map.isEmpty()) {
			return argb;
		}
		int rgb = argb & 0xFFFFFF;
		// 0xFFFFFF is a valid target, so test for the key instead of comparing with a default value
		if (!map.containsKey(rgb)) {
			return argb;
		}
		return (argb & 0xFF000000) | map.get(rgb);
	}

	/** Parses a 6 digit hex colour with an optional leading '#'; returns -1 if it is not one. */
	static int parseHex(String s) {
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		if (s.length() != 6) {
			return -1;
		}
		int v = 0;
		for (int i = 0; i < 6; i++) {
			char c = s.charAt(i);
			int d = c < 128 ? Character.digit(c, 16) : -1;
			if (d < 0) {
				return -1;
			}
			v = (v << 4) | d;
		}
		return v;
	}

	public static final class Builder {
		private final Int2IntOpenHashMap map = new Int2IntOpenHashMap();

		private Builder() {
		}

		/** Later calls for the same source colour win. */
		public Builder put(int fromRgb, int toRgb) {
			map.put(fromRgb & 0xFFFFFF, toRgb & 0xFFFFFF);
			return this;
		}

		/**
		 * Reads one {@code text_colors.json} and puts its entries. Invalid file (bad JSON, "replace" not an object): one
		 * warning, nothing is put, returns false. Invalid entries: one warning listing their keys, the rest is put.
		 *
		 * @return false if the file was invalid as a whole
		 */
		public boolean parseInto(Reader json, Consumer<String> warn) {
			JsonObject root;
			try {
				JsonElement parsed = JsonParser.parseReader(json);
				if (!parsed.isJsonObject()) {
					warn.accept("top level is not a JSON object");
					return false;
				}
				root = parsed.getAsJsonObject();
			} catch (RuntimeException e) {
				warn.accept("invalid JSON: " + e.getMessage());
				return false;
			}
			JsonElement replace = root.get("replace");
			if (replace == null) {
				return true;
			}
			if (!replace.isJsonObject()) {
				warn.accept("\"replace\" is not an object");
				return false;
			}
			Int2IntMap staged = new Int2IntOpenHashMap();
			List<String> bad = new ArrayList<>();
			for (Map.Entry<String, JsonElement> e : replace.getAsJsonObject().entrySet()) {
				int from = parseHex(e.getKey());
				int to = -1;
				JsonElement value = e.getValue();
				if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
					to = parseHex(value.getAsString());
				}
				if (from < 0 || to < 0) {
					bad.add(e.getKey());
					continue;
				}
				staged.put(from, to);
			}
			if (!bad.isEmpty()) {
				warn.accept("skipped invalid entries (need 6 hex digits as key and value): " + String.join(", ", bad));
			}
			map.putAll(staged);
			return true;
		}

		public TextColorTable build() {
			return map.isEmpty() ? EMPTY : new TextColorTable(new Int2IntOpenHashMap(map));
		}
	}
}
