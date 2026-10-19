package dev.emi.remastered.loot.devtmp;

import dev.emi.remastered.loot.LootContent;
import net.fabricmc.api.ModInitializer;

public class LootDevMain implements ModInitializer {
	@Override
	public void onInitialize() {
		LootContent.init();
	}
}
