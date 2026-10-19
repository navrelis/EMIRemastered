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

import org.junit.jupiter.api.Test;

/**
 * EmiTags.getIngredient collapses stacks into tags greedily: it walks the tags in order and takes every tag whose
 * values are all still present. With the index it only walks the candidate tags. Both must pick the same tags in
 * the same order and leave the same stacks.
 */
class EmiTagIndexTest {

	/** Upstream's loop (EmiTags.getIngredient before the index). */
	private static List<Integer> fullScan(Map<Integer, String> map, List<List<Integer>> tags) {
		List<Integer> picked = new ArrayList<>();
		for (int i = 0; i < tags.size(); i++) {
			List<Integer> values = tags.get(i);
			if (values.size() < 2) {
				continue;
			}
			if (map.keySet().containsAll(values)) {
				values.forEach(map::remove);
				picked.add(i);
			}
			if (map.isEmpty()) {
				break;
			}
		}
		return picked;
	}

	/** The indexed loop (EmiTags.getIngredient now). */
	private static List<Integer> indexedScan(Map<Integer, String> map, List<List<Integer>> tags, EmiTagIndex index) {
		List<Integer> picked = new ArrayList<>();
		BitSet candidates = index.candidates(new HashSet<>(map.keySet()));
		for (int i = candidates.nextSetBit(0); i >= 0; i = candidates.nextSetBit(i + 1)) {
			List<Integer> values = tags.get(i);
			if (map.keySet().containsAll(values)) {
				values.forEach(map::remove);
				picked.add(i);
			}
			if (map.isEmpty()) {
				break;
			}
		}
		return picked;
	}

	private static Map<Integer, String> stacks(Set<Integer> values) {
		Map<Integer, String> map = new HashMap<>();
		for (int v : values) {
			map.put(v, "stack" + v);
		}
		return map;
	}

	@Test
	void indexedScanPicksTheSameTagsAsTheFullScan() {
		Random random = new Random(42);
		for (int round = 0; round < 200; round++) {
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
					ingredient.add(random.nextInt(universe));
				}
				Map<Integer, String> a = stacks(ingredient);
				Map<Integer, String> b = stacks(ingredient);
				assertEquals(fullScan(a, tags), indexedScan(b, tags, index), "round " + round + " query " + q);
				assertEquals(a, b);
			}
		}
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
