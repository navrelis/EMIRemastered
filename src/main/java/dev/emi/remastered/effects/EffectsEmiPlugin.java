package dev.emi.remastered.effects;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code effects} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emiffect}
 * (the old add-on id), shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class EffectsEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emiffect";

	public EffectsEmiPlugin() {
		super(Modules.EFFECTS, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		EffectsContent.register(registry);
	}
}
