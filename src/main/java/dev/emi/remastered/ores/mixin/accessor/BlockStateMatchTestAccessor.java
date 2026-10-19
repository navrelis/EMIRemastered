/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.block.BlockState;
import net.minecraft.structure.rule.BlockStateMatchRuleTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStateMatchRuleTest.class)
public interface BlockStateMatchTestAccessor {
	@Accessor("blockState")
	BlockState emi_ores$getBlockState();
}
