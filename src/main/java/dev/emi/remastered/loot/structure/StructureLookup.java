package dev.emi.remastered.loot.structure;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import com.mojang.datafixers.util.Either;
import dev.emi.remastered.loot.mixin.CombinedEntryAccessor;
import dev.emi.remastered.loot.mixin.JigsawStructureAccessor;
import dev.emi.remastered.loot.mixin.ListPoolElementAccessor;
import dev.emi.remastered.loot.mixin.LootPoolAccessor;
import dev.emi.remastered.loot.mixin.LootTableAccessor;
import dev.emi.remastered.loot.mixin.LootTableEntryAccessor;
import dev.emi.remastered.loot.mixin.SinglePoolElementAccessor;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.CombinedEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registry;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.ListPoolElement;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Finds the structures a chest loot table is found in. For every jigsaw structure the structure templates reachable
 * from its start pool are read, and their chests, vaults and other containers say which loot tables they use. Chests
 * that vanilla places in code (desert pyramid, stronghold, nether fortress, ...) come from a built-in list by structure
 * type ({@link CodePlacedLoot}), so every structure of such a type gets them. Structures of mods with their own
 * structure type that place their chests in code remain unknown and show no structure for their tables.
 * The result per structure also lists the dimensions whose biome source can produce the structure's biomes.
 * <p>
 * Reading every template of every structure can take seconds in a large pack, so the lookup is built on a daemon
 * background thread that starts when the server has started (and again, lazily, after a data pack reload). The
 * server thread only captures what the build reads. {@link #forTable} waits for a running build for at most
 * {@value #WAIT_SECONDS} seconds and shows no structures if it is not done by then.
 */
public final class StructureLookup {
	private static final int MAX_POOLS_PER_STRUCTURE = 600;
	private static final int MAX_TEMPLATES_PER_STRUCTURE = 4000;
	private static final int MAX_TABLE_DEPTH = 8;
	private static final long WAIT_SECONDS = 30;

	private static int generation;
	private static MinecraftServer currentServer;
	private static CompletableFuture<StructureLookup> current;
	private static boolean timeoutLogged;

	private final Map<Identifier, List<Identifier>> structureDimensions = new LinkedHashMap<>();
	private final Map<Identifier, Set<Identifier>> structuresByTable = new HashMap<>();
	private final Map<Identifier, List<StructureRef>> resolved = new HashMap<>();

	/** Everything a build reads, captured on the server thread. Registries are frozen and resources immutable by then. */
	private record Context(Registry<Structure> structures, Registry<StructurePool> pools, ResourceManager resources,
		ReloadableRegistries.Lookup loot, Map<Identifier, Set<RegistryEntry<Biome>>> biomes, List<Identifier> chestTables) {
	}

	/**
	 * Call when the data packs reload or the server stops: a build that is still running is abandoned and never
	 * published, the lookup is built again for the next player that needs it.
	 */
	public static synchronized void invalidate() {
		generation++;
		current = null;
		currentServer = null;
	}

	private static synchronized boolean isStale(int gen) {
		return gen != generation;
	}

	/**
	 * Starts the build on a background thread right after the server started so the first player does not wait for it.
	 * It logs how many chest loot tables could be assigned to a structure; in a development environment also the result
	 * for every chest loot table. Does nothing while {@code loot.chest-structures} is off.
	 */
	public static void prefetch(MinecraftServer server) {
		if (LootSettings.chestStructures) {
			start(server);
		}
	}

	/** The structures of a chest loot table (empty when unknown). Server thread; waits for a running build. */
	public static List<StructureRef> forTable(MinecraftServer server, Identifier table) {
		if (!LootSettings.chestStructures) {
			return List.of();
		}
		CompletableFuture<StructureLookup> future = start(server);
		try {
			return future.get(WAIT_SECONDS, TimeUnit.SECONDS).resolve(table);
		} catch (TimeoutException e) {
			logTimeoutOnce();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot table {}", table, e);
		}
		return List.of();
	}

	private static synchronized void logTimeoutOnce() {
		if (!timeoutLogged) {
			timeoutLogged = true;
			EMILoot.LOGGER.warn("Looking up the structures of chest loot tables took more than {} seconds, chest pages will not show where a chest is found until the next reload", WAIT_SECONDS);
		}
	}

	private static synchronized CompletableFuture<StructureLookup> start(MinecraftServer server) {
		if (current != null && currentServer == server) {
			return current;
		}
		int gen = ++generation;
		timeoutLogged = false;
		CompletableFuture<StructureLookup> future = new CompletableFuture<>();
		try {
			Context context = capture(server);
			Thread thread = new Thread(() -> run(context, gen, future), "EMI Loot structure lookup");
			thread.setDaemon(true);
			thread.start();
		} catch (Throwable e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot tables, chest pages will not show where a chest is found", e);
			future.complete(new StructureLookup());
		}
		current = future;
		currentServer = server;
		return future;
	}

	private static Context capture(MinecraftServer server) {
		Map<Identifier, Set<RegistryEntry<Biome>>> biomes = new LinkedHashMap<>();
		for (ServerWorld world : server.getWorlds()) {
			biomes.put(world.getRegistryKey().getValue(), Set.copyOf(world.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes()));
		}
		ReloadableRegistries.Lookup loot = server.getReloadableRegistries();
		List<Identifier> chestTables = loot.getIds(RegistryKeys.LOOT_TABLE).stream().filter(id -> id.getPath().startsWith("chests/")).sorted().toList();
		return new Context(server.getRegistryManager().get(RegistryKeys.STRUCTURE), server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL),
			server.getResourceManager(), loot, biomes, chestTables);
	}

	private static void run(Context context, int gen, CompletableFuture<StructureLookup> future) {
		StructureLookup lookup = new StructureLookup();
		long start = System.currentTimeMillis();
		try {
			lookup.scan(context, gen);
		} catch (Throwable e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot tables, chest pages will not show where a chest is found", e);
		}
		if (isStale(gen)) {
			// the data changed while this build ran: never hand out its results
			future.complete(new StructureLookup());
			return;
		}
		future.complete(lookup);
		EMILoot.LOGGER.info("Looked up {} structures and the loot tables of their pieces in {}ms", lookup.structureDimensions.size(), System.currentTimeMillis() - start);
		boolean dev = LootSettings.isDevelopment();
		int resolvedTables = 0;
		for (Identifier table : context.chestTables()) {
			List<StructureRef> refs = lookup.resolve(table);
			if (!refs.isEmpty()) {
				resolvedTables++;
			}
			if (dev) {
				EMILoot.LOGGER.info("chest structures {} -> {}", table, refs.stream().map(r -> r.structure() + " " + r.dimensions()).toList());
			}
		}
		EMILoot.LOGGER.info("Chest loot tables with a known structure: {} of {}", resolvedTables, context.chestTables().size());
	}

	private synchronized List<StructureRef> resolve(Identifier table) {
		List<StructureRef> cached = resolved.get(table);
		if (cached != null) {
			return cached;
		}
		Set<Identifier> structures = structuresByTable.getOrDefault(table, Set.of());
		List<StructureRef> refs = new ArrayList<>();
		for (Identifier structure : structures) {
			refs.add(new StructureRef(structure, structureDimensions.getOrDefault(structure, List.of())));
		}
		refs.sort((a, b) -> a.structure().compareTo(b.structure()));
		List<StructureRef> result = List.copyOf(refs);
		resolved.put(table, result);
		return result;
	}

	private void scan(Context context, int gen) {
		Map<Identifier, TemplateInfo> templates = new HashMap<>();
		for (Map.Entry<RegistryKey<Structure>, Structure> entry : context.structures().getEntrySet()) {
			if (isStale(gen)) {
				return;
			}
			Identifier id = entry.getKey().getValue();
			Structure structure = entry.getValue();
			structureDimensions.put(id, dimensionsOf(structure, context.biomes()));
			for (RegistryKey<LootTable> table : CodePlacedLoot.forType(structure.getType())) {
				if (context.chestTables().contains(table.getValue())) {
					structuresByTable.computeIfAbsent(table.getValue(), t -> new LinkedHashSet<>()).add(id);
				}
			}
			if (structure instanceof JigsawStructure jigsaw) {
				try {
					for (Identifier table : lootTablesOf(jigsaw, context.pools(), context.resources(), templates)) {
						structuresByTable.computeIfAbsent(table, t -> new LinkedHashSet<>()).add(id);
					}
				} catch (Throwable e) {
					EMILoot.LOGGER.warn("Could not read the pieces of structure {}: {}", id, e.toString());
				}
			}
		}
		propagateToReferencedTables(context.loot());
	}

	/** A chest whose table points to other tables (trial chamber rewards) is found in the same structures as those tables. */
	private void propagateToReferencedTables(ReloadableRegistries.Lookup loot) {
		Map<Identifier, Set<Identifier>> additions = new HashMap<>();
		for (Map.Entry<Identifier, Set<Identifier>> entry : structuresByTable.entrySet()) {
			Set<Identifier> reached = new HashSet<>();
			collectReferences(loot, entry.getKey(), reached, 0);
			reached.remove(entry.getKey());
			for (Identifier child : reached) {
				additions.computeIfAbsent(child, t -> new LinkedHashSet<>()).addAll(entry.getValue());
			}
		}
		additions.forEach((table, found) -> structuresByTable.computeIfAbsent(table, t -> new LinkedHashSet<>()).addAll(found));
	}

	private static void collectReferences(ReloadableRegistries.Lookup loot, Identifier table, Set<Identifier> reached, int depth) {
		if (depth > MAX_TABLE_DEPTH) {
			return;
		}
		collectReferences(loot, loot.getLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, table)), reached, depth);
	}

	private static void collectReferences(ReloadableRegistries.Lookup loot, LootTable lootTable, Set<Identifier> reached, int depth) {
		for (LootPool pool : ((LootTableAccessor) lootTable).emi_loot$getPools()) {
			for (LootPoolEntry entry : ((LootPoolAccessor) pool).emi_loot$getEntries()) {
				collectReferences(loot, entry, reached, depth);
			}
		}
	}

	private static void collectReferences(ReloadableRegistries.Lookup loot, LootPoolEntry entry, Set<Identifier> reached, int depth) {
		if (entry instanceof LootTableEntry) {
			Either<RegistryKey<LootTable>, LootTable> value = ((LootTableEntryAccessor) entry).emi_loot$getValue();
			value.ifLeft(key -> {
				if (reached.add(key.getValue())) {
					collectReferences(loot, key.getValue(), reached, depth + 1);
				}
			});
			value.ifRight(inline -> collectReferences(loot, inline, reached, depth + 1));
		} else if (entry instanceof CombinedEntry) {
			for (LootPoolEntry child : ((CombinedEntryAccessor) entry).emi_loot$getChildren()) {
				collectReferences(loot, child, reached, depth);
			}
		}
	}

	private static List<Identifier> dimensionsOf(Structure structure, Map<Identifier, Set<RegistryEntry<Biome>>> biomes) {
		RegistryEntryList<Biome> valid = structure.getValidBiomes();
		List<Identifier> dimensions = new ArrayList<>();
		for (Map.Entry<Identifier, Set<RegistryEntry<Biome>>> world : biomes.entrySet()) {
			for (RegistryEntry<Biome> biome : world.getValue()) {
				if (valid.contains(biome)) {
					dimensions.add(world.getKey());
					break;
				}
			}
		}
		return dimensions;
	}

	private static Set<Identifier> lootTablesOf(JigsawStructure structure, Registry<StructurePool> pools, ResourceManager resources, Map<Identifier, TemplateInfo> templates) {
		Set<Identifier> tables = new LinkedHashSet<>();
		Set<Identifier> visitedPools = new HashSet<>();
		Set<Identifier> visitedTemplates = new HashSet<>();
		Deque<Identifier> queue = new ArrayDeque<>();
		((JigsawStructureAccessor) (Object) structure).emi_loot$getStartPool().getKey().ifPresent(key -> queue.add(key.getValue()));
		Random random = Random.create(0);
		while (!queue.isEmpty() && visitedPools.size() < MAX_POOLS_PER_STRUCTURE && visitedTemplates.size() < MAX_TEMPLATES_PER_STRUCTURE) {
			Identifier poolId = queue.poll();
			if (!visitedPools.add(poolId)) {
				continue;
			}
			Optional<StructurePool> pool = pools.getOrEmpty(poolId);
			if (pool.isEmpty()) {
				continue;
			}
			pool.get().getFallback().getKey().ifPresent(key -> queue.add(key.getValue()));
			Set<Identifier> poolTemplates = new LinkedHashSet<>();
			for (StructurePoolElement element : pool.get().getElementIndicesInRandomOrder(random)) {
				collectTemplates(element, poolTemplates);
			}
			for (Identifier templateId : poolTemplates) {
				if (!visitedTemplates.add(templateId)) {
					continue;
				}
				TemplateInfo info = templates.computeIfAbsent(templateId, id -> TemplateInfo.read(resources, id));
				tables.addAll(info.lootTables);
				queue.addAll(info.pools);
			}
		}
		return tables;
	}

	private static void collectTemplates(StructurePoolElement element, Set<Identifier> out) {
		if (element instanceof SinglePoolElement single) {
			((SinglePoolElementAccessor) single).emi_loot$getLocation().left().ifPresent(out::add);
		} else if (element instanceof ListPoolElement list) {
			for (StructurePoolElement nested : ((ListPoolElementAccessor) list).emi_loot$getElements()) {
				collectTemplates(nested, out);
			}
		}
	}

	/** The loot tables used by the blocks of one structure template and the pools its jigsaw blocks lead to. */
	static final class TemplateInfo {
		static final TemplateInfo EMPTY = new TemplateInfo(Set.of(), Set.of());

		final Set<Identifier> lootTables;
		final Set<Identifier> pools;

		TemplateInfo(Set<Identifier> lootTables, Set<Identifier> pools) {
			this.lootTables = lootTables;
			this.pools = pools;
		}

		static TemplateInfo read(ResourceManager resources, Identifier templateId) {
			Identifier file = Identifier.of(templateId.getNamespace(), "structure/" + templateId.getPath() + ".nbt");
			Optional<Resource> resource = resources.getResource(file);
			if (resource.isEmpty()) {
				return EMPTY;
			}
			try (InputStream in = resource.get().getInputStream()) {
				return of(NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes()));
			} catch (Exception e) {
				return EMPTY;
			}
		}

		static TemplateInfo of(NbtCompound root) {
			Set<Identifier> tables = new LinkedHashSet<>();
			Set<Identifier> pools = new LinkedHashSet<>();
			NbtList blocks = root.getList("blocks", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < blocks.size(); i++) {
				NbtCompound block = blocks.getCompound(i);
				if (block.contains("nbt", NbtElement.COMPOUND_TYPE)) {
					scan(block.getCompound("nbt"), tables, pools);
				}
			}
			NbtList entities = root.getList("entities", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < entities.size(); i++) {
				NbtCompound entity = entities.getCompound(i);
				if (entity.contains("nbt", NbtElement.COMPOUND_TYPE)) {
					scan(entity.getCompound("nbt"), tables, pools);
				}
			}
			return tables.isEmpty() && pools.isEmpty() ? EMPTY : new TemplateInfo(tables, pools);
		}

		/** Block entity data keeps the loot table in "LootTable" (containers) or "loot_table" (vaults), jigsaws name their "pool". */
		private static void scan(NbtCompound nbt, Set<Identifier> tables, Set<Identifier> pools) {
			for (String key : nbt.getKeys()) {
				NbtElement value = nbt.get(key);
				if (value instanceof NbtCompound compound) {
					scan(compound, tables, pools);
				} else if (value instanceof NbtList list) {
					for (NbtElement element : list) {
						if (element instanceof NbtCompound compound) {
							scan(compound, tables, pools);
						}
					}
				} else if (value != null && value.getType() == NbtElement.STRING_TYPE) {
					String text = value.asString();
					if (key.equals("LootTable") || key.equals("loot_table")) {
						Identifier id = Identifier.tryParse(text);
						if (id != null) {
							tables.add(id);
						}
					} else if (key.equals("pool") && nbt.contains("target", NbtElement.STRING_TYPE)) {
						Identifier id = Identifier.tryParse(text);
						if (id != null) {
							pools.add(id);
						}
					}
				}
			}
		}
	}
}
