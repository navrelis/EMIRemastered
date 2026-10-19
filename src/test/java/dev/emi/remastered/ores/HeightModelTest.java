package dev.emi.remastered.ores;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.emi.remastered.ores.display.HeightModel;
import dev.emi.remastered.ores.display.HeightModel.Anchor;
import dev.emi.remastered.ores.display.HeightModel.Kind;
import dev.emi.remastered.ores.display.HeightModel.Position;
import dev.emi.remastered.ores.display.HeightModel.Resolved;
import dev.emi.remastered.ores.display.HeightModel.Shape;
import dev.emi.remastered.ores.display.HeightModel.Spec;
import org.junit.jupiter.api.Test;

class HeightModelTest {
	private static Anchor fixed(int y) {
		return new Anchor(Kind.FIXED, y);
	}

	private static Anchor bottom(int offset) {
		return new Anchor(Kind.ABOVE_BOTTOM, offset);
	}

	private static Anchor top(int offset) {
		return new Anchor(Kind.BELOW_TOP, offset);
	}

	@Test
	void resolvesAnchors() {
		assertEquals(12, fixed(12).resolve(-64, 320));
		assertEquals(-64, bottom(0).resolve(-64, 320));
		assertEquals(16, bottom(80).resolve(-64, 320));
		// the highest block is 319, the top is exclusive
		assertEquals(319, top(0).resolve(-64, 320));
		assertEquals(309, top(10).resolve(-64, 320));
		// the nether generates 0 to 127
		assertEquals(117, top(10).resolve(0, 128));
	}

	@Test
	void uniformPeakIsTheWholeBand() {
		Resolved r = HeightModel.resolve(new Spec(true, bottom(0), fixed(15), 0), -64, 320);
		assertEquals(Shape.UNIFORM, r.shape());
		assertEquals(-64, r.min());
		assertEquals(15, r.max());
		assertEquals(-64, r.peakLow());
		assertEquals(15, r.peakHigh());
		assertEquals(1.0, r.density(0));
		assertEquals(0.0, r.density(16));
		assertEquals(Position.AT_PEAK, r.position(-10));
		assertEquals(Position.ABOVE, r.position(64));
		assertEquals(Position.BELOW, r.position(-65));
	}

	@Test
	void triangleWithEvenWidthPeaksAtTheMiddle() {
		// vanilla diamond: trapezoid from bottom+(-80) to bottom+80 in the overworld is -144 to 16
		Resolved r = HeightModel.resolve(new Spec(false, bottom(-80), bottom(80), 0), -64, 320);
		assertEquals(Shape.TRIANGULAR, r.shape());
		assertEquals(-144, r.min());
		assertEquals(16, r.max());
		assertEquals(-64, r.peakLow());
		assertEquals(-64, r.peakHigh());
		assertEquals(1.0, r.density(-64));
		assertTrue(r.density(-144) < r.density(-100));
		assertTrue(r.density(-100) < r.density(-64));
		assertTrue(r.density(16) < r.density(-20));
		assertEquals(Position.AT_PEAK, r.position(-64));
		assertEquals(Position.IN_RANGE, r.position(-63));
	}

	@Test
	void triangleWithOddWidthHasTwoPeakLevels() {
		Resolved r = HeightModel.resolve(new Spec(false, fixed(0), fixed(11), 0), -64, 320);
		assertEquals(5, r.peakLow());
		assertEquals(6, r.peakHigh());
		assertEquals(1.0, r.density(5));
		assertEquals(1.0, r.density(6));
	}

	@Test
	void trapezoidPeakIsThePlateau() {
		// width 100, plateau 20: rolls 0..60 and 0..40, flat from 40 to 60
		Resolved r = HeightModel.resolve(new Spec(false, fixed(0), fixed(100), 20), -64, 320);
		assertEquals(Shape.TRAPEZOID, r.shape());
		assertEquals(40, r.peakLow());
		assertEquals(60, r.peakHigh());
		assertEquals(1.0, r.density(40));
		assertEquals(1.0, r.density(50));
		assertEquals(1.0, r.density(60));
		assertTrue(r.density(39) < 1.0);
		assertTrue(r.density(61) < 1.0);
		assertEquals(1 / 41.0, r.density(0), 1e-9);
		assertEquals(1 / 41.0, r.density(100), 1e-9);
	}

	@Test
	void densitySumsToOne() {
		Resolved r = HeightModel.resolve(new Spec(false, fixed(10), fixed(77), 13), 0, 256);
		double total = 0;
		for (int y = r.min(); y <= r.max(); y++) {
			total += r.density(y);
		}
		// relative density times the peak probability is a probability: peak = 1 / (left + 1) / ... the sum of the ways is (left+1)*(right+1)
		assertEquals((r.left() + 1) * (r.right() + 1) / (double) (r.left() + 1), total, 1e-9);
	}

	@Test
	void plateauAsWideAsTheRangeIsUniform() {
		Resolved r = HeightModel.resolve(new Spec(false, fixed(0), fixed(10), 10), 0, 256);
		assertEquals(Shape.UNIFORM, r.shape());
		assertEquals(0, r.peakLow());
		assertEquals(10, r.peakHigh());
	}

	@Test
	void reversedRangeIsASingleLevel() {
		Resolved r = HeightModel.resolve(new Spec(true, fixed(20), fixed(10), 0), 0, 256);
		assertEquals(20, r.min());
		assertEquals(20, r.max());
		assertEquals(0, r.pixelOf(20, 32));
	}

	@Test
	void relativeRangeResolvesPerDimension() {
		Spec spec = new Spec(true, bottom(10), top(10), 0);
		Resolved overworld = HeightModel.resolve(spec, -64, 320);
		Resolved nether = HeightModel.resolve(spec, 0, 128);
		assertEquals(-54, overworld.min());
		assertEquals(309, overworld.max());
		assertEquals(10, nether.min());
		assertEquals(117, nether.max());
	}

	@Test
	void pixelMappingCoversTheGraph() {
		Resolved r = HeightModel.resolve(new Spec(true, fixed(0), fixed(62), 0), 0, 256);
		assertEquals(0, r.pixelOf(0, 32));
		assertEquals(31, r.pixelOf(62, 32));
		assertEquals(0, r.pixelOf(-30, 32));
		assertEquals(31, r.pixelOf(300, 32));
		assertEquals(16, r.pixelOf(32, 32));
		assertEquals(0, r.levelAt(0, 32));
		assertEquals(62, r.levelAt(31, 32));
	}

	@Test
	void peakAnchorsStayRelativeForTheSameKind() {
		Spec spec = new Spec(false, bottom(-80), bottom(80), 0);
		Resolved r = HeightModel.resolve(spec, -64, 320);
		Anchor[] peak = HeightModel.peakAnchors(spec, r);
		assertEquals(bottom(0), peak[0]);
		assertEquals(bottom(0), peak[1]);

		Spec fromTop = new Spec(false, top(60), top(20), 0);
		Resolved rt = HeightModel.resolve(fromTop, -64, 320);
		Anchor[] peakTop = HeightModel.peakAnchors(fromTop, rt);
		assertEquals(top(40), peakTop[0]);
		assertEquals(rt.peakLow(), peakTop[0].resolve(-64, 320));
	}

	@Test
	void peakAnchorsAreAbsoluteForMixedKinds() {
		Spec spec = new Spec(false, bottom(0), fixed(64), 0);
		Resolved r = HeightModel.resolve(spec, -64, 320);
		Anchor[] peak = HeightModel.peakAnchors(spec, r);
		assertArrayEquals(new Anchor[] {fixed(0), fixed(0)}, peak);
	}
}
