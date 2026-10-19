package dev.emi.remastered.world;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code world} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_world}.
 * Does nothing while the module switch is off.
 */
public class WorldEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = World.MOD_ID;

	public WorldEmiPlugin() {
		super(Modules.WORLD, PLUGIN_ID);
	}

	@Override
	protected void initializeContent(EmiInitRegistry registry) {
		WorldContent.initialize(registry);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		WorldContent.register(registry);
	}
}
