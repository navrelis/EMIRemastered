package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetEnchantmentsLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.NumberProcessors;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SetEnchantmentsFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        Map<RegistryEntry<Enchantment>, LootNumberProvider> enchantments = ((SetEnchantmentsLootFunctionAccessor)function).emi_loot$getEnchantments();
        boolean add = ((SetEnchantmentsLootFunctionAccessor)function).emi_loot$getAdd();
        if (stack.isOf(Items.BOOK)) {
            stack = new ItemStack(Items.ENCHANTED_BOOK);
            AtomicReference<ItemStack> finalStack = new AtomicReference<>(stack);
            enchantments.forEach((enchantment, provider)-> {
                float rollAvg = NumberProcessors.getRollAvg(provider);
                finalStack.set(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, (int) rollAvg)));
            });
            return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_enchant_book"), finalStack.get(), conditionTexts);
        } else if (!stack.isEmpty()) {
            ItemEnchantmentsComponent.Builder finalStackMap = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            if (add) {
                ItemEnchantmentsComponent stackMap = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                enchantments.forEach((enchantment, provider)-> {
                    float rollAvg = NumberProcessors.getRollAvg(provider);
                    finalStackMap.set(enchantment, Math.max(((int)rollAvg) + stackMap.getLevel(enchantment), 0));
                });
            } else {
                enchantments.forEach((enchantment, provider)-> {
                    float rollAvg = NumberProcessors.getRollAvg(provider);
                    finalStackMap.set(enchantment, Math.max(((int)rollAvg), 1));
                });
            }
            stack.set(DataComponentTypes.ENCHANTMENTS, finalStackMap.build());
            return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_enchant_item"), stack, conditionTexts);
        }
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_enchant_item"), ItemStack.EMPTY, conditionTexts);
    }
}