package dev.emi.remastered.enchanting.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * Lookup tables over the {@link EnchantmentInfo}s of the last EMI reload, for the Item Info screen. Replaced as a
 * whole on every reload, so readers never see a half built index.
 * <p>
 * The item lookup is built lazily, once per distinct item column set (enchantments that accept the same items share
 * their columns, see {@link EnchantmentCatalog}), not per enchantment and item on the reload thread.
 */
public final class EnchantmentIndex {
	private static volatile EnchantmentIndex current = new EnchantmentIndex(List.of());

	private final List<EnchantmentInfo> infos;
	private final Map<Identifier, EnchantmentInfo> byId = new HashMap<>();
	/** Items of each distinct column set, filled on first use; guarded by {@code this}. */
	private final Map<List<List<EmiStack>>, Set<Item>> itemsByColumns = new IdentityHashMap<>();

	private EnchantmentIndex(List<EnchantmentInfo> infos) {
		this.infos = List.copyOf(infos);
		for (EnchantmentInfo info : infos) {
			byId.put(info.id(), info);
		}
	}

	public static EnchantmentIndex get() {
		return current;
	}

	/** Replaces the index with one over {@code infos}. */
	public static void set(List<EnchantmentInfo> infos) {
		current = new EnchantmentIndex(infos);
	}

	public boolean isEmpty() {
		return byId.isEmpty();
	}

	/** @return the info of the enchantment with this registry id, or null */
	public @Nullable EnchantmentInfo byId(Identifier id) {
		return byId.get(id);
	}

	/** @return the enchantments that can be applied to the item, in registry order; never null */
	public List<EnchantmentInfo> forItem(Item item) {
		List<EnchantmentInfo> result = null;
		for (EnchantmentInfo info : infos) {
			if (itemsOf(info.validItems()).contains(item)) {
				if (result == null) {
					result = new ArrayList<>();
				}
				result.add(info);
			}
		}
		return result == null ? List.of() : result;
	}

	private synchronized Set<Item> itemsOf(List<List<EmiStack>> columns) {
		return itemsByColumns.computeIfAbsent(columns, cols -> {
			Set<Item> items = Collections.newSetFromMap(new IdentityHashMap<>());
			for (List<EmiStack> column : cols) {
				for (EmiStack stack : column) {
					if (stack.getKey() instanceof Item item) {
						items.add(item);
					}
				}
			}
			return items;
		});
	}
}
