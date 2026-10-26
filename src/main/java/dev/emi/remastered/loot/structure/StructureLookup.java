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
import java.util.concurrent.CompletionException;
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
 * the template reads over a few worker threads of its own ({@link StructureWalks}). The
 * server thread only captures what the build reads and never waits for it: {@link #ready} and
 * {@link #forTableIfReady} answer with what is there now, {@link Build#whenReady} runs code on the server thread once
 * the build of the current data is done. Every build has a generation; a data pack reload or a server stop
 * ({@link #invalidate}) makes it stale, and the results and callbacks of a stale build are dropped.
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
			Thread thread = new Thread(() -> run(context, gen, future), "EMI Loot structure lookup");
			thread.setDaemon(true);
			thread.start();
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

	private static void run(Context context, int gen, CompletableFuture<StructureLookup> future) {
		StructureLookup lookup = new StructureLookup();
		long start = System.currentTimeMillis();
		try {
			devDelay();
			lookup.scan(context, gen);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot tables, chest pages will not show where a chest is found", e);
		}
		if (isStale(gen)) {
			// the data changed while this build ran: never hand out its results
			future.complete(EMPTY);
			return;
		}
		EMILoot.LOGGER.info("Looked up {} structures and the loot tables of their pieces in {}ms", lookup.structureDimensions.size(), System.currentTimeMillis() - start);
		future.complete(lookup);
		boolean dev = LootSettings.isDevelopment();
		int resolvedTables = 0;
		for (Identifier table : context.chestTables()) {
			List<StructureRef> refs = lookup.structuresOf(table);
			if (!refs.isEmpty()) {
				resolvedTables++;
			}
			if (dev) {
				EMILoot.LOGGER.info("chest structures {} -> {}", table, refs.stream().map(r -> r.structure() + " " + r.dimensions()).toList());
			}
		}
		EMILoot.LOGGER.info("Chest loot tables with a known structure: {} of {}", resolvedTables, context.chestTables().size());
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
	 * Reading and decompressing the structure templates is nearly all of the build's time (several seconds in a large
	 * pack), so the walks of the jigsaw structures run on {@link StructureWalks} workers, each template read once
	 * whichever walk needs it first. This thread merges the results in registry order, as if the structures had been
	 * walked one after another: the same structures per table in the same order, the same warnings.
	 */
	private void scan(Context context, int gen) {
		// TEMP-XPAR timing
		long t0 = System.nanoTime();
		T_GETRES.set(0); T_NBT.set(0); T_OF.set(0); N_READ.set(0);
		List<Map.Entry<RegistryKey<Structure>, Structure>> entries = new ArrayList<>(context.structures().getEntrySet());
		try (StructureWalks walks = new StructureWalks(StructureWalks.defaultThreads(), () -> isStale(gen))) {
			StructureWalks.OnceCache<Identifier, TemplateInfo> templates = new StructureWalks.OnceCache<>(id -> TemplateInfo.read(context.resources(), id));
			List<CompletableFuture<Set<Identifier>>> walked = new ArrayList<>(entries.size());
			for (Map.Entry<RegistryKey<Structure>, Structure> entry : entries) {
				walked.add(entry.getValue() instanceof JigsawStructure jigsaw
					? walks.submit(() -> lootTablesOf(jigsaw, context.pools(), templates::get, walks::aborted))
					: null);
			}
			for (int i = 0; i < entries.size(); i++) {
				if (i == entries.size() - 1) LAST_IDS = new ArrayList<>(templates.keys()); // TEMP-XPAR (approx: last walk may still run)
				if (isStale(gen)) {
					// TEMP-XPAR
					long cancelled = walked.stream().filter(f -> f != null && f.isCancelled()).count();
					EMILoot.LOGGER.info("TEMP-XPAR stale at structure {} of {} after {}ms", i, entries.size(), (System.nanoTime() - t0) / 1_000_000);
					long staleAt = System.nanoTime();
					Thread watcher = new Thread(() -> {
						for (int k = 0; k < 50; k++) {
							long alive = Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().startsWith(StructureWalks.THREAD_NAME)).count();
							if (alive == 0) {
								EMILoot.LOGGER.info("TEMP-XPAR all workers ended {}ms after the stale check", (System.nanoTime() - staleAt) / 1_000_000);
								return;
							}
							try { Thread.sleep(20); } catch (InterruptedException e) { return; }
						}
						EMILoot.LOGGER.info("TEMP-XPAR workers still alive 1s after the stale check");
					});
					watcher.setDaemon(true);
					watcher.start();
					return;
				}
				Identifier id = entries.get(i).getKey().getValue();
				Structure structure = entries.get(i).getValue();
				structureDimensions.put(id, dimensionsOf(structure, context.biomes()));
				for (RegistryKey<LootTable> table : CodePlacedLoot.forType(structure.getType())) {
					if (context.chestTables().contains(table.getValue())) {
						structuresByTable.computeIfAbsent(table.getValue(), t -> new LinkedHashSet<>()).add(id);
					}
				}
				CompletableFuture<Set<Identifier>> tables = walked.get(i);
				if (tables != null) {
					try {
						for (Identifier table : tables.join()) {
							structuresByTable.computeIfAbsent(table, t -> new LinkedHashSet<>()).add(id);
						}
					} catch (CompletionException e) {
						EMILoot.LOGGER.warn("Could not read the pieces of structure {}: {}", id, e.getCause().toString());
					}
				}
			}
		}
		propagateToReferencedTables(context.loot());
		// TEMP-XPAR
		EMILoot.LOGGER.info("TEMP-XPAR scan {}ms (threads {}), reads {}: getResource {}ms, readCompressed {}ms, TemplateInfo.of {}ms (summed over threads)",
			(System.nanoTime() - t0) / 1_000_000, StructureWalks.defaultThreads(), N_READ.get(), T_GETRES.get() / 1_000_000, T_NBT.get() / 1_000_000, T_OF.get() / 1_000_000);
		TempDump.dump(this, context.chestTables(), structureDimensions, structuresByTable);
		if (Boolean.getBoolean("emi.xpar.bench") && !BENCHING) {
			BENCHING = true;
			TempDump.phases(context.resources(), LAST_IDS);
			String[][] configs = {{"seq", "true", "1", "5"}, {"seq", "false", "1", "5"}, {"par", "true", "4", "5"}, {"par", "false", "4", "5"}, {"par", "false", "4", "4"}, {"par", "false", "2", "5"}, {"par", "false", "6", "5"}};
			for (int round = 0; round < 3; round++) {
				StringBuilder sb = new StringBuilder("TEMP-XPAR bench round " + round + ":");
				for (String[] cfg : configs) {
					long b0 = System.nanoTime();
					System.setProperty("emi.xpar.fullread", cfg[1]);
					if (cfg[0].equals("seq")) {
						new StructureLookup().scanSequential(context, gen);
					} else {
						System.setProperty("emi.xpar.threads", cfg[2]);
						System.setProperty("emi.xpar.prio", cfg[3]);
						new StructureLookup().scan(context, gen);
					}
					sb.append(" [").append(String.join("/", cfg)).append("]=").append((System.nanoTime() - b0) / 1_000_000).append("ms");
				}
				System.clearProperty("emi.xpar.threads");
				System.clearProperty("emi.xpar.prio");
				System.clearProperty("emi.xpar.fullread");
				EMILoot.LOGGER.info(sb.toString());
			}
		}
		if (Boolean.getBoolean("emi.xpar.verify") && !BENCHING) {
			StructureLookup sequential = new StructureLookup();
			long s0 = System.nanoTime();
			System.setProperty("emi.xpar.fullread", "true");
			sequential.scanSequential(context, gen);
			System.clearProperty("emi.xpar.fullread");
			long s1 = System.nanoTime();
			boolean sameDims = List.copyOf(structureDimensions.entrySet()).equals(List.copyOf(sequential.structureDimensions.entrySet()));
			boolean sameTables = structuresByTable.keySet().equals(sequential.structuresByTable.keySet());
			int orderDiffs = 0;
			for (Identifier k : structuresByTable.keySet()) {
				if (!List.copyOf(structuresByTable.get(k)).equals(List.copyOf(sequential.structuresByTable.getOrDefault(k, Set.of())))) {
					orderDiffs++;
				}
			}
			EMILoot.LOGGER.info("TEMP-XPAR verify against sequential (in {}ms, same JVM): dimensions identical+ordered {}, same table keys {}, tables whose ordered structure list differs {}",
				(s1 - s0) / 1_000_000, sameDims, sameTables, orderDiffs);
		}
	}

	static boolean BENCHING;
	static List<Identifier> LAST_IDS = List.of();
	// TEMP-XPAR: the old sequential scan, for the in-process comparison only
	static final java.util.concurrent.atomic.AtomicLong T_GETRES = new java.util.concurrent.atomic.AtomicLong(), T_NBT = new java.util.concurrent.atomic.AtomicLong(), T_OF = new java.util.concurrent.atomic.AtomicLong(), N_READ = new java.util.concurrent.atomic.AtomicLong();
	private void scanSequential(Context context, int gen) {
		long tDim = 0, tCode = 0, tWalk = 0, t0 = System.nanoTime();
		Map<Identifier, TemplateInfo> templates = new HashMap<>();
		for (Map.Entry<RegistryKey<Structure>, Structure> entry : context.structures().getEntrySet()) {
			if (isStale(gen)) {
				return;
			}
			Identifier id = entry.getKey().getValue();
			Structure structure = entry.getValue();
			long a = System.nanoTime();
			structureDimensions.put(id, dimensionsOf(structure, context.biomes()));
			long b = System.nanoTime();
			for (RegistryKey<LootTable> table : CodePlacedLoot.forType(structure.getType())) {
				if (context.chestTables().contains(table.getValue())) {
					structuresByTable.computeIfAbsent(table.getValue(), t -> new LinkedHashSet<>()).add(id);
				}
			}
			long c = System.nanoTime();
			tDim += b - a; tCode += c - b;
			if (structure instanceof JigsawStructure jigsaw) try {
				try {
					for (Identifier table : lootTablesOf(jigsaw, context.pools(), tid -> templates.computeIfAbsent(tid, x -> TemplateInfo.read(context.resources(), x)), () -> false)) {
						structuresByTable.computeIfAbsent(table, t -> new LinkedHashSet<>()).add(id);
					}
				} catch (Throwable e) {
					EMILoot.LOGGER.warn("Could not read the pieces of structure {}: {}", id, e.toString());
				}
			} finally { tWalk += System.nanoTime() - c; }
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

	/**
	 * The loot tables of one jigsaw structure's templates, in the order the walk meets them. Runs on a lookup worker:
	 * it only reads frozen registries, creates its own random (a {@link Random#create} random must stay on one thread)
	 * and gets its templates from the shared {@code templates} cache. Stops early (with a partial result that is
	 * never published) once {@code aborted} says the build is stale or ended.
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

		static TemplateInfo read(ResourceManager resources, Identifier templateId) {
			Identifier file = Identifier.of(templateId.getNamespace(), "structure/" + templateId.getPath() + ".nbt");
			long a = System.nanoTime();
			N_READ.incrementAndGet();
			Optional<Resource> resource = resources.getResource(file);
			T_GETRES.addAndGet(System.nanoTime() - a);
			if (resource.isEmpty()) {
				return EMPTY;
			}
			try (InputStream in = resource.get().getInputStream()) {
				long b0 = System.nanoTime();
				InputStream src = in;
				if (Boolean.getBoolean("emi.xpar.preread")) {
					src = new java.io.ByteArrayInputStream(in.readAllBytes());
				}
				long b = System.nanoTime();
				TempDump.RAW.addAndGet(b - b0);
				List<NbtCompound> parts = Boolean.getBoolean("emi.xpar.fullread") ? TemplateNbt.containerNbt(NbtIo.readCompressed(src, NbtSizeTracker.ofUnlimitedBytes())) : TemplateNbt.read(src);
				long c = System.nanoTime();
				T_NBT.addAndGet(c - b);
				TemplateInfo r = of(parts);
				T_OF.addAndGet(System.nanoTime() - c);
				return r;
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
