package dev.emi.remastered.world.data;

import java.util.Locale;

/** Number and id formatting of the world pages that does not need Minecraft. */
public final class WorldFormat {
	/** Blocks per chunk side. */
	public static final int CHUNK = 16;

	private WorldFormat() {
	}

	/** Chunks as blocks: spacing 34 chunks is 544 blocks. */
	public static int blocks(int chunks) {
		return chunks * CHUNK;
	}

	/** A climate value with at most two decimals and no trailing zeros: {@code 0.8}, {@code 2}, {@code -0.5}, {@code 0.05}. */
	public static String climate(float value) {
		String text = String.format(Locale.ROOT, "%.2f", value);
		if (text.contains(".")) {
			text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return text.equals("-0") ? "0" : text;
	}

	/** {@code twilight_forest/dark_forest} becomes {@code Twilight Forest Dark Forest}. */
	public static String prettify(String path) {
		StringBuilder name = new StringBuilder();
		for (String token : path.split("[/_.\\-]+")) {
			if (token.isEmpty()) {
				continue;
			}
			if (!name.isEmpty()) {
				name.append(' ');
			}
			name.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
		}
		return name.isEmpty() ? path : name.toString();
	}

	/** The name without a leading {@code prefix_}; unchanged when it does not start with it or nothing would be left. */
	public static String withoutPrefix(String name, String prefix) {
		String start = prefix + "_";
		return name.startsWith(start) && name.length() > start.length() ? name.substring(start.length()) : name;
	}

	/** The path of an id, or the whole id when its namespace is not {@code minecraft}. */
	public static String shortId(String id) {
		return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
	}

	/** Group size of a spawn entry: {@code 4} or {@code 2-4}. */
	public static String groupSize(int min, int max) {
		return min >= max ? Integer.toString(min) : min + "-" + max;
	}

	/** Whether the structure shares its set with other structures (a village is one of five). */
	public static boolean hasSiblings(int weight, int totalWeight) {
		return totalWeight > weight;
	}
}
