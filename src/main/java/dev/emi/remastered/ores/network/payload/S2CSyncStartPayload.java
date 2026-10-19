/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network.payload;

import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.sync.SyncHeader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * First packet of an ore sync: the sync id, the biome table, the dimensions with their biomes and height range, and how many batches and
 * features follow. Replaces the biome info of the original EMI Ores (new id, a client with an older version of this
 * module does not register it and is simply not synced instead of being kicked).
 */
public record S2CSyncStartPayload(SyncHeader header) implements CustomPayload {
	public static final Id<S2CSyncStartPayload> TYPE = new Id<>(Ores.id("sync_start"));
	public static final PacketCodec<PacketByteBuf, S2CSyncStartPayload> CODEC = PacketCodec.ofStatic(S2CSyncStartPayload::encode, S2CSyncStartPayload::decode);

	public static S2CSyncStartPayload decode(PacketByteBuf buffer) {
		int syncId = buffer.readVarInt();
		int batchCount = buffer.readVarInt();
		int featureCount = buffer.readVarInt();
		int biomeCount = buffer.readVarInt();
		List<Identifier> biomes = new ArrayList<>(biomeCount);
		for (int i = 0; i < biomeCount; i++) {
			biomes.add(buffer.readIdentifier());
		}
		int dimensionCount = buffer.readVarInt();
		List<SyncHeader.Dimension> dimensions = new ArrayList<>(dimensionCount);
		for (int i = 0; i < dimensionCount; i++) {
			Identifier id = buffer.readIdentifier();
			int bottomY = buffer.readInt();
			int topY = buffer.readInt();
			dimensions.add(new SyncHeader.Dimension(id, bottomY, topY, buffer.readIntArray()));
		}
		return new S2CSyncStartPayload(new SyncHeader(syncId, batchCount, featureCount, biomes, dimensions));
	}

	public static void encode(PacketByteBuf buffer, S2CSyncStartPayload packet) {
		SyncHeader header = packet.header;
		buffer.writeVarInt(header.syncId());
		buffer.writeVarInt(header.batchCount());
		buffer.writeVarInt(header.featureCount());
		buffer.writeVarInt(header.biomes().size());
		header.biomes().forEach(buffer::writeIdentifier);
		buffer.writeVarInt(header.dimensions().size());
		for (SyncHeader.Dimension dimension : header.dimensions()) {
			buffer.writeIdentifier(dimension.id());
			buffer.writeInt(dimension.bottomY());
			buffer.writeInt(dimension.topY());
			buffer.writeIntArray(dimension.biomeIndices());
		}
	}

	@NotNull
	@Override
	public Id<S2CSyncStartPayload> getId() {
		return TYPE;
	}
}
