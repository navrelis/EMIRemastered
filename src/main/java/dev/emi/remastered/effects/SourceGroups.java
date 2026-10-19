package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** Pure helpers to group equal sources under one label and to lay stacks out in rows (unit tested). */
public final class SourceGroups {

	private SourceGroups() {
	}

	/** Key of a group: level (amplifier, -1 = unknown), duration in ticks (-1 = unknown) and chance. */
	public record Key(int amplifier, int duration, float probability) implements Comparable<Key> {
		@Override
		public int compareTo(Key o) {
			int c = Integer.compare(amplifier, o.amplifier);
			if (c == 0) {
				c = Integer.compare(duration, o.duration);
			}
			if (c == 0) {
				// the more likely source first
				c = Float.compare(o.probability, probability);
			}
			return c;
		}
	}

	/** Groups the items by key, ordered by level, then duration, then chance; the order inside a group is kept. */
	public static <T> Map<Key, List<T>> group(Iterable<T> items, Function<T, Key> key) {
		Map<Key, List<T>> groups = new TreeMap<>();
		for (T item : items) {
			groups.computeIfAbsent(key.apply(item), k -> new ArrayList<>()).add(item);
		}
		return groups;
	}

	/** Number of rows needed for {@code cells} stacks with {@code perRow} stacks in a row. */
	public static int rows(int cells, int perRow) {
		return cells <= 0 ? 0 : (cells - 1) / perRow + 1;
	}
}
