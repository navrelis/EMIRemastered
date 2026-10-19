package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.village.raid.Raid;

import java.util.List;
import java.util.Optional;

public class OminousBannerFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Parsing an ominous banner function");
        Optional<RegistryEntryLookup<BannerPattern>> patternLookup = LootTableParser.registryOps.getEntryLookup(RegistryKeys.BANNER_PATTERN);
		return patternLookup.map(
                bannerPatternRegistryEntryLookup -> new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.ominous_banner"), Raid.getOminousBanner(bannerPatternRegistryEntryLookup), conditionTexts)
        ).orElseGet(
                () -> new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.ominous_banner"), ItemStack.EMPTY, conditionTexts)
        );
    }

}