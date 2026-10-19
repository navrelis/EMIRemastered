package dev.emi.remastered.info.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.client.InfoLinks;
import net.minecraft.util.Identifier;

/**
 * Target of a click in the Item Info screen. Use the factories; EMI's recipe screen opened from the Item Info screen
 * returns to it with EMI's back key or Escape. Client render thread only.
 */
@FunctionalInterface
public interface InfoLink {

	/** Opens the target; returns false when there was nothing to open (e.g. an unknown recipe id). */
	boolean open();

	/** EMI's recipe screen showing the recipe with this id. */
	static InfoLink recipe(Identifier recipeId) {
		return InfoLinks.recipe(recipeId);
	}

	/** EMI's recipe screen showing this recipe. */
	static InfoLink recipe(EmiRecipe recipe) {
		return InfoLinks.recipe(recipe);
	}

	/** EMI's recipe screen showing these recipes (tabs by category). */
	static InfoLink recipes(List<EmiRecipe> recipes) {
		return InfoLinks.recipes(recipes);
	}

	/** EMI's recipe screen showing every recipe of the category with this id. */
	static InfoLink category(Identifier categoryId) {
		return InfoLinks.category(categoryId);
	}

	/** EMI's recipe screen showing every recipe of the category. */
	static InfoLink category(EmiRecipeCategory category) {
		return InfoLinks.category(category);
	}

	/** EMI's recipes of {@code stack} (like EMI's "view recipes" key), on the tab of {@code focus} if given. */
	static InfoLink recipesOf(EmiStack stack, @Nullable EmiRecipeCategory focus) {
		return InfoLinks.recipesOf(stack, focus);
	}

	/** EMI's uses of {@code stack} (like EMI's "view uses" key), on the tab of {@code focus} if given. */
	static InfoLink usesOf(EmiStack stack, @Nullable EmiRecipeCategory focus) {
		return InfoLinks.usesOf(stack, focus);
	}

	/** The Item Info screen of another stack. */
	static InfoLink info(EmiStack stack) {
		return InfoLinks.info(stack);
	}
}
