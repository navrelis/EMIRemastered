package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.mixin.AlternativeLootConditionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

public class MultiplesConditionParser implements ConditionParser {

    public MultiplesConditionParser(String key) {
        this.key = key;
    }

    private final String key;

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        List<LootCondition> terms = ((AlternativeLootConditionAccessor)condition).emi_loot$getConditions();
        List<Text> args =terms.stream().map((term)-> {
            List<LootTableParser.LootConditionResult> termResults = LootTableParser.parseLootCondition(term, stack);
            return LootTableParser.compileConditionTexts(stack, termResults);
        }).toList();
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of(key, args)));

    }
}