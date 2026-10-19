package dev.emi.remastered;

import java.util.ArrayList;
import java.util.List;

import dev.emi.remastered.module.Module;
import dev.emi.remastered.module.Modules;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint (fabric.mod.json {@code client}, after EMI's {@code EmiClientFabric}, which loads emi.css).
 * Runs {@link Module#initClient()} for every module whose switch is on.
 */
public class EmiRemasteredClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		List<String> enabled = new ArrayList<>();
		List<String> disabled = new ArrayList<>();
		for (Module module : Modules.all()) {
			if (!module.isEnabled()) {
				disabled.add(module.getId());
				continue;
			}
			enabled.add(module.getId());
			try {
				module.initClient();
			} catch (Throwable t) {
				EmiRemastered.LOGGER.error("Module {} failed to initialize on the client", module.getId(), t);
				throw t;
			}
		}
		EmiRemastered.LOGGER.info("Client modules on: [{}], off: [{}]", String.join(", ", enabled), String.join(", ", disabled));
	}
}
