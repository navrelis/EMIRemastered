package dev.emi.remastered.loot;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.ConfigEnum;
import net.minecraft.text.Text;

/**
 * How the conditions of a loot pool are shown in a loot recipe.
 */
public enum LootConditionStyle implements ConfigEnum {
	/** Little icons next to each group of drops, the text is in their tooltip. */
	DEFAULT("default"),
	/** Plain text lines in the tooltip of each drop. More compact, fits in a narrower recipe. */
	TOOLTIP("tooltip"),
	/** Like tooltip, but the conditions are not shown at all. */
	PLAIN("plain"),
	;

	private final String name;

	LootConditionStyle(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Text getText() {
		return EmiPort.translatable("emi_loot.config.condition_style." + name);
	}
}
