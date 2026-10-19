package dev.emi.remastered.world.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The complete world data of one server sync with the lookups the pages need. Immutable, so EMI's reload thread can
 * read it while the network thread assembles the next one.
 */
public final class WorldSnapshot {
	public static final WorldSnapshot EMPTY = new WorldSnapshot(false, List.of(), List.of());

	private final boolean complete;
	private final List<StructureRow> structures;
	private final Map<String, StructureRow> structuresById;
	private final Map<String, List<String>> biomesOfDimension;
	private final Map<String, List<String>> dimensionsOfBiome;
	private final Map<String, List<StructureRow>> structuresOfBiome;

	public WorldSnapshot(boolean complete, List<DimensionRow> dimensions, List<StructureRow> structures) {
		this.complete = complete;
		this.structures = List.copyOf(structures);
		Map<String, StructureRow> byId = new LinkedHashMap<>();
		Map<String, List<StructureRow>> byBiome = new LinkedHashMap<>();
		for (StructureRow structure : this.structures) {
			byId.put(structure.id(), structure);
			for (String biome : structure.biomes()) {
				byBiome.computeIfAbsent(biome, k -> new ArrayList<>()).add(structure);
			}
		}
		this.structuresById = Collections.unmodifiableMap(byId);
		this.structuresOfBiome = Collections.unmodifiableMap(byBiome);
		Map<String, List<String>> biomes = new LinkedHashMap<>();
		Map<String, List<String>> dims = new LinkedHashMap<>();
		for (DimensionRow dimension : dimensions) {
			biomes.put(dimension.id(), dimension.biomes());
			for (String biome : dimension.biomes()) {
				dims.computeIfAbsent(biome, k -> new ArrayList<>()).add(dimension.id());
			}
		}
		this.biomesOfDimension = Collections.unmodifiableMap(biomes);
		this.dimensionsOfBiome = Collections.unmodifiableMap(dims);
	}

	/** Whether this is a complete sync of a server (false for servers without the module). */
	public boolean isComplete() {
		return complete;
	}

	public List<StructureRow> structures() {
		return structures;
	}

	public StructureRow structure(String id) {
		return structuresById.get(id);
	}

	/** Dimension ids in the order of the server (overworld, nether and end first). */
	public List<String> dimensions() {
		return List.copyOf(biomesOfDimension.keySet());
	}

	public int dimensionCount() {
		return biomesOfDimension.size();
	}

	public List<String> biomesOf(String dimension) {
		return biomesOfDimension.getOrDefault(dimension, List.of());
	}

	/** The dimensions whose biome source can produce the biome, in the order of the server. */
	public List<String> dimensionsOfBiome(String biome) {
		return dimensionsOfBiome.getOrDefault(biome, List.of());
	}

	/** The structures that can generate in the biome, sorted by id. */
	public List<StructureRow> structuresOfBiome(String biome) {
		return structuresOfBiome.getOrDefault(biome, List.of());
	}

	/**
	 * Identifies the content; EMI reloads when the data changes after it was registered. The chest loot tables are not
	 * part of it: the structure pages read them live (they can arrive after the data, see {@code ClientWorldData}).
	 */
	public int fingerprint() {
		int hash = 0;
		for (StructureRow structure : structures) {
			hash = 31 * hash + structure.withLootTables(List.of()).hashCode();
		}
		return 31 * hash + biomesOfDimension.hashCode();
	}
}
