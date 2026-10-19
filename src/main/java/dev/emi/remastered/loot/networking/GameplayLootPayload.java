package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * One gameplay loot table, in the same binary layout the upstream mod used: see the matching table sender and client table.
 */
public record GameplayLootPayload(byte[] data) implements CustomPayload {
	public static final Id<GameplayLootPayload> TYPE = new Id<>(EMILoot.identity("gameplay"));
	public static final PacketCodec<PacketByteBuf, GameplayLootPayload> CODEC = PacketCodec.of(GameplayLootPayload::write, GameplayLootPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBytes(data);
	}

	private static GameplayLootPayload read(PacketByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new GameplayLootPayload(data);
	}

	@Override
	public Id<GameplayLootPayload> getId() {
		return TYPE;
	}
}
