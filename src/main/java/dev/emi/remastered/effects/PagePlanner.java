package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;

/** Splits a list of blocks (heights in pixels) into pages. Pure logic, unit tested. */
public final class PagePlanner {

	private PagePlanner() {
	}

	/**
	 * @param heights          height of every block
	 * @param keepWithNext     a block flagged here (a heading) is never the last block of a page
	 * @param capacity         total height of one page
	 * @param navHeight        height reserved at the bottom of every page when there is more than one page
	 * @param continuationHeight extra height used at the top of every page after the first
	 * @return the index of the first block of every page (at least {@code {0}})
	 */
	public static int[] plan(int[] heights, boolean[] keepWithNext, int capacity, int navHeight, int continuationHeight) {
		int n = heights.length;
		int total = 0;
		for (int h : heights) {
			total += h;
		}
		if (total <= capacity) {
			return new int[] {0};
		}
		int cap = capacity - navHeight;
		List<Integer> starts = new ArrayList<>();
		starts.add(0);
		int used = 0;
		int blocksOnPage = 0;
		for (int i = 0; i < n; i++) {
			// a heading carries the blocks after it up to the first non-heading block
			int need = heights[i];
			for (int j = i; keepWithNext[j] && j + 1 < n; j++) {
				need += heights[j + 1];
			}
			if (blocksOnPage > 0 && used + need > cap) {
				starts.add(i);
				used = continuationHeight;
				blocksOnPage = 0;
			}
			used += heights[i];
			blocksOnPage++;
		}
		int[] result = new int[starts.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = starts.get(i);
		}
		return result;
	}
}
