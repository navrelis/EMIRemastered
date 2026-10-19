/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.List;
import net.minecraft.util.Identifier;

/**
 * One batch of features of a sync.
 *
 * @param last true for the final batch of the sync: the client may use the data once it has it
 */
public record SyncBatch<F>(int syncId, boolean last, List<Entry<F>> entries) {

	/** A feature (null when the client could not decode it) and the indices of its biomes in the header's biome table. */
	public record Entry<F>(Identifier id, F feature, int[] biomeIndices) {
	}
}
