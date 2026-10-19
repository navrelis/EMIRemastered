package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetCountLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.NumberProcessors;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;

import java.util.List;

public class SetCountFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        LootNumberProvider provider = ((SetCountLootFunctionAccessor)function).emi_loot$getCountRange();
        float rollAvg = NumberProcessors.getRollAvg(provider);
        boolean add = ((SetCountLootFunctionAccessor)function).emi_loot$getAdd();
        if (!stack.isEmpty()) {
            if (add) {
                stack.setCount(Math.max(stack.getCount() + (int) rollAvg, 1));
            } else {
                stack.setCount(Math.max((int) rollAvg, 1));
            }
        }
        if (add) {
            return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_count_add"), ItemStack.EMPTY, conditionTexts);
        }
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_count_set"), ItemStack.EMPTY, conditionTexts);
    }
}