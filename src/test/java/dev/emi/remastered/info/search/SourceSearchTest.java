package dev.emi.remastered.info.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SourceSearchTest {
	private static final String CHEST = SourceText.join(List.of("Chest Loot", "emi_loot:chest_loot", "Stronghold Library", "Overworld"));

	private static boolean plain(String labels, boolean noSource, String query) {
		return SourceMatch.matches(labels, noSource, SourceText.normalize(query), SourceText.isNoSource(query));
	}

	@Test
	void normalizeLowersAndTrims() {
		assertEquals("stronghold library", SourceText.normalize("  Stronghold Library "));
		assertEquals("", SourceText.normalize(null));
		assertEquals("i", SourceText.normalize("I"), "no locale specific lower casing (Turkish dotless i)");
	}

	@Test
	void joinDeduplicatesKeepsOrderAndDropsBlanks() {
		assertEquals("a\nb\nc", SourceText.join(List.of("A", "b", "a", " ", "", "C")));
		assertEquals("", SourceText.join(List.of()));
		Set<String> set = new LinkedHashSet<>();
		SourceText.add(set, "  ");
		SourceText.add(set, null);
		SourceText.add(set, "Cleric");
		assertEquals(Set.of("cleric"), set);
	}

	@Test
	void spacedTurnsPathsIntoWords() {
		assertEquals("chests stronghold library", SourceText.spaced("chests/stronghold_library"));
		assertEquals("entities zombie", SourceText.spaced("entities/zombie"));
	}

	@Test
	void substringMatchesAnyLabelCaseInsensitively() {
		assertTrue(plain(CHEST, false, "stronghold"));
		assertTrue(plain(CHEST, false, "STRONGHOLD"));
		assertTrue(plain(CHEST, false, "chest"));
		assertTrue(plain(CHEST, false, "emi_loot:chest_loot"));
		assertTrue(plain(CHEST, false, "library"));
		assertFalse(plain(CHEST, false, "desert"));
	}

	@Test
	void queryNeverMatchesAcrossTwoLabels() {
		assertFalse(plain(CHEST, false, "loot stronghold"), "a space is not the label separator, and labels do not run together");
		assertFalse(plain("ab\ncd", false, "bc"));
		assertFalse(plain("chest loot\nstronghold", false, "lootstronghold"));
	}

	@Test
	void emptyQueryMatchesStacksWithAnySource() {
		assertTrue(plain(CHEST, false, ""));
		assertFalse(plain("", true, ""));
	}

	@Test
	void noSourceKeywordIsExactAndIgnoresLabels() {
		assertTrue(SourceText.isNoSource("nosource"));
		assertTrue(SourceText.isNoSource("NoSource"));
		assertFalse(SourceText.isNoSource("nosources"));
		assertFalse(SourceText.isNoSource("no source"));
		assertTrue(plain("", true, "nosource"));
		assertFalse(plain(CHEST, false, "nosource"));
		assertFalse(plain("nosource", false, "nosource"), "a label called nosource does not make a stack a no-source stack");
	}

	@Test
	void regexMatchesLabelsAndInvalidPatternMatchesNothing() {
		Pattern village = SourceMatch.compile("village|desert");
		assertNotNull(village);
		assertTrue(SourceMatch.matchesRegex(village, SourceText.join(List.of("Chest Loot", "Desert Pyramid"))));
		assertTrue(SourceMatch.matchesRegex(village, SourceText.join(List.of("Village Weaponsmith"))));
		assertFalse(SourceMatch.matchesRegex(village, CHEST));
		assertNull(SourceMatch.compile("(unclosed"));
		assertFalse(SourceMatch.matchesRegex(null, CHEST));
		Pattern anchored = SourceMatch.compile("^overworld$");
		assertFalse(SourceMatch.matchesRegex(anchored, CHEST), "anchors work per label only with (?m); the joined string is one input");
		assertTrue(SourceMatch.matchesRegex(SourceMatch.compile("(?m)^overworld$"), CHEST));
	}

	@Test
	void noSourceRuleMirrorsTheItemInfoRecipeProvider() {
		// category, produces, consumes
		assertTrue(SourceMatch.countsAsSource(false, true, false));
		assertFalse(SourceMatch.countsAsSource(true, true, false), "info, tag, fuel pages are no source");
		assertFalse(SourceMatch.countsAsSource(false, false, false), "the stack is not an output");
		assertFalse(SourceMatch.countsAsSource(false, true, true), "repairing, cloning, dyeing is no source");
	}

	@Test
	void noSourceDecisionOnFakeRecipes() {
		record Fake(boolean excluded, boolean produces, boolean consumes, String label) {
		}
		List<Fake> none = List.of(new Fake(true, true, false, "Information"), new Fake(false, true, true, "Repair"));
		List<Fake> some = List.of(new Fake(true, true, false, "Information"), new Fake(false, true, false, "Smelting"));
		assertFalse(none.stream().anyMatch(r -> SourceMatch.countsAsSource(r.excluded(), r.produces(), r.consumes())));
		assertTrue(some.stream().anyMatch(r -> SourceMatch.countsAsSource(r.excluded(), r.produces(), r.consumes())));
	}
}
