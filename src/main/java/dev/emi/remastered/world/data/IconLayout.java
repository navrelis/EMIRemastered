package dev.emi.remastered.world.data;

/**
 * How many icons of a section fit: {@code rows} rows of {@code columns} cells; when there are more icons than cells the
 * last cell is taken by the "+N more" label.
 *
 * @param shown  icons drawn
 * @param hidden icons that did not fit
 * @param rows   rows used (0 for an empty section), the "+N" label included
 */
public record IconLayout(int shown, int hidden, int rows) {

	public static IconLayout of(int total, int rows, int columns) {
		if (total <= 0) {
			return new IconLayout(0, 0, 0);
		}
		int cells = Math.max(1, rows) * Math.max(1, columns);
		if (total <= cells) {
			return new IconLayout(total, 0, (total + columns - 1) / columns);
		}
		int shown = cells - 1;
		return new IconLayout(shown, total - shown, Math.max(1, rows));
	}
}
