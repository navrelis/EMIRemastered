package dev.emi.remastered.stats.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.HoneyBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** The food data of a stack, read from its {@code DataComponentTypes.FOOD} component (works for modded items). */
record FoodFacts(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, ItemStack convertsTo, List<EffectFact> effects) {
	private static final float TICK_RATE = 20.0f;

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
		return new FoodFacts(food.nutrition(), food.saturation(), food.canAlwaysEat(), eatSeconds(stack, food), convertsTo(stack, food),
			List.copyOf(effects));
	}

	/**
	 * The time the item really takes to use. Items override {@code Item#getMaxUseTime} (the honey bottle takes 2 s
	 * while its component says 1.6 s); the default implementation returns the component's time, so asking the item
	 * is right for every food. A modded item that cannot answer without a user falls back to the component.
	 */
	private static float eatSeconds(ItemStack stack, FoodComponent food) {
		try {
			int ticks = stack.getItem().getMaxUseTime(stack, MinecraftClient.getInstance().player);
			if (ticks > 0) {
				return ticks / TICK_RATE;
			}
		} catch (RuntimeException | LinkageError e) {
			// fall through to the component
		}
		return food.eatSeconds();
	}

	/**
	 * What eating leaves behind: the component's {@code using_converts_to} (bowls), else the glass bottle of the
	 * honey bottle, which {@code HoneyBottleItem#finishUsing} hands out in code, not through the component. The
	 * item's crafting remainder is deliberately not used: it describes crafting, and would claim a leftover for any
	 * modded food that merely has a crafting remainder.
	 */
	private static ItemStack convertsTo(ItemStack stack, FoodComponent food) {
		ItemStack converts = food.usingConvertsTo().orElse(ItemStack.EMPTY);
		if (converts.isEmpty() && stack.getItem() instanceof HoneyBottleItem) {
			return new ItemStack(Items.GLASS_BOTTLE);
		}
		return converts;
	}
}
