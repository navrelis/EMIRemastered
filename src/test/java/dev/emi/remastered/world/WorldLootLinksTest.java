package dev.emi.remastered.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.world.data.DimensionRow;
import dev.emi.remastered.world.data.LootLinksWire;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.StructureRow.Placement;
import dev.emi.remastered.world.data.WorldSnapshot;

/** The chest loot tables that follow the world data when the structure lookup finishes late. */
class WorldLootLinksTest {

	@Test
	void lootLinksRoundTrip() {
		Map<String, List<String>> tables = new TreeMap<>();
		tables.put("minecraft:desert_pyramid", List.of("minecraft:chests/desert_pyramid"));
		tables.put("minecraft:village_plains", List.of("minecraft:chests/village/village_plains_house", "minecraft:chests/village/village_weaponsmith"));
		tables.put("mod:tower", List.of("minecraft:chests/village/village_weaponsmith", "mod:chests/tower"));
		List<byte[]> batches = LootLinksWire.encode(tables, 1_000_000);
		assertEquals(1, batches.size());
		assertEquals(tables, LootLinksWire.decode(batches.get(0)));
	}

	@Test
	void lootLinksAreSplitByBudgetAndNothingIsLost() {
		Map<String, List<String>> tables = new LinkedHashMap<>();
		for (int i = 0; i < 500; i++) {
			tables.put("mod:structure_" + i, List.of("mod:chests/a_" + i, "mod:chests/shared"));
		}
		List<byte[]> batches = LootLinksWire.encode(tables, 2_000);
		assertTrue(batches.size() > 1);
		Map<String, List<String>> merged = new LinkedHashMap<>();
		batches.forEach(batch -> merged.putAll(LootLinksWire.decode(batch)));
		assertEquals(tables, merged);
	}

	@Test
	void emptyLootLinksAreOneBatch() {
		List<byte[]> batches = LootLinksWire.encode(Map.of(), 1000);
		assertEquals(1, batches.size());
		assertTrue(LootLinksWire.decode(batches.get(0)).isEmpty());
	}

	@Test
	void malformedLootLinksAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> LootLinksWire.decode(new byte[] {9}));
		assertThrows(IllegalArgumentException.class, () -> LootLinksWire.decode(new byte[] {1, 5}));
	}

	@Test
	void fingerprintIgnoresLootTablesSoLateTablesCauseNoReload() {
		List<DimensionRow> dimensions = List.of(new DimensionRow("minecraft:overworld", List.of("minecraft:desert")));
		StructureRow without = new StructureRow("minecraft:desert_pyramid", "surface_structures", List.of("minecraft:overworld"),
			List.of("minecraft:desert"), "", "minecraft:desert_pyramids", 1, 1, Placement.randomSpread(32, 8, 0), List.of());
		StructureRow with = without.withLootTables(List.of("minecraft:chests/desert_pyramid"));
		WorldSnapshot early = new WorldSnapshot(true, dimensions, List.of(without));
		WorldSnapshot late = new WorldSnapshot(true, dimensions, List.of(with));
		assertEquals(early.fingerprint(), late.fingerprint());
		WorldSnapshot other = new WorldSnapshot(true, dimensions, List.of(without, new StructureRow("mod:tower", "surface_structures",
			List.of(), List.of(), "", "", 0, 0, Placement.NONE, List.of())));
		assertNotEquals(early.fingerprint(), other.fingerprint());
	}
}
