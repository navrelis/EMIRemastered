package dev.emi.remastered.effects;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Settings of the {@code effects} module (group {@code effects} in EMI's config screen).
 * Values are read live; the page layout is rebuilt on the next EMI reload.
 */
public final class EffectsSettings {

	@Comment("Add the status effects to EMI's item list (index), so that they can be searched and opened. Needs an EMI reload.")
	@ConfigValue("effects.show-in-index")
	public static boolean showInIndex = true;

	@Comment("Show the attribute modifiers an effect applies (for example +20% movement speed per level) on its page. Needs an EMI reload.")
	@ConfigValue("effects.show-attribute-modifiers")
	public static boolean showAttributeModifiers = true;

	@Comment("Show potion variants (splash, lingering, tipped arrow) next to the potion in the potion sources. Needs an EMI reload.")
	@ConfigValue("effects.show-potion-variants")
	public static boolean showPotionVariants = true;

	@Comment("Show hand-written sources (mobs, totem, conduit, ...) and cures from the effect_sources data table. Needs an EMI reload.")
	@ConfigValue("effects.show-hardcoded-sources")
	public static boolean showHardcodedSources = true;

	@Comment("Show the effects an item applies (potions, tipped arrows, suspicious stew, ...) and the sources of an effect on the Item Info page. Open pages update after an EMI reload.")
	@ConfigValue("effects.item-info")
	public static boolean itemInfo = true;

	private EffectsSettings() {
	}
}
