package dev.emi.remastered.loot.mixin;

import net.minecraft.predicate.entity.EntitySubPredicateTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySubPredicateTypes.VariantType.VariantPredicate.class)
public interface VariantPredicateAccessor<V> {
	@Accessor("variant")
	V emi_loot$getVariant();
}