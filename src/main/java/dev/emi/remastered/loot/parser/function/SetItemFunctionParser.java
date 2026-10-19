package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetItemLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;

public class SetItemFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        RegistryEntry<Item> item = ((SetItemLootFunctionAccessor)function).emi_loot$getItem();
        stack = stack.copyComponentsToNewStack(item.value(), stack.getCount());
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_item", item.value().getName()), stack, conditionTexts);
    }
}