package dev.emi.remastered.world.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.emi.remastered.loot.structure.LateDelivery;
import dev.emi.remastered.loot.structure.StructureLookup;
import dev.emi.remastered.loot.structure.StructureRef;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.data.DimensionRow;
import dev.emi.remastered.world.data.LootLinksWire;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.StructureRow.Placement;
import dev.emi.remastered.world.data.WorldWire;
import dev.emi.remastered.world.network.StructureLootPayload;
import dev.emi.remastered.world.network.StructuresPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Server side of the world module: collects every structure (dimensions, valid biomes, generation step, placement
 * from its structure set, chest loot tables from the loot module's structure lookup) and the biomes of every
 * dimension, and sends them to clients that can receive them. Structures, structure sets and biome sources are not
 * synced to clients by vanilla. Safe to load on a dedicated server.
 * <p>
 * The data is built once per data generation and shared by all players: the registry-bound part runs on the server
 * thread (it is cheap), the encoding on a daemon thread. The chest loot tables come from the loot module's structure
 * lookup, which reads structure templates in the background and can take many seconds in a big pack; nothing waits
 * for it. When it is done before the data is collected the rows carry the tables; otherwise the data goes out
 * without them and, once the lookup is done (server thread), every player who got the data gets a
 * {@link StructureLootPayload} with the tables, players who join later get it right after the data.
 */
public final class WorldServer {
	private static final Object LOCK = new Object();
	private static final LateDelivery<UUID> LOOT_WAITING = new LateDelivery<>();
	private static Plan current;
	private static int generation;

	/**
	 * The encoded batches of one data generation. A new data pack reload gives the server new reloadable registries
	 * (and with them new tags), so the plan is kept only for the same server, structure registry and reload.
	 */
	private static final class Plan {
		final MinecraftServer server;
		final Registry<Structure> registry;
		final ReloadableRegistries.Lookup reloadable;
		final int generation;
		final CompletableFuture<List<byte[]>> batches = new CompletableFuture<>();
		/** Whether the batches have no chest loot tables because the lookup was not done; they follow. Server thread. */
		boolean lootLater;
		/** The follow-up with the chest loot tables once the lookup is done. Server thread. */
		@Nullable List<byte[]> lootBatches;
		List<Identifier> chestTables = List.of();

		Plan(MinecraftServer server, Registry<Structure> registry, ReloadableRegistries.Lookup reloadable, int generation) {
			this.server = server;
			this.registry = registry;
			this.reloadable = reloadable;
			this.generation = generation;
		}
	}

	/** What the server thread collects. */
	private record Snapshot(List<DimensionRow> dimensions, List<StructureRow> structures, List<Identifier> chestTables) {
	}

	/** Where a structure's set places it. */
	private record SetInfo(String set, int weight, int totalWeight, Placement placement) {
	}

	private WorldServer() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.playS2C().register(StructuresPayload.TYPE, StructuresPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StructureLootPayload.TYPE, StructureLootPayload.CODEC);
	}

	/** {@code ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS}: on join and after a data pack reload. */
	public static void onSyncDataPackContents(ServerPlayerEntity player, boolean joined) {
		// Clients without the module switched on (or vanilla clients) did not register the receiver
		if (!ServerPlayNetworking.canSend(player, StructuresPayload.TYPE)) {
			return;
		}
		MinecraftServer server = player.server;
		Plan plan = plan(server);
		if (plan.batches.isDone()) {
			send(player, plan, plan.batches.join());
			return;
		}
		plan.batches.thenAcceptAsync(batches -> {
			// the player may have left, or the data may have been reloaded (which syncs every player again)
			if (server.getPlayerManager().getPlayerList().contains(player) && isCurrent(plan)) {
				send(player, plan, batches);
			}
		}, server);
	}

	/** {@code ServerLifecycleEvents.SERVER_STARTED}: builds the data now, so that the first player does not wait for it. */
	public static void prefetch(MinecraftServer server) {
		plan(server);
	}

	/** Drops the cached data and releases the server: server stop. */
	public static void invalidate() {
		synchronized (LOCK) {
			current = null;
		}
		LOOT_WAITING.clear();
	}

	private static boolean isCurrent(Plan plan) {
		synchronized (LOCK) {
			return current == plan;
		}
	}

	/** Server thread: the world data, then the chest loot tables if they came later. */
	private static void send(ServerPlayerEntity player, Plan plan, List<byte[]> batches) {
		for (int i = 0; i < batches.size(); i++) {
			ServerPlayNetworking.send(player, new StructuresPayload(i == 0, i == batches.size() - 1, batches.get(i)));
		}
		World.LOGGER.info("Sent the world data in {} payloads to {}", batches.size(), player.getGameProfile().getName());
		if (!plan.lootLater || !ServerPlayNetworking.canSend(player, StructureLootPayload.TYPE)) {
			return;
		}
		if (plan.lootBatches != null) {
			sendLoot(player, plan.lootBatches);
		} else {
			LOOT_WAITING.add(plan.generation, player.getUuid());
		}
	}

	private static void sendLoot(ServerPlayerEntity player, List<byte[]> batches) {
		for (int i = 0; i < batches.size(); i++) {
			ServerPlayNetworking.send(player, new StructureLootPayload(i == 0, batches.get(i)));
		}
	}

	private static Plan plan(MinecraftServer server) {
		Registry<Structure> registry = server.getRegistryManager().get(RegistryKeys.STRUCTURE);
		ReloadableRegistries.Lookup reloadable = server.getReloadableRegistries();
		Plan plan;
		StructureLookup.Build lookup = null;
		synchronized (LOCK) {
			if (current != null && current.server == server && current.registry == registry && current.reloadable == reloadable) {
				return current;
			}
			plan = new Plan(server, registry, reloadable, ++generation);
			current = plan;
			try {
				long start = System.nanoTime();
				Snapshot snapshot = collect(server);
				World.LOGGER.info("Collected {} structures and {} dimensions in {}ms", snapshot.structures().size(), snapshot.dimensions().size(),
					(System.nanoTime() - start) / 1_000_000);
				plan.chestTables = snapshot.chestTables();
				lookup = lootLookup(server);
				StructureLookup ready = lookup == null ? null : lookup.now();
				plan.lootLater = lookup != null && ready == null;
				Plan started = plan;
				Thread thread = new Thread(() -> finish(snapshot, ready, started), "EMI World data");
				thread.setDaemon(true);
				thread.start();
			} catch (RuntimeException e) {
				World.LOGGER.error("Could not collect the structures and biomes of the world, the world pages will have no server data", e);
				plan.lootLater = false;
				plan.batches.complete(WorldWire.encode(List.of(), List.of(), WorldWire.MAX_BATCH_BYTES));
			}
		}
		if (plan.lootLater) {
			Plan waiting = plan;
			StructureLookup.Build build = lookup;
			build.whenReady(ready -> lootReady(waiting, build, ready));
		}
		return plan;
	}

	/** The loot module's structure lookup (started now when it is not running), null when it is not available. */
	private static StructureLookup.@Nullable Build lootLookup(MinecraftServer server) {
		try {
			return StructureLookup.current(server);
		} catch (Throwable e) {
			World.LOGGER.warn("The loot module's structure lookup is not available: {}", e.toString());
			return null;
		}
	}

	/** Background part: the chest loot tables if the lookup was already done, then the encoding. */
	private static void finish(Snapshot snapshot, @Nullable StructureLookup lookup, Plan plan) {
		List<StructureRow> structures = snapshot.structures();
		try {
			Map<String, List<String>> tables = lookup == null ? Map.of() : lootTables(lookup, snapshot.chestTables());
			if (!tables.isEmpty()) {
				List<StructureRow> withLoot = new ArrayList<>(structures.size());
				for (StructureRow row : structures) {
					withLoot.add(tables.containsKey(row.id()) ? row.withLootTables(tables.get(row.id())) : row);
				}
				structures = withLoot;
			}
			int withTables = 0;
			for (StructureRow row : structures) {
				if (!row.lootTables().isEmpty()) {
					withTables++;
				}
			}
			World.LOGGER.info("World data {}: {} structures ({} with chest loot tables{}), {} dimensions", plan.generation, structures.size(), withTables,
				plan.lootLater ? ", the tables follow when the structure lookup is done" : "", snapshot.dimensions().size());
			plan.batches.complete(WorldWire.encode(snapshot.dimensions(), structures, WorldWire.MAX_BATCH_BYTES));
		} catch (Throwable e) {
			World.LOGGER.error("Could not build the world data", e);
			plan.batches.complete(WorldWire.encode(snapshot.dimensions(), snapshot.structures(), WorldWire.MAX_BATCH_BYTES));
		}
	}

	/** Server thread, once the structure lookup is done: the chest loot tables for everyone who got the data without. */
	private static void lootReady(Plan plan, StructureLookup.Build build, StructureLookup lookup) {
		if (!isCurrent(plan)) {
			return;
		}
		Map<String, List<String>> tables = lootTables(lookup, plan.chestTables);
		plan.lootBatches = LootLinksWire.encode(tables, WorldWire.MAX_BATCH_BYTES);
		List<UUID> waiting = LOOT_WAITING.take(plan.generation);
		if (waiting.isEmpty()) {
			// nobody got the data yet: everyone gets the tables right after it
			return;
		}
		int sent = 0;
		for (UUID uuid : waiting) {
			ServerPlayerEntity player = plan.server.getPlayerManager().getPlayer(uuid);
			if (player != null && ServerPlayNetworking.canSend(player, StructureLootPayload.TYPE)) {
				sendLoot(player, plan.lootBatches);
				sent++;
			}
		}
		World.LOGGER.info("The chest loot tables of {} structures were done {}ms after the structure lookup started, sent them to {} players",
			tables.size(), build.elapsedMillis(), sent);
	}

	/** Chest loot tables by structure id, from the structure lookup of the loot module (empty if it knows none). */
	private static Map<String, List<String>> lootTables(StructureLookup lookup, List<Identifier> chestTables) {
		Map<String, List<String>> result = new TreeMap<>();
		for (Identifier table : chestTables) {
			for (StructureRef ref : lookup.structuresOf(table)) {
				result.computeIfAbsent(ref.structure().toString(), k -> new ArrayList<>()).add(table.toString());
			}
		}
		return result;
	}

	/** Server thread: structures, structure sets and dimensions of the registries. */
	private static Snapshot collect(MinecraftServer server) {
		DynamicRegistryManager registries = server.getRegistryManager();
		Map<String, Set<String>> dimensionBiomes = collectDimensions(server);
		List<DimensionRow> dimensions = new ArrayList<>();
		dimensionBiomes.forEach((id, biomes) -> dimensions.add(new DimensionRow(id, biomes.stream().sorted().toList())));

		Map<String, SetInfo> sets = collectSets(registries);
		List<StructureRow> structures = new ArrayList<>();
		for (Map.Entry<RegistryKey<Structure>, Structure> entry : registries.get(RegistryKeys.STRUCTURE).getEntrySet()) {
			String id = entry.getKey().getValue().toString();
			try {
				structures.add(structureRow(id, entry.getValue(), sets.get(id), dimensionBiomes));
			} catch (RuntimeException e) {
				World.LOGGER.warn("Not sending structure {} to clients: {}", id, e.toString());
			}
		}
		structures.sort(Comparator.comparing(StructureRow::id));

		List<Identifier> chestTables = new ArrayList<>(server.getReloadableRegistries().getIds(RegistryKeys.LOOT_TABLE).stream()
			.filter(id -> id.getPath().startsWith("chests/")).sorted().toList());
		return new Snapshot(dimensions, structures, chestTables);
	}

	private static StructureRow structureRow(String id, Structure structure, SetInfo set, Map<String, Set<String>> dimensionBiomes) {
		RegistryEntryList<Biome> valid = structure.getValidBiomes();
		Set<String> biomes = new LinkedHashSet<>();
		for (RegistryEntry<Biome> biome : valid) {
			biome.getKey().ifPresent(key -> biomes.add(key.getValue().toString()));
		}
		String tag = valid.getTagKey().map(key -> key.id().toString()).orElse("");
		List<String> dimensions = new ArrayList<>();
		for (Map.Entry<String, Set<String>> dimension : dimensionBiomes.entrySet()) {
			if (intersects(dimension.getValue(), biomes)) {
				dimensions.add(dimension.getKey());
			}
		}
		return new StructureRow(id, structure.getFeatureGenerationStep().asString(), dimensions, biomes.stream().sorted().toList(), tag,
			set == null ? "" : set.set(), set == null ? 0 : set.weight(), set == null ? 0 : set.totalWeight(),
			set == null ? Placement.NONE : set.placement(), List.of());
	}

	private static boolean intersects(Set<String> a, Set<String> b) {
		Set<String> small = a.size() <= b.size() ? a : b;
		Set<String> large = small == a ? b : a;
		for (String s : small) {
			if (large.contains(s)) {
				return true;
			}
		}
		return false;
	}

	/** Structure id to the set it is in (the first set by id when a structure is in several). */
	private static Map<String, SetInfo> collectSets(DynamicRegistryManager registries) {
		Map<String, SetInfo> result = new HashMap<>();
		Map<Identifier, StructureSet> sorted = new TreeMap<>();
		for (Map.Entry<RegistryKey<StructureSet>, StructureSet> entry : registries.get(RegistryKeys.STRUCTURE_SET).getEntrySet()) {
			sorted.put(entry.getKey().getValue(), entry.getValue());
		}
		sorted.forEach((setId, set) -> {
			int total = 0;
			for (StructureSet.WeightedEntry weighted : set.structures()) {
				total += weighted.weight();
			}
			Placement placement = placement(set.placement());
			for (StructureSet.WeightedEntry weighted : set.structures()) {
				int totalWeight = total;
				weighted.structure().getKey().ifPresent(key -> result.putIfAbsent(key.getValue().toString(),
					new SetInfo(setId.toString(), weighted.weight(), totalWeight, placement)));
			}
		});
		return result;
	}

	private static Placement placement(StructurePlacement placement) {
		if (placement instanceof RandomSpreadStructurePlacement spread) {
			return Placement.randomSpread(spread.getSpacing(), spread.getSeparation(), spread.getSpreadType() == SpreadType.TRIANGULAR ? 1 : 0);
		}
		if (placement instanceof ConcentricRingsStructurePlacement rings) {
			return Placement.rings(rings.getDistance(), rings.getSpread(), rings.getCount());
		}
		return new Placement(StructureRow.Kind.OTHER, 0, 0, 0);
	}

	/**
	 * Every dimension with the biomes its chunk generator's biome source can produce, whichever mod added it: the
	 * loaded worlds first, then registered dimensions that have no loaded world. Overworld, nether and end come first.
	 */
	private static Map<String, Set<String>> collectDimensions(MinecraftServer server) {
		Map<String, Set<String>> dimensions = new LinkedHashMap<>();
		for (ServerWorld world : server.getWorlds()) {
			String id = world.getRegistryKey().getValue().toString();
			try {
				dimensions.put(id, biomesOf(world.getChunkManager().getChunkGenerator()));
			} catch (RuntimeException e) {
				World.LOGGER.warn("Not sending the biomes of dimension {} to clients: {}", id, e.toString());
			}
		}
		for (Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : server.getRegistryManager().get(RegistryKeys.DIMENSION).getEntrySet()) {
			String id = entry.getKey().getValue().toString();
			if (dimensions.containsKey(id)) {
				continue;
			}
			try {
				dimensions.put(id, biomesOf(entry.getValue().chunkGenerator()));
			} catch (RuntimeException e) {
				World.LOGGER.warn("Not sending the biomes of dimension {} to clients: {}", id, e.toString());
			}
		}
		List<Map.Entry<String, Set<String>>> sorted = new ArrayList<>(dimensions.entrySet());
		sorted.sort(Comparator.comparingInt(e -> vanillaOrder(e.getKey())));
		Map<String, Set<String>> result = new LinkedHashMap<>();
		sorted.forEach(e -> result.put(e.getKey(), e.getValue()));
		return result;
	}

	private static Set<String> biomesOf(ChunkGenerator generator) {
		Collection<RegistryEntry<Biome>> entries = generator.getBiomeSource().getBiomes();
		Set<String> biomes = new LinkedHashSet<>();
		for (RegistryEntry<Biome> biome : entries) {
			biome.getKey().ifPresent(key -> biomes.add(key.getValue().toString()));
		}
		return biomes;
	}

	private static int vanillaOrder(String id) {
		return switch (id) {
			case "minecraft:overworld" -> 0;
			case "minecraft:the_nether" -> 1;
			case "minecraft:the_end" -> 2;
			default -> 3;
		};
	}
}
