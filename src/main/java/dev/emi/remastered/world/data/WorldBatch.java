package dev.emi.remastered.world.data;

import java.util.List;

/** The content of one {@code emi_world:structures} payload. */
public record WorldBatch(List<DimensionRow> dimensions, List<StructureRow> structures) {

	public WorldBatch {
		dimensions = List.copyOf(dimensions);
		structures = List.copyOf(structures);
	}
}
