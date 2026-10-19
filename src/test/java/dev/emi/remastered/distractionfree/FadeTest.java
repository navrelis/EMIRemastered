package dev.emi.remastered.distractionfree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FadeTest {
	@Test
	void zeroDurationJumps() {
		assertEquals(1f, Fade.step(0f, true, 1, 0));
		assertEquals(0f, Fade.step(1f, false, 1, 0));
		assertEquals(0f, Fade.step(0.4f, false, 0, -5));
	}

	@Test
	void movesLinearlyAndClamps() {
		// 50 ms of a 200 ms fade is a quarter
		assertEquals(0.25f, Fade.step(0f, true, 50_000_000L, 200), 1e-6f);
		assertEquals(0.75f, Fade.step(1f, false, 50_000_000L, 200), 1e-6f);
		assertEquals(1f, Fade.step(0.9f, true, 1_000_000_000L, 200));
		assertEquals(0f, Fade.step(0.1f, false, 1_000_000_000L, 200));
		assertEquals(0.5f, Fade.step(0.5f, true, 0L, 200));
	}

	@Test
	void durationIsClamped() {
		assertEquals(Fade.MAX_DURATION_MS, Fade.clampDuration(100_000));
		assertEquals(0, Fade.clampDuration(-1));
		// an absurd setting does not make the fade slower than the maximum
		assertEquals(Fade.step(0f, true, 100_000_000L, Fade.MAX_DURATION_MS), Fade.step(0f, true, 100_000_000L, Integer.MAX_VALUE), 1e-9f);
	}

	@Test
	void easeIsMonotonicWithFixedEnds() {
		assertEquals(0f, Fade.ease(0f));
		assertEquals(1f, Fade.ease(1f));
		assertEquals(0.5f, Fade.ease(0.5f), 1e-6f);
		float last = 0;
		for (int i = 1; i <= 100; i++) {
			float e = Fade.ease(i / 100f);
			assertTrue(e >= last);
			last = e;
		}
		assertEquals(0f, Fade.ease(-3f));
		assertEquals(1f, Fade.ease(3f));
	}
}
