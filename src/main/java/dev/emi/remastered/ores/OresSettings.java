/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

public class OresSettings {

	@Comment("Whether all biomes should be added to the EMI index (applies after an EMI reload).")
	@ConfigValue("ores.add-biomes-to-index")
	public static boolean addBiomesToIndex = true;

	@Comment("Whether ore distribution graphs should show height values resolved for the current dimension by default, instead of relative to the world bottom and top. Holding shift inverts this.")
	@ConfigValue("ores.show-height-values-for-current-dimension-by-default")
	public static boolean showHeightValuesForCurrentDimensionByDefault = false;

	@Comment("How many biome icons a biome list shows before it is cut down to a +N more label (1 to 16). Whole dimensions are summed up as All biomes.")
	@ConfigValue("ores.biome-icons")
	public static int biomeIcons = 5;

	@Comment("Mark the player's current height on the height graph when the page is for the player's dimension.")
	@ConfigValue("ores.show-player-height")
	public static boolean showPlayerHeight = true;

	@Comment("Highlight the best height (the peak of the distribution, or the whole band for uniform ones) on the height graph.")
	@ConfigValue("ores.highlight-best-height")
	public static boolean highlightBestHeight = true;

	/** The icon limit, kept in a sensible range. */
	public static int biomeIconLimit() {
		return Math.max(1, Math.min(16, biomeIcons));
	}
}
