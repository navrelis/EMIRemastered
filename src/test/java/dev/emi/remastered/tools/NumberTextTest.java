package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.tools.tags.NumberText;

class NumberTextTest {
	@Test
	void gameLanguageCodesBecomeLocales() {
		assertEquals(Locale.forLanguageTag("en-US"), NumberText.locale("en_us"));
		assertEquals(Locale.forLanguageTag("de-DE"), NumberText.locale("de_de"));
	}

	@Test
	void unknownCodesFallBackToRoot() {
		assertEquals(Locale.ROOT, NumberText.locale(null));
		assertEquals(Locale.ROOT, NumberText.locale(""));
		assertEquals(Locale.ROOT, NumberText.locale("123"));
		// a joke language is a well-formed tag with no number data, it formats like the root locale
		assertEquals("1,005", NumberText.format(1005, NumberText.locale("enws")));
	}

	@Test
	void englishGroupsWithCommasGermanWithDots() {
		assertEquals("1,005", NumberText.format(1005, NumberText.locale("en_us")));
		assertEquals("1.005", NumberText.format(1005, NumberText.locale("de_de")));
		assertEquals("1,005", NumberText.format(1005, Locale.ROOT));
	}

	@Test
	void smallNumbersAreUnchanged() {
		assertEquals("0", NumberText.format(0, NumberText.locale("en_us")));
		assertEquals("999", NumberText.format(999, NumberText.locale("de_de")));
	}
}
