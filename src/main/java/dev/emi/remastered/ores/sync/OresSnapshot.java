/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;

/**
 * The complete result of one ore sync. Immutable, so EMI's reload thread can read it while the network thread
 * assembles the next one.
 */
public final class OresSnapshot<F> {
	/** Id of the snapshot that is used before the first sync is complete. */
	public static final int EMPTY_ID = 0;

	private final int syncId;
	private final int expectedFeatures;
	private final Map<Identifier, F> features;
	private final Map<Identifier, Set<Identifier>> featureBiomes;
	private final Map<Identifier, Dimension> dimensions;
	private final Map<Identifier, List<Dimension>> dimensionsOfBiome;

	public OresSnapshot(int syncId, int expectedFeatures, Map<Identifier, F> features,
		Map<Identifier, Set<Identifier>> featureBiomes, Map<Identifier, Dimension> dimensions) {
		this.syncId = syncId;
		this.expectedFeatures = expectedFeatures;
		this.features = Collections.unmodifiableMap(new LinkedHashMap<>(features));
		Map<Identifier, Set<Identifier>> biomes = new LinkedHashMap<>();
		featureBiomes.forEach((id, set) -> biomes.put(id, Collections.unmodifiableSet(new LinkedHashSet<>(set))));
		this.featureBiomes = Collections.unmodifiableMap(biomes);
		this.dimensions = Collections.unmodifiableMap(new LinkedHashMap<>(dimensions));
		Map<Identifier, List<Dimension>> byBiome = new LinkedHashMap<>();
		for (Dimension dimension : this.dimensions.values()) {
			for (Identifier biome : dimension.biomes()) {
				byBiome.computeIfAbsent(biome, k -> new ArrayList<>()).add(dimension);
			}
		}
		this.dimensionsOfBiome = Collections.unmodifiableMap(byBiome);
	}

	public static <F> OresSnapshot<F> empty() {
		return new OresSnapshot<>(EMPTY_ID, 0, Map.of(), Map.of(), Map.of());
	}

	public int syncId() {
		return syncId;
	}

	/** Number of features the server announced. More than {@code features().size()} means features were skipped. */
	public int expectedFeatures() {
		return expectedFeatures;
	}

	public Map<Identifier, F> features() {
		return features;
	}

	public Map<Identifier, Dimension> dimensions() {
		return dimensions;
	}

	public Dimension dimension(Identifier id) {
		return id == null ? null : dimensions.get(id);
	}

	/** All biomes the feature is in, whatever the dimension. */
	public Set<Identifier> biomesOf(Identifier feature) {
		return featureBiomes.getOrDefault(feature, Set.of());
	}

	/** The dimensions the feature generates in, in the order of the server's worlds. */
	public List<Dimension> dimensionsOf(Identifier feature) {
		Set<Identifier> biomes = biomesOf(feature);
		List<Dimension> result = new ArrayList<>();
		for (Dimension dimension : dimensions.values()) {
			if (intersects(dimension.biomes(), biomes)) {
				result.add(dimension);
			}
		}
		return result;
	}

	/** The dimensions that can contain the biome. */
	public List<Dimension> dimensionsOfBiome(Identifier biome) {
		return dimensionsOfBiome.getOrDefault(biome, List.of());
	}

	private static boolean intersects(Set<Identifier> a, Set<Identifier> b) {
		Set<Identifier> small = a.size() <= b.size() ? a : b;
		Set<Identifier> large = small == a ? b : a;
		for (Identifier id : small) {
			if (large.contains(id)) {
				return true;
			}
		}
		return false;
	}

	/** A dimension of the server: its height range as feature placement sees it and the biomes it can generate. */
	public record Dimension(Identifier id, int bottomY, int topY, Set<Identifier> biomes) {
		public Dimension {
			biomes = Collections.unmodifiableSet(new LinkedHashSet<>(biomes));
		}
	}
}
