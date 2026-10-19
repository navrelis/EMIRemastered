package dev.emi.remastered.mobs.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Spawn rows indexed by entity type id. Not thread safe, callers synchronise. */
public final class SpawnTable {
	private final Map<String, List<SpawnRow>> byEntity = new HashMap<>();
	private int rowCount;

	public void add(SpawnRow row) {
		byEntity.computeIfAbsent(row.entity(), k -> new ArrayList<>()).add(row);
		rowCount++;
	}

	public void addAll(List<SpawnRow> rows) {
		for (SpawnRow row : rows) {
			add(row);
		}
	}

	public void clear() {
		byEntity.clear();
		rowCount = 0;
	}

	public boolean isEmpty() {
		return rowCount == 0;
	}

	public int rowCount() {
		return rowCount;
	}

	public int entityCount() {
		return byEntity.size();
	}

	public int biomeCount() {
		Set<String> biomes = new HashSet<>();
		for (List<SpawnRow> rows : byEntity.values()) {
			for (SpawnRow row : rows) {
				biomes.add(row.biome());
			}
		}
		return biomes.size();
	}

	/** Rows of one mob, heaviest weight first, then by biome id. Never null. */
	public List<SpawnRow> rowsOf(String entity) {
		List<SpawnRow> rows = byEntity.get(entity);
		if (rows == null) {
			return List.of();
		}
		List<SpawnRow> sorted = new ArrayList<>(rows);
		sorted.sort(Comparator.comparingInt(SpawnRow::weight).reversed().thenComparing(SpawnRow::biome));
		return Collections.unmodifiableList(sorted);
	}
}
