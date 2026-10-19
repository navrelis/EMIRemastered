package dev.emi.remastered.mobs.network;

import dev.emi.remastered.mobs.Mobs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Natural spawn entries of all biomes, server to client. A sync is one or more batches: the first has {@code first}
 * set (the client drops its old data), the last has {@code last} set (the data is complete).
 */
public record MobSpawnsPayload(boolean first, boolean last, byte[] data) implements CustomPayload {
	public static final CustomPayload.Id<MobSpawnsPayload> TYPE = new CustomPayload.Id<>(Mobs.id("spawns"));
	public static final PacketCodec<RegistryByteBuf, MobSpawnsPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOL, MobSpawnsPayload::first,
		PacketCodecs.BOOL, MobSpawnsPayload::last,
		PacketCodecs.BYTE_ARRAY, MobSpawnsPayload::data,
		MobSpawnsPayload::new);

	@Override
	public Id<MobSpawnsPayload> getId() {
		return TYPE;
	}
}
