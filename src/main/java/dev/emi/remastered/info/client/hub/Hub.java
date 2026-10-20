package dev.emi.remastered.info.client.hub;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.info.InfoModule;
import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.client.InfoScreen;
import dev.emi.remastered.info.client.InfoScreens;
import dev.emi.remastered.info.mixin.EmiHistoryAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * The item view: EMI's recipe screen and the Item Info screen of one stack, joined by a tab bar
 * ({@code Info | Recipes | Uses}). Remembers which stack and tab each recipe screen shows, routes EMI's view
 * binds to the tab of the {@code info.primary-view} setting and switches tabs in place. Render thread only.
 */
public final class Hub {
	private static final Map<Screen, Context> SCREENS = new WeakHashMap<>();
	private static @Nullable Context pending;

	/** The stack a screen focuses and the tab it is. */
	public record Context(EmiStack stack, HubTab tab) {
	}

	private Hub() {
	}

	/** The module and the {@code info.tab-bar} setting are on. */
	public static boolean active() {
		return InfoModule.enabled && InfoSettings.tabBar;
	}

	/** {@code EmiApi.displayRecipes}/{@code displayUses} starts: remember the stack for the screen it creates. */
	public static void beginDisplay(EmiIngredient ingredient, boolean uses) {
		pending = null;
		if (ingredient instanceof EmiFavorite fav) {
			ingredient = fav.getStack();
		}
		if (ingredient == null || ingredient.isEmpty()) {
			return;
		}
		HubTab tab = HubRules.tabOf(HubRules.viewOf(uses, ingredient instanceof TagEmiIngredient,
			ingredient.getEmiStacks().size()));
		EmiStack stack = InfoScreens.toStack(ingredient);
		if (tab != null && !stack.isEmpty()) {
			pending = new Context(stack, tab);
		}
	}

	/** {@code EmiApi.displayRecipes}/{@code displayUses} returned. */
	public static void endDisplay() {
		pending = null;
	}

	/** A recipe screen was created (constructor): it belongs to the display call that is running, if any. */
	public static void attach(RecipeScreen screen) {
		if (pending != null) {
			SCREENS.put(screen, pending);
			pending = null;
		}
	}

	/** The stack and tab of a screen when it shows the tab bar, else null. */
	public static @Nullable Context of(Screen screen) {
		if (!active()) {
			return null;
		}
		if (screen instanceof InfoScreen info) {
			return new Context(info.stack, HubTab.INFO);
		}
		if (screen instanceof RecipeScreen && RecipeScreen.resolve == null) {
			return SCREENS.get(screen);
		}
		return null;
	}

	/** Whether the bar fits above the recipe panel for this screen height (else it is left out). */
	public static boolean fits(int screenHeight) {
		int height = HubRules.emiPanelHeight(screenHeight, EmiConfig.maximumRecipeScreenHeight, EmiConfig.verticalMargin);
		int top = HubRules.emiPanelTop(screenHeight, height);
		return height - HubRules.extraTop(top) >= HubRules.MIN_PANEL_HEIGHT;
	}

	/** Top of the plate (recipe screen) and of the Item Info panel, see {@link HubRules#plateTop}. */
	public static int plateTop(int screenHeight) {
		return HubRules.plateTop(screenHeight, EmiConfig.maximumRecipeScreenHeight, EmiConfig.verticalMargin);
	}

	/** Width available to the bar; the same on every screen, so the tabs stay in place when switching. */
	public static int barWidth() {
		return Math.max(EmiConfig.minimumRecipeScreenWidth, 56) - 8;
	}

	/**
	 * EMI's "view recipes"/"view uses" bind on a stack, before EMI handles it: opens the Item Info tab instead when
	 * the primary view setting asks for it or the bind's own tab is empty. Returns false to let EMI handle the bind.
	 */
	public static boolean onViewBind(EmiIngredient ingredient, HubRules.Bind bind) {
		if (!active()) {
			return false;
		}
		if (ingredient instanceof EmiFavorite fav) {
			ingredient = fav.getStack();
		}
		if (ingredient instanceof TagEmiIngredient || ingredient.getEmiStacks().size() != 1) {
			return false;
		}
		EmiStack stack = InfoScreens.toStack(ingredient);
		if (stack.isEmpty()) {
			return false;
		}
		HubTab wanted = HubRules.forBind(bind, InfoSettings.primaryView.tab());
		HubTab tab = switch (wanted) {
			case RECIPES -> HubRules.resolveOpen(wanted, HubCounts.recipes(stack), 0);
			case USES -> HubRules.resolveOpen(wanted, 0, HubCounts.uses(stack));
			case INFO -> HubTab.INFO;
		};
		if (tab != HubTab.INFO) {
			return false;
		}
		return InfoScreens.open(stack);
	}

	/** Counts shown in the tab labels. */
	static int count(EmiStack stack, HubTab tab) {
		return switch (tab) {
			case INFO -> 1;
			case RECIPES -> HubCounts.recipes(stack);
			case USES -> HubCounts.uses(stack);
		};
	}

	/**
	 * Switches the item view of {@code stack} to {@code tab} in place: the new screen replaces the current one in
	 * EMI's back history.
	 */
	public static void switchTo(EmiStack stack, HubTab tab) {
		MinecraftClient client = MinecraftClient.getInstance();
		Screen before = client.currentScreen;
		List<Screen> history = EmiHistoryAccessor.getHistories();
		int size = history.size();
		switch (tab) {
			case INFO -> InfoScreens.open(stack);
			case RECIPES -> EmiApi.displayRecipes(stack);
			case USES -> EmiApi.displayUses(stack);
		}
		if (client.currentScreen != before) {
			HubRules.replaceTop(history, size);
		}
	}
}
