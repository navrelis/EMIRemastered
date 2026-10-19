package dev.emi.emi.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * EmiTags.getIngredient collapses stacks into tags greedily: it walks the tags in order and takes every tag whose
 * values are all still present ({@link EmiTagIndex#scanAll}, upstream's loop). With the index it walks only the
 * candidate tags ({@link EmiTagIndex#scan}). Both must take the same tags in the same order and leave the same stacks.
 */
class EmiTagIndexTest {
	private static final Function<List<Integer>, List<Integer>> VALUES = Function.identity();

	/** Upstream's loop verbatim (EmiTags.getIngredient in EMI 1.1.24), as an independent reference. */
	private static List<List<Integer>> upstream(Map<Integer, String> map, List<List<Integer>> tags) {
		List<List<Integer>> keys = new ArrayList<>();
		for (List<Integer> key : tags) {
			List<Integer> values = key;
			if (values.size() < 2) {
				continue;
			}
			if (map.keySet().containsAll(values)) {
				values.forEach(map::remove);
				keys.add(key);
			}
			if (map.isEmpty()) {
				break;
			}
		}
		return keys;
	}

	private static Map<Integer, String> stacks(Set<Integer> values) {
		Map<Integer, String> map = new HashMap<>();
		for (int v : values) {
			map.put(v, "stack" + v);
		}
		return map;
	}

	private static void check(List<List<Integer>> tags, EmiTagIndex index, Set<Integer> ingredient, String what) {
		Map<Integer, String> reference = stacks(ingredient);
		Map<Integer, String> full = stacks(ingredient);
		Map<Integer, String> indexed = stacks(ingredient);
		List<List<Integer>> expected = upstream(reference, tags);
		List<List<Integer>> fullOut = new ArrayList<>();
		EmiTagIndex.scanAll(tags, VALUES, full, fullOut);
		List<List<Integer>> indexedOut = new ArrayList<>();
		index.scan(tags, VALUES, indexed, new HashSet<>(indexed.keySet()), indexedOut);
		assertEquals(expected.size(), fullOut.size(), what);
		assertEquals(expected.size(), indexedOut.size(), what);
		for (int i = 0; i < expected.size(); i++) {
			// same tag objects, same order (identity, not just equal values)
			assertTrue(expected.get(i) == fullOut.get(i), what);
			assertTrue(expected.get(i) == indexedOut.get(i), what);
		}
		assertEquals(reference, full, what);
		assertEquals(reference, indexed, what);
	}

	@Test
	void indexedScanTakesTheSameTagsAsTheFullScan() {
		Random random = new Random(42);
		for (int round = 0; round < 300; round++) {
			int universe = 10 + random.nextInt(80);
			List<List<Integer>> tags = new ArrayList<>();
			int tagCount = random.nextInt(300);
			for (int t = 0; t < tagCount; t++) {
				int size = random.nextInt(10) < 8 ? random.nextInt(6) : random.nextInt(40);
				List<Integer> values = new ArrayList<>();
				for (int k = 0; k < size; k++) {
					values.add(random.nextInt(universe)); // duplicates on purpose
				}
				tags.add(values);
				if (t > 0 && random.nextInt(20) == 0) {
					tags.add(new ArrayList<>(tags.get(random.nextInt(tags.size())))); // equal value lists
				}
			}
			// like EmiTags' size order: biggest first (stable)
			tags.sort((a, b) -> Integer.compare(b.size(), a.size()));
			EmiTagIndex index = EmiTagIndex.build(tags, tags, 0);
			for (int q = 0; q < 50; q++) {
				Set<Integer> ingredient = new HashSet<>();
				if (!tags.isEmpty() && random.nextBoolean()) {
					// union of some tags plus noise, so that tags really get collapsed
					int parts = 1 + random.nextInt(3);
					for (int p = 0; p < parts; p++) {
						ingredient.addAll(tags.get(random.nextInt(tags.size())));
					}
				}
				int noise = random.nextInt(6);
				for (int n = 0; n < noise; n++) {
					ingredient.add(random.nextInt(universe + 5)); // also values in no tag
				}
				check(tags, index, ingredient, "round " + round + " query " + q);
			}
		}
	}

	@Test
	void edgeCases() {
		List<List<Integer>> tags = List.of(List.of(1, 2, 3), List.of(1, 2), List.of(3, 4), List.of(5), List.of(),
			List.of(6, 6), List.of(4, 7));
		EmiTagIndex index = EmiTagIndex.build(tags, tags, 0);
		check(tags, index, Set.of(), "empty ingredient");
		check(tags, index, Set.of(1, 2, 3), "exactly one tag, stops when empty");
		check(tags, index, Set.of(1, 2, 3, 4), "overlapping tags: the first one wins");
		check(tags, index, Set.of(1, 2, 4, 7), "two tags");
		check(tags, index, Set.of(5), "single value tags are never taken");
		check(tags, index, Set.of(6), "a tag of one value listed twice");
		check(tags, index, Set.of(8, 9), "values in no tag");
	}

	@Test
	void ignoresTagsWithFewerThanTwoValues() {
		List<List<Integer>> tags = List.of(List.of(1), List.of(), List.of(1, 2), List.of(2, 2));
		EmiTagIndex index = EmiTagIndex.build(tags, tags, 0);
		BitSet candidates = index.candidates(Set.of(1, 2));
		assertFalse(candidates.get(0));
		assertFalse(candidates.get(1));
		assertTrue(candidates.get(2));
		assertTrue(candidates.get(3));
	}

	@Test
	void isBoundToItsListAndVersion() {
		List<List<Integer>> tags = new ArrayList<>(List.of(List.of(1, 2)));
		EmiTagIndex index = EmiTagIndex.build(tags, tags, 3);
		assertTrue(index.isFor(tags, 3));
		assertFalse(index.isFor(tags, 4));
		assertFalse(index.isFor(new ArrayList<>(tags), 3));
	}
}
