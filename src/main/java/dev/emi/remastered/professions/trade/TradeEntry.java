package dev.emi.remastered.professions.trade;

import java.util.List;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * One concrete trade as shown on a recipe page.
 *
 * @param id           Synthetic recipe id, stable between reloads
 * @param wanderer     Whether the trade belongs to the wandering trader instead of a villager profession
 * @param owner        Name of the profession or of the wandering trader
 * @param levelText    Level (villagers) or pool (wandering trader) shown on the page
 * @param workstations Workstation blocks of the profession, empty for the wandering trader
 * @param first        First item bought
 * @param second       Second item bought, {@link EmiStack#EMPTY} if the trade has none
 * @param output       Item sold
 * @param maxUses      Uses before the trade locks
 * @param experience   Experience the villager gains
 * @param villagerTypes Names of the villager types that offer this trade, empty if all of them do
 * @param mapName      Name of the map for placeholder map trades, otherwise null
 * @param mapStructure Structure tag the placeholder map points to, null if the trade is no placeholder
 * @param villagerApi  Whether the trade comes from the Villager API mod
 */
public record TradeEntry(Identifier id, boolean wanderer, Text owner, Text levelText, List<EmiStack> workstations,
		EmiStack first, EmiStack second, EmiStack output, int maxUses, int experience, List<String> villagerTypes,
		Text mapName, String mapStructure, boolean villagerApi) {

	public boolean mapPlaceholder() {
		return mapName != null;
	}
}
