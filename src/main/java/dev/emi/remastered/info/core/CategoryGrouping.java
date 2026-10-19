package dev.emi.remastered.info.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Groups recipes by category and picks example stacks. Generic and free of Minecraft/EMI classes so it can be unit
 * tested; the info module calls it with {@code EmiRecipe}/{@code EmiRecipeCategory}/{@code EmiIngredient}.
 */
public final class CategoryGrouping {

	private CategoryGrouping() {
	}

	/** One category with its recipes (in the order they were given). */
	public record Group<C, R>(C category, List<R> recipes) {
		public int count() {
			return recipes.size();
		}
	}

	/**
	 * Groups {@code recipes} by {@code categoryOf} and sorts the groups by {@code orderOf} (EMI's category order).
	 * A negative order (category unknown to EMI) sorts after all known ones; ties keep the order of first appearance.
	 * Recipes whose category is in {@code excluded} or null are dropped.
	 */
	public static <C, R> List<Group<C, R>> group(List<? extends R> recipes, Function<? super R, ? extends C> categoryOf,
			ToIntFunction<? super C> orderOf, Set<?> excluded) {
		Map<C, List<R>> map = new LinkedHashMap<>();
		for (R recipe : recipes) {
			C category = categoryOf.apply(recipe);
			if (category == null || excluded.contains(category)) {
				continue;
			}
			map.computeIfAbsent(category, c -> new ArrayList<>()).add(recipe);
		}
		List<Group<C, R>> groups = new ArrayList<>(map.size());
		for (Map.Entry<C, List<R>> e : map.entrySet()) {
			groups.add(new Group<>(e.getKey(), Collections.unmodifiableList(e.getValue())));
		}
		// List.sort is stable: equal orders keep first-appearance order
		groups.sort((a, b) -> Integer.compare(sortKey(orderOf.applyAsInt(a.category())), sortKey(orderOf.applyAsInt(b.category()))));
		return groups;
	}

	private static int sortKey(int order) {
		return order < 0 ? Integer.MAX_VALUE : order;
	}

	/**
	 * Picks up to {@code max} example items: for each recipe in order, the first candidate that is not empty and not
	 * already picked (one per recipe). If that leaves room, a second pass adds further distinct candidates.
	 */
	public static <R, I> List<I> examples(List<? extends R> recipes, Function<? super R, ? extends List<? extends I>> candidates,
			Predicate<? super I> isEmpty, BiPredicate<? super I, ? super I> same, int max) {
		List<I> picked = new ArrayList<>();
		if (max <= 0) {
			return picked;
		}
		for (R recipe : recipes) {
			if (picked.size() >= max) {
				return picked;
			}
			for (I candidate : candidates.apply(recipe)) {
				if (candidate != null && !isEmpty.test(candidate) && !contains(picked, candidate, same)) {
					picked.add(candidate);
					break;
				}
			}
		}
		for (R recipe : recipes) {
			for (I candidate : candidates.apply(recipe)) {
				if (picked.size() >= max) {
					return picked;
				}
				if (candidate != null && !isEmpty.test(candidate) && !contains(picked, candidate, same)) {
					picked.add(candidate);
				}
			}
		}
		return picked;
	}

	private static <I> boolean contains(List<I> list, I item, BiPredicate<? super I, ? super I> same) {
		for (I i : list) {
			if (same.test(i, item)) {
				return true;
			}
		}
		return false;
	}

	/** Total number of recipes over all groups. */
	public static int total(List<? extends Group<?, ?>> groups) {
		int n = 0;
		for (Group<?, ?> g : groups) {
			n += g.count();
		}
		return n;
	}
}
