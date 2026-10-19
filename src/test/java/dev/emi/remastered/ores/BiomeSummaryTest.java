package dev.emi.remastered.ores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.emi.remastered.ores.display.BiomeSummary;
import dev.emi.remastered.ores.display.BiomeSummary.Kind;
import dev.emi.remastered.ores.display.BiomeSummary.Section;
import dev.emi.remastered.ores.display.DimensionNames;
import dev.emi.remastered.ores.sync.OresSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class BiomeSummaryTest {
	private static Identifier mc(String path) {
		return Identifier.of("minecraft", path);
	}

	private static List<Identifier> biomes(String namespace, int count) {
		List<Identifier> list = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			list.add(Identifier.of(namespace, "biome_" + String.format("%02d", i)));
		}
		return list;
	}

	private static OresSnapshot.Dimension dimension(String path, List<Identifier> biomes) {
		return new OresSnapshot.Dimension(mc(path), 0, 256, Set.copyOf(biomes));
	}

	@Test
	void allBiomesOfADimension() {
		List<Identifier> overworld = biomes("minecraft", 60);
		List<Section> sections = BiomeSummary.summarize(overworld, List.of(dimension("overworld", overworld)), 5);
		assertEquals(1, sections.size());
		assertEquals(Kind.ALL, sections.get(0).kind());
		assertEquals(mc("overworld"), sections.get(0).dimension());
		assertEquals(60, sections.get(0).total());
		assertTrue(sections.get(0).listed().isEmpty());
	}

	@Test
	void allExceptAFewBiomes() {
		List<Identifier> overworld = biomes("minecraft", 60);
		List<Identifier> feature = overworld.subList(3, 60);
		List<Section> sections = BiomeSummary.summarize(feature, List.of(dimension("overworld", overworld)), 5);
		assertEquals(1, sections.size());
		assertEquals(Kind.ALL_EXCEPT, sections.get(0).kind());
		assertEquals(57, sections.get(0).total());
		assertEquals(overworld.subList(0, 3), sections.get(0).listed());
	}

	@Test
	void listsWhenTheFeatureIsInFewBiomes() {
		List<Identifier> overworld = biomes("minecraft", 60);
		List<Identifier> feature = overworld.subList(0, 10);
		Section section = BiomeSummary.summarize(feature, List.of(dimension("overworld", overworld)), 5).get(0);
		assertEquals(Kind.LIST, section.kind());
		assertEquals(feature, section.listed());
	}

	@Test
	void listsWhenAboutHalfOfTheBiomesAreMissing() {
		List<Identifier> overworld = biomes("minecraft", 60);
		Section section = BiomeSummary.summarize(overworld.subList(0, 30), List.of(dimension("overworld", overworld)), 5).get(0);
		assertEquals(Kind.LIST, section.kind());
		assertEquals(30, section.listed().size());
	}

	@Test
	void smallDimensionsAreListedInsteadOfSummedUp() {
		List<Identifier> nether = biomes("minecraft", 5);
		// 4 of 5 fit into the icons, "all except one" would not be shorter
		Section section = BiomeSummary.summarize(nether.subList(0, 4), List.of(dimension("the_nether", nether)), 5).get(0);
		assertEquals(Kind.LIST, section.kind());
		assertEquals(4, section.listed().size());
	}

	@Test
	void severalDimensionsAndBiomesOfNoDimension() {
		List<Identifier> overworld = biomes("minecraft", 20);
		List<Identifier> nether = biomes("nether", 6);
		List<Identifier> feature = new ArrayList<>(overworld);
		feature.addAll(nether.subList(0, 2));
		feature.add(Identifier.of("lost", "island"));
		List<Section> sections = BiomeSummary.summarize(feature,
			List.of(dimension("overworld", overworld), dimension("the_nether", nether), dimension("the_end", biomes("end", 3))), 5);
		assertEquals(3, sections.size());
		assertEquals(Kind.ALL, sections.get(0).kind());
		assertEquals(Kind.LIST, sections.get(1).kind());
		assertEquals(mc("the_nether"), sections.get(1).dimension());
		assertEquals(2, sections.get(1).listed().size());
		assertNull(sections.get(2).dimension());
		assertEquals(List.of(Identifier.of("lost", "island")), sections.get(2).listed());
	}

	@Test
	void dimensionsWithoutTheFeatureAreLeftOut() {
		List<Identifier> overworld = biomes("minecraft", 20);
		assertTrue(BiomeSummary.summarize(List.of(), List.of(dimension("overworld", overworld)), 5).isEmpty());
	}

	@Test
	void sortsMinecraftFirst() {
		List<Identifier> sorted = BiomeSummary.sorted(List.of(Identifier.of("b", "x"), Identifier.of("a", "y"), mc("z"), mc("a")));
		assertEquals(List.of(mc("a"), mc("z"), Identifier.of("a", "y"), Identifier.of("b", "x")), sorted);
	}

	@Test
	void splitShowsTheFirstIconsAndHidesTheRest() {
		List<Identifier> list = biomes("minecraft", 40);
		BiomeSummary.Split split = BiomeSummary.split(list, 5);
		assertEquals(list.subList(0, 5), split.shown());
		assertEquals(list.subList(5, 40), split.hidden());
	}

	@Test
	void splitDoesNotHideOneOrTwoBiomes() {
		List<Identifier> list = biomes("minecraft", 7);
		assertEquals(7, BiomeSummary.split(list, 5).shown().size());
		assertTrue(BiomeSummary.split(list, 5).hidden().isEmpty());
		assertEquals(5, BiomeSummary.split(biomes("minecraft", 8), 5).shown().size());
		assertEquals(3, BiomeSummary.split(biomes("minecraft", 8), 5).hidden().size());
	}

	@Test
	void splitKeepsAtLeastOneIcon() {
		assertEquals(1, BiomeSummary.split(biomes("minecraft", 20), 0).shown().size());
	}

	@Test
	void iconRows() {
		assertEquals(0, BiomeSummary.rows(0, false, 8));
		assertEquals(1, BiomeSummary.rows(8, false, 8));
		assertEquals(2, BiomeSummary.rows(9, false, 8));
		// 5 icons and the 3 cell wide label fill one row exactly
		assertEquals(1, BiomeSummary.rows(5, true, 8));
		assertEquals(2, BiomeSummary.rows(6, true, 8));
		assertEquals(2, BiomeSummary.rows(8, true, 8));
	}

	@Test
	void readableDimensionNames() {
		assertEquals("Twilight Forest", DimensionNames.readable(Identifier.of("twilightforest", "twilight_forest")));
		assertEquals("The Nether", DimensionNames.readable(mc("the_nether")));
		assertEquals("Spatial Storage", DimensionNames.readable(Identifier.of("ae2", "spatial/storage")));
	}
}
