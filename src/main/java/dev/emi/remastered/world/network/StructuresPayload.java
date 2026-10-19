package dev.emi.remastered.world.network;

import dev.emi.remastered.world.World;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Structures and dimension biome sets, server to client. A sync is one or more batches: the first has {@code first}
 * set (the client drops its old data), the last has {@code last} set (the data is complete). The data is the format
 * of {@link dev.emi.remastered.world.data.WorldWire}.
 */
public record StructuresPayload(boolean first, boolean last, byte[] data) implements CustomPayload {
	public static final CustomPayload.Id<StructuresPayload> TYPE = new CustomPayload.Id<>(World.id("structures"));
	public static final PacketCodec<RegistryByteBuf, StructuresPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOL, StructuresPayload::first,
		PacketCodecs.BOOL, StructuresPayload::last,
		PacketCodecs.BYTE_ARRAY, StructuresPayload::data,
		StructuresPayload::new);

	@Override
	public Id<StructuresPayload> getId() {
		return TYPE;
	}
}
