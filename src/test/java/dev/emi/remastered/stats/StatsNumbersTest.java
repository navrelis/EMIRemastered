package dev.emi.remastered.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.stats.core.StatsNumbers;

class StatsNumbersTest {

	@Test
	void languageCodesMapToLocales() {
		assertEquals(Locale.forLanguageTag("en-US"), StatsNumbers.localeOf("en_us"));
		assertEquals(Locale.forLanguageTag("de-DE"), StatsNumbers.localeOf("de_de"));
		assertEquals(Locale.forLanguageTag("de-AT"), StatsNumbers.localeOf("de_at"));
		assertEquals(Locale.forLanguageTag("zh-CN"), StatsNumbers.localeOf("zh_cn"));
		assertEquals(Locale.forLanguageTag("en-GB"), StatsNumbers.localeOf(" en_gb "));
	}

	@Test
	void missingOrUnparseableCodesFallBackToRoot() {
		assertEquals(Locale.ROOT, StatsNumbers.localeOf(null));
		assertEquals(Locale.ROOT, StatsNumbers.localeOf(""));
		assertEquals(Locale.ROOT, StatsNumbers.localeOf("  "));
		assertEquals(Locale.ROOT, StatsNumbers.localeOf("???"));
	}

	@Test
	void englishUsesPointAndCommaGrouping() {
		Locale en = StatsNumbers.localeOf("en_us");
		assertEquals("9.6", StatsNumbers.format(9.6, en));
		assertEquals("1.6", StatsNumbers.format(1.6, en));
		assertEquals("1,561", StatsNumbers.format(1561, en));
		assertEquals("11.2", StatsNumbers.format(11.2, en));
	}

	@Test
	void germanUsesCommaAndPointGrouping() {
		Locale de = StatsNumbers.localeOf("de_de");
		assertEquals("9,6", StatsNumbers.format(9.6, de));
		assertEquals("1.561", StatsNumbers.format(1561, de));
		assertEquals("0,83", StatsNumbers.format(0.83, de));
	}

	@Test
	void formattingIgnoresTheJvmDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertEquals("9.6", StatsNumbers.format(9.6, StatsNumbers.localeOf("en_us")));
			assertEquals("9.6", StatsNumbers.format(9.6, Locale.ROOT));
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void atMostTwoDecimalsAndNoTrailingZeros() {
		Locale en = StatsNumbers.localeOf("en_us");
		assertEquals("2", StatsNumbers.format(2.0, en));
		assertEquals("0.83", StatsNumbers.format(0.8333333, en));
		assertEquals("0", StatsNumbers.format(0.0, en));
		assertEquals("100", StatsNumbers.format(100.0, en));
	}
}
