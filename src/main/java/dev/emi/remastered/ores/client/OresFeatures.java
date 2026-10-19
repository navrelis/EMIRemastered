/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import dev.emi.remastered.ores.network.payload.S2CSendBiomeInfoPayload;
import dev.emi.remastered.ores.network.payload.S2CSendFeaturesPayload;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Holds the data last sent by the server. EMI's reload thread reads it while the network
 * thread may replace it, so every update swaps in a new immutable snapshot.
 */
public class OresFeatures {
	private static volatile Map<Identifier, PlacedFeature> features = Collections.emptyMap();
	private static volatile SetMultimap<RegistryKey<PlacedFeature>, RegistryKey<Biome>> biomes = HashMultimap.create();

	public static void clearFeatures() {
		features = Collections.emptyMap();
		biomes = HashMultimap.create();
	}

	public static Map<Identifier, PlacedFeature> getFeatures() {
		return features;
	}

	public static SetMultimap<RegistryKey<PlacedFeature>, RegistryKey<Biome>> getBiomes() {
		return biomes;
	}

	/** The server sends the features in one or more batches, so they are merged. */
	public static void receive(S2CSendFeaturesPayload payload) {
		Map<Identifier, PlacedFeature> merged = new HashMap<>(features);
		merged.putAll(payload.features());
		features = Collections.unmodifiableMap(merged);
	}

	/** Always sent first by the server, so it starts a new sync. */
	public static void receive(S2CSendBiomeInfoPayload payload) {
		features = Collections.emptyMap();
		biomes = payload.biomeInfo();
	}
}
