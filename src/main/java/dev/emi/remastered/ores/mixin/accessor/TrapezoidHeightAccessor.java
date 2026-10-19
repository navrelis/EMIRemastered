/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.heightprovider.TrapezoidHeightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrapezoidHeightProvider.class)
public interface TrapezoidHeightAccessor {
	@Accessor("minOffset")
	YOffset emi_ores$getMinInclusive();

	@Accessor("maxOffset")
	YOffset emi_ores$getMaxInclusive();

	@Accessor("plateau")
	int emi_ores$getPlateau();
}
