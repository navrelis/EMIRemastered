package dev.emi.remastered.stats.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Locale-aware number formatting of the {@code stats} module (pure, unit tested). The locale is the one of the
 * selected game language, never the JVM's default, so an English UI shows {@code 9.6} on a German system.
 */
public final class StatsNumbers {
	private static final String PATTERN = "#,##0.##";

	private StatsNumbers() {
	}

	/**
	 * Locale for a Minecraft language code: {@code en_us} gives {@code en-US}, {@code de_de} gives {@code de-DE}.
	 * Null, blank or unparseable codes give {@link Locale#ROOT}.
	 */
	public static Locale localeOf(String languageCode) {
		if (languageCode == null || languageCode.isBlank()) {
			return Locale.ROOT;
		}
		String tag = languageCode.trim().replace('_', '-');
		Locale locale = Locale.forLanguageTag(tag);
		return locale.getLanguage().isEmpty() ? Locale.ROOT : locale;
	}

	/** Number with at most two decimals and thousands grouping in the given locale. */
	public static String format(double value, Locale locale) {
		// DecimalFormat is not thread safe, and creating one is cheap for the few numbers of a page
		DecimalFormat format = new DecimalFormat(PATTERN, DecimalFormatSymbols.getInstance(locale));
		return format.format(value);
	}
}
