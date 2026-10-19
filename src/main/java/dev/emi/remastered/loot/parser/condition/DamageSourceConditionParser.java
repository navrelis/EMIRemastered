package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.DamageSourcePredicateParser;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.DamageSourcePropertiesLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.predicate.entity.DamageSourcePredicate;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

public class DamageSourceConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        DamageSourcePredicate damageSourcePredicate = ((DamageSourcePropertiesLootCondition)condition).predicate().orElse(null); // TODO?
		if (damageSourcePredicate == null) {
			return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.damage_source", LText.literal("Unknown"))));
		}
		Text damageText = DamageSourcePredicateParser.parseDamageSourcePredicate(damageSourcePredicate);
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.damage_source", damageText)));
    }
}