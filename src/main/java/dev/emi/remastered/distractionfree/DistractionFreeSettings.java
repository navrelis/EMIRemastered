package dev.emi.remastered.distractionfree;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Settings of distraction-free mode (group {@code distraction_free}); the switch is {@link DistractionFreeModule#enabled}.
 * Lang keys in {@code assets/distraction_free_recipes/lang/}.
 */
public final class DistractionFreeSettings {

	@Comment("Show a hint that searching shows recipes.")
	@ConfigValue("distraction_free.show-help-text")
	public static boolean showHelpText = true;

	private DistractionFreeSettings() {
	}
}
