package dev.emi.remastered.info.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TagNamesTest {

	@Test
	void usesLastSegmentWithSpacesAndCapital() {
		assertEquals("Melee weapon", TagNames.readable("tools/melee_weapon"));
		assertEquals("Swords", TagNames.readable("swords"));
		assertEquals("Iron", TagNames.readable("ores/deep/iron"));
	}

	@Test
	void handlesEdgeCases() {
		assertEquals("Planks", TagNames.readable("planks"));
		assertEquals("X", TagNames.readable("x"));
		assertEquals("tools/", TagNames.readable("tools/"));
		assertEquals("", TagNames.readable(""));
	}

	@Test
	void uniqueNamesGetNoSuffix() {
		List<TagNames.Label> labels = TagNames.disambiguate(List.of("Fruits", "Swords"), List.of("c:foods/fruit", "minecraft:swords"));
		assertEquals(List.of(new TagNames.Label("Fruits", ""), new TagNames.Label("Swords", "")), labels);
	}

	@Test
	void equalNamesFromDifferentNamespacesGetTheNamespace() {
		List<TagNames.Label> labels = TagNames.disambiguate(List.of("Fruits", "Planks", "Fruits"),
			List.of("c:foods/fruit", "minecraft:planks", "minecraft:fruits"));
		assertEquals("Fruits", labels.get(0).name());
		assertEquals(" (c)", labels.get(0).suffix());
		assertEquals("", labels.get(1).suffix());
		assertEquals(" (minecraft)", labels.get(2).suffix());
	}

	@Test
	void equalNamesFromOneNamespaceGetTheWholeId() {
		List<TagNames.Label> labels = TagNames.disambiguate(List.of("Fruits", "Fruits", "Fruits"),
			List.of("c:foods/fruit", "c:fruits", "minecraft:fruits"));
		assertEquals(" (c:foods/fruit)", labels.get(0).suffix());
		assertEquals(" (c:fruits)", labels.get(1).suffix());
		assertEquals(" (minecraft:fruits)", labels.get(2).suffix());
	}

	@Test
	void emptyListStaysEmpty() {
		assertEquals(List.of(), TagNames.disambiguate(List.of(), List.of()));
	}
}
