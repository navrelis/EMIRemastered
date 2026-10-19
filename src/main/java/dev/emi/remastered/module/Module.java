package dev.emi.remastered.module;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Descriptor and lifecycle hooks of one EMI Remastered module.
 * <p>
 * A module lives in {@code dev.emi.remastered.<id>} and is registered with one line in {@link Modules}.
 * The module class itself holds the module switch ({@code @ConfigValue("modules.<id>") public static boolean enabled});
 * the settings class (porter-owned {@code <Prefix>Settings}) holds the module's settings
 * ({@code @ConfigValue("<id>.<key>")} on public static fields).
 * <p>
 * Subclass constructors, the switch and settings classes must not touch Minecraft classes during class
 * initialisation: they are loaded on dedicated servers, very early on the client (EMI's config static init) and in
 * unit tests.
 */
public abstract class Module {
	private final String id;
	private final String langNamespace;
	private final Class<?> settingsClass;
	private final boolean defaultEnabled;
	private final boolean needsServer;
	private final ApplyMode applyMode;
	private volatile Field switchField;

	/**
	 * @param id             module id, {@code [a-z0-9_]+}; also the config group of the module's settings
	 * @param langNamespace  asset namespace that holds the module's lang files (the old add-on's namespace)
	 * @param settingsClass  class with the module's settings (plain public static {@code @ConfigValue} fields)
	 * @param defaultEnabled default of the switch {@code modules.<id>}
	 * @param needsServer    whether the module has a server side (payloads, data sent from the server)
	 * @param applyMode      when a change of the module switch takes effect (shown in the switch tooltip)
	 */
	protected Module(String id, String langNamespace, Class<?> settingsClass, boolean defaultEnabled, boolean needsServer,
			ApplyMode applyMode) {
		this.id = id;
		this.langNamespace = langNamespace;
		this.settingsClass = settingsClass;
		this.defaultEnabled = defaultEnabled;
		this.needsServer = needsServer;
		this.applyMode = applyMode;
	}

	/**
	 * Common initialisation on BOTH sides (dedicated server, integrated server, client), always called, whatever the
	 * module switch says (a server has no EMI config). Register payload types and server events here.
	 */
	public void init() {
	}

	/**
	 * Client initialisation, called after EMI's config is loaded and only when the module switch is on at startup.
	 * Register client receivers, key binds and client events here. Keep client-only code in separate classes that
	 * are only reached from this method.
	 */
	public void initClient() {
	}

	public final String getId() {
		return id;
	}

	public final String getLangNamespace() {
		return langNamespace;
	}

	/** Class with the module's settings (group {@code <id>}). */
	public final Class<?> getSettingsClass() {
		return settingsClass;
	}

	/** Class with the module switch: the module class itself. */
	public final Class<?> getSwitchClass() {
		return getClass();
	}

	public final boolean isDefaultEnabled() {
		return defaultEnabled;
	}

	public final boolean needsServer() {
		return needsServer;
	}

	public final ApplyMode getApplyMode() {
		return applyMode;
	}

	/** Config key of the module switch, {@code modules.<id>}. */
	public final String getSwitchKey() {
		return "modules." + id;
	}

	/** Lang key of the module's display name, which is also the title of its config group. */
	public final String getNameKey() {
		return "config.emi.group." + id;
	}

	/**
	 * State of the module switch. Client only: on a dedicated server there is no EMI config and this returns the
	 * default. Server code must not depend on it.
	 */
	public final boolean isEnabled() {
		try {
			return getSwitchField().getBoolean(null);
		} catch (IllegalAccessException e) {
			return defaultEnabled;
		}
	}

	public final Field getSwitchField() {
		Field field = switchField;
		if (field == null) {
			field = findSwitchField(getSwitchClass(), getSwitchKey());
			if (field == null) {
				throw new IllegalStateException("Module '" + id + "': " + getSwitchClass().getName()
					+ " needs a field '@ConfigValue(\"" + getSwitchKey() + "\") public static boolean enabled'");
			}
			switchField = field;
		}
		return field;
	}

	static Field findSwitchField(Class<?> clazz, String key) {
		for (Field field : clazz.getDeclaredFields()) {
			ConfigValue annot = field.getAnnotation(ConfigValue.class);
			if (annot != null && annot.value().equals(key) && field.getType() == boolean.class
					&& Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
				return field;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "Module[" + id + "]";
	}

	/** When a change of the module switch takes effect. */
	public enum ApplyMode {
		/** Read live, e.g. by screen code. */
		IMMEDIATE("emi_remastered.config.applies.immediate"),
		/** The module's EMI plugin checks the switch; closing the config screen starts an EMI reload. */
		RELOAD("emi_remastered.config.applies.reload"),
		/** Hooks registered at startup (networking, key binds) depend on it. */
		RESTART("emi_remastered.config.applies.restart");

		public final String langKey;

		ApplyMode(String langKey) {
			this.langKey = langKey;
		}
	}
}
