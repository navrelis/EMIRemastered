package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.TableBonusLootCondition;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

public class TableBonusConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        RegistryEntry<Enchantment> enchant = ((TableBonusLootCondition)condition).enchantment();
        Text name = LText.enchant(enchant);

        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.table_bonus", name)));
    }
}