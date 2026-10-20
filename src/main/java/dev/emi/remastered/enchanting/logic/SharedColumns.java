package dev.emi.remastered.enchanting.logic;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Spreads the elements an enchantment accepts round robin over a fixed number of display columns. Enchantments that
 * accept exactly the same elements get the same (unmodifiable) column lists, so everything derived from them (EMI
 * ingredients, the item index) is built once per distinct set instead of once per enchantment. That matters when
 * many enchantments share an item tag, and a lot when a mod makes every enchantment accept every item.
 * Pure logic, generic so it can be tested without Minecraft. One instance per catalog build, not thread safe.
 */
public final class SharedColumns<T> {
	private final int columns;
	private final IntFunction<T> element;
	private final Map<BitSet, List<List<T>>> byAccepted = new HashMap<>();

	/**
	 * @param columns number of columns, at least 1
	 * @param element the element with the given index; asked once per index and set of columns that holds it
	 */
	public SharedColumns(int columns, IntFunction<T> element) {
		if (columns < 1) {
			throw new IllegalArgumentException("columns < 1");
		}
		this.columns = columns;
		this.element = element;
	}

	/**
	 * @param accepted indices of the accepted elements; not kept, may be reused by the caller
	 * @return the accepted elements in index order, element number {@code n} in column {@code n % columns}; the same
	 * list instance for equal {@code accepted} sets
	 */
	public List<List<T>> columnsOf(BitSet accepted) {
		List<List<T>> existing = byAccepted.get(accepted);
		if (existing != null) {
			return existing;
		}
		List<List<T>> lists = new ArrayList<>(columns);
		for (int i = 0; i < columns; i++) {
			lists.add(new ArrayList<>());
		}
		int counter = 0;
		for (int i = accepted.nextSetBit(0); i >= 0; i = accepted.nextSetBit(i + 1)) {
			lists.get(counter++ % columns).add(element.apply(i));
		}
		List<List<T>> result = new ArrayList<>(columns);
		for (List<T> list : lists) {
			result.add(Collections.unmodifiableList(list));
		}
		result = Collections.unmodifiableList(result);
		byAccepted.put((BitSet) accepted.clone(), result);
		return result;
	}

	/** @return how many distinct accepted sets were seen */
	public int distinct() {
		return byAccepted.size();
	}
}
