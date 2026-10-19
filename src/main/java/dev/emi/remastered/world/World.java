package dev.emi.remastered.world;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants of the world module. Safe to load on a dedicated server. */
public final class World {
	/** Resource namespace, plugin id and recipe namespace of the module. */
	public static final String MOD_ID = "emi_world";
	public static final Logger LOGGER = LoggerFactory.getLogger("EMI World");

	private World() {
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
