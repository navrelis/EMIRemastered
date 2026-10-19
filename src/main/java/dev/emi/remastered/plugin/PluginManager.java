package dev.emi.remastered.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.EmiPluginContainer;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.config.RemasteredConfig;

/**
 * Client-side plugin manager: the persisted set of disabled plugin ids and per-plugin numbers of the last reload.
 * <p>
 * The reload hooks ({@code begin/record/...}) are called from {@code EmiReloadManager.ReloadWorker.run} on EMI's
 * reload thread; the screen reads the results on the render thread.
 */
public final class PluginManager {
	private static volatile List<PluginEntry> discovered = List.of();
	private static volatile Map<String, Stats> stats = new ConcurrentHashMap<>();
	private static volatile long lastReloadMs = -1;
	private static volatile boolean reloadedOnce = false;
	// Reload thread only
	private static final List<String> lateOwners = new ArrayList<>();
	private static int recipesBefore, stacksBefore, lateBefore;

	private PluginManager() {
	}

	// ---------------------------------------------------------------- disabled set

	public static Set<String> getDisabled() {
		return PluginFilter.parse(RemasteredConfig.disabledPlugins);
	}

	public static boolean isDisabled(String id) {
		return PluginFilter.isDisabled(id, getDisabled());
	}

	/** Changes the persisted set (in memory; {@code EmiConfig.writeConfig()} saves it). */
	public static void setDisabled(String id, boolean disabled) {
		RemasteredConfig.disabledPlugins = PluginFilter.withDisabled(RemasteredConfig.disabledPlugins, id, disabled);
	}

	public static void save() {
		EmiConfig.writeConfig();
	}

	// ---------------------------------------------------------------- reload hooks

	/**
	 * Remembers every plugin found and removes the disabled ones from {@code plugins} (in place). Called right after
	 * {@code EmiAgnos.getPlugins()} (and Sporran's additions) returned.
	 */
	public static void beginReload(List<EmiPluginContainer> plugins) {
		Set<String> disabled = getDisabled();
		List<PluginEntry> entries = new ArrayList<>();
		for (EmiPluginContainer container : plugins) {
			entries.add(new PluginEntry(container.id(), container.plugin()));
		}
		discovered = List.copyOf(entries);
		stats = new ConcurrentHashMap<>();
		lateOwners.clear();
		for (EmiPluginContainer removed : PluginFilter.removeDisabled(plugins, EmiPluginContainer::id, disabled)) {
			EmiLog.info("Skipping plugin from " + removed.id() + " (disabled in the plugin manager)");
		}
	}

	public static void recordInit(String id, long ms) {
		stats(id).initMs = ms;
	}

	public static void recordError(String id, Phase phase, Throwable t) {
		Stats s = stats(id);
		s.errorPhase = phase;
		s.error = String.valueOf(t);
	}

	public static void beforeRegister() {
		recipesBefore = EmiRecipes.getRecipeCount();
		stacksBefore = EmiStackList.stacks.size();
		lateBefore = EmiRecipes.lateRecipes.size();
	}

	/** After {@code register} returned or threw: time, added recipes/stacks and ownership of deferred recipes. */
	public static void afterRegister(String id, long ms) {
		Stats s = stats(id);
		s.registerMs = ms;
		s.recipes += EmiRecipes.getRecipeCount() - recipesBefore;
		s.stacks += EmiStackList.stacks.size() - stacksBefore;
		for (int i = lateBefore; i < EmiRecipes.lateRecipes.size(); i++) {
			lateOwners.add(id);
		}
	}

	public static void beforeLate() {
		recipesBefore = EmiRecipes.getRecipeCount();
	}

	/** After the {@code index}-th deferred recipe consumer ran; attributes its recipes to the plugin that added it. */
	public static void afterLate(int index, Throwable error) {
		if (index < 0 || index >= lateOwners.size()) {
			return;
		}
		String id = lateOwners.get(index);
		stats(id).recipes += EmiRecipes.getRecipeCount() - recipesBefore;
		if (error != null) {
			recordError(id, Phase.LATE, error);
		}
	}

	public static void endReload(long ms) {
		lastReloadMs = ms;
		reloadedOnce = true;
	}

	private static Stats stats(String id) {
		return stats.computeIfAbsent(id, k -> new Stats());
	}

	// ---------------------------------------------------------------- queries (render thread)

	/** Plugins found by the last reload, or (before any reload) by asking EMI's plugin discovery now. */
	public static List<PluginEntry> getPlugins() {
		List<PluginEntry> list = discovered;
		if (list.isEmpty()) {
			List<PluginEntry> found = new ArrayList<>();
			try {
				for (EmiPluginContainer container : EmiAgnos.getPlugins()) {
					found.add(new PluginEntry(container.id(), container.plugin()));
				}
			} catch (Throwable t) {
				EmiLog.error("Error listing EMI plugins", t);
			}
			found.sort((a, b) -> Boolean.compare(!a.id().equals(PluginFilter.CORE_ID), !b.id().equals(PluginFilter.CORE_ID)));
			list = found;
		}
		return list;
	}

	/** Numbers of the last reload, or null if the plugin did not run in it. */
	public static Stats getStats(String id) {
		return stats.get(id);
	}

	public static boolean hasReloaded() {
		return reloadedOnce;
	}

	public static long getLastReloadMs() {
		return lastReloadMs;
	}

	public record PluginEntry(String id, EmiPlugin plugin) {
	}

	public enum Phase {
		INITIALIZE("initialize"), REGISTER("register"), LATE("late");

		public final String key;

		Phase(String key) {
			this.key = key;
		}
	}

	/** Per plugin numbers of one reload. -1 = phase did not finish. */
	public static final class Stats {
		public volatile long initMs = -1;
		public volatile long registerMs = -1;
		public volatile int recipes;
		public volatile int stacks;
		public volatile Phase errorPhase;
		public volatile String error;
	}
}
