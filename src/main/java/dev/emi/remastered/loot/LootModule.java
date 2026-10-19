package dev.emi.remastered.loot;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code loot}: EMI Loot 0.7.9 (fzzyhmstrs, MIT), ported in task P7.
 * EMI plugin: {@link LootEmiPlugin} (plugin id {@code emi_loot}); module code: {@link LootContent}.
 * Settings: {@link LootSettings} (group {@code loot}); lang files in {@code assets/emi_loot/lang/}.
 */
public final class LootModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show chest, block, mob, gameplay and archaeology loot (EMI Loot).")
	@ConfigValue("modules.loot")
	public static boolean enabled = true;

	public LootModule() {
		super("loot", "emi_loot", LootSettings.class, true, true, ApplyMode.RESTART);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		LootContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		LootContent.initClient();
	}
}
