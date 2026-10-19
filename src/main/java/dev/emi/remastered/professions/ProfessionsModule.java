package dev.emi.remastered.professions;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code professions}: EMI Professions 1.0.3 (Mrbysco, MIT), ported in task P3.
 * EMI plugin: {@link ProfessionsEmiPlugin} (plugin id {@code emiprofessions}); module code: {@link ProfessionsContent}.
 * Settings: {@link ProfessionsSettings} (group {@code professions}); lang files in {@code assets/emiprofessions/lang/}.
 */
public final class ProfessionsModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show villager professions and their workstations (EMI Professions).")
	@ConfigValue("modules.professions")
	public static boolean enabled = true;

	public ProfessionsModule() {
		super("professions", "emiprofessions", ProfessionsSettings.class, true, false, ApplyMode.RELOAD);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		ProfessionsContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		ProfessionsContent.initClient();
	}
}
