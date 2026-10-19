/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.DataPool;
import net.minecraft.world.gen.stateprovider.WeightedBlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WeightedBlockStateProvider.class)
public interface WeightedStateProviderAccessor {
	@Accessor("states")
	DataPool<BlockState> emi_ores$getWeightedList();
}
