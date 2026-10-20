package dev.emi.remastered.loot;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code loot} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_loot}
 * (the old add-on id), shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class LootEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emi_loot";

	public LootEmiPlugin() {
		super(Modules.LOOT, PLUGIN_ID);
	}

	@Override
	protected void initializeContent(EmiInitRegistry registry) {
		LootContent.initializeEmi();
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		LootContent.register(registry);
	}
}
