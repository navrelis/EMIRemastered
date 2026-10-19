package dev.emi.remastered.mobs.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Number and list formatting of the mob pages. Pure logic, no Minecraft classes. */
public final class MobFormat {
	/**
	 * Blocks per second per point of the movement speed attribute. The real speed also depends on the mob's goals
	 * (walking, chasing), so the result is only an approximation.
	 */
	public static final double BLOCKS_PER_SECOND_PER_SPEED = 43.17;

	private MobFormat() {
	}

	/** Number without trailing zeros: 20 -> "20", 0.5 -> "0.5", 1.2500001 -> "1.25". Locale independent. */
	public static String number(double value, int maxDecimals) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return "-";
		}
		BigDecimal decimal = BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros();
		if (decimal.scale() < 0) {
			decimal = decimal.setScale(0);
		}
		return decimal.toPlainString();
	}

	/** Health points as hearts (2 points = 1 heart). */
	public static String hearts(double points) {
		return number(points / 2.0, 2);
	}

	/** Approximate walking speed in blocks per second for a movement speed attribute value. */
	public static String blocksPerSecond(double speedAttribute) {
		return number(speedAttribute * BLOCKS_PER_SECOND_PER_SPEED, 1);
	}

	/** "3" or "2-4". */
	public static String groupSize(int min, int max) {
		return min == max ? Integer.toString(min) : min + "-" + max;
	}

	/** Entity dimensions as "0.6 x 1.95". */
	public static String size(double width, double height) {
		return number(width, 2) + " x " + number(height, 2);
	}

	/**
	 * The first entries of a list for a compact display. When only one entry would be hidden it is shown instead,
	 * because "+1 more" takes the same room as the entry.
	 */
	public record Compact<T>(List<T> shown, int hidden) {
		public static <T> Compact<T> of(List<T> all, int limit) {
			int max = Math.max(1, limit);
			if (all.size() <= max + 1) {
				return new Compact<>(all, 0);
			}
			return new Compact<>(all.subList(0, max), all.size() - max);
		}
	}
}
