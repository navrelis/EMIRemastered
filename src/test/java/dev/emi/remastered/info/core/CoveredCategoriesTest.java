package dev.emi.remastered.info.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CoveredCategoriesTest {
	private static final Map<String, Set<String>> COVERS = Map.of(
		"trades", Set.of("villager_trades", "wandering_trader"),
		"effects", Set.of("status_effect_info"),
		"loot", Set.of());

	@Test
	void unionOfActiveProviders() {
		Set<String> covered = CoveredCategories.compute(List.of("trades", "effects", "loot"), p -> true, COVERS::get);
		assertEquals(Set.of("villager_trades", "wandering_trader", "status_effect_info"), covered);
	}

	@Test
	void inactiveProvidersCoverNothing() {
		Set<String> covered = CoveredCategories.compute(List.of("trades", "effects"), p -> p.equals("effects"), COVERS::get);
		assertEquals(Set.of("status_effect_info"), covered);
		assertTrue(CoveredCategories.compute(List.of("trades"), p -> false, COVERS::get).isEmpty());
	}

	@Test
	void failingOrNullProviderIsIgnored() {
		Set<String> covered = CoveredCategories.compute(List.of("boom", "null", "effects"), p -> true, p -> {
			if (p.equals("boom")) {
				throw new IllegalStateException();
			}
			return p.equals("null") ? null : COVERS.get(p);
		});
		assertEquals(Set.of("status_effect_info"), covered);
	}

	@Test
	void noProvidersCoverNothing() {
		assertTrue(CoveredCategories.<String, String>compute(List.of(), p -> true, COVERS::get).isEmpty());
	}
}
