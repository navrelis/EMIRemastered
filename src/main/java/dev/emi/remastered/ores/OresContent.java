/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.ores.client.OresClient;
import dev.emi.remastered.ores.client.OresEmiRegistration;
import dev.emi.remastered.ores.network.OresFeaturesSender;
import dev.emi.remastered.ores.network.payload.S2CSyncStartPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncBatchPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncCompletePayload;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class OresContent {

	/** Common init, runs on the client and the dedicated server. */
	public static void init() {
		PayloadTypeRegistry.playS2C().register(S2CSyncStartPayload.TYPE, S2CSyncStartPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CSyncBatchPayload.TYPE, S2CSyncBatchPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CSyncCompletePayload.TYPE, S2CSyncCompletePayload.CODEC);

		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(OresFeaturesSender::onSyncDataPackContents);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> OresFeaturesSender.invalidate());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> OresFeaturesSender.invalidate());
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			ServerLifecycleEvents.SERVER_STARTED.register(OresSelfCheck::run);
		}
	}

	/** Client-only init, only called when the module is enabled. */
	public static void initClient() {
		OresClient.init();
	}

	public static void initialize(EmiInitRegistry registry) {
		OresEmiRegistration.initialize(registry);
	}

	public static void register(EmiRegistry registry) {
		OresEmiRegistration.register(registry);
	}
}
