package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Function;

/**
 * Server side networking helpers and payload registration. Safe to load on a dedicated server.
 */
public class LootNetworking {
	/** Hard limit of a clientbound custom payload is 1 MiB, stay below it. */
	private static final int MAX_PAYLOAD_SIZE = 1_000_000;

	public static void registerPayloads() {
		PayloadTypeRegistry.playS2C().register(ClearPayload.TYPE, ClearPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ChestLootPayload.TYPE, ChestLootPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BlockLootPayload.TYPE, BlockLootPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MobLootPayload.TYPE, MobLootPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GameplayLootPayload.TYPE, GameplayLootPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ArchaeologyLootPayload.TYPE, ArchaeologyLootPayload.CODEC);
	}

	public static boolean canSend(ServerPlayerEntity player, CustomPayload.Id<?> type) {
		return ServerPlayNetworking.canSend(player, type);
	}

	/**
	 * Sends the content of the buffer to the player if the client can receive the payload type.
	 * The buffer is released. Tables that exceed the packet size limit are skipped with a warning.
	 */
	public static <P extends CustomPayload> void send(ServerPlayerEntity player, PacketByteBuf buf, Function<byte[], P> payloadFactory, String tableId) {
		try {
			if (buf.readableBytes() > MAX_PAYLOAD_SIZE) {
				EMILoot.LOGGER.warn("Loot table {} is too big to send ({} bytes), skipping it", tableId, buf.readableBytes());
				return;
			}
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			ServerPlayNetworking.send(player, payloadFactory.apply(data));
		} finally {
			buf.release();
		}
	}

	public static PacketByteBuf newBuffer() {
		return new PacketByteBuf(Unpooled.buffer());
	}
}
