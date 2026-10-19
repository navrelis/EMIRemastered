package dev.emi.remastered.info.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Readable names for item tags that have no translation. Pure logic, no Minecraft classes. */
public final class TagNames {

	private TagNames() {
	}

	/**
	 * A readable name for a tag path: the last path segment, {@code _} turned into spaces, first letter capitalised
	 * ({@code tools/melee_weapon} becomes {@code Melee weapon}).
	 */
	public static String readable(String path) {
		String last = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ').trim();
		if (last.isEmpty()) {
			return path;
		}
		return last.substring(0, 1).toUpperCase(Locale.ROOT) + last.substring(1);
	}

	/** A tag name and the text that tells it apart from a tag with the same name; the suffix is empty for a unique name. */
	public record Label(String name, String suffix) {
	}

	/**
	 * Adds a suffix to tag names that appear more than once in the list (different tags can share a friendly name, for
	 * example {@code c:foods/fruit} and {@code minecraft:fruits} are both "Fruits"). The suffix is the namespace
	 * ({@code (c)}) when that is enough to tell the equal names apart, else the whole tag id ({@code (c:foods/fruit)}).
	 * Unique names keep an empty suffix. The result has one label per input, in the same order.
	 *
	 * @param names the friendly names
	 * @param ids   the tag ids ({@code namespace:path}) in the same order
	 */
	public static List<Label> disambiguate(List<String> names, List<String> ids) {
		Map<String, List<Integer>> byName = new HashMap<>();
		for (int i = 0; i < names.size(); i++) {
			byName.computeIfAbsent(names.get(i), k -> new ArrayList<>()).add(i);
		}
		String[] suffixes = new String[names.size()];
		Arrays.fill(suffixes, "");
		for (List<Integer> group : byName.values()) {
			if (group.size() < 2) {
				continue;
			}
			Set<String> namespaces = new HashSet<>();
			for (int i : group) {
				namespaces.add(namespaceOf(ids.get(i)));
			}
			boolean byNamespace = namespaces.size() == group.size();
			for (int i : group) {
				suffixes[i] = " (" + (byNamespace ? namespaceOf(ids.get(i)) : ids.get(i)) + ")";
			}
		}
		List<Label> labels = new ArrayList<>(names.size());
		for (int i = 0; i < names.size(); i++) {
			labels.add(new Label(names.get(i), suffixes[i]));
		}
		return labels;
	}

	private static String namespaceOf(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? "minecraft" : id.substring(0, colon);
	}
}
