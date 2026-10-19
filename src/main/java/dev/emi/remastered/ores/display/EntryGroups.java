/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.display;

import dev.emi.remastered.ores.display.HeightModel.Resolved;
import dev.emi.remastered.ores.display.HeightModel.Shape;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Merges equal rows of the Item Info page, for example the several placed features of one ore. No Minecraft client types. */
public final class EntryGroups {
	private EntryGroups() {
	}

	/** Groups the items by key. The groups and the items in a group keep the order of their first appearance. */
	public static <T> List<List<T>> group(List<T> items, Function<? super T, ?> key) {
		Map<Object, List<T>> groups = new LinkedHashMap<>();
		for (T item : items) {
			groups.computeIfAbsent(key.apply(item), k -> new ArrayList<>()).add(item);
		}
		return new ArrayList<>(groups.values());
	}

	/** What an Item Info row shows of a height: the range and the best level text. Empty for no height. */
	public static List<Object> heightKey(Resolved height) {
		if (height == null) {
			return List.of();
		}
		Object best = height.peakLow() == height.peakHigh() ? List.of(height.peakLow())
			: height.shape() == Shape.UNIFORM ? "range" : List.of(height.peakLow(), height.peakHigh());
		return List.of(height.min(), height.max(), best);
	}
}
