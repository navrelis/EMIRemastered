/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.List;
import net.minecraft.util.Identifier;

/**
 * Start of an ore sync: what the server is about to send. Sent before the feature batches.
 *
 * @param syncId       counter of the server, a client drops batches that belong to another sync
 * @param batchCount   number of feature batches that follow; the sync is only complete when all of them arrived
 * @param featureCount number of features in the batches that follow
 * @param biomes       biome table, the batches and dimensions refer to biomes by index into this list
 * @param dimensions   the dimensions of the server and the biomes each one can generate
 */
public record SyncHeader(int syncId, int batchCount, int featureCount, List<Identifier> biomes, List<Dimension> dimensions) {

	/**
	 * @param bottomY      lowest block of the dimension as seen by feature placement
	 * @param topY         first block above the dimension as seen by feature placement (exclusive)
	 * @param biomeIndices indices into {@link SyncHeader#biomes()}
	 */
	public record Dimension(Identifier id, int bottomY, int topY, int[] biomeIndices) {
	}
}
