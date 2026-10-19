package dev.emi.remastered.effects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.SuspiciousStewIngredient;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Collects the data driven sources of all effects in ONE pass: every potion, every item (food components, ominous
 * bottle, items with potion contents, suspicious stew ingredients, which also covers all flower blocks through their
 * block items) and the beacon list. EMIffect 2.1.5 scanned all blocks and all items again for every single effect.
 */
final class EffectSourceCollector {
	/** {@code OminousBottleItem} always gives Bad Omen for 120000 ticks (100 minutes). */
	static final int OMINOUS_BOTTLE_DURATION = 120000;

	private EffectSourceCollector() {
	}

	static Map<StatusEffect, EffectSources> collect() {
		Map<StatusEffect, EffectSources> map = new HashMap<>();

		Registries.POTION.streamEntries().forEach(potion -> {
			for (StatusEffectInstance instance : potion.value().getEffects()) {
				of(map, instance.getEffectType()).potions.add(
					new EffectSources.PotionSource(potion, instance.getAmplifier(), instance.getDuration()));
			}
		});

		for (Item item : Registries.ITEM) {
			if (item == Items.AIR) {
				continue;
			}
			ComponentMap components = item.getComponents();
			FoodComponent food = components.get(DataComponentTypes.FOOD);
			if (food != null) {
				for (FoodComponent.StatusEffectEntry entry : food.effects()) {
					// effect() builds a copy on every call
					StatusEffectInstance instance = entry.effect();
					of(map, instance.getEffectType()).foods.add(new EffectSources.FoodSource(
						item, instance.getAmplifier(), instance.getDuration(), entry.probability()));
				}
			}
			Integer ominous = components.get(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);
			if (ominous != null) {
				of(map, StatusEffects.BAD_OMEN).foods.add(
					new EffectSources.FoodSource(item, ominous, OMINOUS_BOTTLE_DURATION, 1f));
			}
			PotionContentsComponent contents = components.get(DataComponentTypes.POTION_CONTENTS);
			if (contents != null && contents.hasEffects()) {
				for (StatusEffectInstance instance : contents.getEffects()) {
					of(map, instance.getEffectType()).foods.add(new EffectSources.FoodSource(
						item, instance.getAmplifier(), instance.getDuration(), 1f));
				}
			}
			SuspiciousStewIngredient ingredient = SuspiciousStewIngredient.of(item);
			if (ingredient != null) {
				for (SuspiciousStewEffectsComponent.StewEffect effect : ingredient.getStewEffects().effects()) {
					of(map, effect.effect()).stews.add(new EffectSources.StewSource(item, effect.duration()));
				}
			}
		}

		// vanilla: the primary power is chosen from the first rows, the last row is the secondary power at pyramid
		// level 4, where the primary power can be repeated as the secondary one for a level II effect
		List<List<RegistryEntry<StatusEffect>>> levels = BeaconBlockEntity.EFFECTS_BY_LEVEL;
		for (int i = 0; i < levels.size(); i++) {
			boolean secondaryRow = i == levels.size() - 1;
			for (RegistryEntry<StatusEffect> effect : levels.get(i)) {
				if (secondaryRow) {
					of(map, effect).beacon.add(new EffectSources.BeaconSource(i + 1, true, 0));
				} else {
					of(map, effect).beacon.add(new EffectSources.BeaconSource(i + 1, false, 0));
					of(map, effect).beacon.add(new EffectSources.BeaconSource(levels.size(), true, 1));
				}
			}
		}
		return map;
	}

	static EffectSources of(Map<StatusEffect, EffectSources> map, RegistryEntry<StatusEffect> effect) {
		return of(map, effect.value());
	}

	static EffectSources of(Map<StatusEffect, EffectSources> map, StatusEffect effect) {
		return map.computeIfAbsent(effect, e -> new EffectSources());
	}
}
