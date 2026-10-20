package dev.emi.remastered.world;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Settings of the {@code world} module (group {@code world}); the switch is {@link WorldModule#enabled}.
 * Lang keys in {@code assets/emi_world/lang/}.
 */
public final class WorldSettings {

	@Comment("Adds the Biome category: one page per biome with climate, tags, dimensions, mobs, structures and ores. Needs an EMI reload.")
	@ConfigValue("world.biome-pages")
	public static boolean biomePages = true;

	@Comment("Adds all biomes to the EMI index so that they can be searched and opened with R. Needs the biome pages and an EMI reload. If the ores module adds the biomes (ores.add-biomes-to-index), they are not added a second time.")
	@ConfigValue("world.biomes-in-index")
	public static boolean biomesInIndex = true;

	@Comment("Adds the Structure category: one page per structure with dimensions, biomes, generation step, spacing and loot. Needs an EMI reload.")
	@ConfigValue("world.structure-pages")
	public static boolean structurePages = true;

	@Comment("Adds all structures to the EMI index so that they can be searched (needs the structure pages and an EMI reload).")
	@ConfigValue("world.structures-in-index")
	public static boolean structuresInIndex = true;

	@Comment("How many rows of icons a section of a page (mobs, structures, ores, biomes) shows before '+N more' (1 to 4).")
	@ConfigValue("world.icon-rows")
	public static int iconRows = 2;

	@Comment("How many loot table links a structure page lists before '+N more'. 0 hides the loot section (needs the loot module).")
	@ConfigValue("world.max-loot-links")
	public static int maxLootLinks = 6;

	private WorldSettings() {
	}

	/** The row limit, kept in a sensible range. */
	public static int iconRows() {
		return Math.max(1, Math.min(4, iconRows));
	}

	public static int maxLootLinks() {
		return Math.max(0, Math.min(12, maxLootLinks));
	}
}
