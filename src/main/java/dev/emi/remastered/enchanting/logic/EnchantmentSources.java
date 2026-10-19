package dev.emi.remastered.enchanting.logic;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where an enchantment can come from, derived from the enchantment tags of the world. Pure logic: the tags are read
 * into {@link Flags} elsewhere, so this can be tested without Minecraft.
 */
public final class EnchantmentSources {

	/** Declaration order is the display order. */
	public enum Source {
		ENCHANTING_TABLE("enchanting_table"),
		LOOT("loot"),
		LIBRARIAN_TRADES("librarian_trades"),
		TRADED_EQUIPMENT("traded_equipment"),
		MOB_EQUIPMENT("mob_equipment"),
		TREASURE("treasure"),
		CURSE("curse");

		private final String key;

		Source(String key) {
			this.key = key;
		}

		/** Last part of the lang key {@code emi_enchanting.source.<key>}. */
		public String key() {
			return key;
		}
	}

	/**
	 * Membership of one enchantment in the tags that matter.
	 *
	 * @param inEnchantingTable {@code #minecraft:in_enchanting_table}
	 * @param onRandomLoot {@code #minecraft:on_random_loot}
	 * @param tradeable {@code #minecraft:tradeable}
	 * @param inRebalancedTrade in one of the per-biome trade tags {@code #minecraft:trades/*}
	 * @param tradeRebalance whether the world uses the rebalanced villager trades (the biome tags replace {@code tradeable})
	 * @param onTradedEquipment {@code #minecraft:on_traded_equipment}
	 * @param onMobSpawnEquipment {@code #minecraft:on_mob_spawn_equipment}
	 * @param treasure {@code #minecraft:treasure}
	 * @param nonTreasure {@code #minecraft:non_treasure}
	 * @param curse {@code #minecraft:curse}
	 */
	public record Flags(boolean inEnchantingTable, boolean onRandomLoot, boolean tradeable, boolean inRebalancedTrade,
		boolean tradeRebalance, boolean onTradedEquipment, boolean onMobSpawnEquipment, boolean treasure,
		boolean nonTreasure, boolean curse) {
	}

	private EnchantmentSources() {
	}

	/** @return the sources of an enchantment with these tags, in display order; empty if it has none */
	public static Set<Source> classify(Flags flags) {
		EnumSet<Source> sources = EnumSet.noneOf(Source.class);
		if (flags.inEnchantingTable()) {
			sources.add(Source.ENCHANTING_TABLE);
		}
		if (flags.onRandomLoot()) {
			sources.add(Source.LOOT);
		}
		if (flags.tradeRebalance() ? flags.inRebalancedTrade() : flags.tradeable()) {
			sources.add(Source.LIBRARIAN_TRADES);
		}
		if (flags.onTradedEquipment()) {
			sources.add(Source.TRADED_EQUIPMENT);
		}
		if (flags.onMobSpawnEquipment()) {
			sources.add(Source.MOB_EQUIPMENT);
		}
		if (flags.treasure() && !flags.nonTreasure()) {
			sources.add(Source.TREASURE);
		}
		if (flags.curse()) {
			sources.add(Source.CURSE);
		}
		return sources;
	}
}
