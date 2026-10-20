package dev.emi.remastered.search;

import java.util.Random;

import it.unimi.dsi.fastutil.Arrays;

/**
 * The suffix order of Minecraft's {@code SuffixArray.build}, computed faster with the exact same result.
 * <p>
 * Vanilla sorts by prefix doubling and runs a comparison quicksort (fastutil's {@code Arrays.quickSort}) in every
 * round. The order of suffixes with equal keys (same prefix of the sorted length) after a round depends on that
 * quicksort, but the ranks a round produces do not: the rank of a suffix is the number of suffixes with a smaller key,
 * whatever the sort. So every round uses a linear radix sort for the ranks, and the order of the last round comes from
 * the same quicksort on keys that compare exactly like vanilla's (the ranks of the last round's key pairs), which makes
 * the same comparisons and swaps and therefore gives vanilla's permutation, including its order of equal keys (which a
 * query longer than the sorted prefix can observe). When all keys are distinct, any sort gives that permutation.
 * Pure, thread safe.
 */
public final class SuffixSort {
	private static final int END = -2;
	private static final int QUICKSORT_NO_REC = 16;
	private static final int QUICKSORT_MEDIAN_OF_9 = 128;
	/** Whether {@link #quickSort} makes the same moves as the fastutil version on the class path (checked once). */
	private static final boolean OWN_QUICKSORT = selfCheck();

	private SuffixSort() {
	}

	/**
	 * @param characters    the characters of all texts, each text followed by {@code -1} (vanilla's {@code characters})
	 * @param maxTextLength the length of the longest text
	 * @return vanilla's suffix permutation: entry {@code m} is the position of the {@code m}-th suffix in sorted order
	 */
	public static int[] order(int[] characters, int maxTextLength) {
		int n = characters.length;
		int[] ls = new int[n];
		int k = Math.min(n, maxTextLength);
		int last = 0;
		for (int j = 1; j * 2 < k; j *= 2) {
			last = j;
		}
		if (last == 0) {
			// no round: vanilla leaves the permutation all zeros
			return ls;
		}
		int[] rank = characters.clone();
		int[] old = new int[n];
		int[] sa = new int[n];
		int[] order = new int[n];
		boolean distinct = firstRound(rank, old, sa, order);
		for (int j = 2; j <= last && !distinct; j *= 2) {
			distinct = round(rank, old, sa, order, j);
		}
		if (distinct) {
			// later rounds keep these ranks, and distinct keys have one sorted order
			for (int l = 0; l < n; l++) {
				ls[rank[l]] = l;
			}
			return ls;
		}
		for (int l = 0; l < n; l++) {
			ls[l] = l;
		}
		sort(rank, ls);
		return ls;
	}

	/**
	 * Round 1 on the characters: sorts by (char[l], char[l + 1] or END) with two stable counting sorts, leaves the
	 * order in {@code sa} and vanilla's ranks (position of the first suffix with the same key) in {@code rank}.
	 * Returns whether all ranks are distinct.
	 */
	private static boolean firstRound(int[] rank, int[] old, int[] sa, int[] order) {
		int n = rank.length;
		int min = END;
		int max = END;
		for (int l = 0; l < n; l++) {
			min = Math.min(min, rank[l]);
			max = Math.max(max, rank[l]);
		}
		int[] count = new int[max - min + 2];
		for (int l = 0; l < n; l++) {
			count[next(rank, l, 1) - min + 1]++;
		}
		for (int c = 1; c < count.length; c++) {
			count[c] += count[c - 1];
		}
		for (int l = 0; l < n; l++) {
			order[count[next(rank, l, 1) - min]++] = l;
		}
		java.util.Arrays.fill(count, 0);
		for (int l = 0; l < n; l++) {
			count[rank[l] - min + 1]++;
		}
		for (int c = 1; c < count.length; c++) {
			count[c] += count[c - 1];
		}
		for (int t = 0; t < n; t++) {
			int l = order[t];
			sa[count[rank[l] - min]++] = l;
		}
		return rerank(rank, old, sa, 1);
	}

	/**
	 * A later round: {@code sa} is sorted by {@code rank}, and a rank is the position of its group in {@code sa}. The
	 * suffixes sorted by their second key (rank[l + j], END first) are read off {@code sa}; a stable distribution by
	 * the first key (each group starts at its rank) then sorts by (rank[l], rank[l + j]).
	 */
	private static boolean round(int[] rank, int[] old, int[] sa, int[] order, int j) {
		int n = rank.length;
		int t = 0;
		for (int l = Math.max(0, n - j); l < n; l++) {
			order[t++] = l;
		}
		for (int m = 0; m < n; m++) {
			int p = sa[m] - j;
			if (p >= 0) {
				order[t++] = p;
			}
		}
		// old serves as the write pointer of each group (a group starts at its rank)
		for (int l = 0; l < n; l++) {
			old[l] = l;
		}
		for (t = 0; t < n; t++) {
			int l = order[t];
			sa[old[rank[l]]++] = l;
		}
		return rerank(rank, old, sa, j);
	}

