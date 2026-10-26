package dev.emi.remastered.info.sources;

import java.util.Locale;

/** Small pure helpers for the text of an extra source page. */
public final class SourceText {

	private SourceText() {
	}

	/** {@code 0.25} becomes {@code 25%}, {@code 0.025} becomes {@code 2.5%}, {@code 1/3} becomes {@code 33.3%}. */
	public static String percent(double chance) {
		double percent = chance * 100;
		if (Math.abs(percent - Math.rint(percent)) < 0.005) {
			return Math.round(percent) + "%";
		}
		return String.format(Locale.ROOT, "%.1f", percent) + "%";
	}
}
