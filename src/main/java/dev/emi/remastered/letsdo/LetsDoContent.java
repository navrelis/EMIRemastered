package dev.emi.remastered.letsdo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.runtime.EmiLog;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Port of Let's Do EMI Compat 2.0.3 (Pandaismyname1, MIT): EMI categories and recipes for the Let's Do mods.
 * <p>
 * Differences to the add-on: no Architectury at runtime (the add-on's own recipe types and data pack recipes are
 * generated in code), the class and field names of the current pack versions of the Let's Do mods, and the
 * categories that a mod's own EMI plugin already registers are skipped (decision D8):
 * <pre>
 * mod              own EMI plugin (categories)                                        added here
 * vinery 1.5.5     wine_fermentation, apple_press_mashing, apple_press_fermenting     emi_letsdo_compat:grape_mashing
 * bakery 2.1.10    baking_station, jam_cooking                                        emi_letsdo_compat:cake_cut
 * farm_and_charm   cooking_pot, stove, doughing, roaster, drying, mincing,            nothing
 *   1.1.28         cutting_board, assembly, log_stripping
 * candlelight      none (its recipes are Farm &amp; Charm recipe types)                    nothing
 * meadow 1.4.9     none                                                               fondue_fill, fondue_use, meadow:cheese,
 *                                                                                    meadow:cooking, meadow:woodcutting
 * herbalbrews      none                                                               herbalbrews:cauldron_brewing, herbalbrews:cooking
 * beachparty       none                                                               beachparty:mini_fridge_mixing, beachparty:tiki_bar_mixing
 * </pre>
 * Every sub-module lives in its own class that is loaded by name only if its mod is loaded, so a missing mod never
 * loads (or links) one of its classes. A sub-module whose mod has other class or field names fails with a
 * {@link LinkageError}, which skips only the affected category and logs one warning line per sub-module.
 */
public class LetsDoContent {
	/** Mods that bring their own EMI plugin, with the class that proves it (see the table above). */
	static final List<NativePlugin> NATIVE_PLUGINS = List.of(
			new NativePlugin("vinery", "net/satisfy/vinery/core/compat/emi/VineryEmiPlugin.class"),
			new NativePlugin("bakery", "net/satisfy/bakery/core/compat/emi/BakeryEMIPlugin.class"),
			new NativePlugin("farm_and_charm", "net/satisfy/farm_and_charm/core/compat/emi/FarmAndCharmEmiPlugin.class"));

	/** The sub-modules; the class names are strings on purpose, see the class comment. */
	static final List<Entry> SUB_MODULES = List.of(
			new Entry("vinery", "dev.emi.remastered.letsdo.vinery.VineryCompat"),
			new Entry("meadow", "dev.emi.remastered.letsdo.meadow.MeadowCompat"),
			new Entry("herbalbrews", "dev.emi.remastered.letsdo.herbalbrews.HerbalBrewsCompat"),
			new Entry("beachparty", "dev.emi.remastered.letsdo.beachparty.BeachpartyCompat"),
			new Entry("bakery", "dev.emi.remastered.letsdo.bakery.BakeryCompat"));

	/** Both sides, always. Nothing to do: no payloads, no server data. */
	public static void init() {
	}

	/** Client, only when the module is enabled at startup. Nothing to do. */
	public static void initClient() {
	}

	/** EMI registration, only while the module is enabled. */
	public static void register(EmiRegistry registry) {
		FabricLoader loader = FabricLoader.getInstance();
		logNativePlugins(loader);
		for (Entry entry : SUB_MODULES) {
			if (!loader.isModLoaded(entry.modId())) {
				continue;
			}
			LetsDoContext context = new LetsDoContext(registry, entry.modId());
			try {
				Class<?> type = Class.forName(entry.className(), true, LetsDoContent.class.getClassLoader());
				LetsDoSubModule subModule = (LetsDoSubModule) type.getDeclaredConstructor().newInstance();
				subModule.register(context);
			} catch (InvocationTargetException e) {
				context.fail("module", e.getCause() != null ? e.getCause() : e);
			} catch (LinkageError | RuntimeException | ReflectiveOperationException e) {
				context.fail("module", e);
			}
			context.report();
		}
	}

	private static void logNativePlugins(FabricLoader loader) {
		List<String> present = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		for (NativePlugin plugin : NATIVE_PLUGINS) {
			Optional<ModContainer> mod = loader.getModContainer(plugin.modId());
			if (mod.isEmpty()) {
				continue;
			}
			(mod.get().findPath(plugin.pluginClass()).isPresent() ? present : missing).add(plugin.modId());
		}
		if (!present.isEmpty()) {
			EmiLog.info("[Let's Do] " + String.join(", ", present) + " register their own EMI categories, those are not duplicated");
		}
		if (!missing.isEmpty()) {
			EmiLog.warn("[Let's Do] " + String.join(", ", missing) + " ship no EMI plugin in this version, their machine categories are not shown");
		}
	}

	record Entry(String modId, String className) {
	}

	record NativePlugin(String modId, String pluginClass) {
	}
}
