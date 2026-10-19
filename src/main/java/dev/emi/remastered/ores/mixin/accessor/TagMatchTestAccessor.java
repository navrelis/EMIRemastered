/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.rule.TagMatchRuleTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagMatchRuleTest.class)
public interface TagMatchTestAccessor {
	@Accessor("tag")
	TagKey<Block> emi_ores$getTag();
}
