package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

/** Everything that is known about where one effect comes from, collected by {@link EffectSourceCollector}. */
final class EffectSources {
	final List<PotionSource> potions = new ArrayList<>();
	/** Items that give the effect when consumed: food components, ominous bottles, items with potion contents. */
	final List<FoodSource> foods = new ArrayList<>();
	final List<StewSource> stews = new ArrayList<>();
	final List<BeaconSource> beacon = new ArrayList<>();
	/** Resolved entries of the effect_sources data table and of the legacy extra_stacks files. */
	final List<TableSource> table = new ArrayList<>();

	/** Number of entries over all kinds, for the log line. */
	int count() {
		return potions.size() + foods.size() + stews.size() + beacon.size() + table.size();
	}

	boolean isEmpty() {
		return count() == 0;
	}

	record PotionSource(RegistryEntry<Potion> potion, int amplifier, int duration) {
	}

	record FoodSource(Item item, int amplifier, int duration, float probability) {
	}

	record StewSource(Item flower, int duration) {
	}

	/** {@code secondary}: chosen as secondary power; {@code amplifier} 1 = the boosted primary power. */
	record BeaconSource(int pyramidLevel, boolean secondary, int amplifier) {
	}

	/** {@code textKey} may be empty; amplifier and duration are -1 when unknown. */
	record TableSource(boolean cure, List<EmiIngredient> stacks, String textKey, int amplifier, int duration) {
	}
}
