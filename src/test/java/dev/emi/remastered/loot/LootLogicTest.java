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
		assertEquals("0.0", FloatTrimmer.trimFloatString(0f, 1));
		assertEquals("0.05", FloatTrimmer.trimFloatString(0.0512f, 2));
	}

	@Test
	void tinyChancesAreNotShownAsZeroOrInScientificNotation() {
		assertEquals("<0.1", FloatTrimmer.trimFloatString(0.0083f, 1));
		// Float.toString(9.09E-4f) is "9.09E-4", which used to be cut to a wrong "9.0"
		assertEquals("<0.1", FloatTrimmer.trimFloatString(0.000909f, 1));
		assertEquals("<1", FloatTrimmer.trimFloatString(0.5f, 0));
		assertEquals("<0.01", FloatTrimmer.trimFloatString(0.000909f, 2));
	}

	@Test
	void detectsChancesBelowTheResolution() {
		assertTrue(FloatTrimmer.isBelowResolution(0.0009f, 1));
		assertTrue(FloatTrimmer.isBelowResolution(0.0009f, 2));
		assertFalse(FloatTrimmer.isBelowResolution(0.0009f, 4));
		assertFalse(FloatTrimmer.isBelowResolution(0.15f, 1));
		assertTrue(FloatTrimmer.isBelowResolution(0.05f, 1));
		assertFalse(FloatTrimmer.isBelowResolution(0f, 1));
		assertFalse(FloatTrimmer.isBelowResolution(Float.NaN, 1));
	}

	@Test
	void showsTinyChancesAsPercentWithTwoSignificantDigits() {
		// the junk of a fishing roll: 0.000909 of a roll is 0.09 percent
		assertEquals("0.09", FloatTrimmer.trimPercent(0.000909f));
		assertEquals("0.04", FloatTrimmer.trimPercent(0.0004f));
		assertEquals("0.0083", FloatTrimmer.trimPercent(0.000083f));
		assertEquals("0.1", FloatTrimmer.trimPercent(0.001f));
		assertEquals("0.5", FloatTrimmer.trimPercent(0.005f));
		assertEquals("0", FloatTrimmer.trimPercent(0f));
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
