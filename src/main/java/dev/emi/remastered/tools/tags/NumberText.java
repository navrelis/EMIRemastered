package dev.emi.remastered.tools.tags;

import java.text.NumberFormat;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/** Numbers in the language of the game, not of the JVM. Pure logic. */
public final class NumberText {
	private NumberText() {
	}

	/**
	 * The locale of a Minecraft language code: {@code en_us} is {@code en-US}, {@code de_de} is {@code de-DE}.
	 * {@link Locale#ROOT} for a missing code or one that is no locale (some joke languages).
	 */
	public static Locale locale(@Nullable String languageCode) {
		if (languageCode == null || languageCode.isBlank()) {
			return Locale.ROOT;
		}
		Locale locale = Locale.forLanguageTag(languageCode.replace('_', '-'));
		return locale.getLanguage().isEmpty() ? Locale.ROOT : locale;
	}

	/** The number with the digit grouping of the locale: {@code 1,005} in English, {@code 1.005} in German. */
	public static String format(long number, Locale locale) {
		NumberFormat format = NumberFormat.getIntegerInstance(locale);
		format.setGroupingUsed(true);
		return format.format(number);
	}
}
