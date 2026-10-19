package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.FilteredLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.predicate.item.ItemPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FilteredLootFunction.class)
public interface FilteredLootFunctionAccessor {
	@Accessor("itemFilter")
	ItemPredicate emi_loot$getItemFilter();

	@Accessor("modifier")
	LootFunction emi_loot$getModifier();
}