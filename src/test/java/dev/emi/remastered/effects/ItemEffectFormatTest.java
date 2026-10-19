package dev.emi.remastered.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Duration and detail text of the effects an item applies. */
class ItemEffectFormatTest {

	@Test
	void durationRoundsSecondsDownLikeVanilla() {
		assertEquals("3:00", ItemEffectFormat.duration(3600));
		assertEquals("0:22", ItemEffectFormat.duration(450));
		assertEquals("0:01", ItemEffectFormat.duration(39));
		assertEquals("100:00", ItemEffectFormat.duration(120000));
		assertEquals("∞", ItemEffectFormat.duration(-1));
	}

	@Test
	void arrowsAndCloudsApplyAFractionOfTheDuration() {
		assertEquals(450, ItemEffectFormat.scaled(3600, ItemEffectFormat.ARROW_FACTOR));
		assertEquals(900, ItemEffectFormat.scaled(3600, ItemEffectFormat.LINGERING_FACTOR));
		assertEquals(3600, ItemEffectFormat.scaled(3600, 1f));
		assertEquals(-1, ItemEffectFormat.scaled(-1, ItemEffectFormat.ARROW_FACTOR));
	}

	@Test
	void instantEffectsShowNoDuration() {
		assertFalse(ItemEffectFormat.showsDuration(1));
		assertFalse(ItemEffectFormat.showsDuration(19));
		assertTrue(ItemEffectFormat.showsDuration(20));
		assertTrue(ItemEffectFormat.showsDuration(-1));
		assertEquals("", ItemEffectFormat.detail(1, 1f, 1f));
	}

	@Test
	void detailJoinsDurationAndChance() {
		assertEquals("3:00", ItemEffectFormat.detail(3600, 1f, 1f));
		assertEquals("0:22", ItemEffectFormat.detail(3600, ItemEffectFormat.ARROW_FACTOR, 1f));
		assertEquals("0:10 · 30%", ItemEffectFormat.detail(200, 1f, 0.3f));
		assertEquals("30%", ItemEffectFormat.detail(1, 1f, 0.3f));
		assertEquals("∞", ItemEffectFormat.detail(-1, 1f, 1f));
	}
}
