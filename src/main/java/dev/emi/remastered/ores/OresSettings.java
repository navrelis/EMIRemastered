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
}
