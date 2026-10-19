package dev.emi.remastered.distractionfree;

/** The kinds of screens the per-screen settings of distraction-free mode distinguish. Pure logic, no Minecraft classes. */
public enum ScreenKind {
	/** The player inventory (survival and creative). */
	INVENTORY,
	/** The crafting table. */
	CRAFTING,
	/** Every other screen EMI shows its sidebars on: chests, furnaces, villagers, screens of other mods, ... */
	CONTAINER,
	/** EMI's own recipe screen and recipe tree screen. */
	RECIPE,
}
