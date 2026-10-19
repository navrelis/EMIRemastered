package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetComponentsLootFunctionAccessor;
import dev.emi.remastered.loot.mixin.SetCustomDataLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.SetCustomDataLootFunction;
import net.minecraft.nbt.NbtCompound;

import java.util.List;

public class SetCustomDataFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        NbtCompound compound = ((SetCustomDataLootFunctionAccessor)function).emi_loot$getNbt();
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.copyFrom(compound));
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_nbt"), ItemStack.EMPTY, conditionTexts);
    }
}