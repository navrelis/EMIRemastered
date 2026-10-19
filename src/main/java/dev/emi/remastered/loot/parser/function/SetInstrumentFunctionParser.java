package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.mixin.SetInstrumentLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.GoatHornItem;
import net.minecraft.item.Instrument;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.tag.TagKey;

import java.util.List;

public class SetInstrumentFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        TagKey<Instrument> tag = ((SetInstrumentLootFunctionAccessor)function).emi_loot$getInstrumentTag();
        if (!stack.isEmpty()) {
            GoatHornItem.setRandomInstrumentFromTag(stack, tag, EMILoot.emiLootRandom);
        }
        return new LootTableParser.LootFunctionResult(TextKey.empty(), stack, conditionTexts);
    }
}