package dev.emi.remastered.loot.devtmp;

import dev.emi.remastered.loot.LootContent;
import net.fabricmc.api.ClientModInitializer;

public class LootDevClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LootContent.initClient();
	}
}
