package dev.emi.remastered.world.network;

import dev.emi.remastered.world.World;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * The chest loot tables of the structures, server to client, for world data that was sent before the loot module's
 * structure lookup was done. One or more batches; the one with {@code first} set replaces the client's loot links,
 * the others add to them. The data is the format of {@link dev.emi.remastered.world.data.LootLinksWire}. Only sent to
 * clients that registered a receiver.
 */
public record StructureLootPayload(boolean first, byte[] data) implements CustomPayload {
	public static final CustomPayload.Id<StructureLootPayload> TYPE = new CustomPayload.Id<>(World.id("structure_loot"));
	public static final PacketCodec<RegistryByteBuf, StructureLootPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOL, StructureLootPayload::first,
		PacketCodecs.BYTE_ARRAY, StructureLootPayload::data,
		StructureLootPayload::new);

	@Override
	public Id<StructureLootPayload> getId() {
		return TYPE;
	}
}
