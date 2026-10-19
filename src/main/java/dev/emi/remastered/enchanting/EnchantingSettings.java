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

	@Comment("The most enchanted book levels listed per enchantment. Enchantments with a higher maximum level "
		+ "list levels up to this number and the maximum level. Books of any other level still open the info page.")
	@ConfigValue("enchanting.book-level-limit")
	public static int bookLevelLimit = 10;

	private EnchantingSettings() {
	}
}
