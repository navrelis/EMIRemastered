package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.ModifyContentsLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.registry.LootParserRegistry;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;

import java.util.List;

public class ModifyContentsFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        LootFunction f = ((ModifyContentsLootFunctionAccessor)function).emi_loot$getModifier();
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.modify_contents", LootParserRegistry.parseFunction(f, stack, f.getType(), parentIsAlternative, List.of()).text().asText()), ItemStack.EMPTY, conditionTexts);
    }
}