package dev.emi.remastered.enchanting;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code enchanting}: EMI Enchanting 0.1.2 (fzzyhmstrs, MIT), ported in task P4.
 * EMI plugin: {@link EnchantingEmiPlugin} (plugin id {@code emi_enchanting}); module code: {@link EnchantingContent}.
 * Settings: {@link EnchantingSettings} (group {@code enchanting}); lang files in {@code assets/emi_enchanting/lang/}.
 */
public final class EnchantingModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show enchantments with their levels, conflicts and items (EMI Enchanting).")
	@ConfigValue("modules.enchanting")
	public static boolean enabled = true;

	public EnchantingModule() {
		super("enchanting", "emi_enchanting", EnchantingSettings.class, true, false, ApplyMode.RELOAD);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		EnchantingContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		EnchantingContent.initClient();
	}
}
