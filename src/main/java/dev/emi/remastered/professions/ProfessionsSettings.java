package dev.emi.remastered.professions;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

public class ProfessionsSettings {

	@Comment("Whether profession pages render the villager model next to the workstation.")
	@ConfigValue("professions.show-villager-model")
	public static boolean showVillagerModel = true;

	@Comment("Show the trades of every villager profession and of the wandering trader as recipes. Needs an EMI reload.")
	@ConfigValue("professions.show-trades")
	public static boolean showTrades = true;

	@Comment("How often every trade factory is rolled to find the variants of random trades such as enchanted books. Needs an EMI reload.")
	@ConfigValue("professions.trade-rolls")
	public static int tradeRolls = 8;
}
