/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;

/**
 * Client side of the ore sync, without any Minecraft or EMI dependency beyond identifiers.
 *
 * <p>A sync is a header followed by feature batches, the last one flagged. Batches are collected on the
 * side and only a complete sync replaces the snapshot that EMI reads, so a reload that runs while batches
 * are still arriving sees the previous complete data instead of a partial list.
 *
 * <p>EMI reads the data through {@link #consume()}, which also records which snapshot EMI used. When a sync
 * completes and EMI's last reload used an older one, {@link Status#COMPLETE_RELOAD} tells the caller to start
 * a new EMI reload. Everything is synchronized, so no reload can slip between the read and the check.
 */
public final class OresSyncState<F> {
	private static final int NOT_CONSUMED = -1;

	public enum Status {
		/** A batch of another sync, or a batch without a header. */
		IGNORED,
		/** More batches are expected. */
		PENDING,
		/** The sync is complete and swapped in. */
		COMPLETE,
		/** The sync is complete and swapped in, and EMI is working with older data: reload EMI. */
		COMPLETE_RELOAD
	}

	private volatile OresSnapshot<F> snapshot = OresSnapshot.empty();
	private Pending<F> pending;
	private int consumedSyncId = NOT_CONSUMED;

	/** The last complete snapshot, empty before the first sync. Never a partial one. */
	public OresSnapshot<F> snapshot() {
		return snapshot;
	}

	/** Starts a sync. Whatever a previous unfinished sync collected is dropped, the snapshot stays. */
	public synchronized void begin(SyncHeader header) {
		pending = new Pending<>(header);
	}

	public synchronized Status accept(SyncBatch<F> batch) {
		if (pending == null || pending.header.syncId() != batch.syncId()) {
			return Status.IGNORED;
		}
		pending.add(batch);
		if (!batch.last()) {
			return Status.PENDING;
		}
		OresSnapshot<F> complete = pending.build();
		pending = null;
		snapshot = complete;
		return consumedSyncId != NOT_CONSUMED && consumedSyncId != complete.syncId() ? Status.COMPLETE_RELOAD : Status.COMPLETE;
	}

	/** Called by EMI's registration: returns the snapshot to build recipes from and remembers that EMI used it. */
	public synchronized OresSnapshot<F> consume() {
		consumedSyncId = snapshot.syncId();
		return snapshot;
	}

	/** Disconnect: forgets everything. */
	public synchronized void clear() {
		snapshot = OresSnapshot.empty();
		pending = null;
		consumedSyncId = NOT_CONSUMED;
	}

	/** True while batches of a started sync are missing. */
	public synchronized boolean isSyncing() {
		return pending != null;
	}

	private static final class Pending<F> {
		private final SyncHeader header;
		private final Map<Identifier, F> features = new LinkedHashMap<>();
		private final Map<Identifier, Set<Identifier>> featureBiomes = new LinkedHashMap<>();

		private Pending(SyncHeader header) {
			this.header = header;
		}

		private Set<Identifier> resolve(int[] indices) {
			Set<Identifier> biomes = new LinkedHashSet<>();
			for (int index : indices) {
				if (index >= 0 && index < header.biomes().size()) {
					biomes.add(header.biomes().get(index));
				}
			}
			return biomes;
		}

		private void add(SyncBatch<F> batch) {
			for (SyncBatch.Entry<F> entry : batch.entries()) {
				if (entry.feature() == null) {
					continue;
				}
				features.put(entry.id(), entry.feature());
				featureBiomes.put(entry.id(), resolve(entry.biomeIndices()));
			}
		}

		private OresSnapshot<F> build() {
			Map<Identifier, OresSnapshot.Dimension> dimensions = new LinkedHashMap<>();
			for (SyncHeader.Dimension dimension : header.dimensions()) {
				dimensions.put(dimension.id(), new OresSnapshot.Dimension(dimension.id(), dimension.bottomY(), dimension.topY(), resolve(dimension.biomeIndices())));
			}
			return new OresSnapshot<>(header.syncId(), header.featureCount(), features, featureBiomes, dimensions);
		}
	}
}
