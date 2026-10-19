/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network.payload;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import dev.emi.remastered.ores.Ores;
import java.util.HashSet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.jetbrains.annotations.NotNull;

/** The biomes each placed feature generates in. Always sent before the features. */
public record S2CSendBiomeInfoPayload(SetMultimap<RegistryKey<PlacedFeature>, RegistryKey<Biome>> biomeInfo) implements CustomPayload {
	public static final Id<S2CSendBiomeInfoPayload> TYPE = new Id<>(Ores.id("send_biome_info"));
	public static final PacketCodec<PacketByteBuf, S2CSendBiomeInfoPayload> CODEC = PacketCodec.ofStatic(S2CSendBiomeInfoPayload::encode, S2CSendBiomeInfoPayload::decode);

	public static S2CSendBiomeInfoPayload decode(PacketByteBuf buffer) {
		SetMultimap<RegistryKey<PlacedFeature>, RegistryKey<Biome>> map = HashMultimap.create();

		buffer.readMap(
			RegistryKey.createPacketCodec(RegistryKeys.PLACED_FEATURE),
			RegistryKey.createPacketCodec(RegistryKeys.BIOME).collect(PacketCodecs.toList())
		).forEach(map::putAll);

		return new S2CSendBiomeInfoPayload(map);
	}

	public static void encode(PacketByteBuf buffer, S2CSendBiomeInfoPayload packet) {
		buffer.writeMap(packet.biomeInfo.asMap(),
			RegistryKey.createPacketCodec(RegistryKeys.PLACED_FEATURE),
			RegistryKey.createPacketCodec(RegistryKeys.BIOME).collect(PacketCodecs.toCollection(HashSet::new))
		);
	}

	@NotNull
	@Override
	public Id<S2CSendBiomeInfoPayload> getId() {
		return TYPE;
	}
}
