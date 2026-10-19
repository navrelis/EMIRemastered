package dev.emi.remastered.professions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.professions.trade.TradeBucket;
import dev.emi.remastered.professions.trade.TradeIds;
import dev.emi.remastered.professions.trade.TradeKey;
import dev.emi.remastered.professions.trade.VillagerApiPlans;

class TradeLogicTest {

	@Test
	void recipeIdsAreSyntheticAndStable() {
		assertEquals("/trade/farmer/1/1", TradeIds.recipePath(TradeIds.ownerKey("minecraft", "farmer"), 1, 1));
		assertEquals("/trade/modid/baker/3/12", TradeIds.recipePath(TradeIds.ownerKey("modid", "baker"), 3, 12));
		assertEquals("/trade/wandering_trader/2/1", TradeIds.recipePath(TradeIds.WANDERING_TRADER, 2, 1));
		assertEquals("/profession/minecraft/armorer", TradeIds.professionPath("minecraft", "armorer"));
		assertEquals("/profession/my_mod/a/b", TradeIds.professionPath("my_mod", "a/b"));
	}

	@Test
	void ownerKeysOnlyContainIdentifierCharacters() {
		assertEquals("a_b/c", TradeIds.sanitize("A B/c"));
		assertTrue(TradeIds.ownerKey("some:ns", "Weird Name").matches("[a-z0-9/._-]+"));
	}

	@Test
	void bucketMergesEqualTradesAndKeepsOrder() {
		TradeBucket<String> bucket = new TradeBucket<>();
		assertTrue(bucket.add(new TradeKey("emerald:1", null, "book:mending"), "a", "Plains"));
		assertTrue(bucket.add(new TradeKey("emerald:5", "book", "book:unbreaking"), "b", "Plains"));
		assertFalse(bucket.add(new TradeKey("emerald:1", null, "book:mending"), "dup", "Desert"));
		assertFalse(bucket.add(new TradeKey("emerald:1", null, "book:mending"), "dup", "Desert"));
		assertEquals(2, bucket.size());
		List<TradeBucket.Entry<String>> entries = bucket.entries();
		assertEquals("a", entries.get(0).value());
		assertEquals(List.of("Plains", "Desert"), entries.get(0).tags());
		assertEquals("b", entries.get(1).value());
		assertEquals(List.of("Plains"), entries.get(1).tags());
	}

	@Test
	void bucketDistinguishesSecondInputAndNullTags() {
		TradeBucket<Integer> bucket = new TradeBucket<>();
		assertTrue(bucket.add(new TradeKey("a", null, "x"), 1, null));
		assertTrue(bucket.add(new TradeKey("a", "b", "x"), 2, null));
		assertEquals(2, bucket.size());
		assertEquals(List.of(), bucket.entries().get(0).tags());
	}

	@Test
	void villagerApiPlanIsReadByReflection() {
		Object plan = new StubPlan(false, Set.of(), List.of(
			new StubGroup(List.of(new StubWeighted("listingA", 3), new StubWeighted("listingB", 1)), 1),
			new StubGroup(List.of(new StubWeighted("listingC", 2)), null)), null);
		VillagerApiPlans.Plan read = VillagerApiPlans.read(plan);
		assertNotNull(read);
		assertFalse(read.includeOriginal());
		assertEquals(List.of("listingA", "listingB", "listingC"), read.listings());
	}

	@Test
	void villagerApiPlanOfWrongShapeIsIgnored() {
		assertNull(VillagerApiPlans.read(null));
		assertNull(VillagerApiPlans.read("not a plan"));
	}

	public record StubPlan(boolean includeOriginal, Set<String> excludedVariants, List<StubGroup> groups, Integer maxTrades) {
	}

	public record StubGroup(List<StubWeighted> listings, Integer amount) {
	}

	public record StubWeighted(Object listing, int weight) {
	}
}
