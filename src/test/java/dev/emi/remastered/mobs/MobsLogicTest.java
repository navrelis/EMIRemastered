package dev.emi.remastered.mobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.mobs.data.MobFormat;
import dev.emi.remastered.mobs.data.MobStats;
import dev.emi.remastered.mobs.data.MobStats.Stat;
import dev.emi.remastered.mobs.data.SpawnRow;
import dev.emi.remastered.mobs.data.SpawnTable;
import dev.emi.remastered.mobs.data.SpawnWire;

class MobsLogicTest {
	private static final double NONE = Double.NaN;

	@Test
	void formatsNumbersWithoutTrailingZeros() {
		assertEquals("20", MobFormat.number(20, 2));
		assertEquals("0.5", MobFormat.number(0.5, 2));
		assertEquals("1.25", MobFormat.number(1.2500001, 2));
		assertEquals("0.1", MobFormat.number(0.1f, 3));
		assertEquals("100", MobFormat.number(100, 0));
		assertEquals("-", MobFormat.number(Double.NaN, 1));
	}

	@Test
	void formatsHeartsAndSpeed() {
		assertEquals("10", MobFormat.hearts(20));
		assertEquals("0.5", MobFormat.hearts(1));
		assertEquals("150", MobFormat.hearts(300));
		assertEquals("9.9", MobFormat.blocksPerSecond(0.23));
		assertEquals("4.3", MobFormat.blocksPerSecond(0.1));
	}

	@Test
	void formatsGroupSizeAndDimensions() {
		assertEquals("3", MobFormat.groupSize(3, 3));
		assertEquals("2-4", MobFormat.groupSize(2, 4));
		assertEquals("0.6 x 1.95", MobFormat.size(0.6f, 1.95f));
	}

	@Test
	void compactListShowsTheFirstEntriesAndCountsTheRest() {
		List<Integer> all = List.of(1, 2, 3, 4, 5, 6, 7, 8);
		MobFormat.Compact<Integer> compact = MobFormat.Compact.of(all, 5);
		assertEquals(List.of(1, 2, 3, 4, 5), compact.shown());
		assertEquals(3, compact.hidden());
	}

	@Test
	void compactListNeverHidesASingleEntry() {
		List<Integer> all = List.of(1, 2, 3, 4, 5, 6);
		MobFormat.Compact<Integer> compact = MobFormat.Compact.of(all, 5);
		assertEquals(all, compact.shown());
		assertEquals(0, compact.hidden());
		assertEquals(0, MobFormat.Compact.of(List.of(), 5).hidden());
		assertEquals(1, MobFormat.Compact.of(List.of(1, 2, 3), 0).shown().size());
	}

	private static MobStats zombie() {
		return new MobStats(20, 2, 0, 3, 0.23, 0, 35, 0.6, 1.95, false, "monster");
	}

	private static MobStats cow() {
		return new MobStats(10, 0, 0, NONE, 0.2, 0, NONE, 0.9, 1.4, false, "creature");
	}

	@Test
	void showsOnlyDefinedAndNonZeroStats() {
		assertEquals(List.of(Stat.HEALTH, Stat.ARMOR, Stat.ATTACK, Stat.SPEED, Stat.FOLLOW_RANGE), zombie().visible());
		assertEquals(List.of(Stat.HEALTH, Stat.SPEED), cow().visible());
	}

	@Test
	void statValuesFollowTheIconSetting() {
		MobStats zombie = zombie();
		assertEquals("10", zombie.value(Stat.HEALTH, true));
		assertEquals("20", zombie.value(Stat.HEALTH, false));
		assertEquals("1.5", zombie.value(Stat.ATTACK, true));
		assertEquals("3", zombie.value(Stat.ATTACK, false));
		assertEquals("1", zombie.value(Stat.ARMOR, true));
		assertEquals("2", zombie.value(Stat.ARMOR, false));
		assertEquals("9.9", zombie.value(Stat.SPEED, true));
		assertEquals("35", zombie.value(Stat.FOLLOW_RANGE, true));
		assertEquals("0.6 x 1.95", zombie.sizeText());
		MobStats golem = new MobStats(100, 0, 0, 15, 0.25, 1, 16, 1.4, 2.7, false, "misc");
		assertEquals("100%", golem.value(Stat.KNOCKBACK_RESISTANCE, true));
		assertEquals(2, zombie.tooltipArgs(Stat.HEALTH).length);
		assertEquals(1, zombie.tooltipArgs(Stat.FOLLOW_RANGE).length);
	}

	private static List<SpawnRow> sampleRows() {
		List<SpawnRow> rows = new ArrayList<>();
		rows.add(new SpawnRow("minecraft:zombie", "minecraft:plains", "monster", 95, 4, 4));
		rows.add(new SpawnRow("minecraft:cow", "minecraft:plains", "creature", 8, 2, 4));
		rows.add(new SpawnRow("minecraft:zombie", "minecraft:desert", "monster", 19, 4, 4));
		rows.add(new SpawnRow("minecraft:zombie", "mod:über_biome", "monster", 100, 1, 255));
		rows.add(new SpawnRow("minecraft:zombie", "minecraft:swamp", "monster", 19, 1, 3));
		return rows;
	}

	@Test
	void wireRoundTrip() {
		List<SpawnRow> rows = sampleRows();
		List<byte[]> batches = SpawnWire.encode(rows, 100);
		assertEquals(1, batches.size());
		assertEquals(rows, SpawnWire.decode(batches.get(0)));
	}

	@Test
	void wireSplitsIntoBatches() {
		List<SpawnRow> rows = new ArrayList<>();
		for (int i = 0; i < 1300; i++) {
			rows.add(new SpawnRow("minecraft:zombie", "minecraft:biome_" + (i % 70), "monster", i % 100 + 1, 1, 4));
		}
		List<byte[]> batches = SpawnWire.encode(rows, SpawnWire.ROWS_PER_BATCH);
		assertEquals(3, batches.size());
		List<SpawnRow> decoded = new ArrayList<>();
		for (byte[] batch : batches) {
			assertTrue(batch.length < 100_000);
			decoded.addAll(SpawnWire.decode(batch));
		}
		assertEquals(rows, decoded);
	}

	@Test
	void wireEncodesEmptyAndRejectsGarbage() {
		List<byte[]> batches = SpawnWire.encode(List.of(), 100);
		assertEquals(1, batches.size());
		assertEquals(List.of(), SpawnWire.decode(batches.get(0)));
		assertThrows(IllegalArgumentException.class, () -> SpawnWire.decode(new byte[] {9}));
		assertThrows(IllegalArgumentException.class, () -> SpawnWire.decode(new byte[] {1, 0, 5}));
	}

	@Test
	void tableSortsByWeightThenBiome() {
		SpawnTable table = new SpawnTable();
		table.addAll(sampleRows());
		List<SpawnRow> zombie = table.rowsOf("minecraft:zombie");
		assertEquals(List.of("mod:über_biome", "minecraft:plains", "minecraft:desert", "minecraft:swamp"),
			zombie.stream().map(SpawnRow::biome).toList());
		assertEquals(List.of(), table.rowsOf("minecraft:creeper"));
		assertEquals(5, table.rowCount());
		assertEquals(2, table.entityCount());
		assertEquals(4, table.biomeCount());
		table.clear();
		assertTrue(table.isEmpty());
	}
}
