/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.List;
import net.minecraft.util.Identifier;

/**
 * One batch of features of a sync.
 *
 * @param index position of the batch in its sync, 0 up to {@link SyncHeader#batchCount()} (exclusive); batches may
 *              arrive in any order and a repeated one is ignored
 */
public record SyncBatch<F>(int syncId, int index, List<Entry<F>> entries) {

	/** A feature (null when the client could not decode it) and the indices of its biomes in the header's biome table. */
	public record Entry<F>(Identifier id, F feature, int[] biomeIndices) {
	}
}
