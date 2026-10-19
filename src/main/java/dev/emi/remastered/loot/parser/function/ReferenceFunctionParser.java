package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.ReferenceLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.registry.LootParserRegistry;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Optional;

public class ReferenceFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        RegistryKey<LootFunction> id = ((ReferenceLootFunctionAccessor)function).emi_loot$getName();
        if (LootTableParser.lootManager != null) {
            Optional<Optional<? extends RegistryEntry<LootFunction>>> referenceFunction = LootTableParser.registryOps.getEntryLookup(RegistryKeys.ITEM_MODIFIER).map((reg) -> reg.getOptional(id));
            if (referenceFunction.isPresent() && referenceFunction.get().isPresent() && referenceFunction.get().get().value().getType() != LootFunctionTypes.REFERENCE) {
                return LootParserRegistry.parseFunction(referenceFunction.get().get().value(), stack, referenceFunction.get().get().value().getType(), parentIsAlternative, conditionTexts);
            }
        }
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.reference", LText.literal(id.toString())), ItemStack.EMPTY, conditionTexts);
    }
}