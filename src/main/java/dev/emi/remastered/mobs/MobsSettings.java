package dev.emi.remastered.mobs;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/** Settings of the mobs module, shown in the EMI config screen. The fields are read live by the mob pages. */
public class MobsSettings {

	@Comment("Show health, attack damage and armour as heart and armour icons instead of plain numbers.")
	@ConfigValue("mobs.attributes-as-icons")
	public static boolean attributesAsIcons = true;

	@Comment("How many spawn biomes are listed on the page before '+N more'. The full list is in the tooltip (1 to 20).")
	@ConfigValue("mobs.compact-biome-count")
	public static int compactBiomeCount = 5;

	@Comment("Show spawn weight and group size next to every spawn biome.")
	@ConfigValue("mobs.show-spawn-details")
	public static boolean showSpawnDetails = true;

	@Comment("Show the link that opens the drops of the mob (needs the loot module).")
	@ConfigValue("mobs.show-drops-link")
	public static boolean showDropsLink = true;

	public static int compactBiomeCount() {
		return Math.max(1, Math.min(20, compactBiomeCount));
	}
}
