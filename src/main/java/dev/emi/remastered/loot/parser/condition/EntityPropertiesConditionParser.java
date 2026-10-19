package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.EntityPredicateParser;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.text.MutableText;

import java.util.Collections;
import java.util.List;

public class EntityPropertiesConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        LootContext.EntityTarget entity = ((EntityPropertiesLootCondition)condition).entity();
        EntityPredicate predicate = ((EntityPropertiesLootCondition)condition).predicate().orElse(null); // TODO?
		if (predicate == null) {
			return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.entity_props", LText.literal("Unknown"))));
		}
        MutableText propText;
        if (entity == LootContext.EntityTarget.THIS) {
            propText = LText.translatable("emi_loot.entity_predicate.entity_this", EntityPredicateParser.parseEntityPredicate(predicate));
        } else {
            propText = LText.translatable("emi_loot.entity_predicate.entity_killer", EntityPredicateParser.parseEntityPredicate(predicate));
        }
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.entity_props", propText)));
    }
}