package dev.emi.remastered.mobs.server;

import java.util.ArrayList;
import java.util.List;

import dev.emi.remastered.mobs.Mobs;
import dev.emi.remastered.mobs.data.SpawnRow;
import dev.emi.remastered.mobs.data.SpawnWire;
import dev.emi.remastered.mobs.network.MobSpawnsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;

/**
 * Server side of the mobs module: collects the natural spawn entries of every biome (biome spawn settings are not
 * synced to clients) and sends them to clients that can receive them. Safe to load on a dedicated server.
 */
public final class MobsServer {

	private MobsServer() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.playS2C().register(MobSpawnsPayload.TYPE, MobSpawnsPayload.CODEC);
	}

	/** {@code ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS}: on join and after a data pack reload. */
	public static void onSyncDataPackContents(ServerPlayerEntity player, boolean joined) {
		// Clients without the module switched on (or vanilla clients) did not register the receiver
		if (!ServerPlayNetworking.canSend(player, MobSpawnsPayload.TYPE)) {
			return;
		}
		List<SpawnRow> rows = collect(player.server.getRegistryManager());
		List<byte[]> batches = SpawnWire.encode(rows, SpawnWire.ROWS_PER_BATCH);
		for (int i = 0; i < batches.size(); i++) {
			ServerPlayNetworking.send(player, new MobSpawnsPayload(i == 0, i == batches.size() - 1, batches.get(i)));
		}
		Mobs.LOGGER.debug("Sent {} spawn entries in {} payloads to {}", rows.size(), batches.size(), player.getGameProfile().getName());
	}

	/** Every spawn entry of every biome of the registry, in biome and spawn group order. */
	public static List<SpawnRow> collect(DynamicRegistryManager registries) {
		List<SpawnRow> rows = new ArrayList<>();
		Registry<Biome> biomes = registries.get(RegistryKeys.BIOME);
		biomes.getEntrySet().forEach(entry -> {
			String biome = entry.getKey().getValue().toString();
			SpawnSettings settings = entry.getValue().getSpawnSettings();
			for (SpawnGroup group : SpawnGroup.values()) {
				for (SpawnSettings.SpawnEntry spawn : settings.getSpawnEntries(group).getEntries()) {
					rows.add(new SpawnRow(Registries.ENTITY_TYPE.getId(spawn.type).toString(), biome, group.getName(),
						spawn.getWeight().getValue(), spawn.minGroupSize, spawn.maxGroupSize));
				}
			}
		});
		return rows;
	}
}
