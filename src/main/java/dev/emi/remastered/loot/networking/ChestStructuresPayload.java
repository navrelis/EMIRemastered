package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * The structures of chest loot tables the client already received without them, because the server's structure
 * lookup was not done yet when the chest payloads were sent. Layout: a VarInt count, then per table its id as the
 * chest payload writes it and a {@link dev.emi.remastered.loot.structure.StructureRef} block. The client applies it to
 * the chest loot tables it holds; clients that do not know the payload never get it (the server checks canSend).
 */
public record ChestStructuresPayload(byte[] data) implements CustomPayload {
	public static final Id<ChestStructuresPayload> TYPE = new Id<>(EMILoot.identity("chest_structures"));
	public static final PacketCodec<PacketByteBuf, ChestStructuresPayload> CODEC = PacketCodec.of(ChestStructuresPayload::write, ChestStructuresPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBytes(data);
	}

	private static ChestStructuresPayload read(PacketByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new ChestStructuresPayload(data);
	}

	@Override
	public Id<ChestStructuresPayload> getId() {
		return TYPE;
	}
}
