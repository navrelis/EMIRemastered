package dev.emi.remastered.info.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProviderOrderingTest {
	private record P(String id, int order) {
	}

	private record S(String name, int kind) {
	}

	@Test
	void sortsByOrderThenId() {
		P recipes = new P("emi_info:recipes", 0);
		P loot = new P("emi_loot:chest", 1000);
		P fuel = new P("emi_info:fuel", 990);
		P ores = new P("emi_ores:ores", 1000);
		P last = new P("emi_info:no_source", Integer.MAX_VALUE);
		P first = new P("x:negative", Integer.MIN_VALUE);
		List<P> sorted = ProviderOrdering.sorted(List.of(last, loot, ores, recipes, first, fuel), P::order, P::id);
		assertEquals(List.of(first, recipes, fuel, loot, ores, last), sorted);
	}

	@Test
	void sectionsGroupByKindAndKeepCollectionOrder() {
		S a = new S("recipes", 0);
		S b = new S("used_in", 1);
		S c = new S("fuel", 2);
		S d = new S("loot", 0);
		S e = new S("no_source", 0);
		assertEquals(List.of(a, d, e, b, c), ProviderOrdering.byKind(List.of(a, b, c, d, e), S::kind));
	}

	@Test
	void isolateCatchesProviderFailures() {
		assertNull(ProviderOrdering.isolate(() -> { }));
		assertInstanceOf(IllegalStateException.class, ProviderOrdering.isolate(() -> {
			throw new IllegalStateException("boom");
		}));
		assertInstanceOf(NoClassDefFoundError.class, ProviderOrdering.isolate(() -> {
			throw new NoClassDefFoundError("some/missing/Mod");
		}));
		assertThrows(OutOfMemoryError.class, () -> ProviderOrdering.isolate(() -> {
			throw new OutOfMemoryError("fake");
		}));
	}
}
