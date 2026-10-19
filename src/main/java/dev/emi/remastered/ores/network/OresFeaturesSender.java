/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.network.payload.S2CSendBiomeInfoPayload;
import dev.emi.remastered.ores.network.payload.S2CSendFeaturesPayload;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;

public class OresFeaturesSender {
	/** Features per payload, keeps every packet far below the 1 MiB limit for custom payloads. */
	private static final int FEATURES_PER_PAYLOAD = 100;

	/** Sends the ore and geode features and the biomes they are in to a player, if the client can receive them. */
	public static void onSyncDataPackContents(ServerPlayerEntity player, boolean joined) {
		// Vanilla clients and clients without the module cannot receive custom payloads they did not register
		if (!ServerPlayNetworking.canSend(player, S2CSendBiomeInfoPayload.TYPE)
			|| !ServerPlayNetworking.canSend(player, S2CSendFeaturesPayload.TYPE)) {
			return;
		}

		Map<Identifier, PlacedFeature> featureMap = new HashMap<>();
		SetMultimap<RegistryKey<PlacedFeature>, RegistryKey<Biome>> features2biomes = HashMultimap.create();
		DynamicRegistryManager access = player.server.getRegistryManager();
		Registry<PlacedFeature> placedFeatureRegistry = access.get(RegistryKeys.PLACED_FEATURE);
		placedFeatureRegistry.getEntrySet().forEach(entry -> {
			// we only care about ore features for now
			PlacedFeature pf = entry.getValue();
			RegistryEntry<ConfiguredFeature<?, ?>> configured = pf.feature();
			// a mod may register a placed feature whose configured feature never gets bound
			if (!configured.hasKeyAndValue()) {
				return;
			}
			FeatureConfig fc = configured.value().config();
			if (fc instanceof OreFeatureConfig || fc instanceof GeodeFeatureConfig) {
				// remove problematic placement modifiers
				List<PlacementModifier> newModifiers = pf.placementModifiers()
					.stream()
					.filter(OresFeaturesSender::isSupported)
					.toList();

				Identifier id = entry.getKey().getValue();
				PlacedFeature stripped = new PlacedFeature(configured, newModifiers);
				if (isSendable(id, stripped)) {
					featureMap.put(id, stripped);
				}
			}
		});
		access.get(RegistryKeys.BIOME).getEntrySet().forEach(biomeEntry -> {
			biomeEntry.getValue().getGenerationSettings().getFeatures().forEach(placedFeatureHolderSet -> {
				placedFeatureHolderSet.forEach(placedFeatureHolder -> {
					placedFeatureHolder.getKey().ifPresent(placedFeatureResourceKey -> {
						features2biomes.put(placedFeatureResourceKey, biomeEntry.getKey());
					});
				});
			});
		});

		SetMultimap<RegistryKey<PlacedFeature>, RegistryKey<Biome>> features2biomes2 = Multimaps.filterKeys(features2biomes, k -> featureMap.containsKey(k.getValue()));

		// the biome info goes first, it tells the client to drop what it got before
		ServerPlayNetworking.send(player, new S2CSendBiomeInfoPayload(features2biomes2));
		Map<Identifier, PlacedFeature> batch = new LinkedHashMap<>();
		for (Map.Entry<Identifier, PlacedFeature> entry : featureMap.entrySet()) {
			batch.put(entry.getKey(), entry.getValue());
			if (batch.size() >= FEATURES_PER_PAYLOAD) {
				ServerPlayNetworking.send(player, new S2CSendFeaturesPayload(batch));
				batch = new LinkedHashMap<>();
			}
		}
		if (!batch.isEmpty() || featureMap.isEmpty()) {
			ServerPlayNetworking.send(player, new S2CSendFeaturesPayload(batch));
		}
	}

	/**
	 * A feature that cannot be encoded would throw while the packet is written and disconnect the
	 * player, so such features are left out.
	 */
	private static boolean isSendable(Identifier id, PlacedFeature feature) {
		try {
			return S2CSendFeaturesPayload.toJson(feature).length() <= S2CSendFeaturesPayload.MAX_JSON_LENGTH;
		} catch (RuntimeException e) {
			Ores.LOG.warn("Not sending placed feature {} to clients, it cannot be serialized: {}", id, e.getMessage());
			return false;
		}
	}

	private static boolean isSupported(PlacementModifier modifier) {
		return modifier instanceof HeightRangePlacementModifier
			|| modifier instanceof CountPlacementModifier
			|| modifier instanceof RarityFilterPlacementModifier;
	}
}
