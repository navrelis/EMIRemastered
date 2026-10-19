package dev.emi.remastered.loot.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Text helpers for structure and dimension names that do not need Minecraft.
 */
public final class StructureNames {

	private StructureNames() {
	}

	/** "ocean_ruin_cold" becomes "Ocean Ruin Cold", used when a structure or dimension has no translation. */
	public static String prettify(String path) {
		StringBuilder name = new StringBuilder();
		for (String token : path.split("[/_]")) {
			if (token.isEmpty()) {
				continue;
			}
			if (!name.isEmpty()) {
				name.append(' ');
			}
			name.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
		}
		return name.toString();
	}

	/**
	 * Merges entries that show the same name, e.g. the five village variants, and joins their dimensions without
	 * repeating one. Keeps the order of first appearance.
	 */
	public static Map<String, List<String>> merge(List<Map.Entry<String, List<String>>> entries) {
		Map<String, Set<String>> merged = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : entries) {
			merged.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).addAll(entry.getValue());
		}
		Map<String, List<String>> result = new LinkedHashMap<>();
		merged.forEach((name, dimensions) -> result.put(name, new ArrayList<>(dimensions)));
		return result;
	}
}
