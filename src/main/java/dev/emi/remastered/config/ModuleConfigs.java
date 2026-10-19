package dev.emi.remastered.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import dev.emi.emi.com.unascribed.qdcss.QDCSS;
import dev.emi.emi.config.EmiConfig.ConfigFilter;
import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.module.Module;
import dev.emi.remastered.module.Modules;

/**
 * Hooks EMI's config ({@code dev.emi.emi.config.EmiConfig} and {@code ConfigScreen}) call to include the module
 * config classes in {@code emi.css} and in the config screen, after EMI's own settings.
 * <p>
 * Field order: group {@code modules} (one switch per module, registry order; the switch is a static field of the
 * module class), group {@code plugins} (hidden in the screen), then each module's settings class (group
 * {@code <id>}) in registry order.
 */
public final class ModuleConfigs {
	public static final String MODULES_GROUP = "modules";
	public static final String PLUGINS_GROUP = "plugins";
	/** Groups written to emi.css but not shown in the config screen. */
	public static final Set<String> HIDDEN_GROUPS = Set.of(PLUGINS_GROUP);

	private static List<Field> fields;

	private ModuleConfigs() {
	}

	/** All config fields of the framework and the modules, in file/screen order. */
	public static synchronized List<Field> getFields() {
		if (fields == null) {
			Map<String, List<Class<?>>> classes = new LinkedHashMap<>();
			List<Field> all = new ArrayList<>();
			for (Module module : Modules.all()) {
				classes.put(module.getId(), List.of(module.getSwitchClass(), module.getSettingsClass()));
				all.addAll(ConfigCodec.configFields(module.getSwitchClass()));
				if (module.getSettingsClass() != module.getSwitchClass()) {
					all.addAll(ConfigCodec.configFields(module.getSettingsClass()));
				}
			}
			List<Field> framework = ConfigCodec.configFields(RemasteredConfig.class);
			all.addAll(framework);
			Set<String> reserved = new java.util.HashSet<>();
			framework.forEach(f -> reserved.add(ConfigCodec.key(f)));
			for (String problem : ConfigCodec.validate(classes, reserved)) {
				EmiRemastered.LOGGER.error("Module config: {}", problem);
			}
			for (Module module : Modules.all()) {
				try {
					// The descriptor owns the default of the switch.
					module.getSwitchField().setBoolean(null, module.isDefaultEnabled());
				} catch (Throwable t) {
					EmiRemastered.LOGGER.error("Module {} has no usable switch field", module.getId(), t);
				}
			}
			fields = List.copyOf(ConfigCodec.orderByGroup(all, List.of(MODULES_GROUP, PLUGINS_GROUP)));
		}
		return fields;
	}

	/** Fields shown in EMI's config screen (everything except {@link #HIDDEN_GROUPS}). */
	public static List<Field> getScreenFields() {
		List<Field> list = new ArrayList<>();
		for (Field field : getFields()) {
			if (!HIDDEN_GROUPS.contains(ConfigCodec.group(ConfigCodec.key(field)))) {
				list.add(field);
			}
		}
		return list;
	}

	/** Called by {@code EmiConfig.loadConfig(QDCSS)}; returns the consumed keys. */
	public static Set<String> load(QDCSS css, ConfigCodec.FieldReader reader) {
		return ConfigCodec.load(css, getFields(), reader, (msg, t) -> EmiRemastered.LOGGER.error(msg, t));
	}

	/** Called by {@code EmiConfig.getSavedConfig()}; appends the module groups. */
	public static void write(Map<String, List<String>> groups, ConfigCodec.FieldWriter writer) {
		ConfigCodec.write(groups, getFields(), writer, (msg, t) -> EmiRemastered.LOGGER.error(msg, t));
	}

	/** {@code @ConfigFilter} predicates declared in module config classes (key -> predicate). */
	public static Map<String, Predicate<?>> collectFilters() {
		Map<String, Predicate<?>> filters = new HashMap<>();
		for (Module module : Modules.all()) {
			for (Field field : module.getSettingsClass().getDeclaredFields()) {
				ConfigFilter annot = field.getAnnotation(ConfigFilter.class);
				if (annot != null) {
					try {
						field.setAccessible(true);
						filters.put(annot.value(), (Predicate<?>) field.get(null));
					} catch (Throwable t) {
						EmiRemastered.LOGGER.error("Bad @ConfigFilter {} in {}", annot.value(), module.getSettingsClass().getName(), t);
					}
				}
			}
		}
		return filters;
	}

	/** The module whose switch this field is, or null. */
	public static Module getModuleForSwitch(Field field) {
		String key = ConfigCodec.key(field);
		if (key == null || !key.startsWith(MODULES_GROUP + ".")) {
			return null;
		}
		return Modules.byId(ConfigCodec.name(key)).orElse(null);
	}

	/** Current state of every module switch (module id -> enabled). */
	public static Map<String, Boolean> snapshotSwitches() {
		Map<String, Boolean> map = new LinkedHashMap<>();
		for (Module module : Modules.all()) {
			map.put(module.getId(), module.isEnabled());
		}
		return map;
	}
}
