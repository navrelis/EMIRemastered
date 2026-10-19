package dev.emi.remastered.devtour;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/**
 * Entry point of the dev-only screenshot tour. Active when the system property {@code emi.tour} is set (the
 * clientTour run config sets it to the tour name or a path to a tour json). See docs/DEVTOUR.md.
 */
public final class TourMod implements ClientModInitializer {
	private static final Logger LOG = LoggerFactory.getLogger("EmiDevTour");

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(DevWindow::tick);
		String tour = System.getProperty("emi.tour");
		if (tour == null || tour.isBlank()) {
			LOG.info("emi.tour is not set, tour disabled");
			return;
		}
		LogCapture logs = LogCapture.install();
		TourRunner runner = new TourRunner(MinecraftClient.getInstance(), logs, tour.trim(), FabricLoader.getInstance().getGameDir());
		ClientTickEvents.END_CLIENT_TICK.register(runner::onTick);
		// frame counter, used so screenshots are taken only after the GUI was drawn a few times
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			TourContext c = runner.context();
			if (c != null) {
				c.frameRendered();
			}
		});
	}
}
