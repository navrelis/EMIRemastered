package dev.emi.remastered.loot.devtmp;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.loot.LootContent;

public class LootDevPlugin implements EmiPlugin {
	@Override
	public void initialize(EmiInitRegistry registry) {
		LootContent.initialize(registry);
	}

	@Override
	public void register(EmiRegistry registry) {
		LootContent.register(registry);
	}
}
