package dev.emi.remastered.info;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code info} module (plugin id {@code emi_info}): the category "Other Sources" with the entries of
 * the {@code emi_sources} resource files (fabric.mod.json entrypoint {@code emi}). Does nothing while the module
 * switch is off.
 */
public class InfoEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emi_info";

	public InfoEmiPlugin() {
		super(Modules.INFO, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		InfoContent.register(registry);
	}
}
