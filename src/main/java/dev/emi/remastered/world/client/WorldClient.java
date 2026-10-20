package dev.emi.remastered.world.client;

import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.data.LootLinksWire;
import dev.emi.remastered.world.data.WorldWire;
import dev.emi.remastered.world.network.StructureLootPayload;
import dev.emi.remastered.world.network.StructuresPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client side of the world module. Only reached from {@code WorldContent.initClient()}. */
public final class WorldClient {

	private WorldClient() {
	}

	public static void init() {
		ItemInfoRegistry.register(new WorldInfoProvider());
		ClientPlayNetworking.registerGlobalReceiver(StructuresPayload.TYPE, (payload, context) -> {
			try {
				ClientWorldData.accept(payload.first(), payload.last(), WorldWire.decode(payload.data()));
			} catch (IllegalArgumentException e) {
				World.LOGGER.error("Ignoring a malformed world data payload", e);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(StructureLootPayload.TYPE, (payload, context) -> {
			try {
				ClientWorldData.acceptLoot(payload.first(), LootLinksWire.decode(payload.data()));
			} catch (IllegalArgumentException e) {
				World.LOGGER.error("Ignoring a malformed structure loot payload", e);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientWorldData.clear());
	}
}
