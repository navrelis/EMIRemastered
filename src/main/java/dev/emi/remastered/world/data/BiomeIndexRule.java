package dev.emi.remastered.world.data;

/**
 * Who puts the biome stacks into the EMI index: the ores module when it is on and its setting
 * {@code ores.add-biomes-to-index} is on, otherwise the world module when its biome pages and its setting
 * {@code world.biomes-in-index} are on. Never both, so no biome is in the index twice. Pure logic.
 */
public final class BiomeIndexRule {
	private BiomeIndexRule() {
	}

	public static boolean oresAdd(boolean oresModule, boolean oresSetting) {
		return oresModule && oresSetting;
	}

	public static boolean worldAdds(boolean biomePages, boolean worldSetting, boolean oresModule, boolean oresSetting) {
		return biomePages && worldSetting && !oresAdd(oresModule, oresSetting);
	}
}
