package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetOminousBottleAmplifierLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.NumberProcessors;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.text.Text;

import java.util.List;

public class SetOminousBottleFunctionParser implements FunctionParser {

	@Override
	public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
		LootNumberProvider amplifier = ((SetOminousBottleAmplifierLootFunctionAccessor)function).emi_loot$getAmplifier();
		Text a = NumberProcessors.processLootNumberProvider(amplifier);
		return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.ominous_bottle", a), ItemStack.EMPTY, conditionTexts);
	}
}