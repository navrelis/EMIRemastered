package dev.emi.remastered.world;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code world}: one EMI page per biome (climate, tags, dimensions, mobs, structures, ores) and one per
 * structure (dimensions, biomes, generation step, spacing, loot). The structure data is sent by the server.
 * EMI plugin: {@link WorldEmiPlugin} (plugin id {@code emi_world}); module code: {@link WorldContent}.
 * Settings: {@link WorldSettings} (group {@code world}); lang files in {@code assets/emi_world/lang/}.
 */
public final class WorldModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Show a page for every biome and every structure: climate, dimensions, mobs, ores, structures and loot.")
	@ConfigValue("modules.world")
	public static boolean enabled = true;

	public WorldModule() {
		super("world", "emi_world", WorldSettings.class, true, true, ApplyMode.RESTART);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says). */
	@Override
	public void init() {
		WorldContent.init();
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
		WorldContent.initClient();
	}
}
