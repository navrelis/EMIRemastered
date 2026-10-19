package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.EnchantmentLevelBasedValueParser;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.RandomChanceWithEnchantedBonusLootCondition;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RandomChanceWithLootingConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        float chance = ((RandomChanceWithEnchantedBonusLootCondition)condition).unenchantedChance();
        EnchantmentLevelBasedValue multiplier = ((RandomChanceWithEnchantedBonusLootCondition)condition).enchantedChance();
        RegistryEntry<Enchantment> enchant = ((RandomChanceWithEnchantedBonusLootCondition)condition).enchantment();
        List<Text> args = new ArrayList<>(Arrays.stream(new Text[]{EnchantmentLevelBasedValueParser.parseValue(multiplier, "%", 100f), LText.enchant(enchant)}).toList());
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.chance_looting", args)));
    }
}
