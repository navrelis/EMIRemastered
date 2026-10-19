package dev.emi.remastered.ores;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code ores} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_ores}
 * (the old add-on id), shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class OresEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emi_ores";

	public OresEmiPlugin() {
		super(Modules.ORES, PLUGIN_ID);
	}

	@Override
	protected void initializeContent(EmiInitRegistry registry) {
		OresContent.initialize(registry);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		OresContent.register(registry);
	}
}
