package dev.emi.remastered.tools;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.ConfigEnum;
import net.minecraft.text.Text;

/** Clipboard format of "copy recipe". */
public enum RecipeFormat implements ConfigEnum {
	TEXT("text"),
	JSON("json");

	public final String name;

	RecipeFormat(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Text getText() {
		return EmiPort.translatable("config.emi.tools.recipe_format." + name);
	}

	/** The format shift + the bind copies. */
	public RecipeFormat other() {
		return this == TEXT ? JSON : TEXT;
	}
}
