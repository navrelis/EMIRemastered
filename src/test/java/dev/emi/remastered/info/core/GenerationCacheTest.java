package dev.emi.remastered.info.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class GenerationCacheTest {

	@Test
	void computesOncePerKeyAndGeneration() {
		GenerationCache<String, String> cache = new GenerationCache<>(8);
		AtomicInteger calls = new AtomicInteger();
		String first = cache.get(1, "iron", k -> k + calls.incrementAndGet());
		assertEquals("iron1", first);
		assertSame(first, cache.get(1, "iron", k -> k + calls.incrementAndGet()));
		assertEquals(1, calls.get());
		assertEquals("gold2", cache.get(1, "gold", k -> k + calls.incrementAndGet()));
		assertEquals(2, cache.size());
	}

	@Test
	void newGenerationInvalidatesEverything() {
		GenerationCache<String, String> cache = new GenerationCache<>(8);
		AtomicInteger calls = new AtomicInteger();
		cache.get(1, "iron", k -> k + calls.incrementAndGet());
		cache.get(1, "gold", k -> k + calls.incrementAndGet());
		assertEquals("iron1", cache.peek(1, "iron"));
		assertNull(cache.peek(2, "iron"));
		assertEquals("iron3", cache.get(2, "iron", k -> k + calls.incrementAndGet()));
		assertEquals(1, cache.size());
		assertNull(cache.peek(2, "gold"));
		assertEquals(2, cache.generation());
	}

	@Test
	void invalidateAndNoCache() {
		GenerationCache<String, String> cache = new GenerationCache<>(8);
		AtomicInteger calls = new AtomicInteger();
		cache.get(5, "iron", k -> k + calls.incrementAndGet());
		cache.invalidate();
		assertEquals(0, cache.size());
		assertEquals("iron2", cache.get(5, "iron", k -> k + calls.incrementAndGet()));
		assertEquals("iron3", cache.get(GenerationCache.NO_CACHE, "iron", k -> k + calls.incrementAndGet()));
		assertEquals("iron4", cache.get(GenerationCache.NO_CACHE, "iron", k -> k + calls.incrementAndGet()));
		assertEquals(1, cache.size());
		assertNull(cache.get(5, "null", k -> null));
		assertEquals(1, cache.size());
	}

	@Test
	void evictsLeastRecentlyUsed() {
		GenerationCache<String, String> cache = new GenerationCache<>(2);
		cache.get(1, "a", k -> k);
		cache.get(1, "b", k -> k);
		cache.get(1, "a", k -> "recomputed");
		cache.get(1, "c", k -> k);
		assertEquals("a", cache.peek(1, "a"));
		assertNull(cache.peek(1, "b"));
		assertEquals("c", cache.peek(1, "c"));
		assertThrows(IllegalArgumentException.class, () -> new GenerationCache<>(0));
	}

	@Test
	void reloadGenerationTracksIdentityAndValueStamps() {
		ReloadGeneration generation = new ReloadGeneration();
		Object managerA = new Object();
		Object managerB = new Object();
		long g1 = generation.update(managerA, List.of(1, true));
		assertTrue(generation.wasReloaded(), "first call counts as a reload");
		assertEquals(g1, generation.update(managerA, List.of(1, true)));
		assertFalse(generation.wasReloaded());
		long g2 = generation.update(managerA, List.of(2, true));
		assertTrue(g2 > g1, "registry change -> new generation");
		assertFalse(generation.wasReloaded(), "but not an EMI reload");
		long g3 = generation.update(managerB, List.of(2, true));
		assertTrue(g3 > g2);
		assertTrue(generation.wasReloaded(), "new recipe manager -> EMI reload");
		assertEquals(g3, generation.update(managerB, List.of(2, true)));
	}

	@Test
	void cacheFollowsReloadGeneration() {
		ReloadGeneration generation = new ReloadGeneration();
		GenerationCache<String, String> cache = new GenerationCache<>(8);
		AtomicInteger calls = new AtomicInteger();
		Object manager = new Object();
		cache.get(generation.update(manager, 0), "iron", k -> k + calls.incrementAndGet());
		cache.get(generation.update(manager, 0), "iron", k -> k + calls.incrementAndGet());
		assertEquals(1, calls.get());
		cache.get(generation.update(new Object(), 0), "iron", k -> k + calls.incrementAndGet());
		assertEquals(2, calls.get(), "EMI reload recomputes");
		cache.get(generation.update(manager, 1), "iron", k -> k + calls.incrementAndGet());
		assertEquals(3, calls.get(), "settings/registry change recomputes");
	}
}
