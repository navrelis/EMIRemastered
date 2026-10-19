/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.util.Identifier;

/**
 * Client side of the ore sync, without any Minecraft or EMI dependency beyond identifiers.
 *
 * <p>A sync is a start header (sync id, number of batches), the numbered feature batches and a complete marker.
 * Batches are collected in a pending buffer and only a sync whose marker matches the header and whose batches all
 * arrived replaces the snapshot that EMI reads, so a reload that runs while batches are still arriving sees the
 * previous complete data (or none) instead of a partial list. Batches may come in any order, a repeated batch is
 * ignored, a new start drops an unfinished sync, and a marker that does not fit drops the pending sync and keeps the
 * snapshot.
 *
 * <p>EMI reads the data through {@link #consume()}, which also records which snapshot EMI used. When a sync
 * completes and EMI's last reload used an older one, {@link Status#COMPLETE_RELOAD} tells the caller to start
 * a new EMI reload. Everything is synchronized, so no reload can slip between the read and the check.
 */
public final class OresSyncState<F> {
	private static final int NOT_CONSUMED = -1;

	public enum Status {
		/** A packet of another sync, a packet without a start, or a batch index the start did not announce. */
		IGNORED,
		/** A batch that was already received. */
		DUPLICATE,
		/** The batch is stored, more packets are expected. */
		PENDING,
		/** The marker did not fit (wrong batch count or missing batches): the pending sync was dropped, the snapshot stays. */
		INCOMPLETE,
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

	/** Stores a batch of the pending sync. */
	public synchronized Status accept(SyncBatch<F> batch) {
		if (pending == null || pending.header.syncId() != batch.syncId()) {
			return Status.IGNORED;
		}
		if (batch.index() < 0 || batch.index() >= pending.header.batchCount()) {
			return Status.IGNORED;
		}
		return pending.batches.putIfAbsent(batch.index(), batch) == null ? Status.PENDING : Status.DUPLICATE;
	}

	/** Ends the pending sync: swaps it in when the marker fits and every batch arrived. */
	public synchronized Status complete(SyncComplete marker) {
		if (pending == null || pending.header.syncId() != marker.syncId()) {
			return Status.IGNORED;
		}
		Pending<F> candidate = pending;
		pending = null;
		if (marker.batchCount() != candidate.header.batchCount() || candidate.batches.size() != candidate.header.batchCount()) {
			return Status.INCOMPLETE;
		}
		OresSnapshot<F> complete = candidate.build();
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

	/** True while a started sync has not been completed or dropped. */
	public synchronized boolean isSyncing() {
		return pending != null;
	}

	/** Number of batches of the pending sync that are still missing, 0 when nothing is pending. */
	public synchronized int missingBatches() {
		return pending == null ? 0 : pending.header.batchCount() - pending.batches.size();
	}

	private static final class Pending<F> {
		private final SyncHeader header;
		private final Map<Integer, SyncBatch<F>> batches = new TreeMap<>();

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

		private OresSnapshot<F> build() {
			Map<Identifier, F> features = new LinkedHashMap<>();
			Map<Identifier, Set<Identifier>> featureBiomes = new LinkedHashMap<>();
			for (SyncBatch<F> batch : batches.values()) {
				for (SyncBatch.Entry<F> entry : batch.entries()) {
					if (entry.feature() == null) {
						continue;
					}
					features.put(entry.id(), entry.feature());
					featureBiomes.put(entry.id(), resolve(entry.biomeIndices()));
				}
			}
			Map<Identifier, OresSnapshot.Dimension> dimensions = new LinkedHashMap<>();
			for (SyncHeader.Dimension dimension : header.dimensions()) {
				dimensions.put(dimension.id(), new OresSnapshot.Dimension(dimension.id(), dimension.bottomY(), dimension.topY(), resolve(dimension.biomeIndices())));
			}
			return new OresSnapshot<>(header.syncId(), header.featureCount(), features, featureBiomes, dimensions);
		}
	}
}
