package dev.emi.remastered.plugin;

import java.util.regex.Pattern;

import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.api.IdentifiedEmiPlugin;

/** Resolves the id under which an EMI plugin is listed (log, plugin manager, {@code plugins.disabled}). */
public final class PluginIds {
	private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+");

	private PluginIds() {
	}

	/**
	 * @param plugin the plugin instance (an {@code EmiPlugin})
	 * @param modId  id of the mod whose fabric.mod.json declares the entrypoint
	 * @return the plugin's own id if it implements {@link IdentifiedEmiPlugin} with a valid id, else {@code modId}.
	 *         Only EMI itself may use the id {@code emi}.
	 */
	public static String resolve(Object plugin, String modId) {
		if (plugin instanceof IdentifiedEmiPlugin identified) {
			String id;
			try {
				id = identified.getPluginId();
			} catch (Throwable t) {
				EmiRemastered.LOGGER.warn("Plugin {} from mod {} threw in getPluginId, using the mod id", plugin.getClass().getName(), modId, t);
				return modId;
			}
			if (id == null || !VALID.matcher(id).matches()) {
				EmiRemastered.LOGGER.warn("Plugin {} from mod {} reports invalid plugin id '{}', using the mod id", plugin.getClass().getName(), modId, id);
				return modId;
			}
			if (id.equals(PluginFilter.CORE_ID) && !PluginFilter.CORE_ID.equals(modId)) {
				EmiRemastered.LOGGER.warn("Plugin {} from mod {} may not use the plugin id 'emi', using the mod id", plugin.getClass().getName(), modId);
				return modId;
			}
			return id;
		}
		return modId;
	}
}
