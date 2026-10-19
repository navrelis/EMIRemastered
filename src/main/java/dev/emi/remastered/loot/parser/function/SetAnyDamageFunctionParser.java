package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;

import java.util.List;

public class SetAnyDamageFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Parsing an any-damage function");
        if (!stack.isEmpty()) {
            stack.setDamage((1 + stack.getMaxDamage()) / 2);
        }
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_any_damage"), stack, conditionTexts);
    }
}