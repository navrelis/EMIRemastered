package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.AndLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.ListProcessors;
import dev.emi.remastered.loot.parser.registry.LootParserRegistry;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;

import java.util.List;

public class SequenceFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        List<LootFunction> list = ((AndLootFunctionAccessor)function).emi_loot$getTerms();
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.sequence", ListProcessors.buildAndList(list.stream().map(lf -> LootParserRegistry.parseFunction(lf, stack, lf.getType(), parentIsAlternative, List.of()).text().asText().copyContentOnly()).toList())), stack, conditionTexts);
    }
}