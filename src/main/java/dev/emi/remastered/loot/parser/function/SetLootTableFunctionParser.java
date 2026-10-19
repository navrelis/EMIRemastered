package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetLootTableLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.RegistryKey;

import java.util.List;

public class SetLootTableFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        RegistryKey<LootTable> id = ((SetLootTableLootFunctionAccessor)function).emi_loot$getLootTable();
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_loot_table", LText.literal(id.getValue().toString())), ItemStack.EMPTY, conditionTexts);
    }
}