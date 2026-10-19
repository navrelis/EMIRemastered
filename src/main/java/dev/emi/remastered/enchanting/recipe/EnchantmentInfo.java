package dev.emi.remastered.enchanting.recipe;

import java.util.List;
import java.util.Set;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.enchanting.logic.EnchantmentSources;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Everything the info pages of one enchantment are built from. Created while EMI loads its plugins and thrown away
 * with the recipe on the next reload; it holds registry entries of the world that was current then, never the
 * world itself. Information pages that need more data read it from {@link #entry} (tags, definition) when they are
 * built, on the client thread, so they always see the current registries.
 *
 * @param entry registry entry of the enchantment
 * @param id registry id of the enchantment
 * @param maxLevel highest level the enchantment can reach naturally (what {@code Enchantment.getMaxLevel()} says,
 * including the changes of mods that mix into it)
 * @param levelCap highest level an item stack can store at all, see {@link dev.emi.remastered.enchanting.logic.LevelCap}
 * @param books enchanted books listed for this enchantment, the recipe outputs
 * @param bookIngredient {@link #books} as one ingredient
 * @param validItems items the enchantment can be applied to, spread over {@link #ITEM_COLUMNS} lists
 * @param exclusions enchantments that cannot be combined with this one
 * @param sources where the enchantment can be found
 * @param tradeRebalance whether the world uses the rebalanced villager trades
 */
public record EnchantmentInfo(RegistryEntry<Enchantment> entry, Identifier id, int maxLevel, int levelCap, List<EmiStack> books,
	EmiIngredient bookIngredient, List<List<EmiStack>> validItems, List<Exclusion> exclusions,
	Set<EnchantmentSources.Source> sources, boolean tradeRebalance) {
	public static final int ITEM_COLUMNS = 8;

	/**
	 * @param entry the enchantment that cannot be combined
	 * @param books its listed books
	 */
	public record Exclusion(RegistryEntry<Enchantment> entry, EmiIngredient books) {
	}
}
