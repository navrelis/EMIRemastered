package dev.emi.remastered.enchanting;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/** Settings of the enchanting module (group {@code enchanting}). Read directly from the fields. */
public final class EnchantingSettings {

	@Comment("Whether the info page lists the enchantments that cannot be combined with this one.")
	@ConfigValue("enchanting.show-exclusions")
	public static boolean showExclusions = true;

	@Comment("Whether the info page shows the enchantment's description, if a resource pack or mod provides one "
		+ "(translation key enchantment.<namespace>.<name>.desc).")
	@ConfigValue("enchanting.show-description")
	public static boolean showDescription = true;

	@Comment("Whether looking up the uses of an item that can be enchanted lists the info pages of its enchantments.")
	@ConfigValue("enchanting.show-for-items")
	public static boolean showForItems = true;

	@Comment("Whether the info page and the Item Info screen list where an enchantment can be found: enchanting table, "
		+ "loot, trades, mob equipment, treasure and curse.")
	@ConfigValue("enchanting.show-sources")
	public static boolean showSources = true;

	@Comment("Whether the Item Info screen shows the enchantments of enchanted books and items, and the enchantments "
		+ "that fit a plain item.")
	@ConfigValue("enchanting.show-in-item-info")
	public static boolean showInItemInfo = true;

	@Comment("The most enchanted book levels listed per enchantment. Enchantments with a higher maximum level "
		+ "list levels up to this number and the maximum level.")
	@ConfigValue("enchanting.book-level-limit")
	public static int bookLevelLimit = 10;

	@Comment("Whether the info page also covers enchanted books above the enchantment's maximum level (up to 5 levels "
		+ "above it, at most level 10, never above the level cap of the game), so a book from loot, commands or a mod "
		+ "that lifts the cap still opens the page. Needs an EMI reload.")
	@ConfigValue("enchanting.show-above-max")
	public static boolean showAboveMax = true;

	private EnchantingSettings() {
	}
}
