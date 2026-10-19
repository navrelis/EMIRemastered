package dev.emi.remastered.professions;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

public class ProfessionsSettings {

	@Comment("Whether profession pages render the villager model next to the workstation.")
	@ConfigValue("professions.show-villager-model")
	public static boolean showVillagerModel = true;
}
