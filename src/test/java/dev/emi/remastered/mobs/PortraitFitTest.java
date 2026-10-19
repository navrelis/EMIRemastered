package dev.emi.remastered.mobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.mobs.data.PortraitFit;

class PortraitFitTest {
	@Test
	void smallMobsAreScaledUpUntilTheyFillTheBox() {
		// a bat model: 0.4 blocks wide and 0.2 high
		PortraitFit bat = PortraitFit.of(-0.2, -0.2, 0.2, 0.0, 22);
		assertNotNull(bat);
		assertEquals(55.0, bat.scale(), 1e-9);
		assertEquals(22.0, 0.4 * bat.scale(), 1e-9);
	}

	@Test
	void largeMobsAreScaledDownToFit() {
		// a ghast, 4 by 4 blocks
		PortraitFit ghast = PortraitFit.of(-2, -4, 2, 0, 22);
		assertEquals(5.5, ghast.scale(), 1e-9);
	}

	@Test
	void theLongerSideDecides() {
		PortraitFit tall = PortraitFit.of(-0.3, -1.95, 0.3, 0, 22);
		assertEquals(22.0 / 1.95, tall.scale(), 1e-9);
		PortraitFit wide = PortraitFit.of(-8, -4, 8, 0, 22);
		assertEquals(22.0 / 16.0, wide.scale(), 1e-9);
	}

	@Test
	void theMiddleOfTheMobEndsUpInTheMiddleOfTheBox() {
		PortraitFit fit = PortraitFit.of(-1, -3, 3, 0, 20);
		double scale = fit.scale();
		// the middle of the extent is at (1, -1.5) blocks from the origin
		assertEquals(0.0, 1 * scale + fit.offsetX(), 1e-9);
		assertEquals(0.0, -1.5 * scale + fit.offsetY(), 1e-9);
	}

	@Test
	void theScaleIsLimitedForAlmostEmptyModels() {
		PortraitFit fit = PortraitFit.of(0, 0, 0.0001, 0.0001, 22);
		assertEquals(PortraitFit.MAX_SCALE, fit.scale(), 1e-9);
		assertTrue(fit.scale() > 0);
	}

	@Test
	void nothingToFitGivesNull() {
		assertNull(PortraitFit.of(0, 0, 0, 0, 22));
		assertNull(PortraitFit.of(Double.NaN, 0, 1, 1, 22));
		assertNull(PortraitFit.of(0, 0, Double.POSITIVE_INFINITY, 1, 22));
	}
}
