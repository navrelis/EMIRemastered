package dev.emi.remastered.stats;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.stats.client.StatsEmi;

/**
 * Entry points of the {@code stats} module (see docs/MODULES.md). The module is client only: everything it does is
 * in the EMI plugin, whose code lives in {@link StatsEmi} (never loaded on a dedicated server).
 */
public final class StatsContent {

	private StatsContent() {
	}

	/** Both sides, always: nothing to do, the module has no server part. */
	public static void init() {
	}

	/** Client, only when the module is enabled at startup: nothing to do, everything happens in the EMI plugin. */
	public static void initClient() {
	}

	/** EMI registration, only while the module is enabled. */
	public static void register(EmiRegistry registry) {
		StatsEmi.register(registry);
	}
}
