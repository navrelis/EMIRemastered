/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.display;

/**
 * Height distribution of a placed feature, resolved for a dimension. Plain integer maths, no Minecraft types, so it
 * can be unit tested. The formulas are the ones of vanilla's {@code UniformHeightProvider} and
 * {@code TrapezoidHeightProvider}.
 */
public final class HeightModel {
	private HeightModel() {
	}

	public enum Kind {
		FIXED,
		/** Offset above the lowest block. */
		ABOVE_BOTTOM,
		/** Offset below the highest block (0 is the highest block itself). */
		BELOW_TOP
	}

	public enum Shape {
		UNIFORM,
		TRIANGULAR,
		TRAPEZOID
	}

	/** Where the player is relative to a distribution. */
	public enum Position {
		BELOW,
		IN_RANGE,
		AT_PEAK,
		ABOVE
	}

	/** A height as written in the feature: absolute or relative to the bottom or top of the dimension. */
	public record Anchor(Kind kind, int value) {
		/**
		 * @param bottomY lowest block of the dimension
		 * @param topY    first block above the dimension (exclusive), like {@code World.getTopY()}
		 */
		public int resolve(int bottomY, int topY) {
			return switch (kind) {
				case FIXED -> value;
				case ABOVE_BOTTOM -> bottomY + value;
				case BELOW_TOP -> topY - 1 - value;
			};
		}

		/** The same kind of anchor {@code dy} blocks higher. */
		public Anchor shifted(int dy) {
			return switch (kind) {
				case FIXED, ABOVE_BOTTOM -> new Anchor(kind, value + dy);
				case BELOW_TOP -> new Anchor(kind, value - dy);
			};
		}
	}

	/**
	 * @param uniform true for a uniform height provider, false for a trapezoid (a triangle has plateau 0)
	 * @param plateau width of the flat top of a trapezoid
	 */
	public record Spec(boolean uniform, Anchor min, Anchor max, int plateau) {
	}

	/**
	 * The distribution in blocks.
	 *
	 * @param peakLow  lowest level with the highest probability
	 * @param peakHigh highest level with the highest probability
	 * @param left     the trapezoid is the sum of two uniform rolls of {@code 0..left} and {@code 0..right}
	 */
	public record Resolved(Shape shape, int min, int max, int peakLow, int peakHigh, int left, int right) {

		public boolean contains(int y) {
			return y >= min && y <= max;
		}

		public Position position(int y) {
			if (y < min) {
				return Position.BELOW;
			}
			if (y > max) {
				return Position.ABOVE;
			}
			return y >= peakLow && y <= peakHigh ? Position.AT_PEAK : Position.IN_RANGE;
		}

		/** Relative probability of a level, 1 at the peak. */
		public double density(int y) {
			if (!contains(y)) {
				return 0;
			}
			if (shape == Shape.UNIFORM) {
				return 1;
			}
			int s = y - min;
			// number of ways to roll s with the two uniform rolls, divided by the maximum (left + 1)
			int ways = s <= left ? s + 1 : s <= right ? left + 1 : left + right - s + 1;
			return ways / (double) (left + 1);
		}

		/** The level at a pixel column of a graph that is {@code width} pixels wide, first column is the minimum. */
		public int levelAt(int pixel, int width) {
			if (width <= 1 || max == min) {
				return min;
			}
			return min + (int) Math.round(pixel * (max - min) / (double) (width - 1));
		}

		/** The pixel column of a level in a graph that is {@code width} pixels wide. */
		public int pixelOf(int y, int width) {
			if (width <= 1 || max == min) {
				return 0;
			}
			long pixel = Math.round((y - min) * (width - 1) / (double) (max - min));
			return (int) Math.max(0, Math.min(width - 1, pixel));
		}
	}

	/** Resolves the heights for a dimension. A reversed range (min above max) is treated as a single level, as vanilla does. */
	public static Resolved resolve(Spec spec, int bottomY, int topY) {
		int min = spec.min().resolve(bottomY, topY);
		int max = spec.max().resolve(bottomY, topY);
		if (min >= max) {
			return new Resolved(Shape.UNIFORM, min, min, min, min, 0, 0);
		}
		int width = max - min;
		if (spec.uniform() || spec.plateau() >= width) {
			return new Resolved(Shape.UNIFORM, min, max, min, max, 0, width);
		}
		int left = (width - spec.plateau()) / 2;
		int right = width - left;
		return new Resolved(spec.plateau() == 0 ? Shape.TRIANGULAR : Shape.TRAPEZOID, min, max, min + left, min + right, left, right);
	}

	/**
	 * The peak as anchors, for relative display: kept relative when both ends are the same kind of anchor, else absolute.
	 *
	 * @return {low, high}
	 */
	public static Anchor[] peakAnchors(Spec spec, Resolved resolved) {
		int lowShift = resolved.peakLow() - resolved.min();
		int highShift = resolved.peakHigh() - resolved.min();
		if (spec.min().kind() == spec.max().kind()) {
			return new Anchor[] {spec.min().shifted(lowShift), spec.min().shifted(highShift)};
		}
		return new Anchor[] {new Anchor(Kind.FIXED, resolved.peakLow()), new Anchor(Kind.FIXED, resolved.peakHigh())};
	}
}
