package dev.emi.remastered.enchanting.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * Lookup tables over the {@link EnchantmentInfo}s of the last EMI reload, for the Item Info screen. Replaced as a
 * whole on every reload, so readers never see a half built index.
 */
public final class EnchantmentIndex {
	private static volatile EnchantmentIndex current = new EnchantmentIndex(List.of());

	private final Map<Identifier, EnchantmentInfo> byId = new HashMap<>();
	private final Map<Item, List<EnchantmentInfo>> byItem = new HashMap<>();

	private EnchantmentIndex(List<EnchantmentInfo> infos) {
		for (EnchantmentInfo info : infos) {
			byId.put(info.id(), info);
			for (List<EmiStack> column : info.validItems()) {
				for (EmiStack stack : column) {
					if (stack.getKey() instanceof Item item) {
						byItem.computeIfAbsent(item, k -> new ArrayList<>()).add(info);
					}
				}
			}
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
		return byItem.getOrDefault(item, List.of());
	}
}
