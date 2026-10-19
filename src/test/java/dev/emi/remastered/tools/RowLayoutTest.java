package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.tools.tags.RowLayout;

class RowLayoutTest {
	@Test
	void everythingFitsWhenThereIsRoom() {
		RowLayout layout = RowLayout.of(200, 100, 20, 40, 6);
		assertTrue(layout.showSize());
		assertTrue(layout.showRegistry());
		assertEquals(100, layout.idWidth());
	}

	@Test
	void theRegistryLabelGoesFirst() {
		// 100 + 20 + 6 = 126 fits, with the registry label 126 + 40 + 6 = 172 does not
		RowLayout layout = RowLayout.of(150, 100, 20, 40, 6);
		assertTrue(layout.showSize());
		assertFalse(layout.showRegistry());
		assertEquals(100, layout.idWidth());
	}

	@Test
	void thenTheCountAndTheIdStaysWhole() {
		RowLayout layout = RowLayout.of(110, 100, 20, 40, 6);
		assertFalse(layout.showSize());
		assertFalse(layout.showRegistry());
		assertEquals(100, layout.idWidth());
	}

	@Test
	void onlyAnIdThatAloneIsTooWideIsCut() {
		RowLayout layout = RowLayout.of(90, 100, 20, 40, 6);
		assertFalse(layout.showSize());
		assertFalse(layout.showRegistry());
		assertEquals(90, layout.idWidth());
	}

	@Test
	void anIdThatJustFitsIsNotCut() {
		assertEquals(100, RowLayout.of(100, 100, 20, 0, 6).idWidth());
	}

	@Test
	void noRegistryColumnWhenThereIsNoLabel() {
		assertFalse(RowLayout.of(300, 100, 20, 0, 6).showRegistry());
	}
}
