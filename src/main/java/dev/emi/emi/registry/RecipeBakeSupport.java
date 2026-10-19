package dev.emi.emi.registry;

import java.util.List;
import java.util.function.BiPredicate;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.runtime.EmiReloadLog;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

/**
 * EMI Remastered: steps of {@link EmiRecipes#bake()}. Recipes without a category are dropped instead of aborting the
 * reload (KB2, upstream PR #1222), and equal stack lists of tag ingredients are shared (KB12, upstream PR #933).
 */
final class RecipeBakeSupport {

	private RecipeBakeSupport() {
	}

	/**
	 * Recipe invalidator: true (drop) for a recipe whose category is null, logged with its id and class. Must run
	 * before every other invalidator, datapack recipe filters read the category.
	 */
	static boolean missingCategory(EmiRecipe recipe) {
		if (recipe.getCategory() != null) {
			return false;
		}
		EmiReloadLog.warn("Recipe " + recipe.getId() + " from " + recipe.getClass().getName() + " has no category and was skipped");
		return true;
	}

	/**
	 * Gives tag ingredients of the recipes' inputs and catalysts whose stack lists are identical (same stacks with the
	 * same components, amount, chance, comparison and remainder, in the same order) one shared list instance.
	 *
	 * @return the number of distinct lists
	 */
	static int shareTagStacks(List<EmiRecipe> recipes) {
		ListInterner<EmiStack> lists = new ListInterner<>(TagEmiIngredient::identicalStacks);
		for (EmiRecipe recipe : recipes) {
			shareTagStacks(recipe.getInputs(), lists);
			shareTagStacks(recipe.getCatalysts(), lists);
		}
		return lists.size();
	}

	private static void shareTagStacks(List<EmiIngredient> ingredients, ListInterner<EmiStack> lists) {
		if (ingredients == null) {
			return;
		}
		for (EmiIngredient ingredient : ingredients) {
			if (ingredient instanceof TagEmiIngredient tag) {
				List<EmiStack> stacks = tag.getEmiStacks();
				List<EmiStack> shared = lists.intern(stacks);
				if (shared != stacks) {
					tag.setEmiStacks(shared);
				}
			}
		}
	}

	/**
	 * One shared instance per group of lists whose elements are pairwise {@code identical}. Lists are hashed with
	 * {@link List#hashCode()}, so identical elements must have equal hash codes.
	 */
	static final class ListInterner<T> {
		private final ObjectOpenCustomHashSet<List<T>> lists;

		ListInterner(BiPredicate<T, T> identical) {
			lists = new ObjectOpenCustomHashSet<>(new Hash.Strategy<List<T>>() {
				@Override
				public int hashCode(List<T> list) {
					return list == null ? 0 : list.hashCode();
				}

				@Override
				public boolean equals(List<T> a, List<T> b) {
					if (a == b) {
						return true;
					}
					if (a == null || b == null || a.size() != b.size()) {
						return false;
					}
					for (int i = 0; i < a.size(); i++) {
						T x = a.get(i);
						T y = b.get(i);
						if (x != y && (x == null || y == null || !identical.test(x, y))) {
							return false;
						}
					}
					return true;
				}
			});
		}

		/** The shared instance for {@code list} ({@code list} itself the first time); null stays null. */
		List<T> intern(List<T> list) {
			return list == null ? null : lists.addOrGet(list);
		}

		int size() {
			return lists.size();
		}
	}
}
