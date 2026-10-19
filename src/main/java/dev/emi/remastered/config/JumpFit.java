package dev.emi.remastered.config;

/** Pure space calculation for the jump buttons of EMI's config screen (no Minecraft classes). */
public final class JumpFit {
	/** Size of one jump button row in pixels. */
	public static final int ICON_SIZE = 16;

	private JumpFit() {
	}

	/**
	 * How many of the module jump entries (the {@code modules} entry plus one per module group) fit next to EMI's own
	 * jumps. Everything fits: {@code moduleJumps}. Otherwise as many as fit, but at least one (the {@code modules}
	 * jump keeps the rest reachable, EMI then shortens its own jumps).
	 *
	 * @param space       free height in pixels (EMI: logical list height minus 10)
	 * @param emiJumps    number of EMI's own jump entries that stay
	 * @param moduleJumps number of module jump entries wanted (including {@code modules})
	 * @param iconSize    height of one jump entry
	 */
	public static int fittingModuleJumps(int space, int emiJumps, int moduleJumps, int iconSize) {
		if (moduleJumps <= 0) {
			return 0;
		}
		if ((emiJumps + moduleJumps) * iconSize <= space) {
			return moduleJumps;
		}
		int free = space / iconSize - emiJumps;
		return Math.max(1, Math.min(moduleJumps - 1, free));
	}
}
