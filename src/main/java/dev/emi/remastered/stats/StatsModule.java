package dev.emi.remastered.stats;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code stats}: food, tool, weapon and armour stats read from item data components and attribute modifiers
 * (tasks A5 and B1). Food gets an EMI recipe category and every stat an Item Info section. The module is client only.
 * EMI plugin: {@link StatsEmiPlugin} (plugin id {@code emi_stats}); module code: {@link StatsContent}.
 * Settings: {@link StatsSettings} (group {@code stats}); lang files in {@code assets/emi_stats/lang/}.
 */
public final class StatsModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Food, tool, weapon and armour stats in a food category and on the Item Info page.")
	@ConfigValue("modules.stats")
	public static boolean enabled = true;

	public StatsModule() {
		super("stats", "emi_stats", StatsSettings.class, true, false, ApplyMode.RELOAD);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		StatsContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		StatsContent.initClient();
	}
}
