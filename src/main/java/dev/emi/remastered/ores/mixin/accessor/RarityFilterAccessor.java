/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.mixin.accessor;

import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RarityFilterPlacementModifier.class)
public interface RarityFilterAccessor {
	@Accessor("chance")
	int emi_ores$getChance();
}
