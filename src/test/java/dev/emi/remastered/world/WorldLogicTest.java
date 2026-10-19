package dev.emi.remastered.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.world.data.DimensionRow;
import dev.emi.remastered.world.data.IconLayout;
import dev.emi.remastered.world.data.StructureIcons;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.StructureRow.Kind;
import dev.emi.remastered.world.data.StructureRow.Placement;
import dev.emi.remastered.world.data.WorldBatch;
import dev.emi.remastered.world.data.WorldFormat;
import dev.emi.remastered.world.data.WorldSnapshot;
import dev.emi.remastered.world.data.WorldWire;

/** Pure logic of the world module: wire format and batching, lookups, icons, layout and formatting. */
class WorldLogicTest {

	private static StructureRow structure(String id, Placement placement, List<String> biomes, List<String> tables) {
		return new StructureRow(id, "surface_structures", List.of("minecraft:overworld"), biomes, "minecraft:has_structure/" + id.substring(id.indexOf(':') + 1),
			"minecraft:villages", 1, 5, placement, tables);
	}

	private static List<DimensionRow> dimensions() {
		return List.of(
			new DimensionRow("minecraft:overworld", List.of("minecraft:plains", "minecraft:desert", "minecraft:taiga")),
			new DimensionRow("minecraft:the_nether", List.of("minecraft:nether_wastes")),
			new DimensionRow("mod:empty", List.of()));
	}

	@Test
	void roundTripKeepsEveryField() {
		StructureRow village = structure("minecraft:village_plains", Placement.randomSpread(34, 8, 1), List.of("minecraft:plains", "minecraft:meadow"),
			List.of("minecraft:chests/village/village_armorer", "minecraft:chests/village/village_weaponsmith"));
		StructureRow stronghold = new StructureRow("minecraft:stronghold", "strongholds", List.of("minecraft:overworld"), List.of("minecraft:plains"), "",
			"minecraft:strongholds", 1, 1, Placement.rings(32, 3, 128), List.of());
		StructureRow loose = new StructureRow("mod:thing", "raw_generation", List.of(), List.of(), "", "", 0, 0, Placement.NONE, List.of());
		StructureRow custom = new StructureRow("mod:custom", "lakes", List.of("mod:dim"), List.of("mod:biome"), "mod:tag", "mod:set", 7, 70,
			new Placement(Kind.OTHER, 0, 0, 0), List.of());
		List<StructureRow> structures = List.of(village, stronghold, loose, custom);

		List<byte[]> batches = WorldWire.encode(dimensions(), structures, WorldWire.MAX_BATCH_BYTES);
		assertEquals(1, batches.size());
		WorldBatch decoded = WorldWire.decode(batches.get(0));
		assertEquals(dimensions(), decoded.dimensions());
		assertEquals(structures, decoded.structures());
		StructureRow back = decoded.structures().get(0);
		assertEquals(34, back.placement().spacing());
		assertEquals(8, back.placement().separation());
		assertTrue(back.placement().triangular());
		assertEquals(128, decoded.structures().get(1).placement().ringCount());
	}

	@Test
	void emptyDataIsOneEmptyBatch() {
		List<byte[]> batches = WorldWire.encode(List.of(), List.of(), WorldWire.MAX_BATCH_BYTES);
		assertEquals(1, batches.size());
		WorldBatch decoded = WorldWire.decode(batches.get(0));
		assertTrue(decoded.dimensions().isEmpty());
		assertTrue(decoded.structures().isEmpty());
	}

	@Test
	void bigSyncsAreSplitAndStayInOrder() {
		List<String> biomes = new ArrayList<>();
		for (int i = 0; i < 400; i++) {
			biomes.add("mod:biome_number_" + i);
		}
		List<StructureRow> structures = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			structures.add(structure("mod:structure_" + i, Placement.randomSpread(20 + i, 5, 0), biomes, List.of()));
		}
		List<DimensionRow> dims = List.of(new DimensionRow("minecraft:overworld", biomes));
		int budget = 40_000;
		List<byte[]> batches = WorldWire.encode(dims, structures, budget);
		assertTrue(batches.size() > 3, "split into several batches, got " + batches.size());

