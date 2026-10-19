/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.ores.Ores;
import net.minecraft.block.Blocks;

public class OresRecipeCategories {
	public static final EmiRecipeCategory OREGEN = new EmiRecipeCategory(Ores.id("oregen"), EmiStack.of(Blocks.DEEPSLATE_DIAMOND_ORE));
	public static final EmiRecipeCategory GEODE = new EmiRecipeCategory(Ores.id("geode"), EmiStack.of(Blocks.BUDDING_AMETHYST));
}
