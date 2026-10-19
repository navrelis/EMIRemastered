package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.ItemPredicateParser;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

public class MatchToolConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        ItemPredicate predicate = ((MatchToolLootCondition)condition).predicate().orElse(null); // TODO?
		if (predicate == null) {
			return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.match_tool", LText.literal("unknown"))));
		}
        Text predicateText = ItemPredicateParser.parseItemPredicate(predicate);
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.match_tool", predicateText)));
    }
}