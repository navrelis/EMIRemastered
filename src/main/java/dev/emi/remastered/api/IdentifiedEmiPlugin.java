package dev.emi.remastered.api;

/**
 * Optional interface for an {@link dev.emi.emi.api.EmiPlugin} that wants its own plugin id instead of the id of
 * the mod that provides it. EMI uses the plugin id in its log ("Reloaded plugin from &lt;id&gt; in N ms"), in the
 * plugin manager and in {@code plugins.disabled}.
 * <p>
 * EMI Remastered's built-in modules use it to keep the ids of the add-ons they replace
 * ({@code emi_loot}, {@code emi_ores}, {@code emiffect}, ...). Ids must match {@code [a-z0-9_.-]+}; an invalid id,
 * or {@code emi} claimed by another mod, falls back to the mod id.
 */
@FunctionalInterface
public interface IdentifiedEmiPlugin {

	String getPluginId();
}
