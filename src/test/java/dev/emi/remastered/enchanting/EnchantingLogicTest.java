package dev.emi.remastered.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.enchanting.logic.EnchantmentSources;
import dev.emi.remastered.enchanting.logic.EnchantmentSources.Flags;
import dev.emi.remastered.enchanting.logic.EnchantmentSources.Source;
import dev.emi.remastered.enchanting.logic.IncompatibilityMatrix;
import dev.emi.remastered.enchanting.logic.LevelCap;

class EnchantingLogicTest {
	private static Flags flags(boolean table, boolean loot, boolean tradeable, boolean rebalancedTag, boolean rebalance,
			boolean tradedEquipment, boolean mob, boolean treasure, boolean nonTreasure, boolean curse) {
		return new Flags(table, loot, tradeable, rebalancedTag, rebalance, tradedEquipment, mob, treasure, nonTreasure, curse);
	}

	private static void noError(String element, Throwable t) {
		throw new AssertionError(element, t);
	}

	@Test
	void noTagsMeansNoSource() {
		assertTrue(EnchantmentSources.classify(flags(false, false, false, false, false, false, false, false, false, false)).isEmpty());
	}

	@Test
	void sharpnessLikeEnchantment() {
		Set<Source> sources = EnchantmentSources.classify(flags(true, true, true, false, false, false, false, false, false, false));
		assertEquals(List.of(Source.ENCHANTING_TABLE, Source.LOOT, Source.LIBRARIAN_TRADES), List.copyOf(sources));
	}

	@Test
	void mendingLikeTreasureIsNotInTheTable() {
		Set<Source> sources = EnchantmentSources.classify(flags(false, true, true, false, false, false, false, true, false, false));
		assertEquals(List.of(Source.LOOT, Source.LIBRARIAN_TRADES, Source.TREASURE), List.copyOf(sources));
	}

	@Test
	void curseAndMobEquipment() {
		Set<Source> sources = EnchantmentSources.classify(flags(false, true, false, false, false, false, true, true, false, true));
		assertEquals(List.of(Source.LOOT, Source.MOB_EQUIPMENT, Source.TREASURE, Source.CURSE), List.copyOf(sources));
	}

	@Test
	void nonTreasureTagWinsOverTreasure() {
		assertFalse(EnchantmentSources.classify(flags(true, false, false, false, false, false, false, true, true, false))
			.contains(Source.TREASURE));
	}

	@Test
	void rebalancedWorldsUseTheBiomeTradeTagsInsteadOfTradeable() {
		assertFalse(EnchantmentSources.classify(flags(false, false, true, false, true, false, false, false, false, false))
			.contains(Source.LIBRARIAN_TRADES));
		assertTrue(EnchantmentSources.classify(flags(false, false, false, true, true, false, false, false, false, false))
			.contains(Source.LIBRARIAN_TRADES));
		assertFalse(EnchantmentSources.classify(flags(false, false, false, true, false, false, false, false, false, false))
			.contains(Source.LIBRARIAN_TRADES));
	}

	@Test
	void tradedEquipmentIsSeparateFromLibrarianTrades() {
		Set<Source> sources = EnchantmentSources.classify(flags(false, false, false, false, true, true, false, false, false, false));
		assertEquals(List.of(Source.TRADED_EQUIPMENT), List.copyOf(sources));
	}

	@Test
	void booksAboveTheMaximumLevelAreFewAndLimited() {
		// Sharpness: five levels above the maximum, but never above level 10
		assertEquals(10, LevelCap.aboveMaxTop(5, LevelCap.VANILLA, 10));
		// Protection: max 4 + 5 = 9
		assertEquals(9, LevelCap.aboveMaxTop(4, LevelCap.VANILLA, 10));
		// Unbreaking: max 3 + 5 = 8
		assertEquals(8, LevelCap.aboveMaxTop(3, LevelCap.UNLIMITED, 10));
		// a lower level cap of the game wins
		assertEquals(6, LevelCap.aboveMaxTop(5, 6, 10));
		assertEquals(5, LevelCap.aboveMaxTop(5, 5, 10));
		assertEquals(5, LevelCap.aboveMaxTop(5, 1, 10));
		// the book level limit setting wins too
		assertEquals(7, LevelCap.aboveMaxTop(5, LevelCap.VANILLA, 7));
		assertEquals(5, LevelCap.aboveMaxTop(5, LevelCap.VANILLA, 3));
		// modded enchantments that already reach the ceiling, and single level enchantments, get no extra books
		assertEquals(10, LevelCap.aboveMaxTop(10, LevelCap.VANILLA, 10));
		assertEquals(30, LevelCap.aboveMaxTop(30, LevelCap.VANILLA, 10));
		assertEquals(1, LevelCap.aboveMaxTop(1, LevelCap.VANILLA, 10));
		assertEquals(Integer.MAX_VALUE, LevelCap.aboveMaxTop(Integer.MAX_VALUE, LevelCap.UNLIMITED, 10));
		// never more than 255 books
		for (int max = 1; max < 300; max++) {
			assertTrue(LevelCap.aboveMaxTop(max, LevelCap.UNLIMITED, 10) - max <= LevelCap.EXTRA_LEVELS);
		}
	}

