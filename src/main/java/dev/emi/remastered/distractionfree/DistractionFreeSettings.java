package dev.emi.remastered.distractionfree;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.emi.input.EmiBind;
import net.minecraft.client.util.InputUtil;

/**
 * Settings of distraction-free mode (group {@code distraction_free}); the switch is {@link DistractionFreeModule#enabled}.
 * Lang keys in {@code assets/distraction_free_recipes/lang/}. All of them take effect immediately.
 */
public final class DistractionFreeSettings {

	@Comment("Show a message prompting you to search to view recipes.")
	@ConfigValue("distraction_free.show-help-text")
	public static boolean showHelpText = true;

	@Comment("Show a button to show the sidebar (and one to hide it again).")
	@ConfigValue("distraction_free.show-toggle-button")
	public static boolean showToggleButton = true;

	@Comment("Make the search bar background translucent.")
	@ConfigValue("distraction_free.lower-search-opacity")
	public static boolean lowerSearchOpacity = false;

	@Comment("Key that pins the sidebar open or closes it again. Unbound by default.")
	@ConfigValue("distraction_free.pin-bind")
	public static EmiBind pinBind = new EmiBind("config.emi.distraction_free.pin_bind", InputUtil.UNKNOWN_KEY.getCode());

	@Comment("Whether the sidebar is pinned open (always shown, even without a search). Saved with the config.")
	@ConfigValue("distraction_free.pinned")
	public static boolean pinned = false;

	@Comment("Duration of the fade in and out in milliseconds, 0 = instant (0 to 2000).")
	@ConfigValue("distraction_free.fade-duration")
	public static int fadeDuration = 150;

	@Comment("Distraction-free mode applies to the player inventory.")
	@ConfigValue("distraction_free.apply-to-inventory")
	public static boolean applyToInventory = true;

	@Comment("Distraction-free mode applies to the crafting table.")
	@ConfigValue("distraction_free.apply-to-crafting")
	public static boolean applyToCrafting = true;

	@Comment("Distraction-free mode applies to all other screens with the sidebar: chests, furnaces, villagers, ...")
	@ConfigValue("distraction_free.apply-to-containers")
	public static boolean applyToContainers = true;

	@Comment("EMI's recipe screen keeps its sidebars visible.")
	@ConfigValue("distraction_free.keep-on-recipe-screens")
	public static boolean keepOnRecipeScreens = true;

	@Comment("Screens that always keep the sidebar: comma separated class names (full or simple names).")
	@ConfigValue("distraction_free.excluded-screens")
	public static String excludedScreens = "";

	private DistractionFreeSettings() {
	}
}
