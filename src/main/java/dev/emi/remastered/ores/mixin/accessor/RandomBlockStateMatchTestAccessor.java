/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.block.BlockState;
import net.minecraft.structure.rule.RandomBlockStateMatchRuleTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomBlockStateMatchRuleTest.class)
public interface RandomBlockStateMatchTestAccessor {
	@Accessor("blockState")
	BlockState emi_ores$getBlockState();

	@Accessor("probability")
	float emi_ores$getProbability();
}
