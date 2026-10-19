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
import dev.emi.remastered.mobs.data.SpawnOverrides;
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
		return new MobStats(20, 2, 0, 3, 0.23, NONE, 0, 35, 0.6, 1.95, false, "monster");
	}

	private static MobStats cow() {
		return new MobStats(10, 0, 0, NONE, 0.2, NONE, 0, NONE, 0.9, 1.4, false, "creature");
	}

	@Test
	void showsOnlyDefinedAndNonZeroStats() {
		assertEquals(List.of(Stat.HEALTH, Stat.ARMOR, Stat.ATTACK, Stat.SPEED, Stat.FOLLOW_RANGE), zombie().visible());
		assertEquals(List.of(Stat.HEALTH, Stat.SPEED), cow().visible());
	}

	@Test
	void statValuesAreAlwaysInPoints() {
		MobStats zombie = zombie();
		// the mob page and the Item Info section of the spawn egg show the same numbers
		assertEquals("20", zombie.value(Stat.HEALTH));
		assertEquals("3", zombie.value(Stat.ATTACK));
		assertEquals("2", zombie.value(Stat.ARMOR));
		assertEquals("10", zombie.tooltipArgs(Stat.HEALTH)[1]);
		assertEquals("1.5", zombie.tooltipArgs(Stat.ATTACK)[1]);
		assertEquals("9.9", zombie.value(Stat.SPEED));
		assertEquals("35", zombie.value(Stat.FOLLOW_RANGE));
		assertEquals("0.6 x 1.95", zombie.sizeText());
		MobStats golem = new MobStats(100, 0, 0, 15, 0.25, NONE, 1, 16, 1.4, 2.7, false, "misc");
		assertEquals("100%", golem.value(Stat.KNOCKBACK_RESISTANCE));
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

	@Test
	void spawnOverridesApplyKnownValuesOnly() {
		assertEquals(4.0, SpawnOverrides.attackDamage("minecraft:wither_skeleton", 2.0));
		assertEquals(6.0, SpawnOverrides.attackDamage("minecraft:phantom", 2.0));
		assertEquals(3.0, SpawnOverrides.attackDamage("minecraft:zombie", 3.0));
		assertEquals(2.0, SpawnOverrides.attackDamage("other:wither_skeleton", 2.0));
		assertTrue(Double.isNaN(SpawnOverrides.attackDamage("minecraft:wither_skeleton", NONE)));
		assertTrue(Double.isNaN(SpawnOverrides.attackDamage("minecraft:cow", NONE)));
	}

	@Test
	void witherSkeletonShowsDamageFour() {
		double damage = SpawnOverrides.attackDamage("minecraft:wither_skeleton", 2.0);
		MobStats skeleton = new MobStats(20, 0, 0, damage, 0.25, NONE, 0, 16, 0.7, 2.4, true, "monster");
		assertEquals("4", skeleton.value(Stat.ATTACK));
		assertEquals("2", skeleton.tooltipArgs(Stat.ATTACK)[1]);
	}

	@Test
	void speedAtTheAttributeDefaultIsNotShown() {
		// bat and ghast never set a walking speed, the container holds the attribute default 0.7 (shown as 30.2 once)
		assertTrue(Double.isNaN(MobStats.walkingSpeed(0.7, 0.7)));
		assertTrue(Double.isNaN(MobStats.walkingSpeed(NONE, 0.7)));
		assertEquals(0.23, MobStats.walkingSpeed(0.23, 0.7));
		MobStats bat = new MobStats(6, 0, 0, NONE, MobStats.walkingSpeed(0.7, 0.7), NONE, 0, 16, 0.5, 0.9, false, "ambient");
		assertEquals(List.of(Stat.HEALTH, Stat.FOLLOW_RANGE), bat.visible());
	}

	@Test
	void flyingMobsShowTheFlyingSpeedAttribute() {
		MobStats bee = new MobStats(10, 0, 0, 2, 0.3, 0.6, 0, 48, 0.7, 0.6, false, "creature");
		assertEquals(List.of(Stat.HEALTH, Stat.ATTACK, Stat.SPEED, Stat.FLYING_SPEED, Stat.FOLLOW_RANGE), bee.visible());
		assertEquals("0.6", bee.value(Stat.FLYING_SPEED));
		assertEquals("0.6", bee.tooltipArgs(Stat.FLYING_SPEED)[0]);
		assertEquals(1, bee.tooltipArgs(Stat.FLYING_SPEED).length);
	}
}
