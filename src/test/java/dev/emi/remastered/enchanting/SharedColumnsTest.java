package dev.emi.remastered.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.enchanting.logic.SharedColumns;

class SharedColumnsTest {
	/** The column split of the catalog before the columns were shared: accepted elements round robin over the columns. */
	private static List<List<String>> reference(int size, int columns, IntPredicate accepts) {
		List<List<String>> result = new ArrayList<>();
		for (int i = 0; i < columns; i++) {
			result.add(new ArrayList<>());
		}
		int counter = 0;
		for (int i = 0; i < size; i++) {
			if (accepts.test(i)) {
				result.get(counter++ % columns).add("e" + i);
			}
		}
		return result;
	}

	private static BitSet bits(int size, IntPredicate accepts) {
		BitSet bits = new BitSet(size);
		for (int i = 0; i < size; i++) {
			if (accepts.test(i)) {
				bits.set(i);
			}
		}
		return bits;
	}

	@Test
	void sameColumnsAsTheUnsharedSplit() {
		Random random = new Random(7);
		SharedColumns<String> shared = new SharedColumns<>(8, i -> "e" + i);
		for (int run = 0; run < 50; run++) {
			int size = random.nextInt(300);
			double density = random.nextDouble();
			long seed = random.nextLong();
			IntPredicate accepts = i -> new Random(seed + i).nextDouble() < density;
			assertEquals(reference(size, 8, accepts), shared.columnsOf(bits(size, accepts)));
		}
	}

	@Test
	void equalSetsShareOneInstanceAndAskEachElementOnce() {
		int[] asked = new int[100];
		SharedColumns<String> shared = new SharedColumns<>(8, i -> {
			asked[i]++;
			return "e" + i;
		});
		List<List<String>> all = shared.columnsOf(bits(100, i -> true));
		BitSet reused = bits(100, i -> true);
		assertSame(all, shared.columnsOf(reused));
		reused.clear(3);
		List<List<String>> fewer = shared.columnsOf(reused);
		assertNotSame(all, fewer);
		assertSame(all, shared.columnsOf(bits(100, i -> true)), "the caller's set is not kept");
		assertEquals(2, shared.distinct());
		assertEquals(2, asked[0]);
		assertEquals(1, asked[3]);
	}

	@Test
	void emptyAndReadOnly() {
		SharedColumns<String> shared = new SharedColumns<>(8, i -> "e" + i);
		List<List<String>> none = shared.columnsOf(new BitSet());
		assertEquals(8, none.size());
		none.forEach(column -> assertEquals(List.of(), column));
		List<List<String>> some = shared.columnsOf(bits(20, i -> i % 3 == 0));
		assertEquals(List.of("e0"), some.get(0));
		assertEquals(List.of("e18"), some.get(6));
		assertEquals(List.of(), some.get(7));
		assertThrows(UnsupportedOperationException.class, () -> some.get(0).add("x"));
		assertThrows(UnsupportedOperationException.class, () -> some.add(List.of()));
		assertThrows(IllegalArgumentException.class, () -> new SharedColumns<String>(0, i -> ""));
	}
}
