/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.client;

import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.network.payload.S2CSyncBatchPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncCompletePayload;
import dev.emi.remastered.ores.network.payload.S2CSyncStartPayload;
import dev.emi.remastered.ores.sync.OresSnapshot;
import dev.emi.remastered.ores.sync.OresSyncState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Holds the data last sent by the server. A sync is only visible once its complete marker arrived and matched (see
 * {@link OresSyncState}), so an EMI reload that starts while batches are arriving works with the previous complete
 * data. When it did, or when it ran before any data came, a new reload is started as soon as the sync is complete.
 */
public class OresFeatures {
	private static final OresSyncState<PlacedFeature> STATE = new OresSyncState<>();

	/** The last complete snapshot, empty before the first sync. */
	public static OresSnapshot<PlacedFeature> snapshot() {
		return STATE.snapshot();
	}

	/** For EMI's registration: the snapshot to build recipes from. Remembers that EMI used it. */
	public static OresSnapshot<PlacedFeature> consume() {
		return STATE.consume();
	}

	public static void clear() {
		STATE.clear();
	}

	/** Always sent first by the server, it starts a new sync. The previous complete data stays until the new one is complete. */
	public static void receive(S2CSyncStartPayload payload) {
		if (STATE.isSyncing()) {
			Ores.LOG.warn("An ore sync was not completed, dropping it for the new sync {}", payload.header().syncId());
		}
		STATE.begin(payload.header());
		Ores.LOG.info("Ore sync {} started: {} features in {} batches, {} dimensions", payload.header().syncId(),
			payload.header().featureCount(), payload.header().batchCount(), payload.header().dimensions().size());
	}

	public static void receive(S2CSyncBatchPayload payload) {
		OresSyncState.Status status = STATE.accept(payload.batch());
		switch (status) {
			case IGNORED -> Ores.LOG.warn("Ignoring batch {} of ore sync {}, there is no matching start", payload.batch().index(), payload.batch().syncId());
			case DUPLICATE -> Ores.LOG.warn("Ignoring batch {} of ore sync {} that arrived twice", payload.batch().index(), payload.batch().syncId());
			default -> {
			}
		}
	}

	public static void receive(S2CSyncCompletePayload payload) {
		int missing = STATE.missingBatches();
		OresSyncState.Status status = STATE.complete(payload.marker());
		switch (status) {
			case IGNORED -> Ores.LOG.warn("Ignoring the end of ore sync {}, there is no matching start", payload.marker().syncId());
			case INCOMPLETE -> Ores.LOG.warn("Ore sync {} is incomplete ({} of {} batches missing), keeping the previous data",
				payload.marker().syncId(), missing, payload.marker().batchCount());
			case COMPLETE, COMPLETE_RELOAD -> {
				OresSnapshot<PlacedFeature> snapshot = STATE.snapshot();
				if (snapshot.features().size() < snapshot.expectedFeatures()) {
					Ores.LOG.warn("Ore sync {} is missing {} of {} features that could not be decoded", snapshot.syncId(),
						snapshot.expectedFeatures() - snapshot.features().size(), snapshot.expectedFeatures());
				}
				Ores.LOG.info("Ore sync {} complete: {} features, {} dimensions", snapshot.syncId(),
					snapshot.features().size(), snapshot.dimensions().size());
				if (status == OresSyncState.Status.COMPLETE_RELOAD) {
					Ores.LOG.info("EMI loaded older ore data, reloading EMI for ore sync {}", snapshot.syncId());
					MinecraftClient.getInstance().execute(EmiReloadManager::reload);
				}
			}
			default -> {
			}
		}
	}
}
