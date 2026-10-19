package dev.emi.remastered.mobs;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants of the mobs module. Safe to load on a dedicated server. */
public final class Mobs {
	/** Resource namespace, plugin id and recipe namespace of the module. */
	public static final String MOD_ID = "emi_mobs";
	public static final Logger LOGGER = LoggerFactory.getLogger("EMI Mobs");

	private Mobs() {
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
