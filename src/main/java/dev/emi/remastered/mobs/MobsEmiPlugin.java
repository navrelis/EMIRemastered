package dev.emi.remastered.mobs;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code mobs} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emi_mobs}.
 * Does nothing while the module switch is off.
 */
public class MobsEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = Mobs.MOD_ID;

	public MobsEmiPlugin() {
		super(Modules.MOBS, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		MobsContent.register(registry);
	}
}
