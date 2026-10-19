package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetFireworksLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.SetFireworksLootFunction;

import java.util.List;

public class SetFireworksFunctionParser implements FunctionParser {

	@Override
	public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
		if (!stack.isEmpty()) {
			stack.apply(DataComponentTypes.FIREWORKS, SetFireworksLootFunction.DEFAULT_FIREWORKS, ((SetFireworksLootFunctionAccessor)function)::emi_loot$callApply);
		}
		return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.fireworks"), ItemStack.EMPTY, conditionTexts);
	}
}