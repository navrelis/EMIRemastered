package dev.emi.remastered.loot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FloatTrimmer {

    public static String trimFloatString(float weight) {
        return trimFloatString(weight, 1);
    }

    /**
     * Cuts the number to the given decimal places without rounding. A number above zero that would be cut to zero
     * (for example 0.0083 with one place, or the 9.1E-4 of a rare fishing junk item) is shown as "&lt;0.1" instead of a
     * wrong "0.0" or, for numbers that Java prints in scientific notation, a wrong "9.1".
     */
    public static String trimFloatString(float weight, int places) {
        if (Float.isNaN(weight) || Float.isInfinite(weight)) {
            return Float.toString(weight);
        }
        int scale = Math.max(0, places);
        BigDecimal exact = new BigDecimal(Float.toString(weight));
        BigDecimal cut = exact.setScale(scale, RoundingMode.DOWN);
        if (cut.signum() == 0 && exact.signum() > 0) {
            return "<" + BigDecimal.ONE.movePointLeft(scale).toPlainString();
        }
        return cut.toPlainString();
    }

}
