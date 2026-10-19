package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetNameLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class SetNameFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        Optional<Text> text = ((SetNameLootFunctionAccessor)function).emi_loot$getName();
        if (!stack.isEmpty()) {
            text.ifPresent(value -> stack.set(DataComponentTypes.CUSTOM_NAME, value));
        }
        return new LootTableParser.LootFunctionResult(TextKey.empty(), stack, conditionTexts);
    }
}