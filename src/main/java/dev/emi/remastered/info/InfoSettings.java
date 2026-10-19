package dev.emi.remastered.info;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.emi.input.EmiBind;

/**
 * Settings of the {@code info} module (group {@code info}); the switch is {@link InfoModule#enabled}.
 * Lang keys in {@code assets/emi_info/lang/}.
 * <p>
 * Client only: the {@link EmiBind} touches client input classes, and EMI's config (which initialises this class) is
 * only loaded on the client.
 */
public final class InfoSettings {

	@Comment("Opens the Item Info screen for the hovered stack.")
	@ConfigValue("info.open-info")
	public static EmiBind openInfo = new EmiBind("config.emi.info.open_info", GLFW.GLFW_KEY_I);

	@Comment("Adds a line with the Item Info key to item tooltips.")
	@ConfigValue("info.tooltip-hint")
	public static boolean tooltipHint = false;

	@Comment("Example stacks shown next to each recipe category.")
	@ConfigValue("info.example-stacks")
	public static int exampleStacks = 6;

	@Comment("Marks stacks that nothing in EMI's index produces.")
	@ConfigValue("info.show-no-source")
	public static boolean showNoSource = true;

	@Comment("Lists advancements whose icon is the item in the Item Info screen.")
	@ConfigValue("info.show-advancements")
	public static boolean showAdvancements = true;

	private InfoSettings() {
	}
}