	/** Vanilla's new ranks for the order in {@code sa} by (rank[l], rank[l + j]); returns whether all are distinct. */
	private static boolean rerank(int[] rank, int[] old, int[] sa, int j) {
		int n = rank.length;
		System.arraycopy(rank, 0, old, 0, n);
		boolean distinct = true;
		for (int m = 0; m < n; m++) {
			int l = sa[m];
			if (m > 0) {
				int p = sa[m - 1];
				if (old[l] == old[p] && next(old, l, j) == next(old, p, j)) {
					rank[l] = rank[p];
					distinct = false;
					continue;
				}
			}
			rank[l] = m;
		}
		return distinct;
	}

	private static int next(int[] rank, int l, int j) {
		return l + j < rank.length ? rank[l + j] : END;
	}

	/** Vanilla's last sort: fastutil's quicksort by key, moving the permutation along. */
	static void sort(int[] key, int[] perm) {
		if (OWN_QUICKSORT) {
			quickSort(key, perm, 0, key.length);
		} else {
			librarySort(key, perm);
		}
	}

	private static void librarySort(int[] key, int[] perm) {
		Arrays.quickSort(0, key.length, (a, b) -> Integer.compare(key[a], key[b]), (a, b) -> swap(key, perm, a, b));
	}

	/*
	 * The quicksort below makes exactly the comparisons and swaps of it.unimi.dsi.fastutil.Arrays.quickSort(int, int,
	 * IntComparator, Swapper) of fastutil 8.5, specialised to an int key array (no comparator/swapper calls).
	 * fastutil: Copyright (C) 2002-2023 Sebastiano Vigna, Apache License 2.0. selfCheck() compares both at runtime.
	 */
	private static void quickSort(int[] key, int[] perm, int from, int to) {
		final int len = to - from;
		if (len < QUICKSORT_NO_REC) {
			for (int i = from; i < to; i++) {
				for (int j = i; j > from && key[j - 1] > key[j]; j--) {
					swap(key, perm, j, j - 1);
				}
			}
			return;
		}
		int m = from + len / 2;
		int l = from;
		int n = to - 1;
		if (len > QUICKSORT_MEDIAN_OF_9) {
			final int s = len / 8;
			l = med3(key, l, l + s, l + 2 * s);
			m = med3(key, m - s, m, m + s);
			n = med3(key, n - 2 * s, n - s, n);
		}
		m = med3(key, l, m, n);
		int a = from;
		int b = a;
		int c = to - 1;
		int d = c;
		while (true) {
			int comparison;
			while (b <= c && (comparison = Integer.compare(key[b], key[m])) <= 0) {
				if (comparison == 0) {
					if (a == m) {
						m = b;
					} else if (b == m) {
						m = a;
					}
					swap(key, perm, a++, b);
				}
				b++;
			}
			while (c >= b && (comparison = Integer.compare(key[c], key[m])) >= 0) {
				if (comparison == 0) {
					if (c == m) {
						m = d;
					} else if (d == m) {
						m = c;
					}
					swap(key, perm, c, d--);
				}
				c--;
			}
			if (b > c) {
				break;
			}
			if (b == m) {
				m = d;
			} else if (c == m) {
				m = c;
			}
			swap(key, perm, b++, c--);
		}
		int s = Math.min(a - from, b - a);
		vecSwap(key, perm, from, b - s, s);
		s = Math.min(d - c, to - d - 1);
		vecSwap(key, perm, b, to - s, s);
		if ((s = b - a) > 1) {
			quickSort(key, perm, from, from + s);
		}
		if ((s = d - c) > 1) {
			quickSort(key, perm, to - s, to);
		}
	}

	private static int med3(int[] key, int a, int b, int c) {
		int ab = Integer.compare(key[a], key[b]);
		int ac = Integer.compare(key[a], key[c]);
		int bc = Integer.compare(key[b], key[c]);
		return ab < 0 ? (bc < 0 ? b : ac < 0 ? c : a) : (bc > 0 ? b : ac > 0 ? c : a);
	}

	private static void vecSwap(int[] key, int[] perm, int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++) {
			swap(key, perm, a, b);
		}
	}

	private static void swap(int[] key, int[] perm, int a, int b) {
		int t = key[a];
		key[a] = key[b];
		key[b] = t;
		t = perm[a];
		perm[a] = perm[b];
		perm[b] = t;
	}

	/** Sorts tie-heavy keys with both quicksorts; any difference means the library's algorithm changed. */
	private static boolean selfCheck() {
		try {
			Random random = new Random(0x5EA4C4L);
			for (int size : new int[] {7, 40, 300, 5000, 20000}) {
				for (int range : new int[] {1, 3, 50, size}) {
					int[] key = new int[size];
					for (int i = 0; i < size; i++) {
						key[i] = random.nextInt(range);
					}
					int[] ownKey = key.clone();
					int[] own = identity(size);
					int[] lib = identity(size);
					quickSort(ownKey, own, 0, size);
					librarySort(key, lib);
					if (!java.util.Arrays.equals(own, lib)) {
						return false;
					}
				}
			}
			return true;
		} catch (RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static int[] identity(int size) {
		int[] array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = i;
		}
		return array;
	}

	/** Whether the specialised quicksort is in use (it matched the library's on this class path). */
	static boolean ownQuickSort() {
		return OWN_QUICKSORT;
	}
}
