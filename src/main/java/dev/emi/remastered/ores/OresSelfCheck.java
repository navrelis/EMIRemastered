/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores;

import dev.emi.remastered.ores.network.OresFeaturesSender;
import dev.emi.remastered.ores.network.payload.S2CSyncBatchPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncCompletePayload;
import dev.emi.remastered.ores.network.payload.S2CSyncStartPayload;
import dev.emi.remastered.ores.sync.OresSnapshot;
import dev.emi.remastered.ores.sync.OresSyncPlan;
import dev.emi.remastered.ores.sync.OresSyncState;
import dev.emi.remastered.ores.sync.SyncBatch;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Development environment only. Runs once when a server has started: takes the sync the server would send (the
 * cached plan), pushes it through the packet codecs and the client's accumulator, and warns about any mismatch.
 */
public class OresSelfCheck {
	/** The limit of a custom payload packet. */
	private static final int PACKET_LIMIT = 1_048_576;

	public static void run(MinecraftServer server) {
		try {
			check(server);
		} catch (RuntimeException e) {
			Ores.LOG.warn("The ore sync self check failed", e);
		}
	}

	private static void check(MinecraftServer server) {
		OresSyncPlan<PlacedFeature> plan = OresFeaturesSender.plan(server);
		OresSyncState<PlacedFeature> state = new OresSyncState<>();
		int largest = 0;

		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		S2CSyncStartPayload.encode(buf, new S2CSyncStartPayload(plan.header()));
		largest = Math.max(largest, buf.readableBytes());
		state.begin(S2CSyncStartPayload.decode(buf).header());
		for (SyncBatch<PlacedFeature> batch : plan.batches()) {
			buf = new PacketByteBuf(Unpooled.buffer());
			S2CSyncBatchPayload.encode(buf, new S2CSyncBatchPayload(batch));
			largest = Math.max(largest, buf.readableBytes());
			state.accept(S2CSyncBatchPayload.decode(buf).batch());
		}
		buf = new PacketByteBuf(Unpooled.buffer());
		S2CSyncCompletePayload.encode(buf, new S2CSyncCompletePayload(plan.complete()));
		OresSyncState.Status status = state.complete(S2CSyncCompletePayload.decode(buf).marker());

		OresSnapshot<PlacedFeature> snapshot = state.snapshot();
		if (status != OresSyncState.Status.COMPLETE) {
			Ores.LOG.warn("The ore sync did not complete when sent through the codecs: {}", status);
		} else if (snapshot.features().size() != snapshot.expectedFeatures()) {
			Ores.LOG.warn("{} of {} ore and geode features could not be decoded again", snapshot.expectedFeatures() - snapshot.features().size(), snapshot.expectedFeatures());
		}
		if (largest > PACKET_LIMIT) {
			Ores.LOG.warn("An ore sync packet of {} bytes is too big for a custom payload", largest);
		} else if (status == OresSyncState.Status.COMPLETE && snapshot.features().size() == snapshot.expectedFeatures()) {
			Ores.LOG.info("Ore sync self check passed: {} features through the codecs, largest packet {} bytes", snapshot.features().size(), largest);
		}
	}
}
