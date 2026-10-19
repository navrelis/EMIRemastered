package dev.emi.remastered;

import dev.emi.remastered.module.Module;
import dev.emi.remastered.module.Modules;
import net.fabricmc.api.ModInitializer;

/**
 * Common entrypoint (fabric.mod.json {@code main}, after EMI's {@code EmiMainFabric}). Runs every module's
 * {@link Module#init()} on both sides, regardless of the client module switches: a dedicated server has no EMI
 * config, so payload types and server hooks are always registered.
 */
public class EmiRemasteredMain implements ModInitializer {

	@Override
	public void onInitialize() {
		for (Module module : Modules.all()) {
			try {
				module.init();
			} catch (Throwable t) {
				EmiRemastered.LOGGER.error("Module {} failed to initialize", module.getId(), t);
				throw t;
			}
		}
		EmiRemastered.LOGGER.info("Initialized {} modules: {}", Modules.all().size(),
			String.join(", ", Modules.all().stream().map(Module::getId).toList()));
	}
}
