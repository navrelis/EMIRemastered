package dev.emi.remastered.stats.core;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure logic of the {@code stats} module (no Minecraft classes, unit tested): attribute maths, rounding, hunger bar
 * counts and the tool tier inferred from tag ids.
 */
public final class StatsMath {
	/** Attack damage of a player's bare hand; item modifiers are added to it. */
	public static final double PLAYER_ATTACK_DAMAGE = 1.0;
	/** Attack speed of a player's bare hand (swings per second); item modifiers are added to it. */
	public static final double PLAYER_ATTACK_SPEED = 4.0;
	/** Food level (and saturation) maximum of a player; a full hunger bar has this many points. */
	public static final int MAX_FOOD = 20;

	/** Matches {@code incorrect_for_<tier>_tool}, with or without a namespace. */
	private static final Pattern INCORRECT_TAG = Pattern.compile("^(?:[a-z0-9_.-]+:)?(?:[a-z0-9_./-]*/)?incorrect_for_([a-z0-9_.-]+?)_tool$");

	private StatsMath() {
	}

	/** Operation of an attribute modifier, same meaning as Minecraft's. */
	public enum Op {
		ADD_VALUE,
		ADD_MULTIPLIED_BASE,
		ADD_MULTIPLIED_TOTAL
	}

	public record Mod(double value, Op op) {
	}

	/**
	 * Value of an attribute after its modifiers, in Minecraft's order: all {@code ADD_VALUE} are added to the base,
	 * then all {@code ADD_MULTIPLIED_BASE} add that share of the base, then every {@code ADD_MULTIPLIED_TOTAL}
	 * multiplies the running total.
	 */
	public static double apply(double base, List<Mod> mods) {
		double value = base;
		for (Mod mod : mods) {
			if (mod.op() == Op.ADD_VALUE) {
				value += mod.value();
			}
		}
		double total = value;
		for (Mod mod : mods) {
			if (mod.op() == Op.ADD_MULTIPLIED_BASE) {
				total += value * mod.value();
			}
		}
		for (Mod mod : mods) {
			if (mod.op() == Op.ADD_MULTIPLIED_TOTAL) {
				total *= 1 + mod.value();
			}
		}
		return total;
	}

	/** Rounds to one decimal place (half up), so that float noise like 11.200000000000001 disappears. */
	public static double round1(double value) {
		double rounded = Math.round(value * 10.0) / 10.0;
		return rounded == 0 ? 0 : rounded;
	}

	/** Rounds to two decimal places (half up). */
	public static double round2(double value) {
		double rounded = Math.round(value * 100.0) / 100.0;
		return rounded == 0 ? 0 : rounded;
	}

	/** Damage per second with full-charge swings: damage times attack speed, rounded to one decimal. */
	public static double dps(double damage, double attackSpeed) {
		return round1(Math.max(0, damage) * Math.max(0, attackSpeed));
	}

	/** Seconds until an attack is fully charged again; 0 when the speed is not positive. */
	public static double cooldownSeconds(double attackSpeed) {
		return attackSpeed > 0 ? 1.0 / attackSpeed : 0;
	}

	/**
	 * The "saturation modifier" of Minecraft's food builder (saturation = nutrition * modifier * 2), recovered from
	 * the absolute saturation that 1.21 stores in the component.
	 */
	public static double saturationModifier(int nutrition, float saturation) {
		return nutrition > 0 ? saturation / (2.0 * nutrition) : 0;
	}

	/** {@code value / max} clamped to 0..1 (0 for a non-positive max). */
	public static float fraction(double value, double max) {
		if (!(max > 0) || !(value > 0)) {
			return 0;
		}
		return (float) Math.min(1.0, value / max);
	}

	/**
	 * Hunger shanks of a food level as {full, half, empty}; always 10 icons in total, so levels above 20 look like 20.
	 */
	public static int[] shanks(int nutrition) {
		int clamped = Math.max(0, Math.min(MAX_FOOD, nutrition));
		int full = clamped / 2;
		int half = clamped % 2;
		return new int[] {full, half, 10 - full - half};
	}

	/**
	 * The tier a tool "mines correctly" as, from the id of a block tag it never mines correctly (vanilla's
	 * {@code minecraft:incorrect_for_diamond_tool} gives {@code diamond}), or empty for any other tag.
	 */
	public static Optional<String> tierOfIncorrectTag(String tagId) {
		Matcher matcher = INCORRECT_TAG.matcher(tagId);
		return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

	/** The first tier found in {@code tagIds}; with several, the first in iteration order wins. */
	public static Optional<String> tier(Iterable<String> tagIds) {
		for (String tag : tagIds) {
			Optional<String> tier = tierOfIncorrectTag(tag);
			if (tier.isPresent()) {
				return tier;
			}
		}
		return Optional.empty();
	}

	/** {@code ruby_alloy} to {@code Ruby Alloy}: fallback display of a tier or key without a translation. */
	public static String humanize(String id) {
		StringBuilder builder = new StringBuilder();
		boolean upper = true;
		for (char c : id.toCharArray()) {
			if (c == '_' || c == '-' || c == '/') {
				builder.append(' ');
				upper = true;
			} else {
				builder.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			}
		}
		return builder.length() == 0 ? id : builder.toString();
	}

	/** Lang key for a block tag target, {@code minecraft:mineable/pickaxe} gives {@code emi_stats.target.minecraft.mineable.pickaxe}. */
	public static String targetKey(String namespace, String path) {
		return "emi_stats.target." + namespace + "." + path.replace('/', '.');
	}
}
