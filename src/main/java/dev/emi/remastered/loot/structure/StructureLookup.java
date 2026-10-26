package dev.emi.remastered.loot.structure;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.server.ChestLootTableSender;
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
import net.minecraft.nbt.NbtList;
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
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Finds the structures a chest loot table is found in. For every jigsaw structure the structure templates reachable
 * from its start pool are read, and their chests, vaults and other containers say which loot tables they use. Chests
 * that vanilla places in code (desert pyramid, stronghold, nether fortress, ...) come from a built-in list by structure
 * type ({@link CodePlacedLoot}), so every structure of such a type gets them. Structures of mods with their own
 * structure type that place their chests in code remain unknown and show no structure for their tables.
 * The result per structure also lists the dimensions whose biome source can produce the structure's biomes.
 * <p>
 * Reading every template of every structure can take many seconds in a large pack, so the lookup is built on a daemon
 * background thread that starts when the server has started (and again, lazily, after a data pack reload); it spreads
 * the template reads over a few worker threads of its own ({@link StructureWalks}). The server thread only captures
 * what the build reads and never waits for it: {@link #ready} and {@link #forTableIfReady} answer with what is there
 * now, {@link Build#whenReady} runs code on the server thread once the build of the current data is done. Every build
 * has a generation; a data pack reload or a server stop ({@link #invalidate}) makes it stale, and the results and
 * callbacks of a stale build are dropped. A build also stops as soon as its server begins to stop.
 */
public final class StructureLookup {
	private static final int MAX_POOLS_PER_STRUCTURE = 600;
	private static final int MAX_TEMPLATES_PER_STRUCTURE = 4000;
	private static final int MAX_TABLE_DEPTH = 8;
	/** Development environment only: delays every build by this many milliseconds, to test late results. */
	private static final String DEV_DELAY_PROPERTY = "emi.devtour.lookupDelayMs";
	private static final StructureLookup EMPTY = new StructureLookup();

	/** Written under the class lock; volatile so that {@link #isStale} (asked by the lookup's workers) needs no lock. */
	private static volatile int generation;
	private static Build current;

	private final Map<Identifier, List<Identifier>> structureDimensions = new LinkedHashMap<>();
	private final Map<Identifier, Set<Identifier>> structuresByTable = new HashMap<>();
	private final Map<Identifier, List<StructureRef>> resolved = new HashMap<>();

	/** Everything a build reads, captured on the server thread. Registries are frozen and resources immutable by then. */
	private record Context(Registry<Structure> structures, Registry<StructurePool> pools, ResourceManager resources,
		ReloadableRegistries.Lookup loot, Map<Identifier, Set<RegistryEntry<Biome>>> biomes, List<Identifier> chestTables) {
	}

	/** One build of the lookup for one server and one data generation. */
	public static final class Build {
		private final int generation;
		private final MinecraftServer server;
		private final CompletableFuture<StructureLookup> future;
		private final long startNanos = System.nanoTime();

		private Build(int generation, MinecraftServer server, CompletableFuture<StructureLookup> future) {
			this.generation = generation;
			this.server = server;
			this.future = future;
		}

		public int generation() {
			return generation;
		}

		/** The finished lookup, or null while the build is still running. Never blocks. */
		public @Nullable StructureLookup now() {
			return future.getNow(null);
		}

		/** Milliseconds since the build started. */
		public long elapsedMillis() {
			return (System.nanoTime() - startNanos) / 1_000_000;
		}

		/** Whether the data changed (reload, server stop) since this build started. */
		public boolean isStale() {
			return StructureLookup.isStale(generation);
		}

		/**
		 * Runs {@code action} on the server thread once the build is done (right away when it is done and this is the
		 * server thread). Dropped when the build became stale or the server stopped before.
		 */
		public void whenReady(Consumer<StructureLookup> action) {
			future.thenAccept(lookup -> {
				if (server.isStopped()) {
					// MinecraftServer.execute would run the task right here, on this thread, once the server stopped
					return;
				}
				server.execute(() -> {
					if (!server.isRunning() || !server.isOnThread() || isStale()) {
						return;
					}
					try {
						action.accept(lookup);
					} catch (Throwable e) {
						EMILoot.LOGGER.error("Could not use the structures of chest loot tables", e);
					}
				});
			});
		}
	}

	/**
	 * Call when the data packs reload or the server stops: a build that is still running is abandoned and never
	 * published, the lookup is built again for the next player that needs it.
	 */
	public static synchronized void invalidate() {
		generation++;
		current = null;
	}

	/** Lock free: the lookup's workers ask often, and the server thread takes the class lock in {@link #current}. */
	private static boolean isStale(int gen) {
		return gen != generation;
	}

	/**
	 * Starts the build on a background thread right after the server started so the first player does not wait for it.
	 * It logs how many chest loot tables could be assigned to a structure; in a development environment also the result
	 * for every chest loot table. Does nothing while {@code loot.chest-structures} is off.
	 */
	public static void prefetch(MinecraftServer server) {
		if (LootSettings.chestStructures) {
			current(server);
		}
	}

	/**
	 * The build for the current data of the server, started when there is none. While {@code loot.chest-structures}
	 * is off it is an empty lookup that is done at once. Never blocks.
	 */
	public static synchronized Build current(MinecraftServer server) {
		if (!LootSettings.chestStructures) {
			return new Build(generation, server, CompletableFuture.completedFuture(EMPTY));
		}
		if (current != null && current.server == server) {
			return current;
		}
		int gen = ++generation;
		CompletableFuture<StructureLookup> future = new CompletableFuture<>();
		Build build = new Build(gen, server, future);
		try {
			Context context = capture(server);
			// a server that stops closes its resource packs right after, invalidate only comes once it stopped
			BooleanSupplier abandoned = () -> isStale(gen) || !server.isRunning();
			Thread thread = new Thread(() -> run(context, abandoned, future), "EMI Loot structure lookup");
			thread.setDaemon(true);
			thread.start();
			// on the server thread, so the chest senders are not read while a reload replaces them
			build.whenReady(lookup -> logSummary(lookup, context.chestTables()));
		} catch (Throwable e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot tables, chest pages will not show where a chest is found", e);
			future.complete(EMPTY);
		}
		current = build;
		return build;
	}

	/** The finished lookup of the current data, or null while it is built (the build is started if needed). */
	public static @Nullable StructureLookup ready(MinecraftServer server) {
		return current(server).now();
	}

	/** The structures of a chest loot table if the lookup is done, else (and when unknown) empty. Never blocks. */
	public static List<StructureRef> forTableIfReady(MinecraftServer server, Identifier table) {
		StructureLookup lookup = ready(server);
		return lookup == null ? List.of() : lookup.structuresOf(table);
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

	private static void run(Context context, BooleanSupplier abandoned, CompletableFuture<StructureLookup> future) {
		StructureLookup lookup = new StructureLookup();
		long start = System.currentTimeMillis();
		try {
			devDelay();
			lookup.scan(context, abandoned);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot tables, chest pages will not show where a chest is found", e);
		}
		if (abandoned.getAsBoolean()) {
			// the data changed or the server stops while this build ran: never hand out its results
			future.complete(EMPTY);
			return;
		}
		EMILoot.LOGGER.info("Looked up {} structures and the loot tables of their pieces in {}ms", lookup.structureDimensions.size(), System.currentTimeMillis() - start);
		future.complete(lookup);
	}

	/**
	 * Server thread, once per build: the chest loot pages that ChestStructureSync sends with a structure. A lookup
	 * that failed to build (empty) is not summarised; a stale build never gets here.
	 */
	private static void logSummary(StructureLookup lookup, List<Identifier> chestTablesByPath) {
		if (lookup == EMPTY) {
			return;
		}
		List<Identifier> pageTables = LootTableParser.chestSenders().stream().map(ChestLootTableSender::getTableId).toList();
		if (LootSettings.isDevelopment()) {
			for (Identifier table : pageTables) {
				EMILoot.LOGGER.info("chest structures {} -> {}", table, lookup.structuresOf(table).stream().map(r -> r.structure() + " " + r.dimensions()).toList());
			}
		}
		EMILoot.LOGGER.info(ChestSummary.of(pageTables, chestTablesByPath, lookup::structuresOf).describe());
	}

	private static void devDelay() throws InterruptedException {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}
		long delay = Long.getLong(DEV_DELAY_PROPERTY, 0L);
		if (delay > 0) {
			EMILoot.LOGGER.info("Development: delaying the structure lookup by {}ms ({})", delay, DEV_DELAY_PROPERTY);
			Thread.sleep(delay);
		}
	}

	/** The structures of a chest loot table, sorted by id; empty when unknown. Any thread. */
	public synchronized List<StructureRef> structuresOf(Identifier table) {
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

	/**
	 * Reading and decompressing the structure templates is nearly all of the build's time (about ten seconds in a large
	 * pack), so the walks of the jigsaw structures run on {@link StructureWalks} workers, each template read once
	 * whichever walk needs it first ({@link StructureWalks.OnceCache}). This thread merges the results in the order of
	 * the registry's entries, as if the structures had been walked one after another: the same structures per table in
	 * the same order, the same warnings. Leaves the lookup incomplete once the build is abandoned.
	 */
	private void scan(Context context, BooleanSupplier abandoned) {
		List<Map.Entry<RegistryKey<Structure>, Structure>> entries = new ArrayList<>(context.structures().getEntrySet());
		boolean complete;
		try (StructureWalks walks = new StructureWalks(StructureWalks.defaultThreads(), abandoned)) {
			StructureWalks.OnceCache<Identifier, TemplateInfo> templates = new StructureWalks.OnceCache<>(id -> TemplateInfo.read(context.resources(), id));
			complete = walks.<Map.Entry<RegistryKey<Structure>, Structure>, Set<Identifier>>walkInOrder(entries,
				entry -> entry.getValue() instanceof JigsawStructure jigsaw ? () -> lootTablesOf(jigsaw, context.pools(), templates::get, walks::aborted) : null,
				(entry, tables, failure) -> {
					Identifier id = entry.getKey().getValue();
					Structure structure = entry.getValue();
					structureDimensions.put(id, dimensionsOf(structure, context.biomes()));
					for (RegistryKey<LootTable> table : CodePlacedLoot.forType(structure.getType())) {
						if (context.chestTables().contains(table.getValue())) {
							structuresByTable.computeIfAbsent(table.getValue(), t -> new LinkedHashSet<>()).add(id);
						}
					}
					if (failure != null) {
						EMILoot.LOGGER.warn("Could not read the pieces of structure {}: {}", id, failure.toString());
					} else if (tables != null) {
						for (Identifier table : tables) {
							structuresByTable.computeIfAbsent(table, t -> new LinkedHashSet<>()).add(id);
						}
					}
				});
		}
		if (complete) {
			propagateToReferencedTables(context.loot());
		}
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

	/**
	 * The loot tables of one jigsaw structure's templates, in the order the walk meets them. Runs on a lookup worker:
	 * it only reads frozen registries, creates its own random (a {@link Random#create} random must stay on one thread)
	 * and gets its templates from the shared {@code templates} cache. Stops early (with a partial result that is
	 * never published) once {@code aborted} says the build was abandoned or ended.
	 */
	private static Set<Identifier> lootTablesOf(JigsawStructure structure, Registry<StructurePool> pools, Function<Identifier, TemplateInfo> templates, BooleanSupplier aborted) {
		Set<Identifier> tables = new LinkedHashSet<>();
		Set<Identifier> visitedPools = new HashSet<>();
		Set<Identifier> visitedTemplates = new HashSet<>();
		Deque<Identifier> queue = new ArrayDeque<>();
		((JigsawStructureAccessor) (Object) structure).emi_loot$getStartPool().getKey().ifPresent(key -> queue.add(key.getValue()));
		Random random = Random.create(0);
		while (!queue.isEmpty() && visitedPools.size() < MAX_POOLS_PER_STRUCTURE && visitedTemplates.size() < MAX_TEMPLATES_PER_STRUCTURE) {
			if (aborted.getAsBoolean()) {
				return tables;
			}
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
				TemplateInfo info = templates.apply(templateId);
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

		/**
		 * Runs on the lookup's workers, several at a time. It only calls {@link ResourceManager#getResource} and
		 * {@link Resource#getInputStream}, which are safe from several threads: the resource manager and its pack lists
		 * do not change after the reload that built them (zip packs are opened by that reload too, when it lists their
		 * namespaces), and every call opens a stream of its own on a {@link java.util.zip.ZipFile}, a zip file system or
		 * a directory, all safe for concurrent reads. The game relies on the same: its worldgen threads load structure
		 * templates through this resource manager at the same time. The file is read with {@link TemplateNbt}, which
		 * finds what the full {@code NbtIo.readCompressed} finds in a fraction of the time. As before the lookup was
		 * parallel, a file that cannot be read has no loot tables, while an exception from finding it fails the walk.
		 */
		static TemplateInfo read(ResourceManager resources, Identifier templateId) {
			Identifier file = Identifier.of(templateId.getNamespace(), "structure/" + templateId.getPath() + ".nbt");
			Optional<Resource> resource = resources.getResource(file);
			if (resource.isEmpty()) {
				return EMPTY;
			}
			try (InputStream in = resource.get().getInputStream()) {
				return of(TemplateNbt.read(in));
			} catch (Exception e) {
				return EMPTY;
			}
		}

		/** What a template read in full holds, for tests: the same as {@link #read} finds in its file. */
		static TemplateInfo of(NbtCompound root) {
			return of(TemplateNbt.containerNbt(root));
		}

		/** The loot tables and pools in the block entity and entity data of a template, in the order of the template. */
		static TemplateInfo of(List<NbtCompound> parts) {
			Set<Identifier> tables = new LinkedHashSet<>();
			Set<Identifier> pools = new LinkedHashSet<>();
			for (NbtCompound nbt : parts) {
				scan(nbt, tables, pools);
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
