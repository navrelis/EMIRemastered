/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;

/** What the server sends for one sync: the start header, the numbered feature batches and the complete marker. */
public record OresSyncPlan<F>(SyncHeader header, List<SyncBatch<F>> batches) {
	/** Features per batch. */
	public static final int MAX_FEATURES_PER_BATCH = 100;
	/** Estimated size of a batch in bytes, far below the 1 MiB limit of a custom payload. */
	public static final int MAX_BATCH_SIZE = 400_000;

	/**
	 * @param size estimated encoded size in bytes
	 */
	public record Feature<F>(F feature, int size, Set<Identifier> biomes) {
	}

	public record DimensionSource(Identifier id, int bottomY, int topY, Collection<Identifier> biomes) {
	}

	/** The marker that ends the sync, sent after the last batch. */
	public SyncComplete complete() {
		return new SyncComplete(header.syncId(), batches.size());
	}

	public static <F> OresSyncPlan<F> build(int syncId, Map<Identifier, Feature<F>> features, List<DimensionSource> dimensions) {
		return build(syncId, features, dimensions, MAX_FEATURES_PER_BATCH, MAX_BATCH_SIZE);
	}

	public static <F> OresSyncPlan<F> build(int syncId, Map<Identifier, Feature<F>> features, List<DimensionSource> dimensions,
		int maxFeatures, int maxSize) {
		Map<Identifier, Integer> table = new LinkedHashMap<>();
		List<SyncHeader.Dimension> headerDimensions = new ArrayList<>();
		for (DimensionSource dimension : dimensions) {
			headerDimensions.add(new SyncHeader.Dimension(dimension.id(), dimension.bottomY(), dimension.topY(), indices(table, dimension.biomes())));
		}

		List<SyncBatch<F>> batches = new ArrayList<>();
		List<SyncBatch.Entry<F>> current = new ArrayList<>();
		int currentSize = 0;
		for (Map.Entry<Identifier, Feature<F>> entry : features.entrySet()) {
			Feature<F> feature = entry.getValue();
			if (!current.isEmpty() && (current.size() >= maxFeatures || currentSize + feature.size() > maxSize)) {
				batches.add(new SyncBatch<>(syncId, batches.size(), current));
				current = new ArrayList<>();
				currentSize = 0;
			}
			current.add(new SyncBatch.Entry<>(entry.getKey(), feature.feature(), indices(table, feature.biomes())));
			currentSize += feature.size();
		}
		if (!current.isEmpty()) {
			batches.add(new SyncBatch<>(syncId, batches.size(), current));
		}

		SyncHeader header = new SyncHeader(syncId, batches.size(), features.size(), new ArrayList<>(table.keySet()), headerDimensions);
		return new OresSyncPlan<>(header, batches);
	}

	private static int[] indices(Map<Identifier, Integer> table, Collection<Identifier> biomes) {
		int[] result = new int[biomes.size()];
		int i = 0;
		for (Identifier biome : biomes) {
			Integer index = table.get(biome);
			if (index == null) {
				index = table.size();
				table.put(biome, index);
			}
			result[i++] = index;
		}
		return result;
	}
}
