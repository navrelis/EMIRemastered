package dev.emi.remastered.enchanting.recipe;

import java.util.List;
import java.util.Set;

import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.remastered.enchanting.logic.EnchantmentSources;
import dev.emi.remastered.enchanting.logic.LevelCap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.TagKey;

/** Reads the tags and components that {@link EnchantmentSources} and {@link LevelCap} work on. */
public final class EnchantmentFacts {
	/** The per-biome trade tags that replace {@code #minecraft:tradeable} in worlds with rebalanced trades. */
	public static final List<TagKey<Enchantment>> REBALANCED_TRADES = List.of(
		EnchantmentTags.TAIGA_SPECIAL_TRADE, EnchantmentTags.SWAMP_SPECIAL_TRADE, EnchantmentTags.SNOW_SPECIAL_TRADE,
		EnchantmentTags.SAVANNA_SPECIAL_TRADE, EnchantmentTags.PLAINS_SPECIAL_TRADE, EnchantmentTags.JUNGLE_SPECIAL_TRADE,
		EnchantmentTags.DESERT_SPECIAL_TRADE, EnchantmentTags.TAIGA_COMMON_TRADE, EnchantmentTags.SWAMP_COMMON_TRADE,
		EnchantmentTags.SNOW_COMMON_TRADE, EnchantmentTags.SAVANNA_COMMON_TRADE, EnchantmentTags.PLAINS_COMMON_TRADE,
		EnchantmentTags.JUNGLE_COMMON_TRADE, EnchantmentTags.DESERT_COMMON_TRADE);

	private EnchantmentFacts() {
	}

	public static Set<EnchantmentSources.Source> sourcesOf(RegistryEntry<Enchantment> entry, boolean tradeRebalance) {
		boolean rebalanced = false;
		if (tradeRebalance) {
			for (TagKey<Enchantment> tag : REBALANCED_TRADES) {
				if (entry.isIn(tag)) {
					rebalanced = true;
					break;
				}
			}
		}
		return EnchantmentSources.classify(new EnchantmentSources.Flags(entry.isIn(EnchantmentTags.IN_ENCHANTING_TABLE),
			entry.isIn(EnchantmentTags.ON_RANDOM_LOOT), entry.isIn(EnchantmentTags.TRADEABLE), rebalanced, tradeRebalance,
			entry.isIn(EnchantmentTags.ON_TRADED_EQUIPMENT), entry.isIn(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT),
			entry.isIn(EnchantmentTags.TREASURE), entry.isIn(EnchantmentTags.NON_TREASURE), entry.isIn(EnchantmentTags.CURSE)));
	}

	/** @return the highest level an item stack can store for enchantments, measured with {@code probe} */
	public static int levelCap(RegistryEntry<Enchantment> probe) {
		try {
			return LevelCap.measure(level -> {
				ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
				builder.set(probe, level);
				return builder.build().getLevel(probe);
			});
		} catch (Throwable t) {
			EmiReloadLog.warn("EMI Enchanting: could not measure the enchantment level cap", t);
			return LevelCap.VANILLA;
		}
	}
}
