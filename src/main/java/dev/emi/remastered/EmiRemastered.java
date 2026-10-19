package dev.emi.remastered;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants shared by the EMI Remastered framework and its modules.
 * <p>
 * Must stay free of Minecraft class references: it is loaded on dedicated servers and in unit tests.
 */
public final class EmiRemastered {
	/** Fabric mod id. EMI Remastered keeps EMI's id (decision D1). */
	public static final String MOD_ID = "emi";
	public static final String NAME = "EMI Remastered";
	/** Namespace of the framework's own assets (lang, textures). */
	public static final String NAMESPACE = "emi_remastered";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private EmiRemastered() {
	}
}
