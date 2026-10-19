/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.block.Block;
import net.minecraft.structure.rule.RandomBlockMatchRuleTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomBlockMatchRuleTest.class)
public interface RandomBlockMatchTestAccessor {
	@Accessor("block")
	Block emi_ores$getBlock();

	@Accessor("probability")
	float emi_ores$getProbability();
}
