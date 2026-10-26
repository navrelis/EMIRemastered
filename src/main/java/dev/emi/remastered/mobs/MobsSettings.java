package dev.emi.remastered.mobs;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/** Settings of the mobs module, shown in the EMI config screen. The fields are read live by the mob pages. */
public class MobsSettings {

	@Comment("Show a heart icon next to health and damage and an armour icon next to armour. The numbers are always points (2 points are one heart or armour icon).")
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

	@Comment("Add the mobs whose spawn egg is missing or not in the index (ender dragon, wither, giant, illusioner, armor stand, ...) to the EMI index as mob stacks, so that their page can be opened with R and found by name. Needs an EMI reload.")
	@ConfigValue("mobs.eggless-in-index")
	public static boolean egglessInIndex = true;

	public static int compactBiomeCount() {
		return Math.max(1, Math.min(20, compactBiomeCount));
	}
}
