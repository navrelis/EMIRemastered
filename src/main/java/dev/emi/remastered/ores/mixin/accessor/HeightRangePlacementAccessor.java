/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeightRangePlacementModifier.class)
public interface HeightRangePlacementAccessor {
	@Accessor("height")
	HeightProvider emi_ores$getHeight();
}
