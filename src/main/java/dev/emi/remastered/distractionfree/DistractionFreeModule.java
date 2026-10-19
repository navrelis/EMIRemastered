package dev.emi.remastered.distractionfree;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code distraction_free}: Distraction Free Recipes 1.2.2 (Toni/txnimc, MIT), ported in task P1.
 * No EMI plugin: the feature lives in EMI core's screen code; it reads {@link #enabled} and
 * {@link DistractionFreeSettings}.
 * Settings: {@link DistractionFreeSettings} (group {@code distraction_free}); lang files in {@code assets/distraction_free_recipes/lang/}.
 */
public final class DistractionFreeModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Hide EMI's index until you search (Distraction Free Recipes).")
	@ConfigValue("modules.distraction_free")
	public static boolean enabled = true;

	public DistractionFreeModule() {
		super("distraction_free", "distraction_free_recipes", DistractionFreeSettings.class, true, false, ApplyMode.IMMEDIATE);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		// Nothing yet: the feature lives in EMI core's screen code and reads the switch and the settings.
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		// Key binds / client events of distraction-free mode go here.
	}
}
