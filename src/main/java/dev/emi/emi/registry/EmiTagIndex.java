package dev.emi.emi.registry;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * EMI Remastered: inverted index over one registry's tag list (in {@link EmiTags#getTags} order): for every value,
 * the positions of the tags with at least two values that contain it. {@link EmiTags#getIngredient} collapses stacks
 * into tags by testing every tag of the registry in order; only a tag sharing a value with the stacks can pass that
 * test, so {@link #scan} tests just those, in the same order, and picks exactly the same tags as {@link #scanAll}.
 * Pure Java on purpose (unit tested without Minecraft).
 */
final class EmiTagIndex {
	private final Object owner;
	private final int version;
	private final Map<Object, int[]> positions;
	private final int size;

	private EmiTagIndex(Object owner, int version, Map<Object, int[]> positions, int size) {
		this.owner = owner;
		this.version = version;
		this.positions = positions;
		this.size = size;
	}

	/**
	 * @param tagValues the values of every tag, in order
	 * @param owner the list the index was built for (identity checked by {@link #isFor})
	 * @param version a version stamp of the values (checked by {@link #isFor})
	 */
	static EmiTagIndex build(List<? extends List<?>> tagValues, Object owner, int version) {
		Map<Object, IntArrayList> lists = new HashMap<>();
		for (int i = 0; i < tagValues.size(); i++) {
			List<?> values = tagValues.get(i);
			if (values.size() < 2) {
				continue;
			}
			for (Object value : values) {
				IntArrayList list = lists.computeIfAbsent(value, v -> new IntArrayList(2));
				if (list.isEmpty() || list.getInt(list.size() - 1) != i) {
					list.add(i);
				}
			}
		}
		Map<Object, int[]> positions = new HashMap<>(lists.size() * 4 / 3 + 1);
		for (Map.Entry<Object, IntArrayList> entry : lists.entrySet()) {
			positions.put(entry.getKey(), entry.getValue().toIntArray());
		}
		return new EmiTagIndex(owner, version, positions, tagValues.size());
	}

	boolean isFor(Object owner, int version) {
		return this.owner == owner && this.version == version;
	}

	/** Positions (ascending when iterated with {@link BitSet#nextSetBit}) of the tags containing any of the values. */
	BitSet candidates(Collection<?> values) {
		BitSet set = new BitSet(size);
		for (Object value : values) {
			int[] p = positions.get(value);
			if (p != null) {
				for (int i : p) {
					set.set(i);
				}
			}
		}
		return set;
	}

	/**
	 * Upstream's loop: every tag (with at least two values) whose values are all keys of {@code map} is added to
	 * {@code out} and its values are removed from {@code map}, in tag order, until {@code map} is empty.
	 */
	static <K, T> void scanAll(List<K> tags, Function<K, List<T>> valuesOf, Map<T, ?> map, List<K> out) {
		for (K key : tags) {
			List<T> values = valuesOf.apply(key);
			if (values.size() < 2) {
				continue;
			}
			if (map.keySet().containsAll(values)) {
				values.forEach(map::remove);
				out.add(key);
			}
			if (map.isEmpty()) {
				break;
			}
		}
	}

	/**
	 * Same result as {@link #scanAll} for the {@code tags} this index was built for; {@code original} are the keys of
	 * {@code map} before the call. A skipped tag has at least two values and none of them in {@code map}, so it fails
	 * {@code containsAll} in {@link #scanAll}, and {@code map} only changes when a tag is taken.
	 */
	<K, T> void scan(List<K> tags, Function<K, List<T>> valuesOf, Map<T, ?> map, Collection<?> original, List<K> out) {
		BitSet candidates = candidates(original);
		for (int i = candidates.nextSetBit(0); i >= 0; i = candidates.nextSetBit(i + 1)) {
			K key = tags.get(i);
			List<T> values = valuesOf.apply(key);
			if (map.keySet().containsAll(values)) {
				values.forEach(map::remove);
				out.add(key);
			}
			if (map.isEmpty()) {
				break;
			}
		}
	}
}