	@Test
	void everySourceHasADistinctLangKey() {
		long distinct = List.of(Source.values()).stream().map(Source::key).distinct().count();
		assertEquals(Source.values().length, distinct);
	}

	@Test
	void matrixIsSymmetricForASymmetricRule() {
		List<String> names = List.of("sharpness", "smite", "bane", "mending", "infinity");
		IncompatibilityMatrix<String> matrix = IncompatibilityMatrix.build(names,
			(a, b) -> !(isDamage(a) && isDamage(b)) && !(a.equals("mending") && b.equals("infinity"))
				&& !(a.equals("infinity") && b.equals("mending")), EnchantingLogicTest::noError);
		assertSymmetric(names, matrix);
		assertEquals(List.of("smite", "bane"), matrix.incompatibleWith("sharpness"));
		assertEquals(List.of("infinity"), matrix.incompatibleWith("mending"));
		assertTrue(matrix.areIncompatible("bane", "smite"));
		assertFalse(matrix.areIncompatible("sharpness", "mending"));
	}

	@Test
	void matrixIsSymmetricEvenForAnAsymmetricRule() {
		// A mod that only answers "no" in one direction
		List<String> names = List.of("a", "b", "c");
		IncompatibilityMatrix<String> matrix = IncompatibilityMatrix.build(names, (x, y) -> !(x.equals("c") && y.equals("a")),
			EnchantingLogicTest::noError);
		assertSymmetric(names, matrix);
		assertEquals(List.of("c"), matrix.incompatibleWith("a"));
		assertEquals(List.of("a"), matrix.incompatibleWith("c"));
		assertTrue(matrix.incompatibleWith("b").isEmpty());
	}

	@Test
	void anElementIsNeverIncompatibleWithItself() {
		List<String> names = List.of("a", "b");
		IncompatibilityMatrix<String> matrix = IncompatibilityMatrix.build(names, (x, y) -> false, EnchantingLogicTest::noError);
		assertEquals(List.of("b"), matrix.incompatibleWith("a"));
		assertEquals(List.of("a"), matrix.incompatibleWith("b"));
	}

	@Test
	void aThrowingTestCountsAsCompatibleAndIsReported() {
		List<String> names = List.of("a", "b", "c");
		List<String> errors = new ArrayList<>();
		IncompatibilityMatrix<String> matrix = IncompatibilityMatrix.build(names, (x, y) -> {
			if (x.equals("a") && y.equals("b")) {
				throw new IllegalStateException("broken");
			}
			return !(x.equals("b") && y.equals("c"));
		}, (e, t) -> errors.add(e + ":" + t.getMessage()));
		assertEquals(List.of("a:broken"), errors);
		assertFalse(matrix.areIncompatible("a", "b"));
		assertTrue(matrix.areIncompatible("b", "c"));
	}

	@Test
	void unknownElementsHaveNoIncompatibilities() {
		IncompatibilityMatrix<String> matrix = IncompatibilityMatrix.build(List.of("a"), (x, y) -> true, EnchantingLogicTest::noError);
		assertTrue(matrix.incompatibleWith("zzz").isEmpty());
	}

	@Test
	void levelCapIsMeasuredFromTheProbe() {
		assertEquals(LevelCap.VANILLA, LevelCap.measure(level -> Math.min(level, 255)));
		assertEquals(LevelCap.UNLIMITED, LevelCap.measure(level -> level));
		assertEquals(100, LevelCap.measure(level -> Math.min(level, 100)));
		assertEquals(1, LevelCap.measure(level -> 0));
	}

	@Test
	void onlyANonVanillaCapIsWorthShowing() {
		assertFalse(LevelCap.isCustom(LevelCap.VANILLA));
		assertTrue(LevelCap.isCustom(LevelCap.UNLIMITED));
		assertTrue(LevelCap.isCustom(100));
	}

	private static boolean isDamage(String name) {
		return name.equals("sharpness") || name.equals("smite") || name.equals("bane");
	}

	private static void assertSymmetric(List<String> names, IncompatibilityMatrix<String> matrix) {
		for (String a : names) {
			assertFalse(matrix.areIncompatible(a, a), a);
			for (String b : names) {
				assertEquals(matrix.areIncompatible(a, b), matrix.areIncompatible(b, a), a + "/" + b);
			}
		}
	}
}
