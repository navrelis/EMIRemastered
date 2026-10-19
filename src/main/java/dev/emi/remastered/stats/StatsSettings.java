package dev.emi.remastered.stats;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Settings of the {@code stats} module (group {@code stats}); the switch is {@link StatsModule#enabled}.
 * Lang keys in {@code assets/emi_stats/lang/}.
 */
public final class StatsSettings {

	@Comment("Adds the Food recipe category: one page per food item with hunger, saturation, eating time and effects. Needs an EMI reload.")
	@ConfigValue("stats.food-category")
	public static boolean foodCategory = true;

	@Comment("Adds Food, Tool, Weapon and Armour sections to the Item Info page of items that have those stats.")
	@ConfigValue("stats.info-sections")
	public static boolean infoSections = true;

	private StatsSettings() {
	}
}
