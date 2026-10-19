/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.stateprovider.NoiseBlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseBlockStateProvider.class)
public interface NoiseProviderAccessor {
	@Accessor("states")
	List<BlockState> emi_ores$getStates();
}
