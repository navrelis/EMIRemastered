package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.ApplyBonusLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.List;

public class ApplyBonusFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        RegistryEntry<Enchantment> enchant = ((ApplyBonusLootFunctionAccessor)function).emi_loot$getEnchantment();
        Text name = LText.enchant(enchant);
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.bonus", name), ItemStack.EMPTY, conditionTexts);
    }
}