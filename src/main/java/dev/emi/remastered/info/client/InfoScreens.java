package dev.emi.remastered.info.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiHistory;
import dev.emi.emi.runtime.EmiSidebars;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.info.InfoModule;
import dev.emi.remastered.info.InfoSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

/** Opening the Item Info screen and EMI's recipe screen from it, with EMI's back history. Client render thread only. */
public final class InfoScreens {

	private InfoScreens() {
	}

	/**
	 * EMI stack interaction hook (hovered stack + a key or mouse bind, see the EmiScreenManager mixin): opens the
	 * Item Info screen when the module is on and the bind matches.
	 */
	public static boolean onStackInteraction(EmiStackInteraction interaction, Function<EmiBind, Boolean> function) {
		if (!InfoModule.enabled || interaction.getStack().isEmpty()) {
			return false;
		}
		EmiBind bind = InfoSettings.openInfo;
		if (!bind.isBound() || !function.apply(bind)) {
			return false;
		}
		return open(interaction.getStack());
	}

	/** The single stack the Item Info screen should show for an ingredient (first stack of tags/lists), or EMPTY. */
	public static EmiStack toStack(EmiIngredient ingredient) {
		if (ingredient instanceof EmiFavorite fav) {
			ingredient = fav.getStack();
		}
		List<EmiStack> stacks = ingredient.getEmiStacks();
		if (stacks.isEmpty() || stacks.get(0).isEmpty()) {
			return EmiStack.EMPTY;
		}
		return stacks.get(0).copy().setAmount(1);
	}

	/**
	 * Opens the Item Info screen for {@code ingredient}, returning to the current screen on close. Does nothing (and
	 * returns false) while the module is off.
	 */
	public static boolean open(EmiIngredient ingredient) {
		if (!InfoModule.enabled) {
			return false;
		}
		EmiStack stack = toStack(ingredient);
		MinecraftClient client = MinecraftClient.getInstance();
		if (stack.isEmpty() || client.player == null) {
			return false;
		}
		Screen current = client.currentScreen;
		if (current instanceof InfoScreen info && info.stack.isEqual(stack)) {
			return true;
		}
		HandledScreen<?> old = EmiApi.getHandledScreen();
		if (old == null) {
			old = new InventoryScreen(client.player);
			client.setScreen(old);
			current = old;
		}
		push(current);
		client.setScreen(new InfoScreen(stack, old));
		return true;
	}

	/**
	 * Shows EMI's recipe screen with {@code recipes} on top of the current screen (like EMI's own private
	 * {@code EmiApi.setPages}, which the mixin redirects here while the Item Info screen is open).
	 */
	public static boolean showRecipes(Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient context) {
		Map<EmiRecipeCategory, List<EmiRecipe>> pages = new LinkedHashMap<>();
		recipes.forEach((category, list) -> {
			if (!list.isEmpty()) {
				pages.put(category, list);
			}
		});
		MinecraftClient client = MinecraftClient.getInstance();
		if (pages.isEmpty() || client.player == null) {
			return false;
		}
		if (!context.isEmpty()) {
			EmiSidebars.lookup(context);
		}
		Screen current = client.currentScreen;
		HandledScreen<?> old = EmiApi.getHandledScreen();
		if (old == null) {
			old = new InventoryScreen(client.player);
			client.setScreen(old);
			current = old;
		}
		push(current);
		client.setScreen(new RecipeScreen(old, pages));
		return true;
	}

	/** Same history rule as EMI: a handled screen starts a new history, EMI screens are pushed onto it. */
	private static void push(Screen current) {
		if (current instanceof HandledScreen<?>) {
			EmiHistory.clear();
		}
		if (current != null) {
			EmiHistory.push(current);
		}
	}

	/** Closes {@code screen}: back to the previous screen in EMI's history, or to the handled screen below it. */
	static void close(InfoScreen screen) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!EmiHistory.isEmpty()) {
			EmiHistory.pop();
		} else {
			client.setScreen(screen.old);
		}
	}
}
