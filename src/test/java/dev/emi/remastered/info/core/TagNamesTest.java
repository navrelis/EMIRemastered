package dev.emi.remastered.info.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
