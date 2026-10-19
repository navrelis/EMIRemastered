package dev.emi.remastered.ores.dev;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.ores.OresContent;

public class OresDevPlugin implements EmiPlugin {
	@Override
	public void initialize(EmiInitRegistry registry) {
		OresContent.initialize(registry);
	}

	@Override
	public void register(EmiRegistry registry) {
		OresContent.register(registry);
	}
}
