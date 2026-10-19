package dev.emi.remastered.loot.structure;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import dev.emi.remastered.loot.mixin.JigsawStructureAccessor;
import dev.emi.remastered.loot.mixin.ListPoolElementAccessor;
import dev.emi.remastered.loot.mixin.SinglePoolElementAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registry;
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
import net.minecraft.world.biome.source.BiomeSource;
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

/**
 * Finds the structures a chest loot table is found in. Two sources are combined:
 * <ol>
 * <li>Jigsaw structures: the structure templates reachable from the start pool are read and their chests (and vaults)
 * say which loot tables they use.</li>
 * <li>A name based match (see {@link StructureMatcher}) for everything the first step did not find.</li>
 * </ol>
 * The result per structure also lists the dimensions whose biome source can produce the structure's biomes.
 * Computed once per data reload, lazily when the first player needs it.
 */
public final class StructureLookup {
	private static final int MAX_POOLS_PER_STRUCTURE = 600;
	private static final int MAX_TEMPLATES_PER_STRUCTURE = 4000;

	private static StructureLookup current;
	private static MinecraftServer currentServer;

	private final Map<Identifier, List<Identifier>> structureDimensions = new LinkedHashMap<>();
	private final Map<Identifier, Set<Identifier>> structuresByTable = new HashMap<>();
	private final List<String> structureNames = new ArrayList<>();
	private final Map<Identifier, List<StructureRef>> resolved = new HashMap<>();

	/** Call when the data packs reload: the lookup is rebuilt for the next player that needs it. */
	public static synchronized void invalidate() {
		current = null;
		currentServer = null;
	}

	/**
	 * Builds the lookup right after the server started so the first player does not wait for it. In a development
	 * environment the result for every chest loot table is logged.
	 */
	public static void prefetch(MinecraftServer server) {
		forTable(server, Identifier.of("minecraft", "chests/spawn_bonus_chest"));
		if (!LootSettings.isDevelopment()) {
			return;
		}
		for (Identifier table : server.getReloadableRegistries().getIds(RegistryKeys.LOOT_TABLE).stream().sorted().toList()) {
			if (table.getPath().startsWith("chests/")) {
				List<StructureRef> refs = forTable(server, table);
				EMILoot.LOGGER.info("chest structures {} -> {}", table, refs.stream().map(r -> r.structure() + r.dimensions()).toList());
			}
		}
	}

	public static synchronized List<StructureRef> forTable(MinecraftServer server, Identifier table) {
		StructureLookup lookup = current;
		if (lookup == null || currentServer != server) {
			lookup = build(server);
			current = lookup;
			currentServer = server;
		}
		return lookup.resolve(table);
	}

	private static StructureLookup build(MinecraftServer server) {
		StructureLookup lookup = new StructureLookup();
		long start = System.currentTimeMillis();
		try {
			lookup.scan(server);
			EMILoot.LOGGER.info("Looked up {} structures and the loot tables of their pieces in {}ms", lookup.structureDimensions.size(), System.currentTimeMillis() - start);
		} catch (Throwable e) {
			EMILoot.LOGGER.error("Could not look up the structures of chest loot tables, chest pages will not show where a chest is found", e);
		}
		return lookup;
	}

	private List<StructureRef> resolve(Identifier table) {
		List<StructureRef> cached = resolved.get(table);
		if (cached != null) {
			return cached;
		}
		Set<Identifier> structures = structuresByTable.get(table);
		if (structures == null || structures.isEmpty()) {
			structures = new LinkedHashSet<>();
			for (String id : StructureMatcher.match(table.toString(), structureNames)) {
				structures.add(Identifier.of(id));
			}
		}
		List<StructureRef> refs = new ArrayList<>();
		for (Identifier structure : structures) {
			refs.add(new StructureRef(structure, structureDimensions.getOrDefault(structure, List.of())));
		}
		refs.sort((a, b) -> a.structure().compareTo(b.structure()));
		List<StructureRef> result = List.copyOf(refs);
		resolved.put(table, result);
		return result;
	}

	private void scan(MinecraftServer server) {
		Registry<Structure> structures = server.getRegistryManager().get(RegistryKeys.STRUCTURE);
		Registry<StructurePool> pools = server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
		ResourceManager resources = server.getResourceManager();
		Map<Identifier, TemplateInfo> templates = new HashMap<>();
		List<ServerWorld> worlds = new ArrayList<>();
		server.getWorlds().forEach(worlds::add);

		for (Map.Entry<RegistryKey<Structure>, Structure> entry : structures.getEntrySet()) {
			Identifier id = entry.getKey().getValue();
			Structure structure = entry.getValue();
			structureNames.add(id.toString());
			structureDimensions.put(id, dimensionsOf(structure, worlds));
			if (structure instanceof JigsawStructure jigsaw) {
				try {
					for (Identifier table : lootTablesOf(jigsaw, pools, resources, templates)) {
						structuresByTable.computeIfAbsent(table, t -> new LinkedHashSet<>()).add(id);
					}
				} catch (Throwable e) {
					EMILoot.LOGGER.warn("Could not read the pieces of structure {}: {}", id, e.toString());
				}
			}
		}
	}

	private static List<Identifier> dimensionsOf(Structure structure, List<ServerWorld> worlds) {
		RegistryEntryList<Biome> valid = structure.getValidBiomes();
		List<Identifier> dimensions = new ArrayList<>();
		for (ServerWorld world : worlds) {
			BiomeSource source = world.getChunkManager().getChunkGenerator().getBiomeSource();
			for (RegistryEntry<Biome> biome : source.getBiomes()) {
				if (valid.contains(biome)) {
					dimensions.add(world.getRegistryKey().getValue());
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
