package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.CopyNameLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.CopyNameLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class CopyNameFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        CopyNameLootFunction.Source source = ((CopyNameLootFunctionAccessor)function).emi_loot$getSource();
        Text sourceText = LText.translatable(sourceKey(source.name()));
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.copy_name", sourceText), ItemStack.EMPTY, conditionTexts);
    }

    public static String sourceKey(String sourceName) {
        return "emi_loot.function.copy_name." + sourceName.toLowerCase(Locale.ROOT);
    }
}
