package dev.emi.remastered.loot.parser;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.util.LText;
import net.minecraft.predicate.entity.DamageSourcePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.text.Text;

import java.util.Optional;

public class DamageSourcePredicateParser {

    public static Text parseDamageSourcePredicate(DamageSourcePredicate predicate) {
        Optional<EntityPredicate> directPredicate = predicate.directEntity();
        if (directPredicate.isPresent()) {
            return EntityPredicateParser.parseEntityPredicate(directPredicate.get());
        }

        Optional<EntityPredicate> sourcePredicate = predicate.sourceEntity();
        if (sourcePredicate.isPresent()) {
            return EntityPredicateParser.parseEntityPredicate(sourcePredicate.get());
        }

        if (EMILoot.isDebug()) EMILoot.LOGGER.warn("Empty or unparsable damage source predicate in table: "  + LootTableParser.currentTable);
        return LText.translatable("emi_loot.predicate.invalid");
    }

}