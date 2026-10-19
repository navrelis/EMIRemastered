package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.LocationPredicateParser;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LocationCheckLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

public class LocationCheckConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        LocationPredicate predicate = ((LocationCheckLootCondition)condition).predicate().orElse(null); // TODO?

        Text locText = LocationPredicateParser.parseLocationPredicate(predicate);
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.location", locText)));
    }
}