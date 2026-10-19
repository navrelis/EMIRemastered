package dev.emi.remastered.professions.trade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects the trades of one level in insertion order, keeping the first of equal trades and remembering every tag
 * (for example a villager type) under which a trade showed up. Pure logic, no Minecraft classes.
 */
public final class TradeBucket<T> {
	private final Map<TradeKey, Slot<T>> slots = new LinkedHashMap<>();

	/**
	 * @return true if the trade was new, false if an equal trade was already present (its tags are extended)
	 */
	public boolean add(TradeKey key, T value, String tag) {
		Slot<T> slot = slots.get(key);
		boolean added = slot == null;
		if (added) {
			slot = new Slot<>(value);
			slots.put(key, slot);
		}
		if (tag != null) {
			slot.tags.add(tag);
		}
		return added;
	}

	public int size() {
		return slots.size();
	}

	/**
	 * @return The trades in insertion order
	 */
	public List<Entry<T>> entries() {
		List<Entry<T>> list = new ArrayList<>(slots.size());
		for (Slot<T> slot : slots.values()) {
			list.add(new Entry<>(slot.value, List.copyOf(slot.tags)));
		}
		return list;
	}

	/**
	 * @param tags Tags in the order they were first seen
	 */
	public record Entry<T>(T value, List<String> tags) {
	}

	private static final class Slot<T> {
		final T value;
		final Set<String> tags = new LinkedHashSet<>();

		Slot(T value) {
			this.value = value;
		}
	}
}
