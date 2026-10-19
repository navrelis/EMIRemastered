package dev.emi.remastered.letsdo;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code letsdo} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_letsdo_compat}
 * (the old add-on id), shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class LetsDoEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emi_letsdo_compat";

	public LetsDoEmiPlugin() {
		super(Modules.LETSDO, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		LetsDoContent.register(registry);
	}
}
