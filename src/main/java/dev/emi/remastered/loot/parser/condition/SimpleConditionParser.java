package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;

import java.util.Collections;
import java.util.List;

public class SimpleConditionParser implements ConditionParser {
    
    private final TextKey key;
    
    public SimpleConditionParser(String key) {
        this.key = TextKey.of(key);
    }
    
    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        return Collections.singletonList(new LootTableParser.LootConditionResult(key));
    }
}