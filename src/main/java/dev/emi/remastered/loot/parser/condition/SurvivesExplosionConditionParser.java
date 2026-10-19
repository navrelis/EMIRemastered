package dev.emi.remastered.loot.parser.condition;

import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;

import java.util.Collections;
import java.util.List;

public class SurvivesExplosionConditionParser implements ConditionParser {

    @Override
    public List<LootTableParser.LootConditionResult> parseCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        if (parentIsAlternative) return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.of("emi_loot.condition.survives_explosion")));
        return Collections.singletonList(new LootTableParser.LootConditionResult(TextKey.empty()));
    }
}