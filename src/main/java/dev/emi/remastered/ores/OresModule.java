package dev.emi.remastered.ores;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code ores}: EMI Ores 1.3 (Abbie, LGPL-3.0-only), ported in task P5.
 * EMI plugin: {@link OresEmiPlugin} (plugin id {@code emi_ores}); module code: {@link OresContent}.
 * Settings: {@link OresSettings} (group {@code ores}); lang files in {@code assets/emi_ores/lang/}.
 */
public final class OresModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show where ores and geodes generate (EMI Ores).")
	@ConfigValue("modules.ores")
	public static boolean enabled = true;

	public OresModule() {
		super("ores", "emi_ores", OresSettings.class, true, true, ApplyMode.RESTART);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		OresContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		OresContent.initClient();
	}
}