		List<StructureRow> all = new ArrayList<>();
		List<DimensionRow> allDims = new ArrayList<>();
		for (byte[] batch : batches) {
			assertTrue(batch.length < budget, "batch of " + batch.length + " bytes");
			WorldBatch decoded = WorldWire.decode(batch);
			all.addAll(decoded.structures());
			allDims.addAll(decoded.dimensions());
		}
		assertEquals(structures, all);
		assertEquals(dims, allDims);
	}

	@Test
	void anItemBiggerThanTheBudgetGetsItsOwnBatch() {
		List<String> biomes = new ArrayList<>();
		for (int i = 0; i < 2000; i++) {
			biomes.add("mod:biome_" + i);
		}
		StructureRow big = structure("mod:big", Placement.NONE, biomes, List.of());
		StructureRow small = structure("mod:small", Placement.NONE, List.of("mod:biome_1"), List.of());
		List<byte[]> batches = WorldWire.encode(List.of(), List.of(small, big, small), 1000);
		assertEquals(3, batches.size());
		assertEquals(2000, WorldWire.decode(batches.get(1)).structures().get(0).biomes().size());
	}

	@Test
	void malformedDataIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> WorldWire.decode(new byte[0]));
		assertThrows(IllegalArgumentException.class, () -> WorldWire.decode(new byte[] {99}));
		byte[] good = WorldWire.encode(dimensions(), List.of(structure("minecraft:village_plains", Placement.NONE, List.of("minecraft:plains"), List.of())),
			WorldWire.MAX_BATCH_BYTES).get(0);
		byte[] cut = java.util.Arrays.copyOf(good, good.length - 6);
		assertThrows(IllegalArgumentException.class, () -> WorldWire.decode(cut));
	}

	@Test
	void snapshotIndexesByBiomeAndDimension() {
		StructureRow village = structure("minecraft:village_plains", Placement.NONE, List.of("minecraft:plains", "minecraft:meadow"), List.of());
		StructureRow outpost = structure("minecraft:pillager_outpost", Placement.NONE, List.of("minecraft:plains", "minecraft:desert"), List.of());
		WorldSnapshot snapshot = new WorldSnapshot(true, dimensions(), List.of(village, outpost));

		assertTrue(snapshot.isComplete());
		assertEquals(List.of(village, outpost), snapshot.structuresOfBiome("minecraft:plains"));
		assertEquals(List.of(village), snapshot.structuresOfBiome("minecraft:meadow"));
		assertEquals(List.of(), snapshot.structuresOfBiome("minecraft:nether_wastes"));
		assertEquals(List.of("minecraft:overworld"), snapshot.dimensionsOfBiome("minecraft:plains"));
		assertEquals(List.of("minecraft:the_nether"), snapshot.dimensionsOfBiome("minecraft:nether_wastes"));
		assertEquals(List.of(), snapshot.dimensionsOfBiome("minecraft:unknown"));
		assertEquals(List.of("minecraft:overworld", "minecraft:the_nether", "mod:empty"), snapshot.dimensions());
		assertEquals(village, snapshot.structure("minecraft:village_plains"));
		assertNull(snapshot.structure("minecraft:nothing"));

		assertFalse(WorldSnapshot.EMPTY.isComplete());
		WorldSnapshot same = new WorldSnapshot(true, dimensions(), List.of(village, outpost));
		assertEquals(snapshot.fingerprint(), same.fingerprint());
		assertTrue(snapshot.fingerprint() != new WorldSnapshot(true, dimensions(), List.of(village)).fingerprint());
	}

	@Test
	void iconsFollowTheStructureId() {
		assertEquals("minecraft:bell", StructureIcons.itemOf("minecraft:village_taiga"));
		assertEquals("minecraft:crossbow", StructureIcons.itemOf("minecraft:pillager_outpost"));
		assertEquals("minecraft:crying_obsidian", StructureIcons.itemOf("minecraft:ruined_portal_nether"));
		assertEquals("minecraft:crying_obsidian", StructureIcons.itemOf("minecraft:ruined_portal"));
		assertEquals("minecraft:oak_boat", StructureIcons.itemOf("minecraft:shipwreck_beached"));
		assertEquals("minecraft:prismarine_bricks", StructureIcons.itemOf("minecraft:ocean_ruin_cold"));
		assertEquals("minecraft:powered_rail", StructureIcons.itemOf("minecraft:mineshaft_mesa"));
		assertEquals("minecraft:rail", StructureIcons.itemOf("minecraft:mineshaft"));
		assertEquals(StructureIcons.FALLBACK, StructureIcons.itemOf("mod:village_plains"));
		assertEquals("minecraft:structure_block", StructureIcons.itemOf("twilightforest:hollow_hill"));
	}

	@Test
	void iconLayoutTakesTheLastCellForTheMoreLabel() {
		assertEquals(new IconLayout(0, 0, 0), IconLayout.of(0, 2, 9));
		assertEquals(new IconLayout(5, 0, 1), IconLayout.of(5, 2, 9));
		assertEquals(new IconLayout(9, 0, 1), IconLayout.of(9, 2, 9));
		assertEquals(new IconLayout(10, 0, 2), IconLayout.of(10, 2, 9));
		assertEquals(new IconLayout(18, 0, 2), IconLayout.of(18, 2, 9));
		// 19 icons in 18 cells: 17 shown, the 18th cell says "+2"
		assertEquals(new IconLayout(17, 2, 2), IconLayout.of(19, 2, 9));
		assertEquals(new IconLayout(8, 52, 1), IconLayout.of(60, 1, 9));
		assertEquals(new IconLayout(35, 5, 4), IconLayout.of(40, 4, 9));
	}

	@Test
	void formatting() {
		assertEquals(544, WorldFormat.blocks(34));
		assertEquals("0.8", WorldFormat.climate(0.8f));
		assertEquals("2", WorldFormat.climate(2.0f));
		assertEquals("-0.5", WorldFormat.climate(-0.5f));
		assertEquals("0.05", WorldFormat.climate(0.05f));
		assertEquals("0", WorldFormat.climate(-0.0f));
		assertEquals("0.25", WorldFormat.climate(0.25f));
		assertEquals("Village Plains", WorldFormat.prettify("village_plains"));
		assertEquals("Twilight Forest Dark Forest", WorldFormat.prettify("twilight_forest/dark_forest"));
		assertEquals("village_plains", WorldFormat.shortId("minecraft:village_plains"));
		assertEquals("mod:thing", WorldFormat.shortId("mod:thing"));
		assertEquals("house", WorldFormat.withoutPrefix("village_plains_house", "village_plains"));
		assertEquals("fisher", WorldFormat.withoutPrefix("village_fisher", "village"));
		assertEquals("village_plains", WorldFormat.withoutPrefix("village_plains", "village_plains"));
		assertEquals("reward", WorldFormat.withoutPrefix("reward", "trial"));
		assertEquals("4", WorldFormat.groupSize(4, 4));
		assertEquals("2-4", WorldFormat.groupSize(2, 4));
		assertTrue(WorldFormat.hasSiblings(1, 5));
		assertFalse(WorldFormat.hasSiblings(1, 1));
	}
}
