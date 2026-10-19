package dev.emi.remastered.stats;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code stats} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_stats},
 * shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class StatsEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emi_stats";

	public StatsEmiPlugin() {
		super(Modules.STATS, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		StatsContent.register(registry);
	}
}
