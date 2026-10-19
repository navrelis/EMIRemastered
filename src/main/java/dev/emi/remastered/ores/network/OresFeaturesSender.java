/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network;

import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.network.payload.S2CSyncBatchPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncStartPayload;
import dev.emi.remastered.ores.sync.OresSyncPlan;
import dev.emi.remastered.ores.sync.SyncBatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;

public class OresFeaturesSender {
	private static final AtomicInteger SYNC_COUNTER = new AtomicInteger();

	/**
	 * Sends the ore and geode features, the biomes they are in and the dimensions with their biomes to a player,
	 * if the client can receive them. The start packet comes first, the last batch carries the end-of-sync flag.
	 */
	public static void onSyncDataPackContents(ServerPlayerEntity player, boolean joined) {
		// Vanilla clients and clients without the module cannot receive custom payloads they did not register
		if (!ServerPlayNetworking.canSend(player, S2CSyncStartPayload.TYPE)
			|| !ServerPlayNetworking.canSend(player, S2CSyncBatchPayload.TYPE)) {
			return;
		}

		OresSyncPlan<PlacedFeature> plan = buildPlan(player.server, SYNC_COUNTER.incrementAndGet());
		ServerPlayNetworking.send(player, new S2CSyncStartPayload(plan.header()));
		for (SyncBatch<PlacedFeature> batch : plan.batches()) {
			ServerPlayNetworking.send(player, new S2CSyncBatchPayload(batch));
		}
		Ores.LOG.info("Sent ore sync {} to {}: {} features in {} batches, {} dimensions", plan.header().syncId(),
			player.getGameProfile().getName(), plan.header().featureCount(), plan.batches().size(), plan.header().dimensions().size());
	}

	public static OresSyncPlan<PlacedFeature> buildPlan(MinecraftServer server, int syncId) {
		DynamicRegistryManager access = server.getRegistryManager();
		Registry<PlacedFeature> placedFeatureRegistry = access.get(RegistryKeys.PLACED_FEATURE);

		// sorted by id, so the output does not depend on hash order
		Map<Identifier, PlacedFeature> featureMap = new TreeMap<>();
		Map<Identifier, Integer> sizes = new LinkedHashMap<>();
		placedFeatureRegistry.getEntrySet().forEach(entry -> {
			// we only care about ore features for now
			PlacedFeature pf = entry.getValue();
			RegistryEntry<ConfiguredFeature<?, ?>> configured = pf.feature();
			// a mod may register a placed feature whose configured feature never gets bound
			if (!configured.hasKeyAndValue()) {
				return;
			}
			FeatureConfig fc = configured.value().config();
			if (fc instanceof OreFeatureConfig || fc instanceof GeodeFeatureConfig) {
				// remove problematic placement modifiers
				List<PlacementModifier> newModifiers = pf.placementModifiers()
					.stream()
					.filter(OresFeaturesSender::isSupported)
					.toList();

				Identifier id = entry.getKey().getValue();
				PlacedFeature stripped = new PlacedFeature(configured, newModifiers);
				int size = encodedSize(id, stripped);
				if (size >= 0) {
					featureMap.put(id, stripped);
					sizes.put(id, size);
				}
			}
		});

		Map<Identifier, Set<Identifier>> features2biomes = new LinkedHashMap<>();
		access.get(RegistryKeys.BIOME).getEntrySet().forEach(biomeEntry -> {
			biomeEntry.getValue().getGenerationSettings().getFeatures().forEach(placedFeatureHolderSet -> {
				placedFeatureHolderSet.forEach(placedFeatureHolder -> {
					placedFeatureHolder.getKey().ifPresent(placedFeatureResourceKey -> {
						Identifier featureId = placedFeatureResourceKey.getValue();
						if (featureMap.containsKey(featureId)) {
							features2biomes.computeIfAbsent(featureId, k -> new LinkedHashSet<>()).add(biomeEntry.getKey().getValue());
						}
					});
				});
			});
		});

		Map<Identifier, OresSyncPlan.Feature<PlacedFeature>> features = new LinkedHashMap<>();
		featureMap.forEach((id, feature) -> features.put(id,
			new OresSyncPlan.Feature<>(feature, sizes.get(id) + 64, features2biomes.getOrDefault(id, Set.of()))));
		return OresSyncPlan.build(syncId, features, collectDimensions(server));
	}

	/**
	 * Every dimension with the biomes its chunk generator's biome source can produce, whichever mod added it, and
	 * the height range feature placement sees (not always the one of the dimension type: the nether is 256 high,
	 * but its noise settings only generate 128).
	 */
	private static List<OresSyncPlan.DimensionSource> collectDimensions(MinecraftServer server) {
		List<OresSyncPlan.DimensionSource> dimensions = new ArrayList<>();
		for (ServerWorld world : server.getWorlds()) {
			Identifier id = world.getRegistryKey().getValue();
			try {
				ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
				Collection<Identifier> biomes = new LinkedHashSet<>();
				for (RegistryEntry<Biome> biome : generator.getBiomeSource().getBiomes()) {
					biome.getKey().ifPresent(key -> biomes.add(key.getValue()));
				}
				HeightContext context = new HeightContext(generator, world);
				dimensions.add(new OresSyncPlan.DimensionSource(id, context.getMinY(), context.getMinY() + context.getHeight(), biomes));
			} catch (RuntimeException e) {
				Ores.LOG.warn("Not sending the biomes of dimension {} to clients: {}", id, e.toString());
			}
		}
		// overworld, nether and end first, in this order, then the rest as the server lists them (the sort is stable)
		dimensions.sort(Comparator.comparingInt(d -> vanillaOrder(d.id())));
		return dimensions;
	}

	private static int vanillaOrder(Identifier id) {
		if (World.OVERWORLD.getValue().equals(id)) {
			return 0;
		}
		if (World.NETHER.getValue().equals(id)) {
			return 1;
		}
		if (World.END.getValue().equals(id)) {
			return 2;
		}
		return 3;
	}

	/**
	 * A feature that cannot be encoded would throw while the packet is written and disconnect the
	 * player, so such features are left out.
	 *
	 * @return the length of the feature's JSON, or -1 when it must not be sent
	 */
	private static int encodedSize(Identifier id, PlacedFeature feature) {
		try {
			int length = S2CSyncBatchPayload.toJson(feature).length();
			return length <= S2CSyncBatchPayload.MAX_JSON_LENGTH ? length : -1;
		} catch (RuntimeException e) {
			Ores.LOG.warn("Not sending placed feature {} to clients, it cannot be serialized: {}", id, e.getMessage());
			return -1;
		}
	}

	private static boolean isSupported(PlacementModifier modifier) {
		return modifier instanceof HeightRangePlacementModifier
			|| modifier instanceof CountPlacementModifier
			|| modifier instanceof RarityFilterPlacementModifier;
	}
}
