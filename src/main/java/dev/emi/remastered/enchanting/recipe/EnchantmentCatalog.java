package dev.emi.remastered.enchanting.recipe;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.remastered.enchanting.EnchantingSettings;
import dev.emi.remastered.enchanting.logic.IncompatibilityMatrix;
import dev.emi.remastered.enchanting.logic.LevelCap;
import dev.emi.remastered.enchanting.logic.SharedColumns;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Reads the enchantment registry into {@link EnchantmentInfo}s. One enchantment that misbehaves (a mod that mixes
 * into the enchantment methods and throws, a broken datapack entry) is reported and left out or filled in as far as
 * it goes; it never stops the others.
 * <p>
 * Applicability and conflicts are asked through {@link Enchantment#isAcceptableItem} and
 * {@link Enchantment#canBeCombined}, not read from the definition, so mods that change those methods are respected.
 */
public final class EnchantmentCatalog {

	private EnchantmentCatalog() {
	}

	public static List<EnchantmentInfo> build(DynamicRegistryManager registries, boolean tradeRebalance) {
		List<RegistryEntry.Reference<Enchantment>> entries = registries.get(RegistryKeys.ENCHANTMENT).streamEntries().toList();
		List<ItemStack> items = new ArrayList<>();
		for (Item item : Registries.ITEM) {
			if (item != Items.AIR) {
				items.add(item.getDefaultStack());
			}
		}
		EmiStack[] itemStacks = new EmiStack[items.size()];
		SharedColumns<EmiStack> columns = new SharedColumns<>(EnchantmentInfo.ITEM_COLUMNS, i -> {
			if (itemStacks[i] == null) {
				itemStacks[i] = EmiStack.of(items.get(i).getItem());
			}
			return itemStacks[i];
		});

		Map<RegistryEntry<Enchantment>, List<EmiStack>> books = new HashMap<>();
		for (RegistryEntry.Reference<Enchantment> entry : entries) {
			try {
				books.put(entry, books(entry, entry.value().getMaxLevel()));
			} catch (Throwable t) {
				warn(entry, "list its books", t);
			}
		}

		List<RegistryEntry.Reference<Enchantment>> listed = entries.stream().filter(books::containsKey).toList();
		boolean[] reported = {false};
		IncompatibilityMatrix<RegistryEntry.Reference<Enchantment>> matrix = IncompatibilityMatrix.build(listed,
			(a, b) -> Enchantment.canBeCombined(a, b), (entry, t) -> {
				if (!reported[0]) {
					reported[0] = true;
					warn(entry, "test it against another enchantment", t);
				}
			});
		int levelCap = listed.isEmpty() ? LevelCap.VANILLA : EnchantmentFacts.levelCap(listed.get(0));

		List<EnchantmentInfo> infos = new ArrayList<>();
		for (RegistryEntry.Reference<Enchantment> entry : listed) {
			List<EmiStack> entryBooks = books.get(entry);
			try {
				List<List<EmiStack>> validItems = validItems(entry, items, columns);
				List<EnchantmentInfo.Exclusion> exclusions = new ArrayList<>();
				for (RegistryEntry.Reference<Enchantment> other : matrix.incompatibleWith(entry)) {
					exclusions.add(new EnchantmentInfo.Exclusion(other, EmiIngredient.of(books.get(other))));
				}
				int maxLevel = entry.value().getMaxLevel();
				infos.add(new EnchantmentInfo(entry, entry.registryKey().getValue(), maxLevel, levelCap,
					entryBooks, aboveMaxBooks(entry, maxLevel, levelCap), EmiIngredient.of(entryBooks), validItems, exclusions,
					EnchantmentFacts.sourcesOf(entry, tradeRebalance), tradeRebalance));
			} catch (Throwable t) {
				warn(entry, "build its info page", t);
			}
		}
		return infos;
	}

	private static List<EmiStack> books(RegistryEntry<Enchantment> entry, int maxLevel) {
		int limit = Math.max(1, EnchantingSettings.bookLevelLimit);
		List<EmiStack> books = new ArrayList<>();
		for (int level = 1; level <= Math.min(maxLevel, limit); level++) {
			books.add(EmiStack.of(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(entry, level))));
		}
		if (maxLevel > limit) {
			books.add(EmiStack.of(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(entry, maxLevel))));
		}
		return books;
	}

	/**
	 * Books above the maximum level, for stacks that got there with commands, loot or a mod that lifts the level cap.
	 * Limited to a few levels, see {@link LevelCap#aboveMaxTop}.
	 */
	private static List<EmiStack> aboveMaxBooks(RegistryEntry<Enchantment> entry, int maxLevel, int levelCap) {
		if (!EnchantingSettings.showAboveMax) {
			return List.of();
		}
		List<EmiStack> books = new ArrayList<>();
		try {
			int top = LevelCap.aboveMaxTop(maxLevel, levelCap, Math.max(1, EnchantingSettings.bookLevelLimit));
			for (int level = maxLevel + 1; level <= top; level++) {
				books.add(EmiStack.of(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(entry, level))));
			}
		} catch (Throwable t) {
			warn(entry, "list its books above the maximum level", t);
		}
		return books;
	}

	/** The items the enchantment applies to, spread over the item columns; shared by enchantments that accept the same items. */
	private static List<List<EmiStack>> validItems(RegistryEntry<Enchantment> entry, List<ItemStack> items, SharedColumns<EmiStack> columns) {
		Enchantment enchantment = entry.value();
		BitSet accepted = new BitSet(items.size());
		boolean reported = false;
		for (int i = 0; i < items.size(); i++) {
			try {
				if (enchantment.isAcceptableItem(items.get(i))) {
					accepted.set(i);
				}
			} catch (Throwable t) {
				if (!reported) {
					reported = true;
					warn(entry, "test an item (" + items.get(i).getItem() + ")", t);
				}
			}
		}
		return columns.columnsOf(accepted);
	}

	private static void warn(RegistryEntry<Enchantment> entry, String action, Throwable t) {
		Identifier id = entry.getKey().map(key -> key.getValue()).orElse(null);
		EmiReloadLog.warn("EMI Enchanting: could not " + action + " for enchantment " + id, t);
	}
}
