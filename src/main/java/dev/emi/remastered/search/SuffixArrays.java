package dev.emi.remastered.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import dev.emi.emi.mixin.accessor.SuffixArrayAccessor;
import dev.emi.emi.runtime.EmiLog;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.search.SuffixArray;

/**
 * Builds EMI's search suffix arrays with the result of {@link SuffixArray#build()}: {@link SuffixSort} instead of
 * vanilla's sort, and the arrays at the same time (they share nothing). Client only.
 */
public final class SuffixArrays {

	private SuffixArrays() {
	}

	/** Builds every array once; the biggest on the calling thread, the others on their own threads. */
	public static void buildAll(SuffixArray<?>... arrays) {
		List<SuffixArray<?>> sorted = new ArrayList<>(Arrays.asList(arrays));
		sorted.sort(Comparator.comparingInt(SuffixArrays::size).reversed());
		List<Thread> threads = new ArrayList<>();
		for (int i = 1; i < sorted.size(); i++) {
			SuffixArray<?> array = sorted.get(i);
			Thread thread = new Thread(() -> build(array), "EMI Search Bake");
			thread.setDaemon(true);
			thread.start();
			threads.add(thread);
		}
		if (!sorted.isEmpty()) {
			build(sorted.get(0));
		}
		boolean interrupted = false;
		for (Thread thread : threads) {
			while (true) {
				try {
					thread.join();
					break;
				} catch (InterruptedException e) {
					// the arrays are published right after this, a cancelled reload is noticed by the caller
					interrupted = true;
				}
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** Vanilla's build with {@link SuffixSort}; vanilla's own build if the accessor is not there. */
	public static void build(SuffixArray<?> array) {
		int[] order;
		IntList objects;
		IntList offsets;
		SuffixArrayAccessor accessor;
		try {
			accessor = (SuffixArrayAccessor) array;
			objects = accessor.emi$getSuffixIndexToObjectIndex();
			offsets = accessor.emi$getOffsetInText();
			order = SuffixSort.order(accessor.emi$getCharacters().toIntArray(), accessor.emi$getMaxTextLength());
		} catch (RuntimeException | LinkageError e) {
			EmiLog.error("EMI could not use its fast search index build, using vanilla's", e);
			array.build();
			return;
		}
		int n = order.length;
		int[] sortedObjects = new int[n];
		int[] sortedOffsets = new int[n];
		for (int m = 0; m < n; m++) {
			int l = order[m];
			sortedObjects[m] = objects.getInt(l);
			sortedOffsets[m] = offsets.getInt(l);
		}
		accessor.emi$setSuffixIndexToObjectIndex(IntArrayList.wrap(sortedObjects));
		accessor.emi$setOffsetInText(IntArrayList.wrap(sortedOffsets));
	}

	private static int size(SuffixArray<?> array) {
		try {
			return ((SuffixArrayAccessor) array).emi$getCharacters().size();
		} catch (RuntimeException | LinkageError e) {
			return 0;
		}
	}
}
