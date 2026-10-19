/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network.payload;

import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.sync.SyncComplete;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Last packet of an ore sync, sent after the final batch. Only now does the client use the data of the sync, and only
 * if the sync id and batch count match the start packet.
 */
public record S2CSyncCompletePayload(SyncComplete marker) implements CustomPayload {
	public static final Id<S2CSyncCompletePayload> TYPE = new Id<>(Ores.id("sync_complete"));
	public static final PacketCodec<PacketByteBuf, S2CSyncCompletePayload> CODEC = PacketCodec.ofStatic(S2CSyncCompletePayload::encode, S2CSyncCompletePayload::decode);

	public static S2CSyncCompletePayload decode(PacketByteBuf buffer) {
		int syncId = buffer.readVarInt();
		int batchCount = buffer.readVarInt();
		return new S2CSyncCompletePayload(new SyncComplete(syncId, batchCount));
	}

	public static void encode(PacketByteBuf buffer, S2CSyncCompletePayload packet) {
		buffer.writeVarInt(packet.marker.syncId());
		buffer.writeVarInt(packet.marker.batchCount());
	}

	@NotNull
	@Override
	public Id<S2CSyncCompletePayload> getId() {
		return TYPE;
	}
}
