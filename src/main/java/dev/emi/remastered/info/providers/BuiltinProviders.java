package dev.emi.remastered.info.providers;

import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.info.providers.advancement.AdvancementInfoProvider;

/** Registers the built-in providers once (idempotent; also when the module was switched on after startup). */
public final class BuiltinProviders {
	private static boolean registered = false;

	private BuiltinProviders() {
	}

	public static synchronized void ensureRegistered() {
		if (registered) {
			return;
		}
		registered = true;
		ItemInfoRegistry.register(new RecipesInfoProvider());
		ItemInfoRegistry.register(new FuelInfoProvider());
		ItemInfoRegistry.register(new CompostInfoProvider());
		ItemInfoRegistry.register(new AdvancementInfoProvider());
		ItemInfoRegistry.register(new NoSourceInfoProvider());
	}
}
