package dev.emi.remastered.stats.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

/** The food data of a stack, read from its {@code DataComponentTypes.FOOD} component (works for modded items). */
record FoodFacts(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, ItemStack convertsTo, List<EffectFact> effects) {

	/** One status effect of a food with its chance. */
	record EffectFact(StatusEffectInstance instance, float chance) {
	}

	@Nullable
	static FoodFacts of(ItemStack stack) {
		FoodComponent food = stack.get(DataComponentTypes.FOOD);
		if (food == null) {
			return null;
		}
		List<EffectFact> effects = new ArrayList<>();
		for (FoodComponent.StatusEffectEntry entry : food.effects()) {
			// the entry returns a copy on every call
			effects.add(new EffectFact(entry.effect(), entry.probability()));
		}
		ItemStack converts = food.usingConvertsTo().orElse(ItemStack.EMPTY);
		return new FoodFacts(food.nutrition(), food.saturation(), food.canAlwaysEat(), food.eatSeconds(), converts, List.copyOf(effects));
	}
}
