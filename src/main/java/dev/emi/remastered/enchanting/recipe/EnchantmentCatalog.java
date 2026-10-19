package dev.emi.remastered.enchanting.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.remastered.enchanting.EnchantingSettings;
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

		Map<RegistryEntry<Enchantment>, List<EmiStack>> books = new HashMap<>();
		for (RegistryEntry.Reference<Enchantment> entry : entries) {
			try {
				books.put(entry, books(entry, entry.value().getMaxLevel()));
			} catch (Throwable t) {
				warn(entry, "list its books", t);
			}
		}

		List<EnchantmentInfo> infos = new ArrayList<>();
		for (RegistryEntry.Reference<Enchantment> entry : entries) {
			List<EmiStack> entryBooks = books.get(entry);
			if (entryBooks == null) {
				continue;
			}
			try {
				List<List<EmiStack>> validItems = validItems(entry, items, itemStacks);
				List<EnchantmentInfo.Exclusion> exclusions = exclusions(entry, entries, books);
				infos.add(new EnchantmentInfo(entry, entry.registryKey().getValue(), entry.value().getMaxLevel(), entryBooks,
					EmiIngredient.of(entryBooks), validItems, exclusions, tradeRebalance));
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

	private static List<List<EmiStack>> validItems(RegistryEntry<Enchantment> entry, List<ItemStack> items, EmiStack[] itemStacks) {
		List<List<EmiStack>> columns = new ArrayList<>();
		for (int i = 0; i < EnchantmentInfo.ITEM_COLUMNS; i++) {
			columns.add(new ArrayList<>());
		}
		Enchantment enchantment = entry.value();
		boolean reported = false;
		int counter = 0;
		for (int i = 0; i < items.size(); i++) {
			try {
				if (!enchantment.isAcceptableItem(items.get(i))) {
					continue;
				}
			} catch (Throwable t) {
				if (!reported) {
					reported = true;
					warn(entry, "test an item (" + items.get(i).getItem() + ")", t);
				}
				continue;
			}
			if (itemStacks[i] == null) {
				itemStacks[i] = EmiStack.of(items.get(i).getItem());
			}
			columns.get(counter++ % EnchantmentInfo.ITEM_COLUMNS).add(itemStacks[i]);
		}
		return columns;
	}

	private static List<EnchantmentInfo.Exclusion> exclusions(RegistryEntry<Enchantment> entry,
			List<RegistryEntry.Reference<Enchantment>> entries, Map<RegistryEntry<Enchantment>, List<EmiStack>> books) {
		List<EnchantmentInfo.Exclusion> exclusions = new ArrayList<>();
		boolean reported = false;
		for (RegistryEntry.Reference<Enchantment> other : entries) {
			List<EmiStack> otherBooks = books.get(other);
			if (other == entry || otherBooks == null) {
				continue;
			}
			try {
				if (!Enchantment.canBeCombined(entry, other)) {
					exclusions.add(new EnchantmentInfo.Exclusion(other, EmiIngredient.of(otherBooks)));
				}
			} catch (Throwable t) {
				if (!reported) {
					reported = true;
					warn(entry, "test it against " + other.registryKey().getValue(), t);
				}
			}
		}
		return exclusions;
	}

	private static void warn(RegistryEntry<Enchantment> entry, String action, Throwable t) {
		Identifier id = entry.getKey().map(key -> key.getValue()).orElse(null);
		EmiReloadLog.warn("EMI Enchanting: could not " + action + " for enchantment " + id, t);
	}
}
