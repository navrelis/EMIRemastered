package dev.emi.remastered.mobs.client;

import dev.emi.remastered.loot.client.ClientResourceData;
import dev.emi.remastered.mobs.Mobs;
import dev.emi.remastered.mobs.data.SpawnWire;
import dev.emi.remastered.mobs.network.MobSpawnsPayload;
import dev.emi.remastered.module.Modules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

/** Client side of the mobs module. Only reached from {@code MobsContent.initClient()}. */
public final class MobsClient {

	private MobsClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(MobSpawnsPayload.TYPE, (payload, context) -> {
			try {
				ClientSpawnData.accept(payload.first(), payload.last(), SpawnWire.decode(payload.data()));
			} catch (IllegalArgumentException e) {
				Mobs.LOGGER.error("Ignoring a malformed spawn data payload", e);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSpawnData.clear());

		// The mob portraits use the entity fixers of the loot module (offsets, scales, rotations). When the loot
		// module is off nobody loads them, so the mobs module does.
		if (!Modules.LOOT.isEnabled()) {
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new ClientResourceData.EntityOffsetsReloadListener());
		}
	}
}
