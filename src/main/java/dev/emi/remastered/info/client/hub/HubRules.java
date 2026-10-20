package dev.emi.remastered.info.client.hub;

import java.util.List;

/** The item view's rules as pure functions (no Minecraft or EMI state), see {@link Hub} for their use. */
public final class HubRules {
	/** Visible height of a tab above the plate or panel it merges into. */
	public static final int TAB_HEIGHT = 20;
	/** Extra height of the selected tab (like EMI's category tabs). */
	public static final int SELECTED_RAISE = 2;
	/** Space from the plate top to the recipe panel top: EMI's category tabs (26) plus the plate border (4). */
	public static final int PLATE_HEIGHT = 30;
	/** Free space kept between the top of the screen and the selected tab. */
	public static final int TOP_MARGIN = 2;
	/** Space the bar and plate need above the recipe panel. */
	public static final int BAND = PLATE_HEIGHT + TAB_HEIGHT + SELECTED_RAISE + TOP_MARGIN;
	/** The recipe panel is not made smaller than this for the bar; below it the bar is left out. */
	public static final int MIN_PANEL_HEIGHT = 64;

	private HubRules() {
	}

	/** EMI binds that open the item view on a stack. */
	public enum Bind {
		/** EMI's "view recipes" (R, left click). */
		VIEW_RECIPES,
		/** EMI's "view uses" (U, right click). */
		VIEW_USES,
		/** The info module's "open item info" (I). */
		OPEN_INFO
	}

	/** What a recipe screen (or the Item Info screen) shows. */
	public enum View {
		/** {@code displayRecipes} of one stack. */
		STACK_RECIPES,
		/** {@code displayUses} of one stack. */
		STACK_USES,
		/** The Item Info screen of one stack. */
		STACK_INFO,
		/** {@code displayRecipes} of a tag (the tag page). */
		TAG,
		/** {@code displayRecipes}/{@code displayUses} of a list ingredient or several stacks. */
		MULTI,
		/** A category ({@code displayRecipeCategory}, the category name button). */
		CATEGORY,
		/** All recipes ({@code displayAllRecipes}, Ctrl+Y). */
		ALL,
		/** A single recipe, a recipe list from an Item Info row, or any other screen made without a stack. */
		OTHER,
		/** A recipe screen opened from the recipe tree to pick a recipe for a resolution. */
		RESOLVE
	}

	/** The tab a bind opens: "view recipes" follows the primary view setting, "view uses" and "item info" are fixed. */
	public static HubTab forBind(Bind bind, HubTab primary) {
		return switch (bind) {
			case VIEW_RECIPES -> primary == HubTab.USES ? HubTab.RECIPES : primary;
			case VIEW_USES -> HubTab.USES;
			case OPEN_INFO -> HubTab.INFO;
		};
	}

	/** The tab to open when {@code wanted} is empty: Info (which always has something to show). */
	public static HubTab resolveOpen(HubTab wanted, int recipes, int uses) {
		return switch (wanted) {
			case RECIPES -> recipes > 0 ? wanted : HubTab.INFO;
			case USES -> uses > 0 ? wanted : HubTab.INFO;
			case INFO -> HubTab.INFO;
		};
	}

	/** Whether a tab can be opened (Info always, the recipe tabs when they have recipes). */
	public static boolean enabled(HubTab tab, int recipes, int uses) {
		return switch (tab) {
			case INFO -> true;
			case RECIPES -> recipes > 0;
			case USES -> uses > 0;
		};
	}

	/** The tab a view selects in the bar, or null when the view gets no bar (no single stack applies). */
	public static HubTab tabOf(View view) {
		return switch (view) {
			case STACK_RECIPES -> HubTab.RECIPES;
			case STACK_USES -> HubTab.USES;
			case STACK_INFO -> HubTab.INFO;
			default -> null;
		};
	}

	/**
	 * Whether the bar is shown: the module and the {@code info.tab-bar} setting are on and the view focuses one stack.
	 */
	public static boolean showsBar(boolean module, boolean setting, View view) {
		return module && setting && tabOf(view) != null;
	}

	/** The view of {@code displayRecipes}/{@code displayUses} for an ingredient. */
	public static View viewOf(boolean uses, boolean tag, int stackCount) {
		if (tag && !uses) {
			return View.TAG;
		}
		if (stackCount != 1) {
			return View.MULTI;
		}
		return uses ? View.STACK_USES : View.STACK_RECIPES;
	}

	/**
	 * Switching tabs replaces the current history entry: everything the switch pushed onto EMI's back history (the
	 * screen it left) is removed again, so Back leaves the item view instead of stepping through its tabs.
	 */
	public static <T> void replaceTop(List<T> history, int sizeBefore) {
		while (history.size() > Math.max(0, sizeBefore)) {
			history.remove(history.size() - 1);
		}
	}

	/**
	 * How many pixels the recipe panel must give up at its top so the bar fits above EMI's category tabs; the panel
	 * keeps its bottom edge (EMI's centred search bar sits below it).
	 */
	public static int extraTop(int panelTop) {
		return Math.max(0, BAND - panelTop);
	}

	/** EMI's recipe panel height ({@code RecipeScreen.init}). */
	public static int emiPanelHeight(int screenHeight, int maxHeight, int verticalMargin) {
		return Math.min(maxHeight, screenHeight - 52 - verticalMargin);
	}

	/** EMI's recipe panel top ({@code RecipeScreen.init}). */
	public static int emiPanelTop(int screenHeight, int panelHeight) {
		return (screenHeight - panelHeight) / 2 + 1;
	}

	/** Top of the plate (and of the Item Info panel) for a screen height, with the bar shown. */
	public static int plateTop(int screenHeight, int maxHeight, int verticalMargin) {
		int top = emiPanelTop(screenHeight, emiPanelHeight(screenHeight, maxHeight, verticalMargin));
		return top + extraTop(top) - PLATE_HEIGHT;
	}

	/** Label levels, from most to least detailed. */
	public enum Fit {
		/** Info tab with icon and label, recipe tabs with label and count. */
		FULL,
		/** Info tab icon only, recipe tabs with label and count. */
		ICON_INFO,
		/** Info tab icon only, recipe tabs with label only (counts in the tooltip). */
		NO_COUNTS,
		/** Info tab icon only, recipe tabs with their short labels (e.g. German "Verw."). */
		SHORT,
		/** Info tab icon only, short labels cut with "..." to fit. */
		TRIMMED
	}

	/**
	 * The most detailed level whose total width fits into {@code available}.
	 *
	 * @param infoIcon width of the icon-only Info tab
	 * @param infoFull width of the Info tab with icon and label
	 * @param labels widths of the recipe tabs with their label only
	 * @param withCounts widths of the recipe tabs with label and count
	 * @param shortLabels widths of the recipe tabs with their short label
	 */
	public static Fit fit(int available, int infoIcon, int infoFull, int[] labels, int[] withCounts,
			int[] shortLabels) {
		int counts = sum(withCounts);
		if (infoFull + counts <= available) {
			return Fit.FULL;
		}
		if (infoIcon + counts <= available) {
			return Fit.ICON_INFO;
		}
		if (infoIcon + sum(labels) <= available) {
			return Fit.NO_COUNTS;
		}
		if (infoIcon + sum(shortLabels) <= available) {
			return Fit.SHORT;
		}
		return Fit.TRIMMED;
	}

	private static int sum(int[] values) {
		int sum = 0;
		for (int value : values) {
			sum += value;
		}
		return sum;
	}
}
