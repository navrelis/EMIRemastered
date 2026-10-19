package dev.emi.emi.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * The recipe index buckets (EmiRecipes.Manager): with {@link RecipeBuckets#allDistinct} the list buckets must hold
 * exactly what upstream's LinkedHashSet buckets hold, in the same order.
 */
class RecipeBucketsTest {

	/** A recipe stand-in: identity equality unless an id is given (then equal by id, like a recipe overriding equals). */
	private static final class Recipe {
		private final Integer id;
		private final List<Integer> stacks;

		Recipe(Integer id, List<Integer> stacks) {
			this.id = id;
			this.stacks = stacks;
		}

		@Override
		public boolean equals(Object o) {
			return id == null ? this == o : o instanceof Recipe r && id.equals(r.id);
		}

		@Override
		public int hashCode() {
			return id == null ? System.identityHashCode(this) : id;
		}
	}

	private static Map<Integer, List<Recipe>> index(List<Recipe> recipes, boolean list) {
		Function<Integer, Collection<Recipe>> bucket = list ? k -> new ArrayList<>(4) : k -> new LinkedHashSet<>();
		Map<Integer, Collection<Recipe>> map = new LinkedHashMap<>();
		for (Recipe recipe : recipes) {
			for (Integer stack : recipe.stacks) {
				RecipeBuckets.add(map.computeIfAbsent(stack, bucket), recipe, list);
			}
		}
		Map<Integer, List<Recipe>> out = new HashMap<>();
		map.forEach((k, v) -> out.put(k, v.stream().toList()));
		return out;
	}

	private static void assertSameBuckets(Map<Integer, List<Recipe>> expected, Map<Integer, List<Recipe>> actual) {
		assertEquals(expected.keySet(), actual.keySet());
		for (Integer key : expected.keySet()) {
			List<Recipe> e = expected.get(key);
			List<Recipe> a = actual.get(key);
			assertEquals(e.size(), a.size(), "bucket " + key);
			for (int i = 0; i < e.size(); i++) {
				assertTrue(e.get(i) == a.get(i), "bucket " + key + " position " + i);
			}
		}
	}

	@Test
	void listBucketsEqualSetBucketsForDistinctRecipes() {
		Random random = new Random(7);
		for (int round = 0; round < 300; round++) {
			List<Recipe> recipes = new ArrayList<>();
			int count = random.nextInt(200);
			for (int r = 0; r < count; r++) {
				List<Integer> stacks = new ArrayList<>();
				int n = random.nextInt(12);
				for (int s = 0; s < n; s++) {
					stacks.add(random.nextInt(30)); // repeats inside one recipe on purpose
				}
				recipes.add(new Recipe(random.nextBoolean() ? null : r, stacks));
			}
			Collections.shuffle(recipes, random); // the worker indexes in sorted order, not insertion order
			assertTrue(RecipeBuckets.allDistinct(recipes));
			assertSameBuckets(index(recipes, false), index(recipes, true));
		}
	}

	@Test
	void duplicatesAreDetected() {
		Recipe a = new Recipe(null, List.of(1, 2));
		Recipe b = new Recipe(null, List.of(2));
		assertFalse(RecipeBuckets.allDistinct(List.of(a, b, a)), "same recipe twice");
		assertFalse(RecipeBuckets.allDistinct(List.of(new Recipe(5, List.of(1)), b, new Recipe(5, List.of(2)))), "equal recipes");
		assertTrue(RecipeBuckets.allDistinct(List.of(a, b, new Recipe(5, List.of(1)), new Recipe(6, List.of(1)))));
		assertTrue(RecipeBuckets.allDistinct(List.of()));
	}

	@Test
	void throwingHashCodeFallsBackToSets() {
		Object bad = new Object() {
			@Override
			public int hashCode() {
				throw new IllegalStateException("broken recipe");
			}
		};
		assertFalse(RecipeBuckets.allDistinct(List.of(bad)));
	}

	@Test
	void setModeIsPlainSetAdd() {
		Collection<String> set = new LinkedHashSet<>();
		RecipeBuckets.add(set, "a", false);
		RecipeBuckets.add(set, "b", false);
		RecipeBuckets.add(set, "a", false);
		assertEquals(List.of("a", "b"), List.copyOf(set));
	}
}
