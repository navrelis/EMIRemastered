package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;

import java.util.List;

public class SimpleFunctionParser implements FunctionParser {
    
    private final TextKey key;
  
    public SimpleFunctionParser(String key) {
      this.key = TextKey.of(key);
    }
    
    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        return new LootTableParser.LootFunctionResult(key, ItemStack.EMPTY, conditionTexts);
    }
}