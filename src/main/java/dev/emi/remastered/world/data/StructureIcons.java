package dev.emi.remastered.world.data;

import java.util.HashMap;
import java.util.Map;

/**
 * The item whose icon stands for a structure (a structure has no icon of its own). Vanilla structures have a
 * hand-picked icon, structures of other mods fall back to the structure block. Plain strings, no Minecraft.
 */
public final class StructureIcons {
	public static final String FALLBACK = "minecraft:structure_block";

	private static final Map<String, String> ICONS = new HashMap<>();
	/** Structure ids that start with the key share the icon (village_plains, ruined_portal_nether, ...). */
	private static final Map<String, String> PREFIXES = new HashMap<>();

	static {
		icon("pillager_outpost", "crossbow");
		icon("mineshaft", "rail");
		icon("mineshaft_mesa", "powered_rail");
		icon("mansion", "totem_of_undying");
		icon("jungle_pyramid", "mossy_cobblestone");
		icon("desert_pyramid", "chiseled_sandstone");
		icon("igloo", "snow_block");
		icon("swamp_hut", "cauldron");
		icon("stronghold", "ender_eye");
		icon("monument", "sea_lantern");
		icon("fortress", "nether_bricks");
		icon("nether_fossil", "bone_block");
		icon("end_city", "purpur_block");
		icon("buried_treasure", "heart_of_the_sea");
		icon("bastion_remnant", "gilded_blackstone");
		icon("ancient_city", "sculk_shrieker");
		icon("trail_ruins", "suspicious_gravel");
		icon("trial_chambers", "trial_spawner");
		prefix("village_", "bell");
		prefix("ruined_portal", "crying_obsidian");
		prefix("shipwreck", "oak_boat");
		prefix("ocean_ruin_", "prismarine_bricks");
	}

	private StructureIcons() {
	}

	private static void icon(String structure, String item) {
		ICONS.put("minecraft:" + structure, "minecraft:" + item);
	}

	private static void prefix(String prefix, String item) {
		PREFIXES.put("minecraft:" + prefix, "minecraft:" + item);
	}

	/** Item id of the icon for a structure id such as {@code minecraft:village_plains}. Never null. */
	public static String itemOf(String structureId) {
		String icon = ICONS.get(structureId);
		if (icon != null) {
			return icon;
		}
		for (Map.Entry<String, String> entry : PREFIXES.entrySet()) {
			if (structureId.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return FALLBACK;
	}
}
