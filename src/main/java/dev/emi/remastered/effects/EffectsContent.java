package dev.emi.remastered.effects;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;

/**
 * Entry points of the {@code effects} module (EMIffect). The module is client only: everything it does is in the
 * EMI plugin, whose code lives in {@link EffectsEmi} (never loaded on a dedicated server).
 */
public final class EffectsContent {

	private EffectsContent() {
	}

	/** Both sides, always: nothing to do, the module has no server part. */
	public static void init() {
	}

	/** Client, only when the module is enabled at startup: nothing to do, everything happens in the EMI plugin. */
	public static void initClient() {
	}

	/** EMI initialization, only while the module is enabled: stack serializers and the registry adapter. */
	public static void initialize(EmiInitRegistry registry) {
		EffectsEmi.initialize(registry);
	}

	/** EMI registration, only while the module is enabled. */
	public static void register(EmiRegistry registry) {
		EffectsEmi.register(registry);
	}
}
