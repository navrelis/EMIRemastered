package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.LimitCountLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.NumberProcessors;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.text.Text;

import java.util.List;

public class LimitCountFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        BoundedIntUnaryOperator operator = ((LimitCountLootFunctionAccessor)function).emi_loot$getLimit();
        Text limit = NumberProcessors.processBoundedIntUnaryOperator(operator);
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.limit_count", limit), ItemStack.EMPTY, conditionTexts);
    }
}