package dev.emi.remastered.enchanting;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code enchanting} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_enchanting}
 * (the old add-on id), shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class EnchantingEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emi_enchanting";

	public EnchantingEmiPlugin() {
		super(Modules.ENCHANTING, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		EnchantingContent.register(registry);
	}
}
