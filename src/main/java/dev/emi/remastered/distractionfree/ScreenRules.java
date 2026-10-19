package dev.emi.remastered.distractionfree;

/** Which kinds of screens distraction-free mode applies to. Pure logic, no Minecraft classes. */
public final class ScreenRules {
	private ScreenRules() {
	}

	/**
	 * @param keepRecipeScreens EMI's recipe screens keep their sidebars (the setting {@code keep-on-recipe-screens})
	 */
	public static boolean applies(ScreenKind kind, boolean inventory, boolean crafting, boolean containers, boolean keepRecipeScreens) {
		return switch (kind) {
			case INVENTORY -> inventory;
			case CRAFTING -> crafting;
			case CONTAINER -> containers;
			case RECIPE -> !keepRecipeScreens;
		};
	}
}
