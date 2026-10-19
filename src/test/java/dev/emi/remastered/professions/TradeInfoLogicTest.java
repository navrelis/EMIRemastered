package dev.emi.remastered.professions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.professions.trade.TradeIds;
import dev.emi.remastered.professions.trade.TradeIndex;
import dev.emi.remastered.professions.trade.TradeRows;

/** Pure logic behind the trade Item Info provider: index, row layout, trade order. */
class TradeInfoLogicTest {

	@Test
	void indexKeepsInsertionOrderAndIgnoresRepeatedInstance() {
		TradeIndex<String, String> index = new TradeIndex<>();
		String a = new String("a");
		String b = new String("b");
		index.add("emerald", a);
		index.add("emerald", a);
		index.add("emerald", b);
		index.add("wheat", a);
		assertEquals(List.of(a, b), index.get("emerald"));
		assertEquals(List.of(a), index.get("wheat"));
		assertEquals(2, index.keys());
		assertFalse(index.isEmpty());
	}

	@Test
	void indexMissingKeyIsEmptyNotNull() {
		TradeIndex<String, String> index = new TradeIndex<>();
		assertTrue(index.isEmpty());
		assertEquals(List.of(), index.get("nothing"));
	}

	@Test
	void fewTradesAreListedOneByOne() {
		assertTrue(TradeRows.detailed(1));
		assertTrue(TradeRows.detailed(TradeRows.DETAIL_LIMIT));
		assertFalse(TradeRows.detailed(TradeRows.DETAIL_LIMIT + 1));
	}

	@Test
	void groupsByOwnerInOrderOfFirstAppearance() {
		List<String> trades = List.of("farmer:1", "librarian:1", "farmer:2", "mason:3", "librarian:2");
		List<TradeRows.Group<String>> groups = TradeRows.groupByOwner(trades, t -> t.substring(0, t.indexOf(':')));
		assertEquals(3, groups.size());
		assertEquals("farmer", groups.get(0).owner());
		assertEquals(List.of("farmer:1", "farmer:2"), groups.get(0).trades());
		assertEquals("librarian", groups.get(1).owner());
		assertEquals(List.of("mason:3"), groups.get(2).trades());
	}

	@Test
	void distinctPicksOnePerTradeUpToTheLimit() {
		List<List<String>> trades = List.of(List.of("emerald"), List.of("emerald", "book"), List.of("bread"), List.of("apple"));
		assertEquals(List.of("emerald", "book", "bread"), TradeRows.distinct(trades, t -> t, 3));
		assertEquals(List.of("emerald", "book", "bread", "apple"), TradeRows.distinct(trades, t -> t, 10));
		assertEquals(List.of(), TradeRows.distinct(trades, t -> t, 0));
	}

	@Test
	void tradePathsParseBackIntoTheirParts() {
		TradeIds.Parsed farmer = TradeIds.parse(TradeIds.recipePath("farmer", 2, 11));
		assertEquals(new TradeIds.Parsed("farmer", 2, 11), farmer);
		assertEquals(new TradeIds.Parsed("modid/baker", 3, 1), TradeIds.parse(TradeIds.recipePath("modid/baker", 3, 1)));
		assertNull(TradeIds.parse("/profession/minecraft/farmer"));
		assertNull(TradeIds.parse("/trade/farmer/x/1"));
		assertNull(TradeIds.parse(null));
	}

	@Test
	void tradesAreOrderedByOwnerThenLevelThenNumericIndex() {
		List<String> paths = new ArrayList<>(List.of(
			TradeIds.recipePath("farmer", 1, 10), TradeIds.recipePath("farmer", 1, 2), TradeIds.recipePath("butcher", 5, 1),
			TradeIds.recipePath("farmer", 2, 1), "/odd/path"));
		paths.sort(TradeIds::compare);
		assertEquals(List.of(TradeIds.recipePath("butcher", 5, 1), TradeIds.recipePath("farmer", 1, 2),
			TradeIds.recipePath("farmer", 1, 10), TradeIds.recipePath("farmer", 2, 1), "/odd/path"), paths);
	}
}
