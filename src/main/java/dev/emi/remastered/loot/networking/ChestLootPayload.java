package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * One chest loot table, in the same binary layout the upstream mod used: see the matching table sender and client table.
 */
public record ChestLootPayload(byte[] data) implements CustomPayload {
	public static final Id<ChestLootPayload> TYPE = new Id<>(EMILoot.identity("chest"));
	public static final PacketCodec<PacketByteBuf, ChestLootPayload> CODEC = PacketCodec.of(ChestLootPayload::write, ChestLootPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBytes(data);
	}

	private static ChestLootPayload read(PacketByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new ChestLootPayload(data);
	}

	@Override
	public Id<ChestLootPayload> getId() {
		return TYPE;
	}
}
