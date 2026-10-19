/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ores {
	// Old add-on id; resources, payloads and recipe ids stay in this namespace
	public static final String MODID = "emi_ores";
	public static final Logger LOG = LoggerFactory.getLogger("EMI Ores");

	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}
