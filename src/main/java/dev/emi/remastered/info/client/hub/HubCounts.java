package dev.emi.remastered.info.client.hub;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRecipes;

/**
 * Recipe counts of the item view's tabs, computed like EMI's {@code displayRecipes}/{@code displayUses} of one stack
 * (so a tab's count is what it shows). Small cache, dropped when EMI's recipe manager changes. Render thread only.
 */
final class HubCounts {
	private static final int CACHE = 32;
	private static final Map<EmiStack, int[]> CACHE_MAP = new LinkedHashMap<>(CACHE, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<EmiStack, int[]> eldest) {
			return size() > CACHE;
		}
	};
	private static EmiRecipeManager cachedFor;

	private HubCounts() {
	}

	/** Recipes {@code EmiApi.displayRecipes(stack)} shows. */
	static int recipes(EmiStack stack) {
		int[] entry = entry(stack);
		if (entry[0] < 0) {
			int count = 0;
			for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
				if (recipe.getOutputs().stream().anyMatch(o -> o.isEqual(stack))) {
					count++;
				}
			}
			entry[0] = count;
		}
		return entry[0];
	}

	/** Recipes {@code EmiApi.displayUses(stack)} shows (inputs, catalysts and workstation categories). */
	static int uses(EmiStack stack) {
		int[] entry = entry(stack);
		if (entry[1] < 0) {
			Set<EmiRecipe> recipes = Collections.newSetFromMap(new IdentityHashMap<>());
			for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByInput(stack)) {
				if (contains(recipe.getInputs(), stack) || contains(recipe.getCatalysts(), stack)) {
					recipes.add(recipe);
				}
			}
			recipes.addAll(EmiRecipes.byWorkstation.getOrDefault(stack, List.of()));
			entry[1] = recipes.size();
		}
		return entry[1];
	}

	private static boolean contains(List<EmiIngredient> ingredients, EmiStack stack) {
		for (EmiIngredient ingredient : ingredients) {
			for (EmiStack s : ingredient.getEmiStacks()) {
				if (s.isEqual(stack)) {
					return true;
				}
			}
		}
		return false;
	}

	private static int[] entry(EmiStack stack) {
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		if (manager != cachedFor) {
			CACHE_MAP.clear();
			cachedFor = manager;
		}
		return CACHE_MAP.computeIfAbsent(stack, s -> new int[] { -1, -1 });
	}
}
