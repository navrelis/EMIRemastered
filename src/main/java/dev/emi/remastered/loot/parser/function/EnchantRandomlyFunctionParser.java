package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.function.LootFunction;

import java.util.List;

public class EnchantRandomlyFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        if (stack.isOf(Items.BOOK)) {
            stack = new ItemStack(Items.ENCHANTED_BOOK);
            return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.randomly_enchanted_book"), stack, conditionTexts);
        } else {
            return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.randomly_enchanted_item"), ItemStack.EMPTY, conditionTexts);
        }
    }
}