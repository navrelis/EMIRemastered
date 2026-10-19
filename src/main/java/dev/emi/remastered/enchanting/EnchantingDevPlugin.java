package dev.emi.remastered.enchanting;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

public class EnchantingDevPlugin implements EmiPlugin {
	@Override
	public void register(EmiRegistry registry) {
		EnchantingContent.register(registry);
	}
}
