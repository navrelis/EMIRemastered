package dev.emi.remastered.info.providers.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.info.providers.advancement.AdvancementMatching.Candidate;

class AdvancementMatchingTest {
	private static Candidate<String, String> c(String id, String key, String title, boolean hidden, boolean done) {
		return new Candidate<>(id, key, title, hidden, done, id);
	}

	private static List<String> ids(List<Candidate<String, String>> list) {
		return list.stream().map(Candidate::id).toList();
	}

	@Test
	void visibilityFollowsTheAdvancementScreen() {
		assertTrue(AdvancementMatching.isVisible(false, false));
		assertTrue(AdvancementMatching.isVisible(false, true));
		assertTrue(AdvancementMatching.isVisible(true, true));
		assertFalse(AdvancementMatching.isVisible(true, false));
	}

	@Test
	void hiddenAndNotDoneIsExcluded() {
		Map<String, List<Candidate<String, String>>> index = AdvancementMatching.index(List.of(
			c("a:secret", "diamond", "Secret", true, false),
			c("a:open", "diamond", "Open", false, false)));
		assertEquals(List.of("a:open"), ids(index.get("diamond")));
	}

	@Test
	void hiddenAndDoneIsIncluded() {
		Map<String, List<Candidate<String, String>>> index = AdvancementMatching.index(List.of(
			c("a:secret", "diamond", "Secret", true, true)));
		assertEquals(List.of("a:secret"), ids(index.get("diamond")));
	}

	@Test
	void keyWithOnlyHiddenAdvancementsHasNoEntry() {
		Map<String, List<Candidate<String, String>>> index = AdvancementMatching.index(List.of(
			c("a:secret", "diamond", "Secret", true, false)));
		assertFalse(index.containsKey("diamond"));
	}

	@Test
	void groupsByKeyAndIgnoresNullKeys() {
		Map<String, List<Candidate<String, String>>> index = AdvancementMatching.index(List.of(
			c("a:one", "diamond", "One", false, false),
			c("a:two", "iron_ingot", "Two", false, true),
			c("a:none", null, "None", false, false)));
		assertEquals(2, index.size());
		assertEquals(List.of("a:one"), ids(index.get("diamond")));
		assertEquals(List.of("a:two"), ids(index.get("iron_ingot")));
	}

	@Test
	void openFirstThenDoneEachByTitleThenId() {
		Map<String, List<Candidate<String, String>>> index = AdvancementMatching.index(List.of(
			c("a:done_a", "x", "alpha", false, true),
			c("a:open_z", "x", "Zeta", false, false),
			c("a:open_b2", "x", "beta", false, false),
			c("a:open_b1", "x", "Beta", false, false),
			c("a:done_b", "x", "Bravo", false, true)));
		assertEquals(List.of("a:open_b1", "a:open_b2", "a:open_z", "a:done_a", "a:done_b"), ids(index.get("x")));
	}

	@Test
	void fingerprintChangesWithDoneStateAndVisibleSet() {
		List<Candidate<String, String>> before = List.of(
			c("a:one", "x", "One", false, false),
			c("a:secret", "x", "Secret", true, false));
		List<Candidate<String, String>> done = List.of(
			c("a:one", "x", "One", false, true),
			c("a:secret", "x", "Secret", true, false));
		List<Candidate<String, String>> revealed = List.of(
			c("a:one", "x", "One", false, false),
			c("a:secret", "x", "Secret", true, true));
		long base = AdvancementMatching.fingerprint(before);
		assertNotEquals(base, AdvancementMatching.fingerprint(done));
		assertNotEquals(base, AdvancementMatching.fingerprint(revealed));
	}

	@Test
	void fingerprintIgnoresOrderAndInvisibleOnes() {
		Candidate<String, String> one = c("a:one", "x", "One", false, false);
		Candidate<String, String> two = c("a:two", "x", "Two", false, true);
		Candidate<String, String> secret = c("a:secret", "x", "Secret", true, false);
		assertEquals(AdvancementMatching.fingerprint(List.of(one, two)), AdvancementMatching.fingerprint(List.of(two, one)));
		assertEquals(AdvancementMatching.fingerprint(List.of(one, two)), AdvancementMatching.fingerprint(List.of(one, secret, two)));
	}

	@Test
	void emptyInputGivesEmptyIndex() {
		assertTrue(AdvancementMatching.index(List.<Candidate<String, String>>of()).isEmpty());
		assertEquals(0, AdvancementMatching.fingerprint(List.<Candidate<String, String>>of()));
	}
}
