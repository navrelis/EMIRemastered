package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.ExplorationMapLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapState;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;

import java.util.List;

public class ExplorationMapFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        ItemStack mapStack;
        String typeKey = "emi_loot.map.unknown";
        if (!stack.isOf(Items.MAP)) {
            mapStack = stack;
        } else {
            RegistryEntry<MapDecorationType> decoration = ((ExplorationMapLootFunctionAccessor)function).emi_loot$getDecoration();
            TagKey<Structure> destination = ((ExplorationMapLootFunctionAccessor)function).emi_loot$getDestination();
            mapStack = new ItemStack(Items.FILLED_MAP);
            MapState.addDecorationsNbt(mapStack, BlockPos.ORIGIN, "+", decoration);
            typeKey = "emi_loot.map."+ destination.id().getPath();
        }
        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.map", LText.translatable(typeKey)), mapStack, conditionTexts);
    }
}