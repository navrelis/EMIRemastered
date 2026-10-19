package dev.emi.remastered.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.loot.structure.LateDelivery;

/** Bookkeeping of the players that wait for the late structure lookup. */
class LateDeliveryTest {

	@Test
	void firstKeyOfAGenerationArmsTheCallbackOnce() {
		LateDelivery<String> waiting = new LateDelivery<>();
		assertTrue(waiting.add(1, "alex"));
		assertFalse(waiting.add(1, "steve"));
		assertFalse(waiting.add(1, "alex"));
		assertEquals(List.of("alex", "steve"), waiting.take(1));
		assertEquals(List.of(), waiting.take(1));
	}

	@Test
	void aNewGenerationDropsTheOldKeysAndArmsAgain() {
		LateDelivery<String> waiting = new LateDelivery<>();
		assertTrue(waiting.add(1, "alex"));
		// a reload: the callback of generation 1 is dropped, generation 2 needs its own
		assertTrue(waiting.add(2, "steve"));
		assertEquals(List.of(), waiting.take(1));
		assertEquals(List.of("steve"), waiting.take(2));
	}

	@Test
	void aKeyAddedAfterTheDeliveryArmsAgain() {
		LateDelivery<String> waiting = new LateDelivery<>();
		assertTrue(waiting.add(3, "alex"));
		assertEquals(List.of("alex"), waiting.take(3));
		assertTrue(waiting.add(3, "steve"));
		assertEquals(List.of("steve"), waiting.take(3));
	}

	@Test
	void removedAndClearedKeysGetNothing() {
		LateDelivery<String> waiting = new LateDelivery<>();
		waiting.add(1, "alex");
		waiting.add(1, "steve");
		// alex rejoined after the lookup was done and got everything with the chest payloads
		waiting.remove("alex");
		assertEquals(1, waiting.size());
		assertEquals(List.of("steve"), waiting.take(1));
		waiting.add(4, "alex");
		waiting.clear();
		assertEquals(List.of(), waiting.take(4));
		assertTrue(waiting.add(4, "alex"));
	}
}
