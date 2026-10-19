/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.display;

import net.minecraft.util.Identifier;

/** Readable names for dimensions that have no translation. */
public final class DimensionNames {
	private DimensionNames() {
	}

	/** {@code twilightforest:twilight_forest} becomes {@code Twilight Forest}. The namespace is left out. */
	public static String readable(Identifier id) {
		StringBuilder name = new StringBuilder();
		for (String word : id.getPath().split("[_/\\-.]+")) {
			if (word.isEmpty()) {
				continue;
			}
			if (!name.isEmpty()) {
				name.append(' ');
			}
			name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return name.isEmpty() ? id.toString() : name.toString();
	}
}
