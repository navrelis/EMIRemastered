package dev.emi.remastered.effects;

/**
 * Pure text helpers for the effects an item applies (Item Info provider, unit tested). Durations follow vanilla's
 * potion tooltips: seconds are rounded down, effects shorter than a second (instant ones) show no duration, and
 * tipped arrows and lingering potions apply only a fraction of the potion's duration.
 */
public final class ItemEffectFormat {
	/** A tipped arrow applies an eighth of the potion's duration. */
	public static final float ARROW_FACTOR = 0.125f;
	/** A lingering potion's cloud applies a quarter of the potion's duration. */
	public static final float LINGERING_FACTOR = 0.25f;

	private ItemEffectFormat() {
	}

	/** Ticks after applying the item's duration factor; an infinite (negative) duration stays infinite. */
	public static int scaled(int ticks, float factor) {
		return ticks < 0 ? ticks : (int) Math.floor(ticks * (double) factor);
	}

	/** Whether vanilla shows a duration for an effect: infinite, or at least one second long. */
	public static boolean showsDuration(int ticks) {
		return ticks < 0 || ticks >= 20;
	}

	/** {@code 450 -> "0:22"}, {@code 3600 -> "3:00"}, {@code -1 -> "∞"}; seconds are rounded down. */
	public static String duration(int ticks) {
		if (ticks < 0) {
			return "∞";
		}
		int seconds = ticks / 20;
		return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
	}

	/**
	 * Right hand detail of an effect row: the duration (left out for instant effects) and the chance (left out at 100%),
	 * joined with a middle dot. Empty when there is nothing to show.
	 *
	 * @param ticks       the effect's own duration, negative for infinite
	 * @param factor      the item's duration factor, see {@link #ARROW_FACTOR}
	 * @param probability chance that the effect is applied, 1 = always
	 */
	public static String detail(int ticks, float factor, float probability) {
		StringBuilder sb = new StringBuilder();
		if (showsDuration(ticks)) {
			sb.append(duration(scaled(ticks, factor)));
		}
		if (probability < 1f) {
			if (sb.length() > 0) {
				sb.append(" · ");
			}
			sb.append(EffectFormat.percent(probability));
		}
		return sb.toString();
	}
}
