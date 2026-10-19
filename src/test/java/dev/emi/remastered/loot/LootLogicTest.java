package dev.emi.remastered.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.emi.remastered.loot.util.FloatTrimmer;

class LootLogicTest {
	private final String skippedBefore = LootSettings.skippedKeys;
	private final int decimalsBefore = LootSettings.chanceDecimalPlaces;

	@AfterEach
	void restore() {
		LootSettings.skippedKeys = skippedBefore;
		LootSettings.chanceDecimalPlaces = decimalsBefore;
	}

	@Test
	void trimsChances() {
		assertEquals("12.3", FloatTrimmer.trimFloatString(12.3456f, 1));
		assertEquals("12.34", FloatTrimmer.trimFloatString(12.3456f, 2));
		assertEquals("12", FloatTrimmer.trimFloatString(12.3456f, 0));
		assertEquals("5.0", FloatTrimmer.trimFloatString(5f));
	}

	@Test
	void parsesSkippedKeys() {
		assertEquals(Set.of("a.b", "c", "d"), LootSettings.parseKeys(" a.b, c ,,d "));
		assertTrue(LootSettings.parseKeys("").isEmpty());
		assertTrue(LootSettings.parseKeys(null).isEmpty());
	}

	@Test
	void defaultSkippedKeysCoverTheHiddenConditions() {
		LootSettings.skippedKeys = LootSettings.DEFAULT_SKIPPED_KEYS;
		assertTrue(LootSettings.isSkippedKey("emi_loot.no_conditions"));
		assertTrue(LootSettings.isSkippedKey("emi_loot.function.limit_count"));
		assertFalse(LootSettings.isSkippedKey("emi_loot.function.set_name"));
	}

	@Test
	void skippedKeysFollowTheSetting() {
		LootSettings.skippedKeys = "emi_loot.function.limit_count";
		assertTrue(LootSettings.isSkippedKey("emi_loot.function.limit_count"));
		assertFalse(LootSettings.isSkippedKey("emi_loot.no_conditions"));
		LootSettings.skippedKeys = "";
		assertFalse(LootSettings.isSkippedKey("emi_loot.function.limit_count"));
	}

	@Test
	void clampsDecimalPlaces() {
		LootSettings.chanceDecimalPlaces = -3;
		assertEquals(0, LootSettings.chanceDecimalPlaces());
		LootSettings.chanceDecimalPlaces = 99;
		assertEquals(10, LootSettings.chanceDecimalPlaces());
		LootSettings.chanceDecimalPlaces = 2;
		assertEquals(2, LootSettings.chanceDecimalPlaces());
	}
}
