package dev.emi.remastered.loot.server;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import dev.emi.remastered.loot.networking.ChestLootPayload;
import dev.emi.remastered.loot.networking.ChestStructuresPayload;
import dev.emi.remastered.loot.networking.LootNetworking;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.structure.LateDelivery;
import dev.emi.remastered.loot.structure.StructureLookup;
import dev.emi.remastered.loot.structure.StructureRef;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The structures of the chest loot pages without blocking the server thread. A player's chest payloads carry the
 * structures when the structure lookup is done; otherwise they go out without them, the player is remembered for the
 * lookup's generation, and once the lookup is done (server thread) every remembered player who is still online gets
 * one {@link ChestStructuresPayload} (split when big) with the structures of all chest tables. A data pack reload or a
 * server stop makes the generation stale: its callback is dropped, the players are synced again anyway.
 */
public final class ChestStructureSync {
	/** Bytes per payload; a clientbound custom payload may carry 1 MiB. */
	private static final int MAX_PAYLOAD_BYTES = 800_000;
	private static final LateDelivery<UUID> WAITING = new LateDelivery<>();

	private ChestStructureSync() {
	}

	/**
	 * Server thread, before the chest payloads of a sync are sent: the lookup to take the structures from, or null when
	 * it is not done yet (the player then gets the structures later).
	 */
	public static @Nullable StructureLookup begin(ServerPlayerEntity player, Collection<ChestLootTableSender> senders) {
		if (!LootSettings.chestStructures) {
			return null;
		}
		MinecraftServer server = player.server;
		StructureLookup.Build build = StructureLookup.current(server);
		StructureLookup lookup = build.now();
		UUID uuid = player.getUuid();
		if (lookup != null) {
			WAITING.remove(uuid);
			return lookup;
		}
		if (!senders.isEmpty() && ServerPlayNetworking.canSend(player, ChestStructuresPayload.TYPE) && WAITING.add(build.generation(), uuid)) {
			build.whenReady(ready -> deliver(server, build, ready));
		}
		return null;
	}

	/** Server stop. */
	public static void clear() {
		WAITING.clear();
	}

	private static void deliver(MinecraftServer server, StructureLookup.Build build, StructureLookup lookup) {
		List<UUID> waiting = WAITING.take(build.generation());
		if (waiting.isEmpty()) {
			return;
		}
		List<byte[]> payloads = null;
		int sent = 0;
		for (UUID uuid : waiting) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player == null || !LootNetworking.canSend(player, ChestLootPayload.TYPE) || !ServerPlayNetworking.canSend(player, ChestStructuresPayload.TYPE)) {
				continue;
			}
			if (payloads == null) {
				payloads = encode(lookup, LootTableParser.chestSenders());
			}
			for (byte[] data : payloads) {
				ServerPlayNetworking.send(player, new ChestStructuresPayload(data));
			}
			sent++;
		}
		EMILoot.LOGGER.info("The structures of chest loot tables were done {}ms after the lookup started, sent them to {} players", build.elapsedMillis(), sent);
	}

	/** Every chest table with known structures, as few payloads as the size limit allows. */
	private static List<byte[]> encode(StructureLookup lookup, Collection<ChestLootTableSender> senders) {
		List<byte[]> payloads = new ArrayList<>();
		PacketByteBuf entries = LootNetworking.newBuffer();
		int count = 0;
		try {
			for (ChestLootTableSender sender : senders) {
				List<StructureRef> refs = lookup.structuresOf(sender.getTableId());
				if (refs.isEmpty()) {
					continue;
				}
				if (count > 0 && entries.readableBytes() > MAX_PAYLOAD_BYTES) {
					payloads.add(finish(entries, count));
					entries.clear();
					count = 0;
				}
				entries.writeString(sender.getId());
				StructureRef.write(entries, refs);
				count++;
			}
			if (count > 0) {
				payloads.add(finish(entries, count));
			}
		} finally {
			entries.release();
		}
		return payloads;
	}

	private static byte[] finish(PacketByteBuf entries, int count) {
		PacketByteBuf buf = LootNetworking.newBuffer();
		try {
			buf.writeVarInt(count);
			buf.writeBytes(entries, entries.readerIndex(), entries.readableBytes());
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return data;
		} finally {
			buf.release();
		}
	}
}
