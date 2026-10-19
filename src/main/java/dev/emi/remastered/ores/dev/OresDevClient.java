package dev.emi.remastered.ores.dev;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.OresContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.ScreenshotRecorder;

public class OresDevClient implements ClientModInitializer {
	static int tick = 0;
	static int step = 0;
	static final String[] IDS = {"minecraft/ore_diamond", "minecraft/ore_iron_upper", "minecraft/ore_ancient_debris_large", "minecraft/amethyst_geode", "minecraft/ore_gold_extra"};

	@Override
	public void onInitializeClient() {
		// OresContent.initClient();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null || !EmiReloadManager.isLoaded()) return;
			tick++;
			if (tick < 100) return;
			int phase = (tick - 100) % 60;
			int idx = (tick - 100) / 60;
			if (idx >= IDS.length) {
				if (idx == IDS.length + 1) { Ores.LOG.info("DEV done"); client.scheduleStop(); }
				return;
			}
			if (phase == 0) {
				EmiRecipe r = EmiApi.getRecipeManager().getRecipe(Ores.id("/ore/" + IDS[idx]));
				if (r == null) r = EmiApi.getRecipeManager().getRecipe(Ores.id("/geode/" + IDS[idx]));
				Ores.LOG.info("DEV display {} -> {}", IDS[idx], r);
				if (r != null) EmiApi.displayRecipe(r);
			} else if (phase == 40) {
				ScreenshotRecorder.saveScreenshot(client.runDirectory, "ores-" + idx + ".png", client.getFramebuffer(), t -> Ores.LOG.info("DEV shot {}", t.getString()));
			}
		});
	}
}
