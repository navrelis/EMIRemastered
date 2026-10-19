package dev.emi.remastered.enchanting.page;

import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.widget.PagedWidgets;

/**
 * One kind of page of an enchantment's info recipe. Pages are built, in the order they are registered with
 * {@link InfoPages#register}, every time the recipe is shown, so a page can depend on state that changes at runtime
 * (settings, language). The page area is {@value dev.emi.remastered.enchanting.recipe.EnchantmentRecipe#WIDTH} x
 * {@value dev.emi.remastered.enchanting.recipe.EnchantmentRecipe#HEIGHT}; the bottom 12 pixels are taken by the
 * previous/next buttons when there is more than one page.
 */
public interface InfoPage {

	/** Adds zero or more pages with {@link PagedWidgets#newPage()}. An exception drops the pages started so far. */
	void addPages(EnchantmentInfo info, PagedWidgets pages);
}
