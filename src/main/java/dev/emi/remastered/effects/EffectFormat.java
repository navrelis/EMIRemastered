package dev.emi.remastered.effects;

import java.util.Locale;

/** Pure text helpers of the effect pages (no Minecraft classes, unit tested). */
public final class EffectFormat {
	private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

	private EffectFormat() {
	}

	/** Roman numeral for 1..3999, the plain number for everything else. */
	public static String roman(int number) {
		if (number < 1 || number > 3999) {
			return Integer.toString(number);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ROMAN_VALUES.length; i++) {
			while (number >= ROMAN_VALUES[i]) {
				sb.append(ROMAN_SYMBOLS[i]);
				number -= ROMAN_VALUES[i];
			}
		}
		return sb.toString();
	}

	/** Effect level (1-based) for an amplifier (0-based). */
	public static String level(int amplifier) {
		return roman(amplifier + 1);
	}

	/** {@code 0.3f -> "30%"}, {@code 0.075f -> "7.5%"}, never a trailing {@code .0}. */
	public static String percent(float probability) {
		double p = Math.round(probability * 1000.0) / 10.0;
		if (p == Math.rint(p)) {
			return (long) p + "%";
		}
		return String.format(Locale.ROOT, "%.1f%%", p);
	}

	/** {@code 900 -> "0:45"}, {@code 120000 -> "100:00"}; a negative (infinite) duration gives an infinity sign. */
	public static String duration(int ticks) {
		if (ticks < 0) {
			return "∞";
		}
		int seconds = (ticks + 19) / 20;
		return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
	}

	/**
	 * Label of a source: level, duration and chance joined with a middle dot, for example {@code "II · 0:45 · 30%"}.
	 *
	 * @param amplifier   level minus one, negative to leave the level out
	 * @param duration    ticks, negative to leave it out
	 * @param probability chance, left out when 1 or more
	 */
	public static String label(int amplifier, int duration, float probability) {
		StringBuilder sb = new StringBuilder();
		if (amplifier >= 0) {
			sb.append(level(amplifier));
		}
		if (duration >= 0) {
			if (sb.length() > 0) {
				sb.append(" · ");
			}
			sb.append(duration(duration));
		}
		if (probability < 1f) {
			if (sb.length() > 0) {
				sb.append(" · ");
			}
			sb.append(percent(probability));
		}
		return sb.toString();
	}

	/**
	 * Number part of a vanilla attribute modifier line, always as magnitude (the sign is chosen by the caller through
	 * the translation key): multiplied operations (id 1 and 2) are shown in percent, at most two decimals, no
	 * trailing zeros ({@code 0.2 -> "20"}, {@code 3.0 -> "3"}, {@code -0.15 -> "15"}).
	 */
	public static String modifierValue(double value, int operationId) {
		double magnitude = Math.abs(operationId == 0 ? value : value * 100.0);
		double rounded = Math.round(magnitude * 100.0) / 100.0;
		if (rounded == Math.rint(rounded)) {
			return Long.toString((long) rounded);
		}
		String s = String.format(Locale.ROOT, "%.2f", rounded);
		return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
	}

	/**
	 * Extracts the effect id from a serialized effect stack string of any of the known spellings:
	 * {@code emiffect:effect:minecraft:speed} (EMIffect 2.1.5), {@code mob_effect:minecraft:speed} (2.1.6),
	 * {@code emiffect:minecraft:speed}. Returns {@code null} when the string has no id part.
	 */
	public static String parseStackString(String serialized) {
		if (serialized == null) {
			return null;
		}
		String[] parts = serialized.split(":");
		if (parts.length < 2) {
			return null;
		}
		int start = 1;
		if (parts.length >= 4 && parts[1].equals("effect")) {
			start = 2;
		}
		StringBuilder id = new StringBuilder();
		for (int i = start; i < parts.length; i++) {
			if (i > start) {
				id.append(':');
			}
			id.append(parts[i]);
		}
		if (start == parts.length - 1) {
			return "minecraft:" + id;
		}
		return id.toString();
	}
}
