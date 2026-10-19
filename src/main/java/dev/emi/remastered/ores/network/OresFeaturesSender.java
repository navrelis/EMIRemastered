/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.network;

import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.network.payload.S2CSyncBatchPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncCompletePayload;
import dev.emi.remastered.ores.network.payload.S2CSyncStartPayload;
import dev.emi.remastered.ores.sync.OresSyncPlan;
import dev.emi.remastered.ores.sync.SyncBatch;
import dev.emi.remastered.ores.sync.SyncHeader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
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
	private static final Object CACHE_LOCK = new Object();
	private static CachedPlan cached;

	private record CachedPlan(MinecraftServer server, Registry<PlacedFeature> registry, OresSyncPlan<PlacedFeature> plan) {
	}

	/**
	 * Sends the ore and geode features, the biomes they are in and the dimensions with their biomes to a player,
	 * if the client can receive them: the start packet (sync id, batch count, biome table, dimensions), the numbered
	 * batches and the complete marker. The client only uses the data once the marker arrived.
	 */
	public static void onSyncDataPackContents(ServerPlayerEntity player, boolean joined) {
		// Vanilla clients and clients without the module cannot receive custom payloads they did not register
		if (!ServerPlayNetworking.canSend(player, S2CSyncStartPayload.TYPE)
			|| !ServerPlayNetworking.canSend(player, S2CSyncBatchPayload.TYPE)
			|| !ServerPlayNetworking.canSend(player, S2CSyncCompletePayload.TYPE)) {
			return;
		}

		OresSyncPlan<PlacedFeature> plan = plan(player.server);
		ServerPlayNetworking.send(player, new S2CSyncStartPayload(plan.header()));
		for (SyncBatch<PlacedFeature> batch : plan.batches()) {
			ServerPlayNetworking.send(player, new S2CSyncBatchPayload(batch));
		}
		ServerPlayNetworking.send(player, new S2CSyncCompletePayload(plan.complete()));
	}

	/**
	 * The sync plan of the server's current data, built once and shared by all players (and by every join and data pack
	 * sync) until {@link #invalidate()} or until the server or its placed feature registry changes. The sync id is the
	 * same for all players of one generation.
	 */
	public static OresSyncPlan<PlacedFeature> plan(MinecraftServer server) {
		Registry<PlacedFeature> registry = server.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);
		synchronized (CACHE_LOCK) {
			CachedPlan current = cached;
			if (current == null || current.server() != server || current.registry() != registry) {
				OresSyncPlan<PlacedFeature> plan = buildPlan(server, SYNC_COUNTER.incrementAndGet());
				logSummary(plan);
				current = new CachedPlan(server, registry, plan);
				cached = current;
			}
			return current.plan();
		}
	}

	/** Drops the cached plan: the next join builds a new one. Data pack reload and server stop. */
	public static void invalidate() {
		synchronized (CACHE_LOCK) {
			cached = null;
		}
	}

	private static void logSummary(OresSyncPlan<PlacedFeature> plan) {
		SyncHeader header = plan.header();
		String dimensions = header.dimensions().stream()
			.map(d -> d.id() + " (Y " + d.bottomY() + ".." + d.topY() + ", " + d.biomeIndices().length + " biomes)")
			.collect(Collectors.joining(", "));
		Ores.LOG.info("Ore sync {}: {} ore and geode features in {} batches, {} dimensions: {}", header.syncId(),
			header.featureCount(), plan.batches().size(), header.dimensions().size(), dimensions);
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
							features2biomes.computeIfAbsent(featureId, k -> new TreeSet<>()).add(biomeEntry.getKey().getValue());
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
		Set<Identifier> seen = new LinkedHashSet<>();
		for (ServerWorld world : server.getWorlds()) {
			Identifier id = world.getRegistryKey().getValue();
			try {
				ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
				HeightContext context = new HeightContext(generator, world);
				dimensions.add(new OresSyncPlan.DimensionSource(id, context.getMinY(), context.getMinY() + context.getHeight(), biomesOf(generator)));
				seen.add(id);
			} catch (RuntimeException e) {
				Ores.LOG.warn("Not sending the biomes of dimension {} to clients: {}", id, e.toString());
			}
		}
		// dimensions that are registered but have no loaded world (a mod may create those on demand)
		Registry<DimensionOptions> registered = server.getRegistryManager().get(RegistryKeys.DIMENSION);
		for (Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : registered.getEntrySet()) {
			Identifier id = entry.getKey().getValue();
			if (seen.contains(id)) {
				continue;
			}
			try {
				DimensionOptions options = entry.getValue();
				ChunkGenerator generator = options.chunkGenerator();
				DimensionType type = options.dimensionTypeEntry().value();
				int bottomY = Math.max(type.minY(), generator.getMinimumY());
				int height = Math.min(type.height(), generator.getWorldHeight());
				dimensions.add(new OresSyncPlan.DimensionSource(id, bottomY, bottomY + height, biomesOf(generator)));
			} catch (RuntimeException e) {
				Ores.LOG.warn("Not sending the biomes of dimension {} to clients: {}", id, e.toString());
			}
		}
		// overworld, nether and end first, in this order, then the rest by id (the registry is hash ordered)
		dimensions.sort(Comparator.comparingInt((OresSyncPlan.DimensionSource d) -> vanillaOrder(d.id())).thenComparing(OresSyncPlan.DimensionSource::id));
		return dimensions;
	}

	private static Collection<Identifier> biomesOf(ChunkGenerator generator) {
		// a sorted set: the biome source gives its biomes in hash order
		Collection<Identifier> biomes = new TreeSet<>();
		for (RegistryEntry<Biome> biome : generator.getBiomeSource().getBiomes()) {
			biome.getKey().ifPresent(key -> biomes.add(key.getValue()));
		}
		return biomes;
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
