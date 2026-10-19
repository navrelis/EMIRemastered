package dev.emi.remastered.effects;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/**
 * Reads the hand written sources from the resource packs and adds them to the collected index:
 * <ul>
 * <li>{@code assets/<ns>/emiffect/effect_sources/*.json}: the table described in {@link EffectSourceDef#parse}</li>
 * <li>{@code assets/<ns>/emiffect/extra_stacks/*.json}: EMIffect's format {@code {"id": effect, "stacks": [...]}}</li>
 * </ul>
 * Unknown effects (a mod that is not installed), unknown entities and stacks that do not resolve are skipped silently,
 * so that packs can list modded content.
 */
final class EffectSourceTable {
	static final String TABLE_PATH = "emiffect/effect_sources";
	static final String EXTRA_PATH = "emiffect/extra_stacks";

	private EffectSourceTable() {
	}

	static void apply(ResourceManager manager, Map<StatusEffect, EffectSources> index) {
		for (Map.Entry<Identifier, List<Resource>> file : manager.findAllResources(TABLE_PATH, EffectSourceTable::isJson).entrySet()) {
			for (Resource resource : file.getValue()) {
				JsonElement root = read(file.getKey(), resource);
				if (root == null) {
					continue;
				}
				String name = file.getKey().toString();
				for (EffectSourceDef def : EffectSourceDef.parse(root, error -> EmiLog.warn("effects: " + name + ": " + error))) {
					add(index, def);
				}
			}
		}
		for (Map.Entry<Identifier, List<Resource>> file : manager.findAllResources(EXTRA_PATH, EffectSourceTable::isJson).entrySet()) {
			for (Resource resource : file.getValue()) {
				JsonElement root = read(file.getKey(), resource);
				if (root == null) {
					continue;
				}
				try {
					EffectSourceDef def = EffectSourceDef.parseExtra(root.getAsJsonObject());
					add(index, def);
				} catch (RuntimeException e) {
					EmiLog.warn("effects: " + file.getKey() + ": " + e.getMessage());
				}
			}
		}
	}

	private static boolean isJson(Identifier id) {
		return id.getPath().endsWith(".json");
	}

	private static JsonElement read(Identifier id, Resource resource) {
		try (BufferedReader reader = resource.getReader()) {
			return JsonParser.parseReader(reader);
		} catch (Exception e) {
			EmiLog.warn("effects: cannot read " + id + ": " + e.getMessage());
			return null;
		}
	}

	private static void add(Map<StatusEffect, EffectSources> index, EffectSourceDef def) {
		List<EmiIngredient> cells = new ArrayList<>();
		for (String entity : def.entities()) {
			Identifier id = Identifier.tryParse(entity);
			EntityType<?> type = id == null ? null : Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
			SpawnEggItem egg = type == null ? null : SpawnEggItem.forEntity(type);
			if (egg != null) {
				cells.add(EmiStack.of(egg));
			}
		}
		for (JsonElement stack : def.stacks()) {
			EmiIngredient ingredient = EmiIngredientSerializer.getDeserialized(stack);
			if (!ingredient.isEmpty()) {
				cells.add(ingredient);
			}
		}
		if (def.hasReferences() && cells.isEmpty()) {
			return;
		}
		EffectSources.TableSource source = new EffectSources.TableSource(def.cure(), List.copyOf(cells), def.text(),
			def.amplifier(), def.duration());
		for (String effectId : def.effects()) {
			if (effectId.equals(EffectSourceDef.ALL_EFFECTS)) {
				for (StatusEffect effect : Registries.STATUS_EFFECT) {
					if (!effect.isInstant()) {
						EffectSourceCollector.of(index, effect).table.add(source);
					}
				}
				continue;
			}
			Identifier id = Identifier.tryParse(effectId);
			StatusEffect effect = id == null ? null : Registries.STATUS_EFFECT.getOrEmpty(id).orElse(null);
			if (effect != null) {
				EffectSourceCollector.of(index, effect).table.add(source);
			}
		}
	}
}
