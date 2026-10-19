package dev.emi.remastered.ores.dev;

import dev.emi.remastered.ores.OresContent;
import net.fabricmc.api.ModInitializer;

public class OresDevMain implements ModInitializer {
	@Override
	public void onInitialize() {
		OresContent.init();
	}
}
