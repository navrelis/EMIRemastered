package dev.emi.remastered.loot.util;

import net.minecraft.text.MutableText;

/**
 * The "x rolls" line of a loot slot tooltip. The value is the chance in percent divided by 100. Values that the
 * decimal places of the setting cannot show (the junk of a fishing roll, for example) are shown as a percent of a roll
 * instead of a "&lt;0.1".
 */
public final class RollsText {

	private RollsText() {
	}

	public static MutableText of(float weight, int places) {
		float rolls = weight / 100f;
		if (FloatTrimmer.isBelowResolution(rolls, places)) {
			return LText.translatable("emi_loot.rolls_small", FloatTrimmer.trimPercent(rolls));
		}
		return LText.translatable("emi_loot.rolls", FloatTrimmer.trimFloatString(rolls, places));
	}
}
