package dev.emi.remastered.letsdo;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code letsdo}: Let's Do EMI Compat 2.0.3 (Pandaismyname1, MIT), ported in task P6.
 * EMI plugin: {@link LetsDoEmiPlugin} (plugin id {@code emi_letsdo_compat}); module code: {@link LetsDoContent}.
 * Settings: {@link LetsDoSettings} (group {@code letsdo}); lang files in {@code assets/emi_letsdo_compat/lang/}.
 */
public final class LetsDoModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show recipes of the Let's Do mods (Let's Do EMI Compat).")
	@ConfigValue("modules.letsdo")
	public static boolean enabled = true;

	public LetsDoModule() {
		super("letsdo", "emi_letsdo_compat", LetsDoSettings.class, true, false, ApplyMode.RELOAD);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		LetsDoContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		LetsDoContent.initClient();
	}
}
