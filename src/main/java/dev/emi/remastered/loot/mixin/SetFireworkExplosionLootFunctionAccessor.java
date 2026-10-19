package dev.emi.remastered.loot.mixin;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.loot.function.SetFireworkExplosionLootFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(SetFireworkExplosionLootFunction.class)
public interface SetFireworkExplosionLootFunctionAccessor {
	@Accessor("shape")
	Optional<FireworkExplosionComponent.Type> emi_loot$getShape();

	@Accessor("colors")
	Optional<IntList> emi_loot$getColors();

	@Accessor("fadeColors")
	Optional<IntList> emi_loot$getFadeColors();

	@Accessor("trail")
	Optional<Boolean> emi_loot$getTrail();

	@Accessor("twinkle")
	Optional<Boolean> emi_loot$getTwinkle();

	@Invoker("apply")
	FireworkExplosionComponent emi_loot$callApply(FireworkExplosionComponent current);
}