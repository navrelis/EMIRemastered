package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.FilteredLootFunctionAccessor;
import dev.emi.remastered.loot.parser.ItemPredicateParser;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.registry.LootParserRegistry;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.predicate.item.ItemPredicate;

import java.util.List;

public class FilteredFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        ItemPredicate p = ((FilteredLootFunctionAccessor)function).emi_loot$getItemFilter();
        LootFunction f = ((FilteredLootFunctionAccessor)function).emi_loot$getModifier();
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.filtered", ItemPredicateParser.parseItemPredicate(p), LootParserRegistry.parseFunction(f, stack, f.getType(), parentIsAlternative, List.of()).text().asText()), ItemStack.EMPTY, conditionTexts);
    }
}