package dev.emi.remastered.enchanting.logic;

import java.util.function.IntUnaryOperator;

/**
 * The hard cap on enchantment levels of a stack, as opposed to the maximum level of an enchantment: vanilla clamps
 * every level a stack can store to {@value #VANILLA} (anvil merges, commands), and mods such as No Enchantment Cap
 * Level lift that. The cap is measured, not looked up by mod name, so any mod that lifts it is noticed.
 */
public final class LevelCap {
	public static final int VANILLA = 255;
	/** Cap value for "no limit". */
	public static final int UNLIMITED = Integer.MAX_VALUE;
	/** The level the probe tries to store; above {@link #VANILLA}. */
	public static final int PROBE_LEVEL = 1000;

	private LevelCap() {
	}

	/**
	 * @param store stores the given level on a fresh stack component and returns the level that stuck
	 * @return the highest level a stack can store, {@link #UNLIMITED} if the probe level went through
	 */
	public static int measure(IntUnaryOperator store) {
		int stored = store.applyAsInt(PROBE_LEVEL);
		return stored >= PROBE_LEVEL ? UNLIMITED : Math.max(1, stored);
	}

	/** @return whether the cap differs from vanilla's, i.e. is worth showing */
	public static boolean isCustom(int cap) {
		return cap != VANILLA;
	}
}
