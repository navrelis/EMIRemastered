package dev.emi.remastered.config;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Framework settings that are not tied to a module. Stored in {@code emi.css} like all EMI settings.
 * The group {@code plugins} is not shown in the config screen; the plugin manager edits it.
 */
public final class RemasteredConfig {

	@Comment("EMI plugins that are not loaded, as plugin ids separated by commas (see the plugin manager).\n"
		+ "EMI's own plugin 'emi' cannot be switched off.")
	@ConfigValue("plugins.disabled")
	public static String disabledPlugins = "";

	private RemasteredConfig() {
	}
}
