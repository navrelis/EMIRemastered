package dev.emi.remastered.loot.client;

import dev.emi.remastered.loot.info.LootInfoProvider;
import dev.emi.remastered.loot.networking.ArchaeologyLootPayload;
import dev.emi.remastered.loot.networking.BlockLootPayload;
import dev.emi.remastered.loot.networking.ChestLootPayload;
import dev.emi.remastered.info.search.SourceIndex;
import dev.emi.remastered.loot.networking.ChestStructuresPayload;
import dev.emi.remastered.loot.networking.ClearPayload;
import dev.emi.remastered.loot.networking.GameplayLootPayload;
import dev.emi.remastered.loot.networking.MobLootPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Consumer;

/**
 * Client side receivers of the loot table payloads. Payload handlers run on the client thread.
 */
public class ClientLootTablesReceiver {

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ClearPayload.TYPE, (payload, ctx) -> ClientLootTables.INSTANCE.clearLoots());
		ClientPlayNetworking.registerGlobalReceiver(ChestLootPayload.TYPE, (payload, ctx) -> receive(payload.data(), ClientLootTables.INSTANCE::receiveChestSender));
		ClientPlayNetworking.registerGlobalReceiver(ChestStructuresPayload.TYPE, (payload, ctx) -> {
			if (ClientLootTables.INSTANCE.receiveChestStructures(new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()))) > 0) {
				// chest pages read the structures when they are built; Item Info pages are cached and need a refresh
				LootInfoProvider.refresh();
				SourceIndex.refreshAsync(); // the `~` search names the structures of chests
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(BlockLootPayload.TYPE, (payload, ctx) -> receive(payload.data(), ClientLootTables.INSTANCE::receiveBlockSender));
		ClientPlayNetworking.registerGlobalReceiver(MobLootPayload.TYPE, (payload, ctx) -> receive(payload.data(), ClientLootTables.INSTANCE::receiveMobSender));
		ClientPlayNetworking.registerGlobalReceiver(GameplayLootPayload.TYPE, (payload, ctx) -> receive(payload.data(), ClientLootTables.INSTANCE::receiveGameplaySender));
		ClientPlayNetworking.registerGlobalReceiver(ArchaeologyLootPayload.TYPE, (payload, ctx) -> receive(payload.data(), ClientLootTables.INSTANCE::receiveArchaeologySender));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLootTables.INSTANCE.clearLoots());
	}

	private static void receive(byte[] data, Consumer<PacketByteBuf> receiver) {
		receiver.accept(new PacketByteBuf(Unpooled.wrappedBuffer(data)));
	}
}
