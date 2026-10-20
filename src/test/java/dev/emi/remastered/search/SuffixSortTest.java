package dev.emi.remastered.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.search.SuffixArray;

/**
 * {@link SuffixSort} must give exactly the permutation of {@code SuffixArray.build()} (including the order of equal
 * keys, which long queries can observe), so an array built with it answers every query like vanilla.
 */
class SuffixSortTest {

	@Test
	void sameOrderAsVanillaOnRandomTexts() throws Exception {
		Random random = new Random(42);
		for (int round = 0; round < 60; round++) {
			int texts = 1 + random.nextInt(round < 30 ? 40 : 400);
			int alphabet = 1 + random.nextInt(round % 3 == 0 ? 2 : 6);
			int maxLength = round % 5 == 0 ? 3 : 1 + random.nextInt(120);
			List<String> list = new ArrayList<>();
			for (int t = 0; t < texts; t++) {
				if (!list.isEmpty() && random.nextInt(3) == 0) {
					list.add(list.get(random.nextInt(list.size()))); // repeated texts: many equal keys
					continue;
				}
				StringBuilder b = new StringBuilder();
				int length = random.nextInt(maxLength + 1);
				for (int c = 0; c < length; c++) {
					b.append((char) ('a' + random.nextInt(alphabet)));
				}
				list.add(b.toString());
			}
			compare(list, random);
		}
	}

	@Test
	void sameOrderAsVanillaOnTooltipLikeTexts() throws Exception {
		String[] lines = {"when in main hand:", " 7 attack damage", " 1.6 attack speed", "durability: 250 / 250",
			"minecraft:iron_sword", "", "a", "ab", "enchanted book", "sharpness v", "unbreaking iii",
			"a very long modded tooltip line that explains what this item does in far too many words, twice over and more"};
		Random random = new Random(7);
		List<String> list = new ArrayList<>();
		for (int t = 0; t < 3000; t++) {
			list.add(lines[random.nextInt(lines.length)]);
		}
		compare(list, random);
	}

	@Test
	void ownQuickSortMatchesFastutil() {
		assertTrue(SuffixSort.ownQuickSort());
	}

	@Test
	void distinctKeysAndLongTexts() throws Exception {
		Random random = new Random(11);
		List<String> list = new ArrayList<>();
		for (int t = 0; t < 500; t++) {
			StringBuilder b = new StringBuilder();
			int length = 1 + random.nextInt(300);
			for (int c = 0; c < length; c++) {
				b.append((char) (' ' + random.nextInt(90)));
			}
			list.add(b.toString());
		}
		compare(list, random);
	}

	@Test
	void degenerateInputs() throws Exception {
		Random random = new Random(1);
		compare(List.of(), random);
		compare(List.of(""), random);
		compare(List.of("a"), random);
		compare(List.of("ab", "ab"), random);
		compare(List.of("abc"), random);
		compare(List.of("abcd", "", "abcd"), random);
	}

	private static void compare(List<String> texts, Random random) throws Exception {
		SuffixArray<Integer> vanilla = new SuffixArray<>();
		SuffixArray<Integer> fast = new SuffixArray<>();
		for (int i = 0; i < texts.size(); i++) {
			vanilla.add(i, texts.get(i));
			fast.add(i, texts.get(i));
		}
		IntList objects = get(fast, "suffixIndexToObjectIndex");
		IntList offsets = get(fast, "offsetInText");
		int[] order = SuffixSort.order(((IntList) get(fast, "characters")).toIntArray(), (int) get(fast, "maxTextLength"));
		vanilla.build();

		int[] sortedObjects = new int[order.length];
		int[] sortedOffsets = new int[order.length];
		for (int m = 0; m < order.length; m++) {
			sortedObjects[m] = objects.getInt(order[m]);
			sortedOffsets[m] = offsets.getInt(order[m]);
		}
		assertArrayEquals(((IntList) get(vanilla, "suffixIndexToObjectIndex")).toIntArray(), sortedObjects);
		assertArrayEquals(((IntList) get(vanilla, "offsetInText")).toIntArray(), sortedOffsets);

		set(fast, "suffixIndexToObjectIndex", IntArrayList.wrap(sortedObjects));
		set(fast, "offsetInText", IntArrayList.wrap(sortedOffsets));
		for (int q = 0; q < 200 && !texts.isEmpty(); q++) {
			String text = texts.get(random.nextInt(texts.size()));
			int from = text.isEmpty() ? 0 : random.nextInt(text.length());
			int to = Math.min(text.length(), from + 1 + random.nextInt(140));
			String query = text.substring(from, to) + (q % 7 == 0 ? "a" : "");
			assertEquals(vanilla.findAll(query), fast.findAll(query), query);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(SuffixArray<?> array, String name) throws Exception {
		Field field = SuffixArray.class.getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(array);
	}

	private static void set(SuffixArray<?> array, String name, Object value) throws Exception {
		Field field = SuffixArray.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(array, value);
	}
}
