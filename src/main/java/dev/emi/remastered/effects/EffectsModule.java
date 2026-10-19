package dev.emi.remastered.effects;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code effects}: EMIffect 2.1.5 (Prismwork, MIT), ported in task P2.
 * EMI plugin: {@link EffectsEmiPlugin} (plugin id {@code emiffect}); module code: {@link EffectsContent}.
 * Settings: {@link EffectsSettings} (group {@code effects}); lang files in {@code assets/emiffect/lang/}.
 */
public final class EffectsModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show status effects with descriptions and sources (EMIffect).")
	@ConfigValue("modules.effects")
	public static boolean enabled = true;

	public EffectsModule() {
		super("effects", "emiffect", EffectsSettings.class, true, false, ApplyMode.RELOAD);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		EffectsContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		EffectsContent.initClient();
	}
}
