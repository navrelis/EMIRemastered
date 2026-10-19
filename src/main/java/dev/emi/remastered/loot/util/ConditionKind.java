package dev.emi.remastered.loot.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The conditions of a drop that get a dedicated icon and a short explanation of how they scale with enchantment levels.
 * Classification works on the translation keys of the already parsed condition text, so no second parser is needed.
 * Does not depend on Minecraft.
 */
public enum ConditionKind {
	LOOTING("looting", "emi_loot.hint.looting"),
	FORTUNE("fortune", "emi_loot.hint.fortune"),
	SILK_TOUCH("silk_touch", "emi_loot.hint.silk_touch"),
	NO_SILK_TOUCH("silk_touch", "emi_loot.hint.no_silk_touch"),
	KILLED_BY_PLAYER("steve", "emi_loot.hint.killed_player"),
	ON_FIRE("fire", "emi_loot.hint.on_fire"),
	SMELTED("smelt", "emi_loot.hint.smelted");

	static final String ENCHANT_LOOTING = "enchantment.minecraft.looting";
	static final String ENCHANT_FORTUNE = "enchantment.minecraft.fortune";
	static final String ENCHANT_SILK_TOUCH = "enchantment.minecraft.silk_touch";

	private final String texture;
	private final String hintKey;

	ConditionKind(String texture, String hintKey) {
		this.texture = texture;
		this.hintKey = hintKey;
	}

	/** File name (without extension) of the 8x8 icon in {@code textures/gui}. */
	public String texture() {
		return texture;
	}

	public String hintKey() {
		return hintKey;
	}

	/**
	 * @param rootKey the translation key of the outermost condition text, i.e. the {@code emi_loot.*} key of the condition
	 * @param allKeys every translation key in the condition text, including the root and the nested arguments
	 */
	@Nullable
	public static ConditionKind classify(@Nullable String rootKey, Collection<String> allKeys) {
		if (rootKey == null) {
			return null;
		}
		switch (rootKey) {
			case "emi_loot.function.looting":
				return LOOTING;
			case "emi_loot.condition.chance_looting":
			case "emi_loot.function.bonus":
			case "emi_loot.condition.table_bonus":
				if (allKeys.contains(ENCHANT_FORTUNE)) {
					return FORTUNE;
				}
				if (allKeys.contains(ENCHANT_LOOTING)) {
					return LOOTING;
				}
				return null;
			case "emi_loot.condition.match_tool":
				return allKeys.contains(ENCHANT_SILK_TOUCH) ? SILK_TOUCH : null;
			case "emi_loot.condition.invert":
				return allKeys.contains(ENCHANT_SILK_TOUCH) ? NO_SILK_TOUCH : null;
			case "emi_loot.condition.killed_player":
				return KILLED_BY_PLAYER;
			case "emi_loot.function.smelt":
				return SMELTED;
			case "emi_loot.condition.entity_props":
				return allKeys.contains("emi_loot.entity_predicate.fire_true") ? ON_FIRE : null;
			default:
				return null;
		}
	}
}
