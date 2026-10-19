package dev.emi.remastered.info.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.info.core.CategoryGrouping.Group;

class CategoryGroupingTest {
	private record Recipe(String category, List<String> inputs) {
	}

	private static final Map<String, Integer> ORDER = Map.of("crafting", 0, "smelting", 1, "blasting", 2);

	private static List<Group<String, Recipe>> group(List<Recipe> recipes, Set<String> excluded) {
		return CategoryGrouping.group(recipes, Recipe::category, c -> ORDER.getOrDefault(c, -1), excluded);
	}

	@Test
	void groupsByCategoryInEmiOrder() {
		Recipe s1 = new Recipe("smelting", List.of("raw_iron"));
		Recipe c1 = new Recipe("crafting", List.of("nugget"));
		Recipe s2 = new Recipe("smelting", List.of("iron_ore"));
		Recipe c2 = new Recipe("crafting", List.of("block"));
		Recipe b1 = new Recipe("blasting", List.of("raw_iron"));
		List<Group<String, Recipe>> groups = group(List.of(s1, c1, s2, b1, c2), Set.of());
		assertEquals(List.of("crafting", "smelting", "blasting"), groups.stream().map(Group::category).toList());
		assertEquals(List.of(c1, c2), groups.get(0).recipes());
		assertEquals(List.of(s1, s2), groups.get(1).recipes());
		assertEquals(2, groups.get(1).count());
		assertEquals(5, CategoryGrouping.total(groups));
	}

	@Test
	void unknownCategoriesGoLastInFirstAppearanceOrder() {
		Recipe x = new Recipe("modded_b", List.of());
		Recipe y = new Recipe("modded_a", List.of());
		Recipe c = new Recipe("crafting", List.of());
		List<Group<String, Recipe>> groups = group(List.of(x, y, c, x), Set.of());
		assertEquals(List.of("crafting", "modded_b", "modded_a"), groups.stream().map(Group::category).toList());
		assertEquals(2, groups.get(1).count());
	}

	@Test
	void excludedAndNullCategoriesAreDropped() {
		Recipe tag = new Recipe("tag", List.of());
		Recipe none = new Recipe(null, List.of());
		Recipe c = new Recipe("crafting", List.of());
		List<Group<String, Recipe>> groups = group(List.of(tag, none, c), Set.of("tag"));
		assertEquals(List.of("crafting"), groups.stream().map(Group::category).toList());
		assertEquals(List.of(), group(List.of(), Set.of()));
	}

	@Test
	void examplesPreferOnePerRecipeThenFillUp() {
		List<Recipe> recipes = List.of(
			new Recipe("crafting", List.of("a", "b")),
			new Recipe("crafting", List.of("a", "c")),
			new Recipe("crafting", List.of("", "d")),
			new Recipe("crafting", List.of("e")));
		// first pass: a (r1), c (r2, a taken), d (r3, "" empty), e (r4); second pass fills with b
		assertEquals(List.of("a", "c", "d", "e", "b"),
			CategoryGrouping.examples(recipes, Recipe::inputs, String::isEmpty, Objects::equals, 10));
		assertEquals(List.of("a", "c"), CategoryGrouping.examples(recipes, Recipe::inputs, String::isEmpty, Objects::equals, 2));
		assertEquals(List.of(), CategoryGrouping.examples(recipes, Recipe::inputs, String::isEmpty, Objects::equals, 0));
	}
}
