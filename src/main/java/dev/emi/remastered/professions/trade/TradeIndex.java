package dev.emi.remastered.professions.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi map from a key (for example the item of a stack) to the trades that mention it, in insertion order. Built once
 * per EMI reload by the Item Info provider and thrown away on the next reload or on disconnect. Pure logic, no
 * Minecraft classes.
 */
public final class TradeIndex<K, T> {
	private final Map<K, List<T>> map = new HashMap<>();

	/** Adds {@code value} under {@code key}; the same instance twice in a row under one key is ignored. */
	public void add(K key, T value) {
		List<T> list = map.computeIfAbsent(key, k -> new ArrayList<>(2));
		if (list.isEmpty() || list.get(list.size() - 1) != value) {
			list.add(value);
		}
	}

	/** @return The trades under {@code key} in insertion order, empty if none (never null) */
	public List<T> get(K key) {
		List<T> list = map.get(key);
		return list == null ? List.of() : list;
	}

	public int keys() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}
}
