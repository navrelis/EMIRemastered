package dev.emi.remastered.mobs;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code mobs}: one EMI page per mob with attributes, natural spawn biomes and a link to its drops.
 * EMI plugin: {@link MobsEmiPlugin} (plugin id {@code emi_mobs}); module code: {@link MobsContent}.
 * Settings: {@link MobsSettings} (group {@code mobs}); lang files in {@code assets/emi_mobs/lang/}.
 */
public final class MobsModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show an info page for every mob: attributes, spawn biomes and a link to its drops.")
	@ConfigValue("modules.mobs")
	public static boolean enabled = true;

	public MobsModule() {
		super("mobs", "emi_mobs", MobsSettings.class, true, true, ApplyMode.RESTART);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		MobsContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		MobsContent.initClient();
	}
}
