package dev.emi.remastered.professions;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.module.Modules;

/**
 * EMI plugin of the {@code professions} module (fabric.mod.json entrypoint {@code emi}). Plugin id {@code emiprofessions}
 * (the old add-on id), shown in EMI's log and plugin manager. Does nothing while the module switch is off.
 */
public class ProfessionsEmiPlugin extends ModuleEmiPlugin {
	public static final String PLUGIN_ID = "emiprofessions";

	public ProfessionsEmiPlugin() {
		super(Modules.PROFESSIONS, PLUGIN_ID);
	}

	@Override
	protected void registerContent(EmiRegistry registry) {
		ProfessionsContent.register(registry);
	}
}
