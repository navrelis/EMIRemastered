package dev.emi.remastered.info.core;

import java.util.Locale;

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
}
