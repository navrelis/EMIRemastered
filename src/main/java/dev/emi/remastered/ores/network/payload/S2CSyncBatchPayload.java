/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network.payload;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.sync.SyncBatch;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.jetbrains.annotations.NotNull;

/**
 * A batch of placed features of a sync, each as an id, a JSON string (the format of the original EMI Ores) and the
 * indices of its biomes in the biome table of the start packet. The batch with the {@code last} flag ends the sync.
 */
public record S2CSyncBatchPayload(SyncBatch<PlacedFeature> batch) implements CustomPayload {
	public static final Id<S2CSyncBatchPayload> TYPE = new Id<>(Ores.id("sync_batch"));
	public static final PacketCodec<PacketByteBuf, S2CSyncBatchPayload> CODEC = PacketCodec.ofStatic(S2CSyncBatchPayload::encode, S2CSyncBatchPayload::decode);
	/** The vanilla string codec would stop at 32767 characters, big modded features can be longer. */
	public static final int MAX_JSON_LENGTH = 262144;
	private static final Gson GSON = new Gson();

	public static S2CSyncBatchPayload decode(PacketByteBuf buffer) {
		int syncId = buffer.readVarInt();
		boolean last = buffer.readBoolean();
		int size = buffer.readVarInt();
		List<SyncBatch.Entry<PlacedFeature>> entries = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Identifier id = buffer.readIdentifier();
			String json = buffer.readString(MAX_JSON_LENGTH);
			int[] biomes = buffer.readIntArray();
			PlacedFeature feature = null;
			try {
				feature = fromJson(json);
			} catch (RuntimeException e) {
				// one feature that does not decode here must not take the whole connection down
				Ores.LOG.warn("Skipping placed feature {} that could not be decoded: {}", id, e.getMessage());
			}
			entries.add(new SyncBatch.Entry<>(id, feature, biomes));
		}
		return new S2CSyncBatchPayload(new SyncBatch<>(syncId, last, entries));
	}

	public static void encode(PacketByteBuf buffer, S2CSyncBatchPayload packet) {
		SyncBatch<PlacedFeature> batch = packet.batch;
		buffer.writeVarInt(batch.syncId());
		buffer.writeBoolean(batch.last());
		buffer.writeVarInt(batch.entries().size());
		for (SyncBatch.Entry<PlacedFeature> entry : batch.entries()) {
			buffer.writeIdentifier(entry.id());
			buffer.writeString(toJson(entry.feature()), MAX_JSON_LENGTH);
			buffer.writeIntArray(entry.biomeIndices());
		}
	}

	public static String toJson(PlacedFeature feature) {
		return GSON.toJson(PlacedFeature.CODEC.encodeStart(JsonOps.INSTANCE, feature)
			.getOrThrow(error -> new EncoderException("Failed to encode json: " + error)));
	}

	public static PlacedFeature fromJson(String json) {
		JsonElement element = JsonHelper.deserialize(GSON, json, JsonElement.class);
		return PlacedFeature.CODEC.parse(JsonOps.INSTANCE, element)
			.getOrThrow(error -> new DecoderException("Failed to decode json: " + error));
	}

	@NotNull
	@Override
	public Id<S2CSyncBatchPayload> getId() {
		return TYPE;
	}
}
