package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * One archaeology loot table, in the same binary layout the upstream mod used: see the matching table sender and client table.
 */
public record ArchaeologyLootPayload(byte[] data) implements CustomPayload {
	public static final Id<ArchaeologyLootPayload> TYPE = new Id<>(EMILoot.identity("archaeology"));
	public static final PacketCodec<PacketByteBuf, ArchaeologyLootPayload> CODEC = PacketCodec.of(ArchaeologyLootPayload::write, ArchaeologyLootPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBytes(data);
	}

	private static ArchaeologyLootPayload read(PacketByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new ArchaeologyLootPayload(data);
	}

	@Override
	public Id<ArchaeologyLootPayload> getId() {
		return TYPE;
	}
}
