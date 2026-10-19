package dev.emi.remastered.distractionfree;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScreenRulesTest {
	@Test
	void eachKindFollowsItsOwnSwitch() {
		assertTrue(ScreenRules.applies(ScreenKind.INVENTORY, true, false, false, true));
		assertFalse(ScreenRules.applies(ScreenKind.INVENTORY, false, true, true, true));
		assertTrue(ScreenRules.applies(ScreenKind.CRAFTING, false, true, false, true));
		assertFalse(ScreenRules.applies(ScreenKind.CRAFTING, true, false, true, true));
		assertTrue(ScreenRules.applies(ScreenKind.CONTAINER, false, false, true, true));
		assertFalse(ScreenRules.applies(ScreenKind.CONTAINER, true, true, false, true));
	}

	@Test
	void recipeScreensKeepTheirSidebarsByDefault() {
		assertFalse(ScreenRules.applies(ScreenKind.RECIPE, true, true, true, true));
		assertFalse(ScreenRules.applies(ScreenKind.RECIPE, false, false, false, true));
	}

	@Test
	void recipeScreensAreDistractionFreeWhenNotKept() {
		assertTrue(ScreenRules.applies(ScreenKind.RECIPE, false, false, false, false));
		assertTrue(ScreenRules.applies(ScreenKind.RECIPE, true, true, true, false));
	}

	@Test
	void everyKindIsCovered() {
		for (ScreenKind kind : ScreenKind.values()) {
			assertFalse(ScreenRules.applies(kind, false, false, false, true));
		}
	}
}
