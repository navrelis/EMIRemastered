package dev.emi.remastered.module;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.api.IdentifiedEmiPlugin;

/**
 * Base class of a module's EMI plugin (fabric.mod.json entrypoint {@code emi}). It reports the module's own plugin
 * id (the old add-on id) and does nothing while the module switch is off; the check runs on every EMI reload.
 * <p>
 * Plugins that the player switched off in the plugin manager are not called at all.
 */
public abstract class ModuleEmiPlugin implements EmiPlugin, IdentifiedEmiPlugin {
	private final Module module;
	private final String pluginId;

	protected ModuleEmiPlugin(Module module, String pluginId) {
		this.module = module;
		this.pluginId = pluginId;
	}

	public final Module getModule() {
		return module;
	}

	@Override
	public final String getPluginId() {
		return pluginId;
	}

	@Override
	public final void initialize(EmiInitRegistry registry) {
		if (!module.isEnabled()) {
			return;
		}
		initializeContent(registry);
	}

	@Override
	public final void register(EmiRegistry registry) {
		if (!module.isEnabled()) {
			return;
		}
		registerContent(registry);
	}

	/** {@link EmiPlugin#initialize}, only called while the module is enabled. */
	protected void initializeContent(EmiInitRegistry registry) {
	}

	/** {@link EmiPlugin#register}, only called while the module is enabled. */
	protected abstract void registerContent(EmiRegistry registry);
}
