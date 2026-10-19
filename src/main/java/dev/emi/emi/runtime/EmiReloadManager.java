package dev.emi.emi.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.jemi.JemiPlugin;
import dev.emi.emi.mixinsupport.EmiMixinTransformation;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.EmiComparisonDefaults;
import dev.emi.emi.registry.EmiDragDropHandlers;
import dev.emi.emi.registry.EmiExclusionAreas;
import dev.emi.emi.registry.EmiIngredientSerializers;
import dev.emi.emi.registry.EmiInitRegistryImpl;
import dev.emi.emi.registry.EmiPluginContainer;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.registry.EmiRegistryImpl;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.registry.EmiStackProviders;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import dev.emi.remastered.plugin.PluginManager;
import dev.emi.remastered.runtime.ReloadGuard;
import dev.emi.remastered.runtime.SaveDebouncer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class EmiReloadManager {
	private static int loadedResourcesMask = 0;
	private static volatile boolean clear = false, restart = false;
	// 0 - empty, 1 - reloading, 2 - loaded, -1 - error
	private static volatile int status = 0;
	private static Thread thread;
	private static volatile int pass = 0; // EMI Remastered: reload pass counter for the delayed interrupt check
	public static volatile Text reloadStep = EmiPort.literal("");
	public static volatile long reloadWorry = Long.MAX_VALUE;

	static {
		EmiMixinTransformation.preach();
	}

	public static void reloadTags() {
		loadedResourcesMask |= 1;
		if (loadedResourcesMask == 3) {
			EmiLog.info("Recipes synchronized, reloading EMI");
			loadedResourcesMask = 0;
			reload();
		} else {
			EmiLog.info("Recipes synchronized, waiting for tags to reload EMI...");
		}
	}

	public static void reloadRecipes() {
		loadedResourcesMask |= 2;
		if (loadedResourcesMask == 3) {
			EmiLog.info("Tags synchronized, reloading EMI");
			loadedResourcesMask = 0;
			reload();
		} else {
			EmiLog.info("Tags synchronized, waiting for recipes to reload EMI...");
		}
	}

	public static void clear() {
		synchronized (EmiReloadManager.class) {
			loadedResourcesMask = 0;
			clear = true;
			status = 0;
			reloadWorry = Long.MAX_VALUE;
			if (thread != null && thread.isAlive()) {
				restart = true;
				// EMI Remastered: wake a plugin that blocks on server data that will never come (KB1, KB38); see interruptIfWaiting
				Thread worker = thread;
				int workerPass = pass;
				if (!interruptIfWaiting(worker, workerPass)) {
					SaveDebouncer.daemonTimer().schedule(() -> interruptIfWaiting(worker, workerPass), 1_000);
				}
			} else {
				thread = new Thread(new ReloadWorker(), "EMI Reload");
				thread.setDaemon(true);
				thread.start();
			}
		}
	}
	
	public static void reload() {
		synchronized (EmiReloadManager.class) {
			step(EmiPort.literal("Starting Reload"));
			status = 1;
			if (thread != null && thread.isAlive()) {
				restart = true;
			} else {
				clear = false;
				thread = new Thread(new ReloadWorker(), "EMI Reload");
				thread.setDaemon(true); // EMI Remastered: closing the game never waits for a reload (KB15)
				thread.start();
			}
		}
	}

	public static void step(Text text) {
		step(text, 5_000);
	}

	public static void step(Text text, long worry) {
		EmiLog.info(text.getString());
		reloadStep = text;
		reloadWorry = System.currentTimeMillis() + worry;
	}

	public static boolean isLoaded() {
		return status == 2 && (thread == null || !thread.isAlive());
	}

	public static int getStatus() {
		return status;
	}

	/** EMI Remastered: true while the running reload pass is obsolete (a clear or a new reload was requested). */
	public static boolean isRestartRequested() {
		return restart;
	}

	// EMI Remastered: only interrupt a worker parked in a wait (Future.get, sleep, park): an interrupt during NIO closes
	// the channel, and a closed zipfs channel breaks that mod's resources for the whole session (KB38)
	private static boolean interruptIfWaiting(Thread worker, int workerPass) {
		synchronized (EmiReloadManager.class) {
			if (worker != thread || !worker.isAlive() || workerPass != pass || !restart) {
				return true;
			}
			Thread.State state = worker.getState();
			if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
				worker.interrupt();
				return true;
			}
			return false;
		}
	}
	
	private static class ReloadWorker implements Runnable {

		// EMI Remastered: the current pass already logged why it gave up (KB1)
		private boolean abortLogged;

		@Override
		public void run() {
			int retries = 3;
			outer:
			do {
				Thread.interrupted(); // EMI Remastered: an interrupt only cancels the pass it was sent to
				abortLogged = false;
				pass++; // EMI Remastered: only this worker thread writes it
				try {
					if (!clear) {
						EmiLog.info("Starting EMI reload...");
					}
					long reloadStart = System.currentTimeMillis();
					restart = false;
					step(EmiPort.literal("Clearing data"));
					EmiRecipes.clear();
					EmiStackList.clear();
					EmiIngredientSerializers.clear();
					EmiExclusionAreas.clear();
					EmiDragDropHandlers.clear();
					EmiStackProviders.clear();
					EmiRecipeFiller.clear();
					EmiHidden.clear();
					EmiTags.ADAPTERS_BY_CLASS.map().clear();
					EmiTags.ADAPTERS_BY_REGISTRY.clear();
					EmiScreenBase.clearScreenBoundsProviders();
					if (clear) {
						clear = false;
						continue;
					}
					MinecraftClient client = MinecraftClient.getInstance();
					if (client.world == null) {
						EmiReloadLog.warn("World is null");
						break;
					} else if (client.world.getRecipeManager() == null) {
						EmiReloadLog.warn("Recipe Manager is null");
						break;
					}
					List<EmiPluginContainer> plugins = Lists.newArrayList();
					plugins.addAll(EmiAgnos.getPlugins().stream()
						.sorted((a, b) -> Integer.compare(entrypointPriority(a), entrypointPriority(b))).toList());
					
					if (EmiAgnos.isModLoaded("jei")) {
						plugins.add(new EmiPluginContainer(new JemiPlugin(), "jemi"));
					}
					// EMI Remastered: plugin manager - remember every plugin found, drop the disabled ones
					PluginManager.beginReload(plugins);
					EmiInitRegistry initRegistry = new EmiInitRegistryImpl();
					for (EmiPluginContainer container : plugins) {
						if (aborted()) { // EMI Remastered
							continue outer;
						}
						step(EmiPort.literal("Initializing plugin from " + container.id()), 5_000);
						long start = System.currentTimeMillis();
						try {
							container.plugin().initialize(initRegistry);
						} catch (Throwable e) {
							if (aborted()) { // EMI Remastered: no stack trace for a plugin that failed because of the abort
								continue outer;
							}
							PluginManager.recordError(container.id(), PluginManager.Phase.INITIALIZE, e); // EMI Remastered
							EmiReloadLog.warn("Exception initializing plugin provided by " + container.id(), e);
							if (restart) {
								continue outer;
							}
							continue;
						}
						PluginManager.recordInit(container.id(), System.currentTimeMillis() - start); // EMI Remastered
						EmiLog.info("Initialized plugin from " + container.id() + " in " + (System.currentTimeMillis() - start) + "ms");
					}
					if (aborted()) { // EMI Remastered
						continue;
					}
					EmiHidden.reload();

					step(EmiPort.literal("Processing tags"));
					EmiTags.reload();
					if (aborted()) { // EMI Remastered
						continue;
					}

					step(EmiPort.literal("Constructing index"));
					EmiComparisonDefaults.comparisons = new HashMap<>();
					EmiStackList.reload();
					if (aborted()) { // EMI Remastered: was restart only
						continue;
					}
					EmiRegistry registry = new EmiRegistryImpl();
					
					for (EmiPluginContainer container : plugins) {
						if (aborted()) { // EMI Remastered
							continue outer;
						}
						step(EmiPort.literal("Loading plugin from " + container.id()), 10_000);
						long start = System.currentTimeMillis();
						PluginManager.beforeRegister(); // EMI Remastered
						try {
							container.plugin().register(registry);
						} catch (Throwable e) {
							if (aborted()) { // EMI Remastered: no stack trace for a plugin that failed because of the abort
								continue outer;
							}
							// EMI Remastered: plugin manager statistics
							PluginManager.afterRegister(container.id(), System.currentTimeMillis() - start);
							PluginManager.recordError(container.id(), PluginManager.Phase.REGISTER, e);
							EmiReloadLog.warn("Exception loading plugin provided by " + container.id(), e);
							if (restart) {
								continue outer;
							}
							continue;
						}
						PluginManager.afterRegister(container.id(), System.currentTimeMillis() - start); // EMI Remastered
						EmiLog.info("Reloaded plugin from " + container.id() + " in " + (System.currentTimeMillis() - start) + "ms");
						if (restart) {
							continue outer;
						}
					}
					if (aborted()) { // EMI Remastered: was restart only
						continue;
					}
					step(EmiPort.literal("Baking index"));
					EmiStackList.bake();
					if (aborted()) { // EMI Remastered
						continue;
					}
					step(EmiPort.literal("Registering late recipes"), 10_000);
					Consumer<EmiRecipe> registerLateRecipe = registry::addRecipe;
					int lateIndex = 0; // EMI Remastered: attribute deferred recipes to the plugin that added them
					for (Consumer<Consumer<EmiRecipe>> consumer : EmiRecipes.lateRecipes) {
						if (aborted()) { // EMI Remastered
							continue outer;
						}
						PluginManager.beforeLate(); // EMI Remastered
						try {
							consumer.accept(registerLateRecipe);
							PluginManager.afterLate(lateIndex, null); // EMI Remastered
						} catch (Exception e) {
							if (aborted()) { // EMI Remastered
								continue outer;
							}
							PluginManager.afterLate(lateIndex, e); // EMI Remastered
							EmiReloadLog.warn("Exception loading late recipes for plugins:", e);
							if (restart) {
								continue outer;
							}
						}
						lateIndex++; // EMI Remastered
					}
					if (aborted()) { // EMI Remastered
						continue;
					}
					step(EmiPort.literal("Baking recipes"), 15_000);
					EmiRecipes.bake();
					BoM.reload();
					if (aborted()) { // EMI Remastered: never read emi.json without a world (KB3)
						continue;
					}
					EmiPersistentData.load();
					if (aborted()) { // EMI Remastered
						continue;
					}
					step(EmiPort.literal("Baking search"), 15_000);
					EmiSearch.bake();
					if (aborted()) { // EMI Remastered
						continue;
					}
					step(EmiPort.literal("Finishing up"));
					EmiScreenManager.search.update();
					EmiScreenManager.forceRecalculate();
					EmiReloadLog.bake();
					PluginManager.endReload(System.currentTimeMillis() - reloadStart); // EMI Remastered
					EmiLog.info("Reloaded EMI in " + (System.currentTimeMillis() - reloadStart) + "ms");
					status = 2;
				} catch (Throwable e) {
					if (aborted()) { // EMI Remastered: a failure caused by the abort is not retried
						continue;
					}
					EmiReloadLog.warn("Critical error occured during reload:", e);
					status = -1;
					if (retries-- > 0) {
						restart = true;
					}
				}
			} while (restart);
			// EMI Remastered: an aborted reload leaves EMI "not loaded" instead of "reloading" forever (KB1)
			Thread.interrupted();
			synchronized (EmiReloadManager.class) {
				if (status == 1 && !restart) {
					status = 0;
					reloadWorry = Long.MAX_VALUE;
				}
			}
			thread = null;
		}

		/** EMI Remastered: checks {@link ReloadGuard#shouldAbort()} and logs the first abort of a pass (KB1, KB38). */
		private boolean aborted() {
			if (!ReloadGuard.shouldAbort()) {
				return false;
			}
			Thread.interrupted(); // the pass ends here; no flag may survive into later channel I/O on this thread
			if (!abortLogged) {
				abortLogged = true;
				if (restart) {
					EmiLog.info("EMI reload cancelled, restarting");
				} else if (ReloadGuard.isWorldGone()) {
					EmiLog.info("World or connection is gone, EMI reload aborted at: " + reloadStep.getString());
				} else {
					EmiLog.info("EMI reload interrupted at: " + reloadStep.getString());
				}
			}
			return true;
		}

		private final static int entrypointPriority(EmiPluginContainer container) {
			return container.id().equals("emi") ? 0 : 1;
		}
	}
}
