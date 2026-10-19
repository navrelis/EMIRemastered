package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.NumberProcessors;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.ValueCheckLootCondition;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.text.MutableText;

import java.util.Collections;
import java.util.List;

public class ValueCheckConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        LootNumberProvider value = ((ValueCheckLootCondition)condition).value();
        MutableText processedValue = NumberProcessors.processLootNumberProvider(value);
        BoundedIntUnaryOperator range = ((ValueCheckLootCondition)condition).range();
        MutableText processedRange = NumberProcessors.processBoundedIntUnaryOperator(range);
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.value_check", processedValue, processedRange)));
    }
}