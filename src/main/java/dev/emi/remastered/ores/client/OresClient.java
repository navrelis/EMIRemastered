/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.client;

import dev.emi.remastered.ores.network.payload.S2CSendBiomeInfoPayload;
import dev.emi.remastered.ores.network.payload.S2CSendFeaturesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class OresClient {

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(S2CSendBiomeInfoPayload.TYPE,
			(payload, context) -> OresFeatures.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(S2CSendFeaturesPayload.TYPE,
			(payload, context) -> OresFeatures.receive(payload));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> OresFeatures.clearFeatures());
	}
}
