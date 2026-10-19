package dev.emi.emi.registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EMI Remastered: the per-stack recipe buckets of the recipe manager's input/output index. Upstream collects each
 * bucket in a {@code LinkedHashSet}. While the index is built, all additions of one recipe happen in one go (its
 * inputs, catalysts and outputs), so a recipe's repeats in a bucket are adjacent unless the same or an equal recipe
 * is indexed twice. When {@link #allDistinct} holds, a list that skips a repeat of its last element therefore ends up
 * with exactly the elements of the set, in the same order. Pure Java on purpose (unit tested without Minecraft).
 */
final class RecipeBuckets {

	private RecipeBuckets() {
	}

	/** Whether no two elements are equal (the {@code HashSet} notion a {@code LinkedHashSet} bucket uses). */
	static boolean allDistinct(List<?> values) {
		try {
			Set<Object> seen = new HashSet<>(values.size() * 4 / 3 + 1);
			for (Object value : values) {
				if (!seen.add(value)) {
					return false;
				}
			}
			return true;
		} catch (RuntimeException e) {
			// a throwing hashCode/equals: keep upstream's sets, which then fail the same way
			return false;
		}
	}

	/** Adds {@code value} like {@code Set.add} would; {@code list} = the bucket is a list (see {@link #allDistinct}). */
	@SuppressWarnings("unchecked")
	static <T> void add(Collection<T> bucket, T value, boolean list) {
		if (list) {
			List<T> l = (List<T>) bucket;
			if (l.isEmpty() || l.get(l.size() - 1) != value) {
				l.add(value);
			}
		} else {
			bucket.add(value);
		}
	}
}
