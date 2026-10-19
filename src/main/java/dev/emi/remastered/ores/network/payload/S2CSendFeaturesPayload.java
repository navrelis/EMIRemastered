/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network.payload;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.emi.remastered.ores.Ores;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.jetbrains.annotations.NotNull;

/**
 * Placed features by id, each as a JSON string. The wire format is the one of the original
 * EMI Ores. The server may send several of these in a row, the client merges them.
 */
public record S2CSendFeaturesPayload(Map<Identifier, PlacedFeature> features) implements CustomPayload {
	public static final Id<S2CSendFeaturesPayload> TYPE = new Id<>(Ores.id("send_features"));
	public static final PacketCodec<PacketByteBuf, S2CSendFeaturesPayload> CODEC = PacketCodec.ofStatic(S2CSendFeaturesPayload::encode, S2CSendFeaturesPayload::decode);
	/** The vanilla string codec would stop at 32767 characters, big modded features can be longer. */
	public static final int MAX_JSON_LENGTH = 262144;
	private static final Gson GSON = new Gson();

	public static S2CSendFeaturesPayload decode(PacketByteBuf buffer) {
		int size = buffer.readVarInt();
		Map<Identifier, PlacedFeature> features = new HashMap<>();
		for (int i = 0; i < size; i++) {
			Identifier id = buffer.readIdentifier();
			String json = buffer.readString(MAX_JSON_LENGTH);
			try {
				features.put(id, fromJson(json));
			} catch (RuntimeException e) {
				// one feature that does not decode here must not take the whole connection down
				Ores.LOG.warn("Skipping placed feature {} that could not be decoded: {}", id, e.getMessage());
			}
		}
		return new S2CSendFeaturesPayload(features);
	}

	public static void encode(PacketByteBuf buffer, S2CSendFeaturesPayload packet) {
		buffer.writeVarInt(packet.features.size());
		packet.features.forEach((id, feature) -> {
			buffer.writeIdentifier(id);
			buffer.writeString(toJson(feature), MAX_JSON_LENGTH);
		});
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
	public Id<S2CSendFeaturesPayload> getId() {
		return TYPE;
	}
}
