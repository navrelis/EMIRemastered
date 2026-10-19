/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.display;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Number formats of the ore pages. No Minecraft types. */
public final class ChanceFormat {
	private ChanceFormat() {
	}

	/** A chance between 0 and 1 as a percent number with at most one decimal place and no trailing zeros: 0.5 gives "50", 0.125 gives "12.5". */
	public static String percent(float chance) {
		BigDecimal percent = new BigDecimal(Float.toString(chance)).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
		return percent.signum() == 0 ? "0" : percent.stripTrailingZeros().toPlainString();
	}
}
