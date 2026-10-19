package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetStewEffectLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.ListProcessors;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.SetStewEffectLootFunction;
import net.minecraft.text.MutableText;

import java.util.LinkedList;
import java.util.List;

public class SetStewFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        List<SetStewEffectLootFunction.StewEffect> effects = ((SetStewEffectLootFunctionAccessor)function).emi_loot$getEffects();
        List<MutableText> list = new LinkedList<>();
        for (SetStewEffectLootFunction.StewEffect effect : effects) {
            list.add(effect.effect().value().getName().copy());
        }
        if (list.isEmpty()) {
            return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_stew", LText.translatable("emi_loot.function.set_stew_unknown")), ItemStack.EMPTY, conditionTexts);
        }
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_stew", ListProcessors.buildOrList(list)), ItemStack.EMPTY, conditionTexts);
    }
}