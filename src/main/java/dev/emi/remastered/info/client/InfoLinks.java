package dev.emi.remastered.info.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.info.api.InfoLink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

/** Implementations of the {@link InfoLink} factories. */
public final class InfoLinks {

	private InfoLinks() {
	}

	private static boolean changed(Screen before) {
		return MinecraftClient.getInstance().currentScreen != before;
	}

	private static void focus(@Nullable EmiRecipeCategory category) {
		if (category != null && MinecraftClient.getInstance().currentScreen instanceof RecipeScreen rs) {
			rs.focusCategory(category);
		}
	}

	public static InfoLink recipe(Identifier recipeId) {
		return () -> {
			EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(recipeId);
			return recipe != null && recipe(recipe).open();
		};
	}

	public static InfoLink recipe(EmiRecipe recipe) {
		return () -> {
			Screen before = MinecraftClient.getInstance().currentScreen;
			EmiApi.displayRecipe(recipe);
			return changed(before);
		};
	}

	public static InfoLink recipes(List<EmiRecipe> recipes) {
		List<EmiRecipe> copy = List.copyOf(recipes);
		return () -> {
			Map<EmiRecipeCategory, List<EmiRecipe>> map = new LinkedHashMap<>();
			for (EmiRecipe recipe : copy) {
				map.computeIfAbsent(recipe.getCategory(), c -> new ArrayList<>()).add(recipe);
			}
			return InfoScreens.showRecipes(map, EmiStack.EMPTY);
		};
	}

	public static InfoLink category(Identifier categoryId) {
		return () -> {
			for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
				if (category.getId().equals(categoryId)) {
					return category(category).open();
				}
			}
			return false;
		};
	}

	public static InfoLink category(EmiRecipeCategory category) {
		return () -> {
			Screen before = MinecraftClient.getInstance().currentScreen;
			EmiApi.displayRecipeCategory(category);
			return changed(before);
		};
	}

	public static InfoLink recipesOf(EmiStack stack, @Nullable EmiRecipeCategory focus) {
		return () -> {
			Screen before = MinecraftClient.getInstance().currentScreen;
			EmiApi.displayRecipes(stack);
			focus(focus);
			return changed(before);
		};
	}

	public static InfoLink usesOf(EmiStack stack, @Nullable EmiRecipeCategory focus) {
		return () -> {
			Screen before = MinecraftClient.getInstance().currentScreen;
			EmiApi.displayUses(stack);
			focus(focus);
			return changed(before);
		};
	}

	public static InfoLink info(EmiStack stack) {
		return () -> InfoScreens.open(stack);
	}
}
