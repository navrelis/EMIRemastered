package dev.emi.remastered.world.data;

import java.util.List;

/** A dimension and the biomes its biome source can produce, as the server sends them. */
public record DimensionRow(String id, List<String> biomes) {

	public DimensionRow {
		biomes = List.copyOf(biomes);
	}
}
