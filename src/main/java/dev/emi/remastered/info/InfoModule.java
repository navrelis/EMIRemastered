package dev.emi.remastered.info;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code info}: the Item Info screen ("How to get" / "Used for" overview of a stack) and its provider API
 * ({@link dev.emi.remastered.info.api.ItemInfoRegistry}), task A1. The screen reads EMI's recipe index. The only EMI
 * plugin is {@link InfoEmiPlugin} (category "Other Sources" from {@code emi_sources} resource files, docs/SOURCES.md).
 * Settings: {@link InfoSettings} (group {@code info}); lang files in {@code assets/emi_info/lang/}.
 */
public final class InfoModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Item Info screen: how to get a stack and what it is used for.")
	@ConfigValue("modules.info")
	public static boolean enabled = true;

	public InfoModule() {
		super("info", "emi_info", InfoSettings.class, true, false, ApplyMode.IMMEDIATE);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		InfoContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		InfoContent.initClient();
	}
}
