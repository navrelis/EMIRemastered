package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.InvertedLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class InvertedConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        LootCondition term = ((InvertedLootCondition)condition).term();
        List<LootTableParser.LootConditionResult> termResults = LootTableParser.parseLootCondition(term, stack);
        List<LootTableParser.LootConditionResult> finalResults = new ArrayList<>();
        termResults.forEach((result)-> {
            Text resultText = result.getText().process(stack, null).text();
            finalResults.add(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.invert", resultText)));
        });
        return finalResults;
    }
}